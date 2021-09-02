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

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.google.common.eventbus.Subscribe;
import com.streamxhub.streamx.common.enums.ExecutionMode;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.enums.FlinkAppState;
import com.streamxhub.streamx.console.core.service.ApplicationService;
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkK8sExecuteMode;
import com.streamxhub.streamx.flink.kubernetes.event.FlinkJobStatusChangeEvent;
import com.streamxhub.streamx.flink.kubernetes.model.JobStatusCV;
import com.streamxhub.streamx.flink.kubernetes.model.TrkId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Listener for K8sFlinkTrkMonitor
 */
@Component
public class K8sFlinkChangeEventListener {

    @Autowired
    private ApplicationService applicationService;

    /**
     * catch FlinkJobStatusChangeEvent then storage it persistently to db.
     */
    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void persistentK8sFlinkJobStatusChangeEvent(FlinkJobStatusChangeEvent event) {
        JobStatusCV jobStatus = event.jobStatus();
        TrkId trkId = event.trkId();
        FlinkAppState state = FlinkAppState.Bridge.fromK8sFlinkJobState(jobStatus.jobState());
        ExecutionMode mode = FlinkK8sExecuteMode.toExecutionMode(trkId.executeMode());

        // update state of relevant application record
        UpdateWrapper<Application> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("state", state.getValue())
            .eq("execution_mode", mode.getMode())
            .eq("cluster_id", trkId.clusterId())
            .eq("k8s_namespace", trkId.namespace());
        applicationService.update(updateWrapper);
    }


}
