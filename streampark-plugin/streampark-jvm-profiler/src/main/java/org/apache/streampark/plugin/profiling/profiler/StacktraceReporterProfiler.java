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

package org.apache.streampark.plugin.profiling.profiler;

import org.apache.streampark.plugin.profiling.Profiler;
import org.apache.streampark.plugin.profiling.Reporter;
import org.apache.streampark.plugin.profiling.reporter.ConsoleOutputReporter;
import org.apache.streampark.plugin.profiling.util.ClassAndMethod;
import org.apache.streampark.plugin.profiling.util.Stacktrace;
import org.apache.streampark.plugin.profiling.util.StacktraceMetricBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class reads the stacktraces from the given buffer and send out via given reporter.
 */
public class StacktraceReporterProfiler extends ProfilerBase implements Profiler {
    public static final String PROFILER_NAME = "Stacktrace";

    private final StacktraceMetricBuffer buffer;

    private Reporter reporter = new ConsoleOutputReporter();

    private long interval = Constants.DEFAULT_METRIC_INTERVAL;

    public StacktraceReporterProfiler(StacktraceMetricBuffer buffer, Reporter reporter) {
        this.buffer = buffer;
        this.reporter = reporter;
    }

    @Override
    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    @Override
    public void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void profile() {
        if (buffer == null) {
            return;
        }

        if (reporter == null) {
            return;
        }

        long startEpoch = buffer.getLastResetMillis();

        Map<Stacktrace, AtomicLong> metrics = buffer.reset();

        long endEpoch = buffer.getLastResetMillis();

        for (Map.Entry<Stacktrace, AtomicLong> entry : metrics.entrySet()) {
            Map<String, Object> map = new HashMap<>();

            map.put("startEpoch", startEpoch);
            map.put("endEpoch", endEpoch);

            map.put("host", getHostName());
            map.put("name", getProcessName());
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

            Stacktrace stacktrace = entry.getKey();

            map.put("threadName", stacktrace.getThreadName());
            map.put("threadState", stacktrace.getThreadState());

            ClassAndMethod[] classAndMethodArray = stacktrace.getStack();
            if (classAndMethodArray != null) {
                List<String> stackArray = new ArrayList<>(classAndMethodArray.length);
                for (int i = 0; i < classAndMethodArray.length; i++) {
                    ClassAndMethod classAndMethod = classAndMethodArray[i];
                    stackArray.add(classAndMethod.getClassName() + "." + classAndMethod.getMethodName());
                }
                map.put("stacktrace", stackArray);
            }

            map.put("count", entry.getValue().get());

            reporter.report(PROFILER_NAME, map);
        }
    }
}
