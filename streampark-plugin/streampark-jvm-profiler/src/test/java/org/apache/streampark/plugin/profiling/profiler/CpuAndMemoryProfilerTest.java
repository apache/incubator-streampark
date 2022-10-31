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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class CpuAndMemoryProfilerTest {

    @Test
    void profile() {
        final List<String> nameList = new ArrayList<>();
        final List<Map<String, Object>> metricList = new ArrayList<>();

        CpuAndMemoryProfiler profiler =
            new CpuAndMemoryProfiler(
                new Reporter() {
                    @Override
                    public void report(String profilerName, Map<String, Object> metrics) {
                        nameList.add(profilerName);
                        metricList.add(metrics);
                    }

                    @Override
                    public void close() {
                    }
                });

        profiler.setInterval(123);
        Assertions.assertEquals(123L, profiler.getInterval());

        profiler.profile();
        profiler.profile();

        Assertions.assertEquals(2, nameList.size());
        Assertions.assertEquals(CpuAndMemoryProfiler.PROFILER_NAME, nameList.get(0));

        Assertions.assertEquals(2, metricList.size());
        Assertions.assertTrue(metricList.get(0).containsKey("processUuid"));
        Assertions.assertTrue(metricList.get(0).containsKey("processCpuLoad"));
        Assertions.assertTrue(metricList.get(0).containsKey("heapMemoryTotalUsed"));
        Assertions.assertTrue(metricList.get(0).containsKey("gc"));

        Object obj = metricList.get(0).get("gc");
        Assertions.assertTrue(obj instanceof List);

        List<Map<String, Object>> gcMetrics = (List<Map<String, Object>>) obj;
        Assertions.assertTrue(gcMetrics.size() >= 1);
    }
}
