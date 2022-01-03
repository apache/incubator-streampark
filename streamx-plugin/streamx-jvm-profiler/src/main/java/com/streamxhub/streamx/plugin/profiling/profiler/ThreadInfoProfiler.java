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

import com.streamxhub.streamx.plugin.profiling.Profiler;
import com.streamxhub.streamx.plugin.profiling.Reporter;
import com.streamxhub.streamx.plugin.profiling.util.AgentLogger;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * ThreadInfoProfiler is used to Collects the Thread Related Metrics.
 *
 * @author benjobs
 */
public class ThreadInfoProfiler extends ProfilerBase implements Profiler {
    public static final String PROFILER_NAME = "ThreadInfo";
    public static final AgentLogger LOGGER =
        AgentLogger.getLogger(ThreadInfoProfiler.class.getName());
    private long interval = Constants.DEFAULT_METRIC_INTERVAL;

    private ThreadMXBean threadMXBean;
    /**
     * to keep track of Total Thread.
     */
    private long previousTotalStartedThreadCount = 0L;

    private Reporter reporter;

    public ThreadInfoProfiler(Reporter reporter) {
        setReporter(reporter);
        init();
    }

    private void init() {
        try {
            this.threadMXBean = ManagementFactory.getThreadMXBean();
        } catch (Throwable ex) {
            LOGGER.warn("Failed to get Thread MXBean", ex);
        }
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    @Override
    public long getInterval() {
        return interval;
    }

    @Override
    public void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void profile() {
        /** total Thread created so far since JVm Launch. */
        long totalStartedThreadCount = 0L;
        /** Number of thread which are currently active. */
        int liveThreadCount = 0;
        /** the peak live thread count since the Java virtual machine started or peak was reset */
        int peakThreadCount = 0;
        /** Number of new thread created since last time time the metrics was created. */
        long newThreadCount = 0;
        /** This is a Derived metrics from previous data point. */
        if (threadMXBean != null) {
            liveThreadCount = threadMXBean.getThreadCount();
            peakThreadCount = threadMXBean.getPeakThreadCount();
            totalStartedThreadCount = threadMXBean.getTotalStartedThreadCount();
            newThreadCount = totalStartedThreadCount - this.previousTotalStartedThreadCount;
            this.previousTotalStartedThreadCount = totalStartedThreadCount;
        }

        Map<String, Object> map = new HashMap<>();

        map.put("epochMillis", System.currentTimeMillis());
        map.put("name", getProcessName());
        map.put("host", getHostName());
        map.put("processUuid", getProcessUuid());
        map.put("appId", getAppId());

        if (getTag() != null) {
            map.put("tag", getTag());
        }

        if (getCluster() != null) {
            map.put("cluster", getCluster());
        }

        if (getRole() != null) {
            map.put("role", getRole());
        }

        map.put("totalStartedThreadCount", totalStartedThreadCount);
        map.put("newThreadCount", newThreadCount);
        map.put("liveThreadCount", liveThreadCount);
        map.put("peakThreadCount", peakThreadCount);

        if (reporter != null) {
            reporter.report(PROFILER_NAME, map);
        }
    }
}
