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

import com.uber.profiling.Reporter;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CpuAndMemoryProfilerTest {
    @Test
    public void profile() {
        final List<String> nameList = new ArrayList<>();
        final List<Map<String, Object>> metricList = new ArrayList<>();

        CpuAndMemoryProfiler profiler = new CpuAndMemoryProfiler(new Reporter() {
            @Override
            public void report(String profilerName, Map<String, Object> metrics) {
                nameList.add(profilerName);
                metricList.add(metrics);
            }

            @Override
            public void close() {
            }
        });

        profiler.setIntervalMillis(123);
        Assert.assertEquals(123L, profiler.getIntervalMillis());

        profiler.profile();
        profiler.profile();

        Assert.assertEquals(2, nameList.size());
        Assert.assertEquals(CpuAndMemoryProfiler.PROFILER_NAME, nameList.get(0));

        Assert.assertEquals(2, metricList.size());
        Assert.assertTrue(metricList.get(0).containsKey("processUuid"));
        Assert.assertTrue(metricList.get(0).containsKey("processCpuLoad"));
        Assert.assertTrue(metricList.get(0).containsKey("heapMemoryTotalUsed"));
        Assert.assertTrue(metricList.get(0).containsKey("gc"));

        Object obj = metricList.get(0).get("gc");
        Assert.assertTrue(obj instanceof List);

        List<Map<String, Object>> gcMetrics = (List<Map<String, Object>>) obj;
        Assert.assertTrue(gcMetrics.size() >= 1);
    }
}
