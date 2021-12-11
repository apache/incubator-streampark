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

package com.streamxhub.streamx.plugin.profiling.profiler;

import com.streamxhub.streamx.plugin.profiling.util.ProcFileUtils;
import com.streamxhub.streamx.plugin.profiling.util.ProcessUtils;
import com.streamxhub.streamx.plugin.profiling.util.SparkUtils;
import com.streamxhub.streamx.plugin.profiling.util.Utils;

import java.util.UUID;

/**
 * @author benjobs
 */
public class ProfilerBase {
    private String tag = null;
    private String cluster = null;
    private String hostName = null;
    private String processName = null;
    private String processUuid = UUID.randomUUID().toString();

    private String jobId = null;
    private String appId = null;

    private String role = null;

    public ProfilerBase() {
        setHostName(Utils.getLocalHostName());
        setProcessName(ProcessUtils.getCurrentProcessName());
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getProcessUuid() {
        return processUuid;
    }

    public void setProcessUuid(String processUuid) {
        this.processUuid = processUuid;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getAppId() {
        if (appId != null && !appId.isEmpty()) {
            return appId;
        }

        appId = SparkUtils.getSparkEnvAppId();
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRole() {
        if (role != null && !role.isEmpty()) {
            return role;
        }

        String cmdline = ProcFileUtils.getCmdline();
        role = SparkUtils.probeRole(cmdline);
        return role;
    }
}
