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

package com.uber.profiling.reporters;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphiteOutputReporterTest {

  GraphiteOutputReporter reporter = null;

  @Before
  public void initReporter() {
    reporter = new GraphiteOutputReporter();
  }

  @Test
  public void testGetFormattedMetrics() {
    Map<String, Object> metrics = new HashMap<>();

    //Stacktrace
    metrics.put("startEpoch", 1569205908214L);
    metrics.put("endEpoch", 1569205908258L);
    metrics.put("appId", "hello_jvm_profile");
    metrics.put("host", "test_host");
    metrics.put("name", "29287@test_host");
    metrics.put("processUuid", "387067c9-f685-49c7-9839-b0a0f96084a2");
    metrics.put("threadState", "WAITING");
    metrics.put("count", 1L);
    metrics.put("tag", "mytag2");
    metrics.put("threadName", "Finalizer");
    metrics.put("stacktrace",
        Arrays.asList("java.lang.Object.wait",
            "java.lang.ref.ReferenceQueue.remove",
            "java.lang.ref.ReferenceQueue.remove",
            "java.lang.ref.Finalizer$FinalizerThread.run"));

    // CpuAndMemory
    metrics.put("nonHeapMemoryTotalUsed", 1.690948E7);
    Map<String, Object> directBufferPoolsMap = new HashMap();
    directBufferPoolsMap.put("totalCapacity", 24575L);
    directBufferPoolsMap.put("name", "direct");
    directBufferPoolsMap.put("count", 3L);
    directBufferPoolsMap.put("memoryUsed", 24575L);

    Map<String, Object> mappedBufferPoolsMap = new HashMap();
    mappedBufferPoolsMap.put("totalCapacity", 0L);
    mappedBufferPoolsMap.put("name", "mapped");
    mappedBufferPoolsMap.put("count", 0L);
    mappedBufferPoolsMap.put("memoryUsed", 0L);
    metrics.put("bufferPools", Arrays.asList(directBufferPoolsMap, mappedBufferPoolsMap));

    metrics.put("heapMemoryTotalUsed", 4.2389864E7);
    metrics.put("vmRSS", 78229504L);
    metrics.put("epochMillis", 1569205913277L);
    metrics.put("nonHeapMemoryCommitted", 1.80224E7);
    metrics.put("heapMemoryCommitted", 5.04889344E8);

    List<Map<String, String>> memoryPoolsList = new ArrayList<>();
    memoryPoolsList.add(parseMap(
        "{peakUsageMax=251658240, usageMax=251658240, peakUsageUsed=2072256, name=Code Cache, "
            + "peakUsageCommitted=2555904, usageUsed=2072256, type=Non-heap memory, "
            + "usageCommitted=2555904}"));

    memoryPoolsList.add(parseMap(
        "{peakUsageMax=-1, usageMax=-1, peakUsageUsed=13434312, name=Metaspace, "
            + "peakUsageCommitted=13893632, usageUsed=13434312, type=Non-heap memory, "
            + "usageCommitted=13893632}"));

    memoryPoolsList.add(parseMap(
        "{peakUsageMax=1073741824, usageMax=1073741824, peakUsageUsed=1402912, name=Compressed "
            + "Class Space, peakUsageCommitted=1572864, usageUsed=1402912, type=Non-heap memory, "
            + "usageCommitted=1572864}"));

    memoryPoolsList.add(parseMap(
        "{peakUsageMax=2754609152, usageMax=2754609152, peakUsageUsed=42389864, name=PS Eden "
            + "Space, peakUsageCommitted=132120576, usageUsed=42389864, type=Heap memory, "
            + "usageCommitted=132120576}"));

    memoryPoolsList.add(parseMap(
        "{peakUsageMax=21495808, usageMax=21495808, peakUsageUsed=0, name=PS Survivor Space, "
            + "peakUsageCommitted=21495808, usageUsed=0, type=Heap memory, "
            + "usageCommitted=21495808}"));

    memoryPoolsList.add(parseMap(
        "{peakUsageMax=5595201536, usageMax=5595201536, peakUsageUsed=0, name=PS Old Gen, "
            + "peakUsageCommitted=351272960, usageUsed=0, type=Heap memory, "
            + "usageCommitted=351272960}"));

    metrics.put("memoryPools", memoryPoolsList);

    Map<String, Object> formattedMetrics = reporter.getFormattedMetrics(metrics);
    Map<String, Object> expectedMetrics = new HashMap<>();

    expectedMetrics.put("appId", "hello_jvm_profile");
    expectedMetrics.put("bufferPools.direct.count", 3L);
    expectedMetrics.put("bufferPools.direct.memoryUsed", 24575L);
    expectedMetrics.put("bufferPools.direct.totalCapacity", 24575L);
    expectedMetrics.put("bufferPools.mapped.count", 0L);
    expectedMetrics.put("bufferPools.mapped.memoryUsed", 0L);
    expectedMetrics.put("bufferPools.mapped.totalCapacity", 0L);
    expectedMetrics.put("count", 1L);
    expectedMetrics.put("endEpoch", 1569205908258L);
    expectedMetrics.put("epochMillis", 1569205913277L);
    expectedMetrics.put("heapMemoryCommitted", 5.04889344E8);
    expectedMetrics.put("heapMemoryTotalUsed", 4.2389864E7);
    expectedMetrics.put("host", "test_host");
    expectedMetrics.put("memoryPools.CodeCache.peakUsageCommitted", 2555904L);
    expectedMetrics.put("memoryPools.CodeCache.peakUsageMax", 251658240L);
    expectedMetrics.put("memoryPools.CodeCache.peakUsageUsed", 2072256L);
    expectedMetrics.put("memoryPools.CodeCache.type", "Non-heap memory");
    expectedMetrics.put("memoryPools.CodeCache.usageCommitted", 2555904L);
    expectedMetrics.put("memoryPools.CodeCache.usageMax", 251658240L);
    expectedMetrics.put("memoryPools.CodeCache.usageUsed", 2072256L);
    expectedMetrics.put("memoryPools.CompressedClassSpace.peakUsageCommitted", 1572864L);
    expectedMetrics.put("memoryPools.CompressedClassSpace.peakUsageMax", 1073741824L);
    expectedMetrics.put("memoryPools.CompressedClassSpace.peakUsageUsed", 1402912L);
    expectedMetrics.put("memoryPools.CompressedClassSpace.type", "Non-heap memory");
    expectedMetrics.put("memoryPools.CompressedClassSpace.usageCommitted", 1572864L);
    expectedMetrics.put("memoryPools.CompressedClassSpace.usageMax", 1073741824L);
    expectedMetrics.put("memoryPools.CompressedClassSpace.usageUsed", 1402912L);
    expectedMetrics.put("memoryPools.Metaspace.peakUsageCommitted", 13893632L);
    expectedMetrics.put("memoryPools.Metaspace.peakUsageMax", -1L);
    expectedMetrics.put("memoryPools.Metaspace.peakUsageUsed", 13434312L);
    expectedMetrics.put("memoryPools.Metaspace.type", "Non-heap memory");
    expectedMetrics.put("memoryPools.Metaspace.usageCommitted", 13893632L);
    expectedMetrics.put("memoryPools.Metaspace.usageMax", -1L);
    expectedMetrics.put("memoryPools.Metaspace.usageUsed", 13434312L);
    expectedMetrics.put("memoryPools.PSEdenSpace.peakUsageCommitted", 132120576L);
    expectedMetrics.put("memoryPools.PSEdenSpace.peakUsageMax", 2754609152L);
    expectedMetrics.put("memoryPools.PSEdenSpace.peakUsageUsed", 42389864L);
    expectedMetrics.put("memoryPools.PSEdenSpace.type", "Heap memory");
    expectedMetrics.put("memoryPools.PSEdenSpace.usageCommitted", 132120576L);
    expectedMetrics.put("memoryPools.PSEdenSpace.usageMax", 2754609152L);
    expectedMetrics.put("memoryPools.PSEdenSpace.usageUsed", 42389864L);
    expectedMetrics.put("memoryPools.PSOldGen.peakUsageCommitted", 351272960L);
    expectedMetrics.put("memoryPools.PSOldGen.peakUsageMax", 5595201536L);
    expectedMetrics.put("memoryPools.PSOldGen.peakUsageUsed", 0L);
    expectedMetrics.put("memoryPools.PSOldGen.type", "Heap memory");
    expectedMetrics.put("memoryPools.PSOldGen.usageCommitted", 351272960L);
    expectedMetrics.put("memoryPools.PSOldGen.usageMax", 5595201536L);
    expectedMetrics.put("memoryPools.PSOldGen.usageUsed", 0L);
    expectedMetrics.put("memoryPools.PSSurvivorSpace.peakUsageCommitted", 21495808L);
    expectedMetrics.put("memoryPools.PSSurvivorSpace.peakUsageMax", 21495808L);
    expectedMetrics.put("memoryPools.PSSurvivorSpace.peakUsageUsed", 0L);
    expectedMetrics.put("memoryPools.PSSurvivorSpace.type", "Heap memory");
    expectedMetrics.put("memoryPools.PSSurvivorSpace.usageCommitted", 21495808L);
    expectedMetrics.put("memoryPools.PSSurvivorSpace.usageMax", 21495808L);
    expectedMetrics.put("memoryPools.PSSurvivorSpace.usageUsed", 0L);
    expectedMetrics.put("name", "29287@test_host");
    expectedMetrics.put("nonHeapMemoryCommitted", 1.80224E7);
    expectedMetrics.put("nonHeapMemoryTotalUsed", 1.690948E7);
    expectedMetrics.put("processUuid", "387067c9-f685-49c7-9839-b0a0f96084a2");
    expectedMetrics.put("stacktrace",
        "java.lang.Object.wait,java.lang.ref.ReferenceQueue.remove,java.lang.ref.ReferenceQueue"
            + ".remove,java.lang.ref.Finalizer$FinalizerThread.run");
    expectedMetrics.put("startEpoch" , 1569205908214L);
    expectedMetrics.put("tag", "mytag2");
    expectedMetrics.put("threadName", "Finalizer");
    expectedMetrics.put("threadState" , "WAITING");
    expectedMetrics.put("vmRSS", 78229504L);

    assertEquals(expectedMetrics, formattedMetrics);
  }

  @Test
  public void testParseMap() throws IOException {
    String mapString = "{peakUsageMax=251658240, usageMax=251658240, peakUsageUsed=2072256, "
        + "name=Code Cache, peakUsageCommitted=2555904, usageUsed=2072256, type=Non-heap memory, "
        + "usageCommitted=2555904}";
    Map map = parseMap(mapString);

    Map expected = new HashMap();
    expected.put("peakUsageMax", 251658240L);
    expected.put("peakUsageUsed", 2072256L);
    expected.put("usageMax", 251658240L);
    expected.put("name", "Code Cache");
    expected.put("peakUsageCommitted", 2555904L);
    expected.put("usageUsed", 2072256L);
    expected.put("usageCommitted", 2555904L);
    expected.put("type", "Non-heap memory");
    assertEquals(expected, map);
  }

  private Map<String, String> parseMap(String mapString) {
    Map result = new HashMap<>();
    String[] fields = mapString.replace("{", "")
        .replace("}", "")
        .split(",");

    for (int i = 0; i < fields.length; i++) {
      String[] kv = fields[i].split("=");
      String k = StringUtils.trim(kv[0]);
      String v = StringUtils.trim(kv[1]);

      try {
        long l = Long.parseLong(v);
        result.put(k, l);
      } catch (Exception e) {
        result.put(k, v);
      }
    }
    return result;
  }

}
