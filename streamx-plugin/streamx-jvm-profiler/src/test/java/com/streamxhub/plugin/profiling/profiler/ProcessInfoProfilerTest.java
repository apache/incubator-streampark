/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.streamxhub.plugin.profiling.profiler;

import com.streamxhub.streamx.plugin.profiling.Reporter;
import com.streamxhub.streamx.plugin.profiling.profiler.ProcessInfoProfiler;
import com.streamxhub.streamx.plugin.profiling.util.ProcFileUtils;
import com.streamxhub.streamx.plugin.profiling.util.ProcessUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProcessInfoProfilerTest {
  @Test
  public void profile() {
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
              public void close() {}
            });

    Assert.assertEquals(0L, profiler.getInterval());

    profiler.profile();
    profiler.profile();

    System.out.println("Metric list:");
    System.out.println(metricList);

    Assert.assertTrue(nameList.size() >= 2);
    Assert.assertEquals(ProcessInfoProfiler.PROFILER_NAME, nameList.get(0));

    Assert.assertTrue(metricList.size() >= 2);

    Assert.assertTrue(metricList.get(0).containsKey("processUuid"));
    Assert.assertTrue(metricList.get(0).containsKey("jvmInputArguments"));
    Assert.assertTrue(metricList.get(0).containsKey("jvmClassPath"));
    Assert.assertTrue(metricList.get(0).containsKey("cmdline"));

    Assert.assertTrue(metricList.get(metricList.size() - 1).containsKey("processUuid"));
    Assert.assertTrue(metricList.get(metricList.size() - 1).containsKey("jvmInputArguments"));
    Assert.assertTrue(metricList.get(metricList.size() - 1).containsKey("jvmClassPath"));
    Assert.assertTrue(metricList.get(metricList.size() - 1).containsKey("cmdline"));

    // Verify: if cmdline is empty, there should be jvmClassPath/jvmInputArguments,
    // otherwise there should be no jvmClassPath/jvmInputArguments
    if (ProcFileUtils.getCmdline() == null || ProcFileUtils.getCmdline().isEmpty()) {
      Assert.assertTrue(
          metricList.stream().filter(map -> !map.get("cmdline").equals("")).count() == 0);

      Assert.assertTrue(
          metricList.stream().filter(map -> !map.get("jvmClassPath").equals("")).count() > 0);

      if (ProcessUtils.getJvmInputArguments().isEmpty()) {
        Assert.assertTrue(
            metricList.stream().filter(map -> !map.get("jvmInputArguments").equals("")).count()
                == 0);
      } else {
        Assert.assertTrue(
            metricList.stream().filter(map -> !map.get("jvmInputArguments").equals("")).count()
                > 0);
      }
    } else {
      Assert.assertTrue(
          metricList.stream().filter(map -> !map.get("cmdline").equals("")).count() > 0);
      Assert.assertTrue(
          metricList.stream().filter(map -> !map.get("jvmClassPath").equals("")).count() == 0);
      Assert.assertTrue(
          metricList.stream().filter(map -> !map.get("jvmInputArguments").equals("")).count() == 0);
    }
  }
}
