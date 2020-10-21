/*
 * Copyright (c) 2018 Uber Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uber.profiling.profilers;

import com.uber.profiling.util.NetworkUtils;
import com.uber.profiling.util.ProcFileUtils;
import com.uber.profiling.util.ProcessUtils;
import com.uber.profiling.util.SparkUtils;

import java.util.UUID;

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
        setHostName(NetworkUtils.getLocalHostName());
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
