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
import com.streamxhub.streamx.plugin.profiling.profiler.ThreadInfoProfiler;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ThreadInfoProfilerTest {
    @Test
    public void profile() {
        final List<String> nameList = new ArrayList<>();
        final List<Map<String, Object>> metricList = new ArrayList<>();

        // create a Profile Instance.
        ThreadInfoProfiler profiler =
            new ThreadInfoProfiler(
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
        // Set interval
        profiler.setInterval(150);
        Assert.assertEquals(150L, profiler.getInterval());

        // run 2 cycles on the profile.
        profiler.profile();
        profiler.profile();

        // start assertion.
        Assert.assertEquals(2, nameList.size());
        Assert.assertEquals(ThreadInfoProfiler.PROFILER_NAME, nameList.get(0));

        Assert.assertEquals(2, metricList.size());
        Assert.assertTrue(metricList.get(0).containsKey("totalStartedThreadCount"));
        Assert.assertTrue(metricList.get(0).containsKey("newThreadCount"));
        Assert.assertTrue(metricList.get(0).containsKey("liveThreadCount"));
        Assert.assertTrue(metricList.get(0).containsKey("peakThreadCount"));
    }
}
