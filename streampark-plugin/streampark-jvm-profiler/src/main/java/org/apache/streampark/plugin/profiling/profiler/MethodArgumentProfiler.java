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
import org.apache.streampark.plugin.profiling.util.ClassAndMethodMetricKey;
import org.apache.streampark.plugin.profiling.util.ClassMethodArgumentMetricBuffer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MethodArgumentProfiler extends ProfilerBase implements Profiler {
    public static final String PROFILER_NAME = "MethodArgument";

    private final ClassMethodArgumentMetricBuffer buffer;

    private Reporter reporter = new ConsoleOutputReporter();

    private long interval = Constants.DEFAULT_METRIC_INTERVAL;

    public MethodArgumentProfiler(ClassMethodArgumentMetricBuffer buffer, Reporter reporter) {
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

        Map<ClassAndMethodMetricKey, AtomicLong> metrics = buffer.reset();

        long epochMillis = System.currentTimeMillis();

        for (Map.Entry<ClassAndMethodMetricKey, AtomicLong> entry : metrics.entrySet()) {
            Map<String, Object> commonMap = new HashMap<>();

            commonMap.put("epochMillis", epochMillis);
            commonMap.put("processName", getProcessName());
            commonMap.put("host", getHostName());
            commonMap.put("processUuid", getProcessUuid());
            commonMap.put("appId", getAppId());

            commonMap.put("className", entry.getKey().getClassName());
            commonMap.put("methodName", entry.getKey().getMethodName());

            if (getTag() != null) {
                commonMap.put("tag", getTag());
            }

            if (getCluster() != null) {
                commonMap.put("cluster", getCluster());
            }

            if (getRole() != null) {
                commonMap.put("role", getRole());
            }

            {
                Map<String, Object> metricMap = new HashMap<>(commonMap);
                metricMap.put("metricName", entry.getKey().getMetricName());
                metricMap.put("metricValue", (double) entry.getValue().get());
                reporter.report(PROFILER_NAME, metricMap);
            }
        }
    }
}
