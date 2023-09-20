/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.console.core.task;

import org.apache.streampark.common.conf.CommonConfig;
import org.apache.streampark.common.conf.InternalConfigHolder;
import org.apache.streampark.common.enums.ClusterStateEnum;
import org.apache.streampark.common.enums.ExecutionModeEnum;
import org.apache.streampark.common.util.HadoopUtils;
import org.apache.streampark.common.util.HttpClientUtils;
import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.bean.AlertTemplate;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.metrics.flink.Overview;
import org.apache.streampark.console.core.metrics.yarn.YarnAppInfo;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.alert.AlertService;
import org.apache.streampark.console.core.service.application.ApplicationInfoService;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hc.client5.http.config.RequestConfig;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** This implementation is currently used for tracing Cluster on yarn,remote,K8s mode */
@Slf4j
@Component
public class FlinkClusterWatcher {

  @Autowired private FlinkClusterService flinkClusterService;

  @Autowired private AlertService alertService;

  @Autowired private ApplicationInfoService applicationInfoService;

  private Long lastWatchTime = 0L;

  // Track interval  every 30 seconds
  private static final Duration WATCHER_INTERVAL = Duration.ofSeconds(30);

  /** Watcher cluster lists */
  private static final Map<Long, FlinkCluster> WATCHER_CLUSTERS = new ConcurrentHashMap<>(8);

  private static final Cache<Long, ClusterStateEnum> FAILED_STATES =
      Caffeine.newBuilder().expireAfterWrite(WATCHER_INTERVAL).build();

  private boolean immediateWatch = false;

  /** Thread pool for processing status monitoring for each cluster */
  private static final ExecutorService EXECUTOR =
      new ThreadPoolExecutor(
          Runtime.getRuntime().availableProcessors() * 5,
          Runtime.getRuntime().availableProcessors() * 10,
          60L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(1024),
          ThreadUtils.threadFactory("flink-cluster-watching-executor"));

  /** Initialize cluster cache */
  @PostConstruct
  private void init() {
    WATCHER_CLUSTERS.clear();
    List<FlinkCluster> flinkClusters =
        flinkClusterService.list(
            new LambdaQueryWrapper<FlinkCluster>()
                .eq(FlinkCluster::getClusterState, ClusterStateEnum.RUNNING.getValue())
                // excluding flink clusters on kubernetes
                .notIn(FlinkCluster::getExecutionMode, ExecutionModeEnum.getKubernetesMode()));
    flinkClusters.forEach(cluster -> WATCHER_CLUSTERS.put(cluster.getId(), cluster));
  }

  @Scheduled(fixedDelay = 1000)
  private void start() {
    Long timeMillis = System.currentTimeMillis();
    if (immediateWatch || timeMillis - lastWatchTime >= WATCHER_INTERVAL.toMillis()) {
      lastWatchTime = timeMillis;
      immediateWatch = false;
      WATCHER_CLUSTERS.forEach(
          (aLong, flinkCluster) ->
              EXECUTOR.execute(
                  () -> {
                    ClusterStateEnum state = getClusterState(flinkCluster);
                    switch (state) {
                      case FAILED:
                      case LOST:
                      case UNKNOWN:
                      case KILLED:
                        flinkClusterService.updateClusterState(flinkCluster.getId(), state);
                        unWatching(flinkCluster);
                        alert(flinkCluster, state);
                        break;
                      default:
                        break;
                    }
                  }));
    }
  }

  private void alert(FlinkCluster cluster, ClusterStateEnum state) {
    if (cluster.getAlertId() != null) {
      cluster.setAllJobs(applicationInfoService.countByClusterId(cluster.getId()));
      cluster.setAffectedJobs(
          applicationInfoService.countAffectedByClusterId(
              cluster.getId(), InternalConfigHolder.get(CommonConfig.SPRING_PROFILES_ACTIVE())));
      cluster.setClusterState(state.getValue());
      cluster.setEndTime(new Date());
      alertService.alert(cluster.getAlertId(), AlertTemplate.of(cluster, state));
    }
  }

  /**
   * Retrieves the state of a cluster from the Flink or YARN API.
   *
   * @param flinkCluster The FlinkCluster object representing the cluster.
   * @return The ClusterState object representing the state of the cluster.
   */
  public ClusterStateEnum getClusterState(FlinkCluster flinkCluster) {
    ClusterStateEnum state = FAILED_STATES.getIfPresent(flinkCluster.getId());
    if (state != null) {
      return state;
    }
    state = httpClusterState(flinkCluster);
    if (ClusterStateEnum.isRunning(state)) {
      FAILED_STATES.invalidate(flinkCluster.getId());
    } else {
      immediateWatch = true;
      FAILED_STATES.put(flinkCluster.getId(), state);
    }
    return state;
  }

