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

import com.uber.profiling.Profiler;
import com.uber.profiling.Reporter;
import com.uber.profiling.reporters.ConsoleOutputReporter;
import com.uber.profiling.util.ClassAndMethodLongMetricBuffer;
import com.uber.profiling.util.ClassAndMethodMetricKey;
import com.uber.profiling.util.Histogram;

import java.util.HashMap;
import java.util.Map;

public class MethodDurationProfiler extends ProfilerBase implements Profiler {
    public static final String PROFILER_NAME = "MethodDuration";

    private ClassAndMethodLongMetricBuffer buffer;

    private Reporter reporter = new ConsoleOutputReporter();

    private long intervalMillis = Constants.DEFAULT_METRIC_INTERVAL;

    public MethodDurationProfiler(ClassAndMethodLongMetricBuffer buffer, Reporter reporter) {
        this.buffer = buffer;
        this.reporter = reporter;
    }

    @Override
    public long getIntervalMillis() {
        return intervalMillis;
    }

    public void setIntervalMillis(long intervalMillis) {
        this.intervalMillis = intervalMillis;
    }

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

        Map<ClassAndMethodMetricKey, Histogram> metrics = buffer.reset();

        long epochMillis = System.currentTimeMillis();

        for (Map.Entry<ClassAndMethodMetricKey, Histogram> entry : metrics.entrySet()) {
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
                metricMap.put("metricName", entry.getKey().getMetricName() + ".count");
                metricMap.put("metricValue", (double) entry.getValue().getCount());
                reporter.report(PROFILER_NAME, metricMap);
            }
            {
                Map<String, Object> metricMap = new HashMap<>(commonMap);
                metricMap.put("metricName", entry.getKey().getMetricName() + ".sum");
                metricMap.put("metricValue", (double) entry.getValue().getSum());
                reporter.report(PROFILER_NAME, metricMap);
            }
            {
                Map<String, Object> metricMap = new HashMap<>(commonMap);
                metricMap.put("metricName", entry.getKey().getMetricName() + ".min");
                metricMap.put("metricValue", (double) entry.getValue().getMin());
                reporter.report(PROFILER_NAME, metricMap);
            }
            {
                Map<String, Object> metricMap = new HashMap<>(commonMap);
                metricMap.put("metricName", entry.getKey().getMetricName() + ".max");
                metricMap.put("metricValue", (double) entry.getValue().getMax());
                reporter.report(PROFILER_NAME, metricMap);
            }
        }
    }
}
