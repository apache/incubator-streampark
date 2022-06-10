/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.task;

import static com.streamxhub.streamx.console.core.enums.FlinkAppState.Bridge.fromK8sFlinkJobState;
import static com.streamxhub.streamx.console.core.enums.FlinkAppState.Bridge.toK8sFlinkJobState;

import com.streamxhub.streamx.common.enums.ExecutionMode;
import com.streamxhub.streamx.common.util.ThreadUtils;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.enums.FlinkAppState;
import com.streamxhub.streamx.console.core.enums.OptionState;
import com.streamxhub.streamx.console.core.service.AlertService;
import com.streamxhub.streamx.console.core.service.ApplicationService;
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkJobState;
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkK8sExecuteMode;
import com.streamxhub.streamx.flink.kubernetes.event.FlinkClusterMetricChangeEvent;
import com.streamxhub.streamx.flink.kubernetes.event.FlinkJobStatusChangeEvent;
import com.streamxhub.streamx.flink.kubernetes.model.ClusterKey;
import com.streamxhub.streamx.flink.kubernetes.model.FlinkMetricCV;
import com.streamxhub.streamx.flink.kubernetes.model.JobStatusCV;
import com.streamxhub.streamx.flink.kubernetes.model.TrkId;
import com.streamxhub.streamx.flink.kubernetes.watcher.FlinkJobStatusWatcher;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.google.common.eventbus.Subscribe;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import scala.Enumeration;

/**
 * Event Listener for K8sFlinkTrkMonitor
 * @author Al-assad
 */
public class K8sFlinkChangeEventListener {

    private final ApplicationService applicationService;
    private final AlertService alertService;

    private final ExecutorService executor = new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors(),
        100,
        20L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1024),
        ThreadUtils.threadFactory("streamx-notify-executor"),
        new ThreadPoolExecutor.AbortPolicy()
    );

    public K8sFlinkChangeEventListener(ApplicationService applicationService, AlertService alertService) {
        this.applicationService = applicationService;
        this.alertService = alertService;
    }

    /**
     * Catch FlinkJobStatusChangeEvent then storage it persistently to db.
     * Actually update com.streamxhub.streamx.console.core.entity.Application records.
     */
    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void persistentK8sFlinkJobStatusChange(FlinkJobStatusChangeEvent event) {
        JobStatusCV jobStatus = event.jobStatus();
        TrkId trkId = event.trkId();
        ExecutionMode mode = FlinkK8sExecuteMode.toExecutionMode(trkId.executeMode());

        // get pre application record
        QueryWrapper<Application> query = new QueryWrapper<>();
        query.eq("execution_mode", mode.getMode())
            .eq("cluster_id", trkId.clusterId())
            .eq("k8s_namespace", trkId.namespace());
        if (ExecutionMode.KUBERNETES_NATIVE_SESSION.equals(mode)) {
            query.eq("job_id", jobStatus.jobId());
        }
        query.orderByDesc("create_time").last("limit 1");
        Application app = applicationService.getOne(query);
        if (app == null) {
            return;
        }

        // update application record
        app = updateApplicationWithJobStatusCV(app, jobStatus);
        // when a flink job status change event can be received, it means
        // that the operation command sent by streamx has been completed.
        app.setOptionState(OptionState.NONE.getValue());
        applicationService.saveOrUpdate(app);

        // email alerts when necessary
        FlinkAppState state = FlinkAppState.of(app.getState());
        if (FlinkAppState.FAILED.equals(state) || FlinkAppState.LOST.equals(state)
            || FlinkAppState.RESTARTING.equals(state) || FlinkAppState.FINISHED.equals(state)) {
            Application finalApp = app;
            executor.execute(() -> alertService.alert(finalApp, state));
        }
    }

    private Application updateApplicationWithJobStatusCV(Application app, JobStatusCV jobStatus) {
        // infer the final flink job state
        Enumeration.Value state = jobStatus.jobState();
        Enumeration.Value preState = toK8sFlinkJobState(FlinkAppState.of(app.getState()));
        state = FlinkJobStatusWatcher.inferFlinkJobStateFromPersist(state, preState);
        app.setState(fromK8sFlinkJobState(state).getValue());

        // update relevant fields of Application from JobStatusCV
        app.setJobId(jobStatus.jobId());
        app.setTotalTask(jobStatus.taskTotal());
        if (FlinkJobState.isEndState(state)) {
            app.setOptionState(OptionState.NONE.getValue());
        }

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
        app.setStartTime(new Date(startTime > 0 ? startTime : 0));
        app.setEndTime(endTime > 0 && endTime >= startTime ? new Date(endTime) : null);
        app.setDuration(duration > 0 ? duration : 0);

        return app;
    }


    /**
     * Catch FlinkClusterMetricChangeEvent then storage it persistently to db.
     * Actually update com.streamxhub.streamx.console.core.entity.Application records.
     */
    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void persistentK8sFlinkMetricsChange(FlinkClusterMetricChangeEvent event) {
        FlinkMetricCV metrics = event.metrics();
        ClusterKey clusterKey = event.clusterKey();
        ExecutionMode mode = FlinkK8sExecuteMode.toExecutionMode(clusterKey.executeMode());
        // discard session mode change
        if (ExecutionMode.KUBERNETES_NATIVE_SESSION.equals(mode)) {
            return;
        }

        UpdateWrapper<Application> update = new UpdateWrapper<>();
        update.set("jm_memory", metrics.totalJmMemory())
            .set("tm_memory", metrics.totalTmMemory())
            .set("total_tm", metrics.totalTm())
            .set("total_slot", metrics.totalSlot())
            .set("available_slot", metrics.availableSlot());
        update.eq("execution_mode", mode.getMode())
            .eq("cluster_id", clusterKey.clusterId())
            .eq("k8s_namespace", clusterKey.namespace())
            .eq("tracking", 1);

        applicationService.update(update);
    }

}
