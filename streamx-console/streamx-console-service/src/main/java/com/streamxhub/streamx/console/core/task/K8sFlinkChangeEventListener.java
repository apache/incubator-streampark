/*
 * Copyright (c) 2021 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.streamxhub.streamx.console.core.task;

import static com.streamxhub.streamx.console.core.enums.FlinkAppState.Bridge.fromK8sFlinkJobState;
import static com.streamxhub.streamx.console.core.enums.FlinkAppState.Bridge.toK8sFlinkJobState;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.eventbus.Subscribe;
import com.streamxhub.streamx.common.enums.ExecutionMode;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.enums.FlinkAppState;
import com.streamxhub.streamx.console.core.enums.OptionState;
import com.streamxhub.streamx.console.core.service.ApplicationService;
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkJobState;
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkK8sExecuteMode;
import com.streamxhub.streamx.flink.kubernetes.event.FlinkJobStatusChangeEvent;
import com.streamxhub.streamx.flink.kubernetes.model.JobStatusCV;
import com.streamxhub.streamx.flink.kubernetes.model.TrkId;
import com.streamxhub.streamx.flink.kubernetes.watcher.FlinkJobStatusWatcher;
import org.apache.commons.lang3.StringUtils;
import scala.Enumeration;

/**
 * Listener for K8sFlinkTrkMonitor
 */
public class K8sFlinkChangeEventListener {

    private final ApplicationService applicationService;

    public K8sFlinkChangeEventListener(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * catch FlinkJobStatusChangeEvent then storage it persistently to db.
     */
    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void persistentK8sFlinkJobStatusChangeEvent(FlinkJobStatusChangeEvent event) {
        JobStatusCV jobStatus = event.jobStatus();
        TrkId trkId = event.trkId();
        Enumeration.Value state = jobStatus.jobState();
        ExecutionMode mode = FlinkK8sExecuteMode.toExecutionMode(trkId.executeMode());

        // get pre application record
        QueryWrapper<Application> query = new QueryWrapper<>();
        query.eq("execution_mode", mode.getMode())
            .eq("cluster_id", trkId.clusterId())
            .eq("k8s_namespace", trkId.namespace());
        if (ExecutionMode.KUBERNETES_NATIVE_SESSION.equals(mode)) {
            query.eq("job_id", jobStatus.jobId());
        }
        query.orderByDesc("create_time");
        Application app = applicationService.getOne(query);
        if (app == null) {
            return;
        }

        // infer the final flink job state
        Enumeration.Value preState = toK8sFlinkJobState(FlinkAppState.of(app.getState()));
        state = FlinkJobStatusWatcher.inferFlinkJobStateFromPersist(state, preState);
        app.setState(fromK8sFlinkJobState(state).getValue());
        // update option State
        if (FlinkJobState.isEndState(state)) {
            app.setOptionState(OptionState.NONE.getValue());
        }
        app.setJobId(event.jobStatus().jobId());

        // update application record
        applicationService.saveOrUpdate(app);
    }

}
