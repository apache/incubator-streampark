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

package com.uber.profiling.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ProcFileUtilsTest {
    @Test
    public void getProcFileAsMap() throws IOException {
        File file = File.createTempFile("test", "test");
        file.deleteOnExit();

        String content = "Name:\tcat\t\n" 
            + "VmSize:	     776 kB \r\n"
            + "VmPeak:	     876 kB \r\n"
            + "VmRSS:	     676 kB \r\n"
            + "\t  Pid \t  : \t 66646 \t\n\r"
            + "Threads: \t 1 \t\n"
                + "";
        Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

        Map<String, String> result = ProcFileUtils.getProcFileAsMap(file.getPath());
        Assert.assertEquals(6, result.size());
        
        Assert.assertEquals("cat", result.get("Name"));
        Assert.assertEquals("676 kB", result.get("VmRSS"));
        Assert.assertEquals("66646", result.get("Pid"));
        Assert.assertEquals("1", result.get("Threads"));
    }

    @Test
    public void getProcFileAsMap_NotExistingFile() {
        Map<String, String> result = ProcFileUtils.getProcFileAsMap("/not/existing/file");
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void getProcFileAsMap_Directory() {
        Map<String, String> result = ProcFileUtils.getProcFileAsMap("/");
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void getProcFileAsRowColumn_NotExistingFile() {
        List<String[]> result = ProcFileUtils.getProcFileAsRowColumn("/not/existing/file");
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void getProcFileAsRowColumn_Directory() {
        List<String[]> result = ProcFileUtils.getProcFileAsRowColumn("/");
        Assert.assertEquals(0, result.size());
    }
    
    @Test
    public void getProcStatus_DefaultFile() {
        Map<String, String> result = ProcFileUtils.getProcStatus();
        
        // Mac has no proc file so result will be empty.
        // Linux has proc file and result should contain some keys;
        if (result.size() >= 1) {
            Assert.assertTrue(result.containsKey("Pid"));
            Assert.assertTrue(result.containsKey("VmRSS"));
            Assert.assertTrue(result.containsKey("VmSize"));
            Assert.assertTrue(result.containsKey("VmPeak"));
        }
    }
    
    @Test
    public void getProcIO_DefaultFile() {
        Map<String, String> result = ProcFileUtils.getProcIO();

        // Mac has no proc file so result will be empty.
        // Linux has proc file and result should contain some keys;
        if (result.size() >= 1) {
            Assert.assertTrue(result.containsKey("rchar"));
            Assert.assertTrue(result.containsKey("wchar"));
            Assert.assertTrue(result.containsKey("read_bytes"));
            Assert.assertTrue(result.containsKey("write_bytes"));
        }
    }
    
    @Test
    public void getBytesValue() throws IOException {
        {
            Map<String, String> map = Collections.emptyMap();
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assert.assertNull(bytesValue);
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content = "Name:\tcat\t\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assert.assertNull(bytesValue);
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content = "Name:\tcat\t\n"
                    + "VmRSS:	     xxx kB \r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assert.assertNull(bytesValue);
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content = "Name:\tcat\t\n"
                    + "VmRSS:	     676 \r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assert.assertEquals(676L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content = "Name:\tcat\t\n"
                    + "VmRSS:	     676 kB \r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assert.assertEquals(676 * 1024L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content = "Name:\tcat\t\n"
                    + "VmRSS:	    \t 676 \t kB \t\r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assert.assertEquals(676 * 1024L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content = "Name:\tcat\t\n"
                    + "VmRSS:	    \t 676 KB \t\r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assert.assertEquals(676 * 1024L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content = "Name:\tcat\t\n"
                    + "VmRSS:	    \t 676KB \t\r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assert.assertEquals(676 * 1024L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content = "Name:\tcat\t\n"
                    + "VmRSS:676KB \t\r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assert.assertEquals(676 * 1024L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content = "Name:\tcat\t\n"
                    + "VmRSS:	    \t 676 mB \t\r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assert.assertEquals(676 * 1024 * 1024L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content = "Name:\tcat\t\n"
                    + "VmRSS:	    \t 676 MB \t\r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assert.assertEquals(676 * 1024 * 1024L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content = "Name:\tcat\t\n"
                    + "VmRSS:	    \t 676 gB \t\r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assert.assertEquals(676 * 1024 * 1024 * 1024L, bytesValue.longValue());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content = "Name:\tcat\t\n"
                    + "VmRSS:	    \t 676 GB \t\r\n"
                    + "Pid: \t 66646 \t\n\r"
                    + "Threads: \t 1 \t\n"
                    + "";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            Map<String, String> map = ProcFileUtils.getProcFileAsMap(file.getPath());
            Long bytesValue = ProcFileUtils.getBytesValue(map, "VmRSS");
            Assert.assertEquals(676 * 1024 * 1024 * 1024L, bytesValue.longValue());
        }
    }

    @Test
    public void getProcStatCpuTime() throws IOException {
        {
            Assert.assertEquals(0, ProcFileUtils.getProcStatCpuTime(null).size());
            Assert.assertEquals(0, ProcFileUtils.getProcStatCpuTime(new ArrayList<>()).size());
        }
        {
            File file = File.createTempFile("test", "test");
            file.deleteOnExit();

            String content = "cpu  1172937054 824289701 468234436 75675853861 1569550 49092 35809349 0 0 0\n"
                    + "cpuXYZ \t  1172937055   \t\t   824289702 468234436 75675853861 1569550 49092 35809349 0 0 0\n\r"
                    + "cpu0 62086703 131209370 38265368 3012668347 284193 6311 5544941 0 0 0\n"
                    + "cpu1 67845808 163124717 46602120 2966238934 85123 5536 4311624 0 0 0\n"
                    + "xyz 67845808 163124717 46602120 2966238934 85123 5536 4311624 0 0 0\n"
                    + "foo\n";
            Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

            List<String[]> rows = ProcFileUtils.getProcFileAsRowColumn(file.getPath());
            Assert.assertEquals(7, rows.size());

            List<Map<String, Object>> cpuTimes = ProcFileUtils.getProcStatCpuTime(rows);
            Assert.assertEquals(4, cpuTimes.size());
            
            Assert.assertEquals("cpu", cpuTimes.get(0).get("cpu"));
            Assert.assertEquals(1172937054L, cpuTimes.get(0).get("user"));
            Assert.assertEquals(824289701L, cpuTimes.get(0).get("nice"));
            Assert.assertEquals(468234436L, cpuTimes.get(0).get("system"));
            Assert.assertEquals(75675853861L, cpuTimes.get(0).get("idle"));
            Assert.assertEquals(1569550L, cpuTimes.get(0).get("iowait"));

            Assert.assertEquals("cpuXYZ", cpuTimes.get(1).get("cpu"));
            Assert.assertEquals(1172937055L, cpuTimes.get(1).get("user"));
            Assert.assertEquals(824289702L, cpuTimes.get(1).get("nice"));
            Assert.assertEquals(468234436L, cpuTimes.get(1).get("system"));
            Assert.assertEquals(75675853861L, cpuTimes.get(1).get("idle"));
            Assert.assertEquals(1569550L, cpuTimes.get(1).get("iowait"));

            Assert.assertEquals("cpu0", cpuTimes.get(2).get("cpu"));
            Assert.assertEquals(62086703L, cpuTimes.get(2).get("user"));
            Assert.assertEquals(131209370L, cpuTimes.get(2).get("nice"));
            Assert.assertEquals(38265368L, cpuTimes.get(2).get("system"));
            Assert.assertEquals(3012668347L, cpuTimes.get(2).get("idle"));
            Assert.assertEquals(284193L, cpuTimes.get(2).get("iowait"));

            Assert.assertEquals("cpu1", cpuTimes.get(3).get("cpu"));
            Assert.assertEquals(67845808L, cpuTimes.get(3).get("user"));
            Assert.assertEquals(163124717L, cpuTimes.get(3).get("nice"));
            Assert.assertEquals(46602120L, cpuTimes.get(3).get("system"));
            Assert.assertEquals(2966238934L, cpuTimes.get(3).get("idle"));
            Assert.assertEquals(85123L, cpuTimes.get(3).get("iowait"));
        }
    }

    @Test
    public void getPid_NoValue() throws IOException {
        File file = File.createTempFile("test", "test");
        file.deleteOnExit();

        String content = "Name:\tcat\t\n"
                + "VmRSS:	     676 kB \r\n"
                + "";
        Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

        String result = ProcFileUtils.getPid(file.getPath());
        Assert.assertEquals(null, result);
    }

    @Test
    public void getPid_HasValue() throws IOException {
        File file = File.createTempFile("test", "test");
        file.deleteOnExit();

        String content = "Name:\tcat\t\n"
                + "VmRSS:	     676 kB \r\n"
                + "\t  Pid \t  : \t 66646 \t\n\r"
                + "";
        Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);

        String result = ProcFileUtils.getPid(file.getPath());
        Assert.assertEquals("66646", result);
    }

    @Test
    public void getCmdline() throws IOException {
        String result = ProcFileUtils.getCmdline();
        Assert.assertTrue(result == null || !result.isEmpty());
    }
}
