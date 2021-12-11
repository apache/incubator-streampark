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

package com.streamxhub.streamx.plugin.profiling;

import com.streamxhub.streamx.plugin.profiling.profiler.CpuAndMemoryProfiler;
import com.streamxhub.streamx.plugin.profiling.profiler.IOProfiler;
import com.streamxhub.streamx.plugin.profiling.profiler.MethodArgumentCollector;
import com.streamxhub.streamx.plugin.profiling.profiler.MethodArgumentProfiler;
import com.streamxhub.streamx.plugin.profiling.profiler.MethodDurationCollector;
import com.streamxhub.streamx.plugin.profiling.profiler.MethodDurationProfiler;
import com.streamxhub.streamx.plugin.profiling.profiler.ProcessInfoProfiler;
import com.streamxhub.streamx.plugin.profiling.profiler.StacktraceCollectorProfiler;
import com.streamxhub.streamx.plugin.profiling.profiler.StacktraceReporterProfiler;
import com.streamxhub.streamx.plugin.profiling.profiler.ThreadInfoProfiler;
import com.streamxhub.streamx.plugin.profiling.transformer.JavaAgentFileTransformer;
import com.streamxhub.streamx.plugin.profiling.transformer.MethodProfilerStaticProxy;
import com.streamxhub.streamx.plugin.profiling.util.AgentLogger;
import com.streamxhub.streamx.plugin.profiling.util.ClassAndMethodLongMetricBuffer;
import com.streamxhub.streamx.plugin.profiling.util.ClassMethodArgumentMetricBuffer;
import com.streamxhub.streamx.plugin.profiling.util.SparkUtils;
import com.streamxhub.streamx.plugin.profiling.util.StacktraceMetricBuffer;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author benjobs
 */
public class AgentImpl {
    public static final String VERSION = "1.0.0";

    private static final AgentLogger LOGGER = AgentLogger.getLogger(AgentImpl.class.getName());

    private static final int MAX_THREAD_POOL_SIZE = 2;

    private boolean started = false;

