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

import org.apache.streampark.plugin.profiling.Reporter;
import org.apache.streampark.plugin.profiling.util.ClassMethodArgumentMetricBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class MethodArgumentProfilerTest {

    @Test
    void profile() {
        final List<String> nameList = new ArrayList<>();
        final List<Map<String, Object>> metricList = new ArrayList<>();

        ClassMethodArgumentMetricBuffer buffer = new ClassMethodArgumentMetricBuffer();

        MethodArgumentCollector collector = new MethodArgumentCollector(buffer);

        Reporter reporter =
            new Reporter() {
                @Override
                public void report(String profilerName, Map<String, Object> metrics) {
                    nameList.add(profilerName);
                    metricList.add(metrics);
                }

                @Override
                public void close() {
                }
            };

        MethodArgumentProfiler profiler = new MethodArgumentProfiler(buffer, reporter);

        profiler.setInterval(123);
        Assertions.assertEquals(123L, profiler.getInterval());

        collector.collectMetric("class1", "method1", "arg1");
        collector.collectMetric("class1", "method1", "arg1");
        collector.collectMetric("class2", "method2", "arg2");

        profiler.profile();

        Assertions.assertEquals(2, nameList.size());
        Assertions.assertEquals(MethodArgumentProfiler.PROFILER_NAME, nameList.get(0));

        Assertions.assertEquals(2, metricList.size());

        List<Map<String, Object>> metricsToCheck =
            metricList.stream()
                .filter(
                    t ->
                        t.get("className").equals("class1")
                            && t.get("methodName").equals("method1")
                            && t.get("metricName").equals("arg1"))
                .collect(Collectors.toList());
        Assertions.assertEquals(1, metricsToCheck.size());
        Assertions.assertEquals(2.0, (Double) metricsToCheck.get(0).get("metricValue"), 0.01);

        metricsToCheck =
            metricList.stream()
                .filter(
                    t ->
                        t.get("className").equals("class2")
                            && t.get("methodName").equals("method2")
                            && t.get("metricName").equals("arg2"))
                .collect(Collectors.toList());
        Assertions.assertEquals(1, metricsToCheck.size());
        Assertions.assertEquals(1.0, (Double) metricsToCheck.get(0).get("metricValue"), 0.01);
    }
}
