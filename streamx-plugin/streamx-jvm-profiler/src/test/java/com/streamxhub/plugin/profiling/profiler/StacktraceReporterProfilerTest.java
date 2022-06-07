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

package com.streamxhub.plugin.profiling.profiler;

import com.streamxhub.streamx.plugin.profiling.Reporter;
import com.streamxhub.streamx.plugin.profiling.profiler.StacktraceReporterProfiler;
import com.streamxhub.streamx.plugin.profiling.util.ClassAndMethod;
import com.streamxhub.streamx.plugin.profiling.util.Stacktrace;
import com.streamxhub.streamx.plugin.profiling.util.StacktraceMetricBuffer;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StacktraceReporterProfilerTest {
    @Test
    public void profile() {
        final List<String> nameList = new ArrayList<>();
        final List<Map<String, Object>> metricList = new ArrayList<>();

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

        long epochMillis1 = System.currentTimeMillis();

        StacktraceMetricBuffer buffer = new StacktraceMetricBuffer();
        StacktraceReporterProfiler profiler = new StacktraceReporterProfiler(buffer, reporter);

        profiler.setInterval(123);
        Assert.assertEquals(123L, profiler.getInterval());

        profiler.profile();
        Assert.assertEquals(0, nameList.size());
        Assert.assertEquals(0, metricList.size());

        Stacktrace stacktrace = new Stacktrace();
        buffer.appendValue(stacktrace);

        profiler.profile();

        long epochMillis2 = System.currentTimeMillis();

        Assert.assertEquals(1, nameList.size());
        Assert.assertEquals("Stacktrace", nameList.get(0));

        Assert.assertEquals(1, metricList.size());

        Map<String, Object> map = metricList.get(0);

        Assert.assertTrue((long) map.get("startEpoch") >= epochMillis1);
        Assert.assertTrue((long) map.get("startEpoch") <= epochMillis2);
        Assert.assertTrue((long) map.get("endEpoch") >= epochMillis1);
        Assert.assertTrue((long) map.get("endEpoch") <= epochMillis2);
        Assert.assertTrue((long) map.get("endEpoch") >= (long) map.get("startEpoch"));
        Assert.assertEquals(1L, (long) map.get("count"));
        Assert.assertNull(map.get("threadName"));
        Assert.assertNull(map.get("threadState"));
        Assert.assertArrayEquals(
            new String[0], ((ArrayList<String>) map.get("stacktrace")).toArray(new String[0]));

        stacktrace = new Stacktrace();
        stacktrace.setThreadName("thread1");
        stacktrace.setThreadState("RUNNING");
        stacktrace.setStack(
            new ClassAndMethod[]{
                new ClassAndMethod("class1", "method1"), new ClassAndMethod("class2", "method2")
            });

        buffer.appendValue(stacktrace);
        buffer.appendValue(stacktrace);

        profiler.profile();

        Assert.assertEquals(2, nameList.size());
        Assert.assertEquals("Stacktrace", nameList.get(0));
        Assert.assertEquals("Stacktrace", nameList.get(1));

        Assert.assertEquals(2, metricList.size());

        map = metricList.get(1);

        Assert.assertEquals(2L, (long) map.get("count"));
        Assert.assertEquals("thread1", map.get("threadName"));
        Assert.assertEquals("RUNNING", map.get("threadState"));
        Assert.assertArrayEquals(
            new String[]{"class1.method1", "class2.method2"},
            ((ArrayList<String>) map.get("stacktrace")).toArray(new String[0]));
    }
}