    public void run(
        Arguments arguments,
        Instrumentation instrumentation,
        Collection<AutoCloseable> objectsToCloseOnShutdown) {
        if (arguments.isNoop()) {
            LOGGER.info("Agent noop is true, do not run anything");
            return;
        }

        Reporter reporter = arguments.getReporter();

        String processUuid = UUID.randomUUID().toString();

        String appId = null;

        String appIdVariable = arguments.getAppIdVariable();
        if (appIdVariable != null && !appIdVariable.isEmpty()) {
            appId = System.getenv(appIdVariable);
        }

        if (appId == null || appId.isEmpty()) {
            appId = SparkUtils.probeAppId(arguments.getAppIdRegex());
        }

        if (!arguments.getDurationProfiling().isEmpty()
            || !arguments.getArgumentProfiling().isEmpty()) {
            instrumentation.addTransformer(
                new JavaAgentFileTransformer(
                    arguments.getDurationProfiling(), arguments.getArgumentProfiling()));
        }

        List<Profiler> profilers = createProfilers(reporter, arguments, processUuid, appId);

        ProfilerGroup profilerGroup = startProfilers(profilers);

        Thread shutdownHook =
            new Thread(
                new ShutdownHookRunner(
                    profilerGroup.getPeriodicProfilers(),
                    Arrays.asList(reporter),
                    objectsToCloseOnShutdown));
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public ProfilerGroup startProfilers(Collection<Profiler> profilers) {
        if (started) {
            LOGGER.warn("Profilers already started, do not start it again");
            return new ProfilerGroup(new ArrayList<>(), new ArrayList<>());
        }

        List<Profiler> oneTimeProfilers = new ArrayList<>();
        List<Profiler> periodicProfilers = new ArrayList<>();

        for (Profiler profiler : profilers) {
            if (profiler.getInterval() == 0) {
                oneTimeProfilers.add(profiler);
            } else if (profiler.getInterval() > 0) {
                periodicProfilers.add(profiler);
            } else {
                LOGGER.log(
                    String.format(
                        "Ignored profiler %s due to its invalid interval %s",
                        profiler, profiler.getInterval()));
            }
        }

        for (Profiler profiler : oneTimeProfilers) {
            try {
                profiler.profile();
                LOGGER.info("Finished one time profiler: " + profiler);
            } catch (Throwable ex) {
                LOGGER.warn("Failed to run one time profiler: " + profiler, ex);
            }
        }

        for (Profiler profiler : periodicProfilers) {
            try {
                profiler.profile();
                LOGGER.info("Ran periodic profiler (first run): " + profiler);
            } catch (Throwable ex) {
                LOGGER.warn("Failed to run periodic profiler (first run): " + profiler, ex);
            }
        }
        scheduleProfilers(periodicProfilers);
        started = true;
        return new ProfilerGroup(oneTimeProfilers, periodicProfilers);
    }

    private List<Profiler> createProfilers(
        Reporter reporter, Arguments arguments, String processUuid, String appId) {
        String tag = arguments.getTag();
        String cluster = arguments.getCluster();
        long metricInterval = arguments.getMetricInterval();

        List<Profiler> profilers = new ArrayList<>();

        CpuAndMemoryProfiler cpuAndMemoryProfiler = new CpuAndMemoryProfiler(reporter);
        cpuAndMemoryProfiler.setTag(tag);
        cpuAndMemoryProfiler.setCluster(cluster);
        cpuAndMemoryProfiler.setInterval(metricInterval);
        cpuAndMemoryProfiler.setProcessUuid(processUuid);
        cpuAndMemoryProfiler.setAppId(appId);
        profilers.add(cpuAndMemoryProfiler);

        ThreadInfoProfiler threadInfoProfiler = new ThreadInfoProfiler(reporter);
        threadInfoProfiler.setTag(tag);
        threadInfoProfiler.setCluster(cluster);
        threadInfoProfiler.setInterval(metricInterval);
        threadInfoProfiler.setProcessUuid(processUuid);
        threadInfoProfiler.setAppId(appId);
        profilers.add(threadInfoProfiler);

        ProcessInfoProfiler processInfoProfiler = new ProcessInfoProfiler(reporter);
        processInfoProfiler.setTag(tag);
        processInfoProfiler.setCluster(cluster);
        processInfoProfiler.setProcessUuid(processUuid);
        processInfoProfiler.setAppId(appId);
        profilers.add(processInfoProfiler);

        if (!arguments.getDurationProfiling().isEmpty()) {
            ClassAndMethodLongMetricBuffer classAndMethodMetricBuffer =
                new ClassAndMethodLongMetricBuffer();

            MethodDurationProfiler methodDurationProfiler =
                new MethodDurationProfiler(classAndMethodMetricBuffer, reporter);
            methodDurationProfiler.setTag(tag);
            methodDurationProfiler.setCluster(cluster);
            methodDurationProfiler.setInterval(metricInterval);
            methodDurationProfiler.setProcessUuid(processUuid);
            methodDurationProfiler.setAppId(appId);

            MethodDurationCollector methodDurationCollector =
                new MethodDurationCollector(classAndMethodMetricBuffer);
            MethodProfilerStaticProxy.setCollector(methodDurationCollector);

            profilers.add(methodDurationProfiler);
        }

        if (!arguments.getArgumentProfiling().isEmpty()) {
            ClassMethodArgumentMetricBuffer classAndMethodArgumentBuffer =
                new ClassMethodArgumentMetricBuffer();

            MethodArgumentProfiler methodArgumentProfiler =
                new MethodArgumentProfiler(classAndMethodArgumentBuffer, reporter);
            methodArgumentProfiler.setTag(tag);
            methodArgumentProfiler.setCluster(cluster);
            methodArgumentProfiler.setInterval(metricInterval);
            methodArgumentProfiler.setProcessUuid(processUuid);
            methodArgumentProfiler.setAppId(appId);

            MethodArgumentCollector methodArgumentCollector =
                new MethodArgumentCollector(classAndMethodArgumentBuffer);
            MethodProfilerStaticProxy.setArgumentCollector(methodArgumentCollector);

            profilers.add(methodArgumentProfiler);
        }

        if (arguments.getSampleInterval() > 0) {
            StacktraceMetricBuffer stacktraceMetricBuffer = new StacktraceMetricBuffer();

            StacktraceCollectorProfiler stacktraceCollectorProfiler =
                new StacktraceCollectorProfiler(stacktraceMetricBuffer, AgentThreadFactory.NAME_PREFIX);
            stacktraceCollectorProfiler.setInterval(arguments.getSampleInterval());

            StacktraceReporterProfiler stacktraceReporterProfiler =
                new StacktraceReporterProfiler(stacktraceMetricBuffer, reporter);
            stacktraceReporterProfiler.setTag(tag);
            stacktraceReporterProfiler.setCluster(cluster);
            stacktraceReporterProfiler.setInterval(metricInterval);
            stacktraceReporterProfiler.setProcessUuid(processUuid);
            stacktraceReporterProfiler.setAppId(appId);

            profilers.add(stacktraceCollectorProfiler);
            profilers.add(stacktraceReporterProfiler);
        }

        if (arguments.isIoProfiling()) {
            IOProfiler ioProfiler = new IOProfiler(reporter);
            ioProfiler.setTag(tag);
            ioProfiler.setCluster(cluster);
            ioProfiler.setInterval(metricInterval);
            ioProfiler.setProcessUuid(processUuid);
            ioProfiler.setAppId(appId);

            profilers.add(ioProfiler);
        }

        return profilers;
    }

    private void scheduleProfilers(Collection<Profiler> profilers) {
        int threadPoolSize = Math.min(profilers.size(), MAX_THREAD_POOL_SIZE);
        ScheduledExecutorService scheduledExecutorService =
            Executors.newScheduledThreadPool(threadPoolSize, new AgentThreadFactory());
        for (Profiler profiler : profilers) {
            if (profiler.getInterval() < Arguments.MIN_INTERVAL_MILLIS) {
                throw new RuntimeException(
                    "Interval too short for profiler: "
                        + profiler
                        + ", must be at least "
                        + Arguments.MIN_INTERVAL_MILLIS);
            }
            ProfilerRunner worker = new ProfilerRunner(profiler);
            scheduledExecutorService.scheduleAtFixedRate(
                worker, 0, profiler.getInterval(), TimeUnit.MILLISECONDS);
            LOGGER.info(
                String.format(
                    "Scheduled profiler %s with interval %s millis", profiler, profiler.getInterval()));
        }
    }
}
