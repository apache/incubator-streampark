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

import org.apache.streampark.common.enums.FlinkExecutionMode;
import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.console.core.bean.AlertTemplate;
import org.apache.streampark.console.core.component.FlinkCheckpointProcessor;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.enums.OptionStateEnum;
import org.apache.streampark.console.core.metrics.flink.CheckPoints;
import org.apache.streampark.console.core.service.alert.AlertService;
import org.apache.streampark.console.core.service.application.ApplicationInfoService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;
import org.apache.streampark.flink.kubernetes.enums.FlinkJobStateEnum;
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sExecuteModeEnum;
import org.apache.streampark.flink.kubernetes.event.FlinkClusterMetricChangeEvent;
import org.apache.streampark.flink.kubernetes.event.FlinkJobCheckpointChangeEvent;
import org.apache.streampark.flink.kubernetes.event.FlinkJobStatusChangeEvent;
import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV;
import org.apache.streampark.flink.kubernetes.model.JobStatusCV;
import org.apache.streampark.flink.kubernetes.model.TrackId;
import org.apache.streampark.flink.kubernetes.watcher.FlinkJobStatusWatcher;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import scala.Enumeration;

import static org.apache.streampark.console.core.enums.FlinkAppStateEnum.Bridge.fromK8sFlinkJobState;
import static org.apache.streampark.console.core.enums.FlinkAppStateEnum.Bridge.toK8sFlinkJobState;

/**
 * Event Listener for K8sFlinkTrackMonitor。
 *
 * <p>Use FlinkK8sChangeListenerV2 listeners instead:
 *
 * @link org.apache.streampark.console.core.watcher.FlinkK8sChangeListenerV2
 */
@Deprecated
@Slf4j
@Component
public class FlinkK8sChangeEventListener {

  @Lazy @Autowired private ApplicationManageService applicationManageService;
  @Autowired private ApplicationInfoService applicationInfoService;

  @Lazy @Autowired private AlertService alertService;

  @Lazy @Autowired private FlinkCheckpointProcessor checkpointProcessor;

  private final ExecutorService executor =
      new ThreadPoolExecutor(
          Runtime.getRuntime().availableProcessors() * 5,
          Runtime.getRuntime().availableProcessors() * 10,
          20L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(1024),
          ThreadUtils.threadFactory("streampark-notify-executor"),
          new ThreadPoolExecutor.AbortPolicy());

  /**
   * Catch FlinkJobStatusChangeEvent then storage it persistently to db. Actually update
   * org.apache.streampark.console.core.entity.Application records.
   */
  @SuppressWarnings("UnstableApiUsage")
  @AllowConcurrentEvents
  @Subscribe
  public void subscribeJobStatusChange(FlinkJobStatusChangeEvent event) {
    JobStatusCV jobStatus = event.jobStatus();
    TrackId trackId = event.trackId();
    // get pre application record
    Application app = applicationManageService.getById(trackId.appId());
    if (app == null) {
      return;
    }
    // update application record
    setByJobStatusCV(app, jobStatus);
    applicationManageService.persistMetrics(app);

    // email alerts when necessary
    FlinkAppStateEnum state = app.getStateEnum();
    if (FlinkAppStateEnum.FAILED == state
        || FlinkAppStateEnum.LOST == state
        || FlinkAppStateEnum.RESTARTING == state
        || FlinkAppStateEnum.FINISHED == state) {
      executor.execute(
          () -> {
            if (app.getProbing()) {
              log.info("application with id {} is probing, don't send alert", app.getId());
              return;
            }
            alertService.alert(app.getAlertId(), AlertTemplate.of(app, state));
          });
    }
  }

  /**
   * Catch FlinkClusterMetricChangeEvent then storage it persistently to db. Actually update
   * org.apache.streampark.console.core.entity.Application records.
   */
  @SuppressWarnings("UnstableApiUsage")
  @AllowConcurrentEvents
  @Subscribe
  public void subscribeMetricsChange(FlinkClusterMetricChangeEvent event) {
    TrackId trackId = event.trackId();
    FlinkExecutionMode mode = FlinkK8sExecuteModeEnum.toExecutionMode(trackId.executeMode());
    // discard session mode change
    if (FlinkExecutionMode.KUBERNETES_NATIVE_SESSION == mode) {
      return;
    }

    Application app = applicationManageService.getById(trackId.appId());
    if (app == null) {
      return;
    }

    FlinkMetricCV metrics = event.metrics();
    app.setJmMemory(metrics.totalJmMemory());
    app.setTmMemory(metrics.totalTmMemory());
    app.setTotalTM(metrics.totalTm());
    app.setTotalSlot(metrics.totalSlot());
    app.setAvailableSlot(metrics.availableSlot());

    applicationManageService.persistMetrics(app);
  }

  @SuppressWarnings("UnstableApiUsage")
  @AllowConcurrentEvents
  @Subscribe
  public void subscribeCheckpointChange(FlinkJobCheckpointChangeEvent event) {
    CheckPoints.CheckPoint completed = new CheckPoints.CheckPoint();
    completed.setId(event.checkpoint().id());
    completed.setCheckpointType(event.checkpoint().checkpointType());
    completed.setExternalPath(event.checkpoint().externalPath());
    completed.setIsSavepoint(event.checkpoint().isSavepoint());
    completed.setStatus(event.checkpoint().status());
    completed.setTriggerTimestamp(event.checkpoint().triggerTimestamp());

    CheckPoints.Latest latest = new CheckPoints.Latest();
    latest.setCompleted(completed);
    CheckPoints checkPoint = new CheckPoints();
    checkPoint.setLatest(latest);

    checkpointProcessor.process(
        applicationManageService.getById(event.trackId().appId()), checkPoint);
  }

  private void setByJobStatusCV(Application app, JobStatusCV jobStatus) {
    // infer the final flink job state
    Enumeration.Value state =
        FlinkJobStatusWatcher.inferFlinkJobStateFromPersist(
            jobStatus.jobState(), toK8sFlinkJobState(app.getStateEnum()));

    // corrective start-time / end-time / duration
    long preStartTime = app.getStartTime() != null ? app.getStartTime().getTime() : 0;
    long startTime = Math.max(jobStatus.jobStartTime(), preStartTime);
    long preEndTime = app.getEndTime() != null ? app.getEndTime().getTime() : 0;
    long endTime = Math.max(jobStatus.jobEndTime(), preEndTime);
    long duration = jobStatus.duration();

    if (FlinkJobStateEnum.isEndState(state)) {
      if (endTime < startTime) {
        endTime = System.currentTimeMillis();
      }
      if (duration <= 0) {
        duration = endTime - startTime;
      }
    }

    app.setState(fromK8sFlinkJobState(state).getValue());
    app.setJobId(jobStatus.jobId());
    app.setTotalTask(jobStatus.taskTotal());

    app.setStartTime(new Date(startTime > 0 ? startTime : 0));
    app.setEndTime(endTime > 0 && endTime >= startTime ? new Date(endTime) : null);
    app.setDuration(duration > 0 ? duration : 0);
    // when a flink job status change event can be received, it means
    // that the operation command sent by streampark has been completed.
    app.setOptionState(OptionStateEnum.NONE.getValue());
  }
}
