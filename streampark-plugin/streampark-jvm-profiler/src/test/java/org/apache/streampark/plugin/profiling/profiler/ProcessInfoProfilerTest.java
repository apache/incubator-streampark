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
import org.apache.streampark.plugin.profiling.util.ProcFileUtils;
import org.apache.streampark.plugin.profiling.util.ProcessUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ProcessInfoProfilerTest {

    @Test
    void profile() {
        final List<String> nameList = new ArrayList<>();
        final List<Map<String, Object>> metricList = new ArrayList<>();

        ProcessInfoProfiler profiler =
            new ProcessInfoProfiler(
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

        Assertions.assertEquals(0L, profiler.getInterval());

        profiler.profile();
        profiler.profile();

        System.out.println("Metric list:");
        System.out.println(metricList);

        Assertions.assertTrue(nameList.size() >= 2);
        Assertions.assertEquals(ProcessInfoProfiler.PROFILER_NAME, nameList.get(0));

        Assertions.assertTrue(metricList.size() >= 2);

        Assertions.assertTrue(metricList.get(0).containsKey("processUuid"));
        Assertions.assertTrue(metricList.get(0).containsKey("jvmInputArguments"));
        Assertions.assertTrue(metricList.get(0).containsKey("jvmClassPath"));
        Assertions.assertTrue(metricList.get(0).containsKey("cmdline"));

        Assertions.assertTrue(metricList.get(metricList.size() - 1).containsKey("processUuid"));
        Assertions.assertTrue(metricList.get(metricList.size() - 1).containsKey("jvmInputArguments"));
        Assertions.assertTrue(metricList.get(metricList.size() - 1).containsKey("jvmClassPath"));
        Assertions.assertTrue(metricList.get(metricList.size() - 1).containsKey("cmdline"));

        // Verify: if cmdline is empty, there should be jvmClassPath/jvmInputArguments,
        // otherwise there should be no jvmClassPath/jvmInputArguments
        if (ProcFileUtils.getCmdline() == null || ProcFileUtils.getCmdline().isEmpty()) {
            Assertions.assertEquals(0, metricList.stream().filter(map -> !map.get("cmdline").equals("")).count());

            Assertions.assertTrue(
                metricList.stream().filter(map -> !map.get("jvmClassPath").equals("")).count() > 0);

            if (ProcessUtils.getJvmInputArguments().isEmpty()) {
                Assertions.assertEquals(0, metricList.stream().filter(map -> !map.get("jvmInputArguments").equals("")).count());
            } else {
                Assertions.assertTrue(
                    metricList.stream().filter(map -> !map.get("jvmInputArguments").equals("")).count() > 0);
            }
        } else {
            Assertions.assertTrue(
                metricList.stream().filter(map -> !map.get("cmdline").equals("")).count() > 0);
            Assertions.assertEquals(0, metricList.stream().filter(map -> !map.get("jvmClassPath").equals("")).count());
            Assertions.assertEquals(0, metricList.stream().filter(map -> !map.get("jvmInputArguments").equals("")).count());
        }
    }
}
