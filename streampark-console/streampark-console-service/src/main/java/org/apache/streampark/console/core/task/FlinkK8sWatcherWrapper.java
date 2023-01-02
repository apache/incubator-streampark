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

import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.flink.kubernetes.FlinkK8sWatcher;
import org.apache.streampark.flink.kubernetes.FlinkK8sWatcherFactory;
import org.apache.streampark.flink.kubernetes.FlinkTrackConfig;
import org.apache.streampark.flink.kubernetes.enums.FlinkJobState;
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sExecuteMode;
import org.apache.streampark.flink.kubernetes.model.TrackId;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Nonnull;

import java.util.List;
import java.util.stream.Collectors;

import scala.Enumeration;

import static org.apache.streampark.console.core.enums.FlinkAppState.Bridge.toK8sFlinkJobState;

/**
 * Flink K8s Tracking Monitor Wrapper.
 *
 * <p>todo Notes Currently Tracking Monitor of Flink on K8s and on YARN are independent of each
 * other, this is because the tracking behavior of Flink on K8s is quite difference. Maybe we need
 * to refactor to a unified Flink Tracking Monitor in the future, both tracking-on-k8s and
 * tracking-on-yarn will exist as plugins for this unified implementation.
 *
 * <p>
 */
@Configuration
public class FlinkK8sWatcherWrapper {

  @Lazy @Autowired private FlinkK8sChangeEventListener flinkK8sChangeEventListener;

  @Lazy @Autowired private ApplicationService applicationService;

  /** Register FlinkTrackMonitor bean for tracking flink job on kubernetes. */
  @Bean(destroyMethod = "close")
  public FlinkK8sWatcher registerFlinkK8sWatcher() {
    // lazy start tracking monitor
    FlinkK8sWatcher flinkK8sWatcher =
        FlinkK8sWatcherFactory.createInstance(FlinkTrackConfig.fromConfigHub(), true);
    initFlinkK8sWatcher(flinkK8sWatcher);

    /* Dev scaffold: watch flink k8s tracking cache,
       see org.apache.streampark.flink.kubernetes.helper.KubernetesWatcherHelper for items.
       Example:
           KubernetesWatcherHelper.watchTrackIdsCache(flinkK8sWatcher);
           KubernetesWatcherHelper.watchJobStatusCache(flinkK8sWatcher);
           KubernetesWatcherHelper.watchAggClusterMetricsCache(flinkK8sWatcher);
           KubernetesWatcherHelper.watchClusterMetricsCache(flinkK8sWatcher);
    */
    return flinkK8sWatcher;
  }

  private void initFlinkK8sWatcher(@Nonnull FlinkK8sWatcher trackMonitor) {
    // register change event listener
    trackMonitor.registerListener(flinkK8sChangeEventListener);
    // recovery tracking list
    List<TrackId> k8sApp = getK8sWatchingApps();
    k8sApp.forEach(trackMonitor::doWatching);
  }

  /** get flink-k8s job tracking application from db. */
  private List<TrackId> getK8sWatchingApps() {
    // query k8s execution mode application from db
    final LambdaQueryWrapper<Application> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper
        .eq(Application::getTracking, 1)
        .in(Application::getExecutionMode, ExecutionMode.getKubernetesMode());

    List<Application> k8sApplication = applicationService.list(queryWrapper);
    if (CollectionUtils.isEmpty(k8sApplication)) {
      return Lists.newArrayList();
    }
    // correct corrupted data
    List<Application> correctApps =
        k8sApplication.stream()
            .filter(app -> !Bridge.toTrackId(app).isLegal())
            .collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(correctApps)) {
      applicationService.saveOrUpdateBatch(correctApps);
    }
    // filter out the application that should be tracking
    return k8sApplication.stream()
        .filter(app -> !FlinkJobState.isEndState(toK8sFlinkJobState(app.getFlinkAppStateEnum())))
        .map(Bridge::toTrackId)
        .collect(Collectors.toList());
  }

  /** Type converter bridge */
  public static class Bridge {

    // covert Application to TrackId
    public static TrackId toTrackId(@Nonnull Application app) {
      Enumeration.Value mode = FlinkK8sExecuteMode.of(app.getExecutionModeEnum());
      if (FlinkK8sExecuteMode.APPLICATION().equals(mode)) {
        return TrackId.onApplication(
            app.getK8sNamespace(), app.getClusterId(), app.getId(), app.getJobId());
      } else if (FlinkK8sExecuteMode.SESSION().equals(mode)) {
        return TrackId.onSession(
            app.getK8sNamespace(), app.getClusterId(), app.getId(), app.getJobId());
      } else {
        throw new IllegalArgumentException(
            "Illegal K8sExecuteMode, mode=" + app.getExecutionMode());
      }
    }
  }

  /** Determine if application it is flink-on-kubernetes mode. */
  public static boolean isKubernetesApp(Application application) {
    if (application == null) {
      return false;
    }
    return ExecutionMode.isKubernetesMode(application.getExecutionMode());
  }
}
