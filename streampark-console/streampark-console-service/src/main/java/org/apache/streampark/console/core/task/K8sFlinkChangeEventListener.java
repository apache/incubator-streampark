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

import static org.apache.streampark.console.core.enums.FlinkAppState.Bridge.fromK8sFlinkJobState;
import static org.apache.streampark.console.core.enums.FlinkAppState.Bridge.toK8sFlinkJobState;

import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.FlinkAppState;
import org.apache.streampark.console.core.enums.OptionState;
import org.apache.streampark.console.core.metrics.flink.CheckPoints;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.alert.AlertService;
import org.apache.streampark.flink.kubernetes.IngressController;
import org.apache.streampark.flink.kubernetes.enums.FlinkJobState;
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sExecuteMode;
import org.apache.streampark.flink.kubernetes.event.FlinkClusterMetricChangeEvent;
import org.apache.streampark.flink.kubernetes.event.FlinkJobCheckpointChangeEvent;
import org.apache.streampark.flink.kubernetes.event.FlinkJobStatusChangeEvent;
import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV;
import org.apache.streampark.flink.kubernetes.model.JobStatusCV;
import org.apache.streampark.flink.kubernetes.model.TrackId;
import org.apache.streampark.flink.kubernetes.watcher.FlinkJobStatusWatcher;

import com.google.common.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import scala.Enumeration;

/**
 * Event Listener for K8sFlinkTrackMonitor
 *
 */
@Component
public class K8sFlinkChangeEventListener {

    @Lazy
    @Autowired
    private ApplicationService applicationService;

    @Lazy
    @Autowired
    private AlertService alertService;

    @Lazy
    @Autowired
    private CheckpointProcessor checkpointProcessor;

    private final ExecutorService executor = new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 5,
        Runtime.getRuntime().availableProcessors() * 10,
        20L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1024),
        ThreadUtils.threadFactory("streampark-notify-executor"),
        new ThreadPoolExecutor.AbortPolicy()
    );

    /**
     * Catch FlinkJobStatusChangeEvent then storage it persistently to db.
     * Actually update org.apache.streampark.console.core.entity.Application records.
     */
    @SuppressWarnings("UnstableApiUsage")
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
        Application newApp = getUpdateAppWithJobStatusCV(app, jobStatus);
        applicationService.updateTracking(newApp);

        // email alerts when necessary
        FlinkAppState state = FlinkAppState.of(newApp.getState());
        if (FlinkAppState.FAILED.equals(state) || FlinkAppState.LOST.equals(state)
            || FlinkAppState.RESTARTING.equals(state) || FlinkAppState.FINISHED.equals(state)) {
            IngressController.deleteIngress(app.getClusterId(), app.getK8sNamespace());
            executor.execute(() -> alertService.alert(app, state));
        }
    }

    /**
     * Catch FlinkClusterMetricChangeEvent then storage it persistently to db.
     * Actually update org.apache.streampark.console.core.entity.Application records.
     */
    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void subscribeMetricsChange(FlinkClusterMetricChangeEvent event) {
        FlinkMetricCV metrics = event.metrics();
        TrackId trackId = event.trackId();
        ExecutionMode mode = FlinkK8sExecuteMode.toExecutionMode(trackId.executeMode());
        // discard session mode change
        if (ExecutionMode.KUBERNETES_NATIVE_SESSION.equals(mode)) {
            return;
        }

        Application app =  applicationService.getById(trackId.appId());
        if (app == null) {
            return;
        }

        Application newApp = new Application();
        newApp.setId(trackId.appId());
        newApp.setJmMemory(metrics.totalJmMemory());
        newApp.setTmMemory(metrics.totalTmMemory());
        newApp.setTotalTM(metrics.totalTm());
        newApp.setTotalSlot(metrics.totalSlot());
        newApp.setAvailableSlot(metrics.availableSlot());

        applicationService.updateTracking(newApp);
    }

    @SuppressWarnings("UnstableApiUsage")
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

        checkpointProcessor.process(event.trackId().appId(), checkPoint);
    }

    private Application getUpdateAppWithJobStatusCV(Application app, JobStatusCV jobStatus) {
        Application newApp = new Application();
        newApp.setId(app.getId());
        newApp.setJobId(jobStatus.jobId());
        newApp.setTotalTask(jobStatus.taskTotal());

        // infer the final flink job state
        Enumeration.Value state = jobStatus.jobState();
        Enumeration.Value preState = toK8sFlinkJobState(FlinkAppState.of(app.getState()));
        state = FlinkJobStatusWatcher.inferFlinkJobStateFromPersist(state, preState);
        newApp.setState(fromK8sFlinkJobState(state).getValue());

        // corrective start-time / end-time / duration
        long preStartTime = app.getStartTime() != null ? app.getStartTime().getTime() : 0;
        long startTime = Math.max(jobStatus.jobStartTime(), preStartTime);
        long preEndTime = app.getEndTime() != null ? app.getEndTime().getTime() : 0;
        long endTime = Math.max(jobStatus.jobEndTime(), preEndTime);

        long duration = jobStatus.duration();
        if (FlinkJobState.isEndState(state)) {
            IngressController.deleteIngress(app.getJobName(), app.getK8sNamespace());
            newApp.setOptionState(OptionState.NONE.getValue());
            if (endTime < startTime) {
                endTime = System.currentTimeMillis();
            }
            if (duration <= 0) {
                duration = endTime - startTime;
            }
        }
        newApp.setStartTime(new Date(startTime > 0 ? startTime : 0));
        newApp.setEndTime(endTime > 0 && endTime >= startTime ? new Date(endTime) : null);
        newApp.setDuration(duration > 0 ? duration : 0);
        // when a flink job status change event can be received, it means
        // that the operation command sent by streampark has been completed.
        newApp.setOptionState(OptionState.NONE.getValue());
        return newApp;
    }

}
