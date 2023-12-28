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

package org.apache.streampark.console.core.watcher;

import org.apache.streampark.common.conf.CommonConfig;
import org.apache.streampark.common.conf.InternalConfigHolder;
import org.apache.streampark.common.enums.ClusterState;
import org.apache.streampark.common.enums.FlinkExecutionMode;
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

  private static final Cache<Long, ClusterState> FAILED_STATES =
      Caffeine.newBuilder().expireAfterWrite(WATCHER_INTERVAL).build();

  private boolean immediateWatch = false;

  /* TODO 3.5 Concurrent Processing */
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
                .eq(FlinkCluster::getClusterState, ClusterState.RUNNING.getState())
                // excluding flink clusters on kubernetes
                .notIn(FlinkCluster::getExecutionMode, FlinkExecutionMode.getKubernetesMode()));
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
                    ClusterState state = getClusterState(flinkCluster);
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

  private void alert(FlinkCluster cluster, ClusterState state) {
    if (cluster.getAlertId() != null) {
      cluster.setAllJobs(applicationInfoService.countByClusterId(cluster.getId()));
      cluster.setAffectedJobs(
          applicationInfoService.countAffectedByClusterId(
              cluster.getId(), InternalConfigHolder.get(CommonConfig.SPRING_PROFILES_ACTIVE())));
      cluster.setClusterState(state.getState());
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
  public ClusterState getClusterState(FlinkCluster flinkCluster) {
    ClusterState state = FAILED_STATES.getIfPresent(flinkCluster.getId());
    if (state != null) {
      return state;
    }
    state = httpClusterState(flinkCluster);
    if (ClusterState.isRunning(state)) {
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
  private ClusterState httpRemoteClusterState(FlinkCluster flinkCluster) {
    return getStateFromFlinkRestApi(flinkCluster);
  }

  /**
   * get yarn session cluster state
   *
   * @param flinkCluster
   * @return
   */
  private ClusterState httpYarnSessionClusterState(FlinkCluster flinkCluster) {
    ClusterState state = getStateFromFlinkRestApi(flinkCluster);
    if (ClusterState.LOST == state) {
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
  private ClusterState httpClusterState(FlinkCluster flinkCluster) {
    switch (flinkCluster.getFlinkExecutionModeEnum()) {
      case REMOTE:
        return httpRemoteClusterState(flinkCluster);
      case YARN_SESSION:
        return httpYarnSessionClusterState(flinkCluster);
      default:
        return ClusterState.UNKNOWN;
    }
  }

  /**
   * cluster get state from flink rest api
   *
   * @param flinkCluster
   * @return
   */
  private ClusterState getStateFromFlinkRestApi(FlinkCluster flinkCluster) {
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
      return ClusterState.RUNNING;
    } catch (Exception ignored) {
      log.error("cluster id:{} get state from flink api failed", flinkCluster.getId());
    }
    return ClusterState.LOST;
  }

  /**
   * cluster get state from yarn rest api
   *
   * @param flinkCluster
   * @return
   */
  private ClusterState getStateFromYarnRestApi(FlinkCluster flinkCluster) {
    String yarnUrl = "ws/v1/cluster/apps/".concat(flinkCluster.getClusterId());
    try {
      String result = YarnUtils.restRequest(yarnUrl);
      if (null == result) {
        return ClusterState.UNKNOWN;
      }
      YarnAppInfo yarnAppInfo = JacksonUtils.read(result, YarnAppInfo.class);
      YarnApplicationState status = HadoopUtils.toYarnState(yarnAppInfo.getApp().getState());
      if (status == null) {
        log.error(
            "cluster id:{} final application status convert failed, invalid string ",
            flinkCluster.getId());
        return ClusterState.UNKNOWN;
      }
      return yarnStateConvertClusterState(status);
    } catch (Exception e) {
      return ClusterState.LOST;
    }
  }

  /**
   * add flinkCluster to watching
   *
   * @param flinkCluster
   */
  public static void addWatching(FlinkCluster flinkCluster) {
    if (!FlinkExecutionMode.isKubernetesMode(flinkCluster.getFlinkExecutionModeEnum())
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
  private ClusterState yarnStateConvertClusterState(YarnApplicationState state) {
    return state == YarnApplicationState.FINISHED
        ? ClusterState.CANCELED
        : ClusterState.of(state.toString());
  }

  /**
   * Verify the cluster connection whether is valid.
   *
   * @return <code>false</code> if the connection of the cluster is invalid, <code>true</code> else.
   */
  public Boolean verifyClusterConnection(FlinkCluster flinkCluster) {
    ClusterState clusterStateEnum = httpClusterState(flinkCluster);
    return ClusterState.isRunning(clusterStateEnum);
  }
}
