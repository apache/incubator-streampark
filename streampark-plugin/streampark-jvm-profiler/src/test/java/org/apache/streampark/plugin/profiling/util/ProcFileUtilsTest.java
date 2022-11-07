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

package org.apache.streampark.plugin.profiling.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class ProcFileUtilsTest {

    @Test
    void getProcFileAsMap() throws IOException {
        File file = File.createTempFile("test", "test");
        file.deleteOnExit();

        String content =
            "Name:\tcat\t\n"
                + "VmSize:	     776 kB \r\n"
                + "VmPeak:	     876 kB \r\n"
                + "VmRSS:	     676 kB \r\n"
                + "\t  Pid \t  : \t 66646 \t\n\r"
                + "Threads: \t 1 \t\n"
                + "";
        Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

        Map<String, String> result = ProcFileUtils.getProcFileAsMap(file.getPath());
        Assertions.assertEquals(6, result.size());

        Assertions.assertEquals("cat", result.get("Name"));
        Assertions.assertEquals("676 kB", result.get("VmRSS"));
        Assertions.assertEquals("66646", result.get("Pid"));
        Assertions.assertEquals("1", result.get("Threads"));
    }

    @Test
    void getProcFileAsMap_NotExistingFile() {
        Map<String, String> result = ProcFileUtils.getProcFileAsMap("/not/existing/file");
        Assertions.assertEquals(0, result.size());
    }

    @Test
    void getProcFileAsMap_Directory() {
        Map<String, String> result = ProcFileUtils.getProcFileAsMap("/");
        Assertions.assertEquals(0, result.size());
    }

    @Test
    void getProcFileAsRowColumn_NotExistingFile() {
        List<String[]> result = ProcFileUtils.getProcFileAsRowColumn("/not/existing/file");
        Assertions.assertEquals(0, result.size());
    }

    @Test
    void getProcFileAsRowColumn_Directory() {
        List<String[]> result = ProcFileUtils.getProcFileAsRowColumn("/");
        Assertions.assertEquals(0, result.size());
    }

    @Test
    void getProcStatus_DefaultFile() {
        Map<String, String> result = ProcFileUtils.getProcStatus();

        // Mac has no proc file so result will be empty.
        // Linux has proc file and result should contain some keys;
        if (result.size() >= 1) {
            Assertions.assertTrue(result.containsKey("Pid"));
            Assertions.assertTrue(result.containsKey("VmRSS"));
            Assertions.assertTrue(result.containsKey("VmSize"));
            Assertions.assertTrue(result.containsKey("VmPeak"));
        }
    }

    @Test
    void getProcIO_DefaultFile() {
        Map<String, String> result = ProcFileUtils.getProcIO();

        // Mac has no proc file so result will be empty.
        // Linux has proc file and result should contain some keys;
        if (result.size() >= 1) {
            Assertions.assertTrue(result.containsKey("rchar"));
            Assertions.assertTrue(result.containsKey("wchar"));
            Assertions.assertTrue(result.containsKey("read_bytes"));
            Assertions.assertTrue(result.containsKey("write_bytes"));
        }
    }

    @Test
    void getBytesValue() throws IOException {
        {
            Map<String, String> map = Collections.emptyMap();
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assertions.assertNull(bytesValue);
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content = "Name:\tcat\t\n" + "Pid: \t 66646 \t\n\r" + "Threads: \t 1 \t\n" + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assertions.assertNull(bytesValue);
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content =
                "Name:\tcat\t\n"
                    + "VmRSS:	     xxx kB \r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assertions.assertNull(bytesValue);
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content =
                "Name:\tcat\t\n"
                    + "VmRSS:	     676 \r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assertions.assertEquals(676L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content =
                "Name:\tcat\t\n"
                    + "VmRSS:	     676 kB \r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assertions.assertEquals(676 * 1024L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content =
                "Name:\tcat\t\n"
                    + "VmRSS:	    \t 676 \t kB \t\r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assertions.assertEquals(676 * 1024L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content =
                "Name:\tcat\t\n"
                    + "VmRSS:	    \t 676 KB \t\r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assertions.assertEquals(676 * 1024L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content =
                "Name:\tcat\t\n"
                    + "VmRSS:	    \t 676KB \t\r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assertions.assertEquals(676 * 1024L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content =
                "Name:\tcat\t\n"
                    + "VmRSS:676KB \t\r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assertions.assertEquals(676 * 1024L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content =
                "Name:\tcat\t\n"
                    + "VmRSS:	    \t 676 mB \t\r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assertions.assertEquals(676 * 1024 * 1024L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content =
                "Name:\tcat\t\n"
                    + "VmRSS:	    \t 676 MB \t\r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assertions.assertEquals(676 * 1024 * 1024L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content =
                "Name:\tcat\t\n"
                    + "VmRSS:	    \t 676 gB \t\r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assertions.assertEquals(676 * 1024 * 1024 * 1024L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content =
                "Name:\tcat\t\n"
                    + "VmRSS:	    \t 676 GB \t\r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assertions.assertEquals(676 * 1024 * 1024 * 1024L, bytesValue.longValue());
        }
    }

    @Test
    void getProcStatCpuTime() throws IOException {
        {
            Assertions.assertEquals(0, ProcFileUtils.getProcStatCpuTime(null).size());
            Assertions.assertEquals(0, ProcFileUtils.getProcStatCpuTime(new ArrayList<>()).size());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content =
                "cpu  1172937054 824289701 468234436 75675853861 1569550 49092 35809349 0 0 0\n"
                    + "cpuXYZ \t  1172937055   \t\t   824289702 468234436 75675853861 1569550 49092 35809349 0 0 0\n\r"
                    + "cpu0 62086703 131209370 38265368 3012668347 284193 6311 5544941 0 0 0\n"
                    + "cpu1 67845808 163124717 46602120 2966238934 85123 5536 4311624 0 0 0\n"
                    + "xyz 67845808 163124717 46602120 2966238934 85123 5536 4311624 0 0 0\n"
                    + "foo\n";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            List<String[]> rows = ProcFileUtils.getProcFileAsRowColumn(file.getPath());
            Assertions.assertEquals(7, rows.size());

            List<Map<String, Object>> cpuTimes = ProcFileUtils.getProcStatCpuTime(rows);
            Assertions.assertEquals(4, cpuTimes.size());

            Assertions.assertEquals("cpu", cpuTimes.get(0).get("cpu"));
            Assertions.assertEquals(1172937054L, cpuTimes.get(0).get("user"));
            Assertions.assertEquals(824289701L, cpuTimes.get(0).get("nice"));
            Assertions.assertEquals(468234436L, cpuTimes.get(0).get("system"));
            Assertions.assertEquals(75675853861L, cpuTimes.get(0).get("idle"));
            Assertions.assertEquals(1569550L, cpuTimes.get(0).get("iowait"));

            Assertions.assertEquals("cpuXYZ", cpuTimes.get(1).get("cpu"));
            Assertions.assertEquals(1172937055L, cpuTimes.get(1).get("user"));
            Assertions.assertEquals(824289702L, cpuTimes.get(1).get("nice"));
            Assertions.assertEquals(468234436L, cpuTimes.get(1).get("system"));
            Assertions.assertEquals(75675853861L, cpuTimes.get(1).get("idle"));
            Assertions.assertEquals(1569550L, cpuTimes.get(1).get("iowait"));

            Assertions.assertEquals("cpu0", cpuTimes.get(2).get("cpu"));
            Assertions.assertEquals(62086703L, cpuTimes.get(2).get("user"));
            Assertions.assertEquals(131209370L, cpuTimes.get(2).get("nice"));
            Assertions.assertEquals(38265368L, cpuTimes.get(2).get("system"));
            Assertions.assertEquals(3012668347L, cpuTimes.get(2).get("idle"));
            Assertions.assertEquals(284193L, cpuTimes.get(2).get("iowait"));

            Assertions.assertEquals("cpu1", cpuTimes.get(3).get("cpu"));
            Assertions.assertEquals(67845808L, cpuTimes.get(3).get("user"));
            Assertions.assertEquals(163124717L, cpuTimes.get(3).get("nice"));
            Assertions.assertEquals(46602120L, cpuTimes.get(3).get("system"));
            Assertions.assertEquals(2966238934L, cpuTimes.get(3).get("idle"));
            Assertions.assertEquals(85123L, cpuTimes.get(3).get("iowait"));
        }
    }

    @Test
    void getPid_NoValue() throws IOException {
        File file = File.createTempFile("test", "test");
        file.deleteOnExit();

        String content = "Name:\tcat\t\n" + "VmRSS:	     676 kB \r\n" + "";
        Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

        String result = ProcFileUtils.getPid(file.getPath());
        Assertions.assertNull(result);
    }

    @Test
    void getPid_HasValue() throws IOException {
        File file = File.createTempFile("test", "test");
        file.deleteOnExit();

        String content =
            "Name:\tcat\t\n" + "VmRSS:	     676 kB \r\n" + "\t  Pid \t  : \t 66646 \t\n\r" + "";
        Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

        String result = ProcFileUtils.getPid(file.getPath());
        Assertions.assertEquals("66646", result);
    }

    @Test
    void getCmdline() {
        String result = ProcFileUtils.getCmdline();
        Assertions.assertTrue(result == null || !result.isEmpty());
    }
}
