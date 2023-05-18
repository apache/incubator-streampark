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

import org.apache.streampark.common.enums.ClusterState;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.util.HttpClientUtils;
import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.metrics.flink.Overview;
import org.apache.streampark.console.core.metrics.yarn.YarnAppInfo;
import org.apache.streampark.console.core.service.FlinkClusterService;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hc.client5.http.config.RequestConfig;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.time.Duration;
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

  private Long lastWatcheringTime = 0L;

  // Track interval  every 30 seconds
  private static final Duration WATCHER_INTERVAL = Duration.ofSeconds(30);

  /** Watcher cluster lists */
  private static final Map<Long, FlinkCluster> WATCHER_CLUSTERS = new ConcurrentHashMap<>(0);

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
                .eq(FlinkCluster::getClusterState, ClusterState.RUNNING.getValue())
                .notIn(FlinkCluster::getExecutionMode, ExecutionMode.getKubernetesMode()));
    flinkClusters.forEach(cluster -> WATCHER_CLUSTERS.put(cluster.getId(), cluster));
  }

  /** flinkcluster persistent */
  @PreDestroy
  private void stop() {
    // TODO: flinkcluster persistent
  }

  @Scheduled(fixedDelay = 1000)
  private void start() {
    if (System.currentTimeMillis() - lastWatcheringTime >= WATCHER_INTERVAL.toMillis()) {
      lastWatcheringTime = System.currentTimeMillis();
      watcher();
    }
  }

  private void watcher() {
    for (Map.Entry<Long, FlinkCluster> entry : WATCHER_CLUSTERS.entrySet()) {
      EXECUTOR.execute(
          () -> {
            FlinkCluster flinkCluster = entry.getValue();
            Integer clusterExecutionMode = flinkCluster.getExecutionMode();
            if (!ExecutionMode.isKubernetesSessionMode(clusterExecutionMode)) {
              ClusterState state = getClusterState(flinkCluster);
              handleClusterState(flinkCluster, state);
            } else {
              // TODO: K8s Session status monitoring
            }
          });
    }
  }

  /**
   * cluster get state from flink or yarn api
   *
   * @param flinkCluster
   * @return
   */
  private ClusterState getClusterState(FlinkCluster flinkCluster) {
    ClusterState state = getClusterStateFromFlinkAPI(flinkCluster);
    if (ClusterState.isRunningState(state)) {
      return state;
    } else {
      return getClusterStateFromYarnAPI(flinkCluster);
    }
  }

  /**
   * cluster get state from flink rest api
   *
   * @param flinkCluster
   * @return
   */
  private ClusterState getClusterStateFromFlinkAPI(FlinkCluster flinkCluster) {
    final String address = flinkCluster.getAddress();
    final String jobManagerUrl = flinkCluster.getJobManagerUrl();
    if (StringUtils.isEmpty(address)) {
      return ClusterState.STOPPED;
    }
    final String flinkUrl =
        StringUtils.isEmpty(jobManagerUrl)
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
    return ClusterState.UNKNOWN;
  }

  /**
   * cluster get state from yarn rest api
   *
   * @param flinkCluster
   * @return
   */
  private ClusterState getClusterStateFromYarnAPI(FlinkCluster flinkCluster) {
    if (ExecutionMode.isRemoteMode(flinkCluster.getExecutionModeEnum())) {
      return ClusterState.STOPPED;
    }
    String clusterId = flinkCluster.getClusterId();
    if (StringUtils.isEmpty(clusterId)) {
      return ClusterState.STOPPED;
    }
    String yarnUrl = "ws/v1/cluster/apps/".concat(flinkCluster.getClusterId());
    try {
      String result = YarnUtils.restRequest(yarnUrl);
      if (null == result) {
        return ClusterState.UNKNOWN;
      }
      YarnAppInfo yarnAppInfo = JacksonUtils.read(result, YarnAppInfo.class);
      FinalApplicationStatus status =
          stringConvertFinalApplicationStatus(yarnAppInfo.getApp().getFinalStatus());
      if (status == null) {
        log.error(
            "cluster id:{} final application status convert failed, invalid string ",
            flinkCluster.getId());
        return ClusterState.UNKNOWN;
      }
      return finalApplicationStatusConvertClusterState(status);
    } catch (Exception e) {
      return ClusterState.LOST;
    }
  }

  /**
   * process cluster state
   *
   * @param flinkCluster
   * @param state
   */
  private void handleClusterState(FlinkCluster flinkCluster, ClusterState state) {
    LambdaUpdateWrapper<FlinkCluster> updateWrapper =
        new LambdaUpdateWrapper<FlinkCluster>()
            .eq(FlinkCluster::getId, flinkCluster.getId())
            .set(FlinkCluster::getClusterState, state.getValue());
    switch (state) {
      case STOPPED:
        {
          updateWrapper
              .set(FlinkCluster::getAddress, null)
              .set(FlinkCluster::getJobManagerUrl, null);
        }
        // fall through
      case LOST:
      case UNKNOWN:
        {
          removeFlinkCluster(flinkCluster);
          break;
        }
    }
    flinkClusterService.update(updateWrapper);
  }

  /**
   * Add a cluster to cache
   *
   * @param flinkCluster
   */
  public static void addFlinkCluster(FlinkCluster flinkCluster) {
    if (WATCHER_CLUSTERS.containsKey(flinkCluster.getId())) {
      return;
    }
    log.info("add the cluster with id:{} to watcher cluster cache", flinkCluster.getId());
    WATCHER_CLUSTERS.put(flinkCluster.getId(), flinkCluster);
  }

  /**
   * Remove a cluster from cache
   *
   * @param flinkCluster
   */
  public static void removeFlinkCluster(FlinkCluster flinkCluster) {
    if (!WATCHER_CLUSTERS.containsKey(flinkCluster.getId())) {
      return;
    }
    log.info("remove the cluster with id:{} from watcher cluster cache", flinkCluster.getId());
    WATCHER_CLUSTERS.remove(flinkCluster.getId());
  }

  /**
   * string conver final application status
   *
   * @param value
   * @return
   */
  private FinalApplicationStatus stringConvertFinalApplicationStatus(String value) {
    for (FinalApplicationStatus status : FinalApplicationStatus.values()) {
      if (status.name().equals(value)) {
        return status;
      }
    }
    return null;
  }

  /**
   * final application status convert cluster state
   *
   * @param status
   * @return
   */
  private ClusterState finalApplicationStatusConvertClusterState(FinalApplicationStatus status) {
    switch (status) {
      case UNDEFINED:
        return ClusterState.RUNNING;
      default:
        return ClusterState.STOPPED;
    }
  }
}