  /**
   * Retrieves the state of a cluster from the Flink or YARN API using the remote HTTP endpoint.
   *
   * @param flinkCluster The FlinkCluster object representing the cluster.
   * @return The ClusterState object representing the state of the cluster.
   */
  private ClusterStateEnum httpRemoteClusterState(FlinkCluster flinkCluster) {
    return getStateFromFlinkRestApi(flinkCluster);
  }

  /**
   * get yarn session cluster state
   *
   * @param flinkCluster
   * @return
   */
  private ClusterStateEnum httpYarnSessionClusterState(FlinkCluster flinkCluster) {
    ClusterStateEnum state = getStateFromFlinkRestApi(flinkCluster);
    if (ClusterStateEnum.LOST == state) {
      return getStateFromYarnRestApi(flinkCluster);
    }
    return state;
  }

  /**
   * get flink cluster state
   *
   * @param flinkCluster
   * @return
   */
  private ClusterStateEnum httpClusterState(FlinkCluster flinkCluster) {
    switch (flinkCluster.getExecutionModeEnum()) {
      case REMOTE:
        return httpRemoteClusterState(flinkCluster);
      case YARN_SESSION:
        return httpYarnSessionClusterState(flinkCluster);
      default:
        return ClusterStateEnum.UNKNOWN;
    }
  }

  /**
   * cluster get state from flink rest api
   *
   * @param flinkCluster
   * @return
   */
  private ClusterStateEnum getStateFromFlinkRestApi(FlinkCluster flinkCluster) {
    String address = flinkCluster.getAddress();
    String jobManagerUrl = flinkCluster.getJobManagerUrl();
    String flinkUrl =
        StringUtils.isBlank(jobManagerUrl)
            ? address.concat("/overview")
            : jobManagerUrl.concat("/overview");
    try {
      String res =
          HttpClientUtils.httpGetRequest(
              flinkUrl,
              RequestConfig.custom().setConnectTimeout(5000, TimeUnit.MILLISECONDS).build());
      JacksonUtils.read(res, Overview.class);
      return ClusterStateEnum.RUNNING;
    } catch (Exception ignored) {
      log.error("cluster id:{} get state from flink api failed", flinkCluster.getId());
    }
    return ClusterStateEnum.LOST;
  }

  /**
   * cluster get state from yarn rest api
   *
   * @param flinkCluster
   * @return
   */
  private ClusterStateEnum getStateFromYarnRestApi(FlinkCluster flinkCluster) {
    String yarnUrl = "ws/v1/cluster/apps/".concat(flinkCluster.getClusterId());
    try {
      String result = YarnUtils.restRequest(yarnUrl);
      if (null == result) {
        return ClusterStateEnum.UNKNOWN;
      }
      YarnAppInfo yarnAppInfo = JacksonUtils.read(result, YarnAppInfo.class);
      YarnApplicationState status = HadoopUtils.toYarnState(yarnAppInfo.getApp().getState());
      if (status == null) {
        log.error(
            "cluster id:{} final application status convert failed, invalid string ",
            flinkCluster.getId());
        return ClusterStateEnum.UNKNOWN;
      }
      return yarnStateConvertClusterState(status);
    } catch (Exception e) {
      return ClusterStateEnum.LOST;
    }
  }

  /**
   * add flinkCluster to watching
   *
   * @param flinkCluster
   */
  public static void addWatching(FlinkCluster flinkCluster) {
    if (!ExecutionModeEnum.isKubernetesMode(flinkCluster.getExecutionModeEnum())
        && !WATCHER_CLUSTERS.containsKey(flinkCluster.getId())) {
      log.info("add the cluster with id:{} to watcher cluster cache", flinkCluster.getId());
      WATCHER_CLUSTERS.put(flinkCluster.getId(), flinkCluster);
    }
  }

  /** @param flinkCluster */
  public static void unWatching(FlinkCluster flinkCluster) {
    if (WATCHER_CLUSTERS.containsKey(flinkCluster.getId())) {
      log.info("remove the cluster with id:{} from watcher cluster cache", flinkCluster.getId());
      WATCHER_CLUSTERS.remove(flinkCluster.getId());
    }
  }

  /**
   * yarn application state convert cluster state
   *
   * @param state
   * @return
   */
  private ClusterStateEnum yarnStateConvertClusterState(YarnApplicationState state) {
    return state == YarnApplicationState.FINISHED
        ? ClusterStateEnum.CANCELED
        : ClusterStateEnum.of(state.toString());
  }

  /**
   * Verify the cluster connection whether is valid.
   *
   * @return <code>false</code> if the connection of the cluster is invalid, <code>true</code> else.
   */
  public Boolean verifyClusterConnection(FlinkCluster flinkCluster) {
    ClusterStateEnum clusterStateEnum = httpClusterState(flinkCluster);
    return ClusterStateEnum.isRunning(clusterStateEnum);
  }
}
