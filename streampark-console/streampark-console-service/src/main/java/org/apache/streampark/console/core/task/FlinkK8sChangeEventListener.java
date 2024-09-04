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
import org.apache.streampark.console.core.enums.FlinkAppState;
import org.apache.streampark.console.core.enums.OptionState;
import org.apache.streampark.console.core.metrics.flink.CheckPoints;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.alert.AlertService;
import org.apache.streampark.flink.kubernetes.FlinkK8sWatcher;
import org.apache.streampark.flink.kubernetes.enums.FlinkJobState;
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sExecuteMode;
import org.apache.streampark.flink.kubernetes.event.FlinkClusterMetricChangeEvent;
import org.apache.streampark.flink.kubernetes.event.FlinkJobCheckpointChangeEvent;
import org.apache.streampark.flink.kubernetes.event.FlinkJobStatusChangeEvent;
import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV;
import org.apache.streampark.flink.kubernetes.model.JobStatusCV;
import org.apache.streampark.flink.kubernetes.model.TrackId;
import org.apache.streampark.flink.kubernetes.watcher.FlinkJobStatusWatcher;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import scala.Enumeration;

import static org.apache.streampark.console.core.enums.FlinkAppState.Bridge.fromK8sFlinkJobState;
import static org.apache.streampark.console.core.enums.FlinkAppState.Bridge.toK8sFlinkJobState;

/** Event Listener for K8sFlinkTrackMonitor */
@Component
@Slf4j
public class FlinkK8sChangeEventListener {

  @Lazy @Autowired private ApplicationService applicationService;

  @Lazy @Autowired private AlertService alertService;

  @Lazy @Autowired private CheckpointProcessor checkpointProcessor;

  private static final Cache<Long, Byte> SAVEPOINT_CACHE =
      Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();
  @Autowired private FlinkK8sWatcherWrapper flinkK8sWatcherWrapper;

  @Autowired private FlinkK8sWatcher registerFlinkK8sWatcher;

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
    Application app = applicationService.getById(trackId.appId());
    if (app == null) {
      return;
    }

    // update application record
    setByJobStatusCV(app, jobStatus);

    applicationService.persistMetrics(app);

    FlinkAppState state = app.getFlinkAppStateEnum();
    // email alerts when necessary
    if (FlinkAppState.FAILED.equals(state)
        || FlinkAppState.LOST.equals(state)
        || FlinkAppState.RESTARTING.equals(state)
        || FlinkAppState.FINISHED.equals(state)) {
      alertService.alert(app, state);
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
    ExecutionMode mode = FlinkK8sExecuteMode.toExecutionMode(trackId.executeMode());
    // discard session mode change
    if (ExecutionMode.KUBERNETES_NATIVE_SESSION.equals(mode)) {
      return;
    }

    if (applicationService.getById(trackId.appId()) == null) {
      return;
    }

    FlinkMetricCV metrics = event.metrics();
    Application app = new Application();
    app.setId(trackId.appId());
    app.setJmMemory(metrics.totalJmMemory());
    app.setTmMemory(metrics.totalTmMemory());
    app.setTotalTM(metrics.totalTm());
    app.setTotalSlot(metrics.totalSlot());
    app.setAvailableSlot(metrics.availableSlot());

    applicationService.persistMetrics(app);
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

    checkpointProcessor.process(applicationService.getById(event.trackId().appId()), checkPoint);
  }

  private void setByJobStatusCV(Application app, JobStatusCV jobStatus) {
    // infer the final flink job state
    Enumeration.Value state =
        FlinkJobStatusWatcher.inferFlinkJobStateFromPersist(
            jobStatus.jobState(), toK8sFlinkJobState(app.getFlinkAppStateEnum()));

    // corrective start-time / end-time / duration
    long preStartTime = app.getStartTime() != null ? app.getStartTime().getTime() : 0;
    long startTime = Math.max(jobStatus.jobStartTime(), preStartTime);
    long preEndTime = app.getEndTime() != null ? app.getEndTime().getTime() : 0;
    long endTime = Math.max(jobStatus.jobEndTime(), preEndTime);
    long duration = jobStatus.duration();

    if (FlinkJobState.isEndState(state)) {
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
    app.setOptionState(OptionState.NONE.getValue());
  }
}
