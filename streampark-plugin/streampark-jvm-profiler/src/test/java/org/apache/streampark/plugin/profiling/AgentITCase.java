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

package org.apache.streampark.plugin.profiling;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class AgentITCase {

    @Test
    void runAgent() throws InterruptedException, IOException {
        String javaHome = System.getProperty("java.home");
        String javaBin = Paths.get(javaHome, "bin/java").toAbsolutePath().toString();

        String agentJar = getAgentJarPath();

        String outputDir = Files.createTempDirectory("jvm_profiler_test_output").toString();
        System.out.println("outputDir: " + outputDir);

        ProcessBuilder pb =
            new ProcessBuilder(
                javaBin,
                "-cp",
                agentJar,
                "-javaagent:"
                    + agentJar
                    +
                    "=configProvider=org.apache.streampark.plugin.profiling.util.DummyConfigProvider,reporter=org.apache.streampark.plugin.profiling.reporter.FileOutputReporter,outputDir="
                    + outputDir
                    +
                    ",tag=mytag,metricInterval=200,durationProfiling=org.apache.streampark.plugin.profiling.example.HelloWorldApplication.publicSleepMethod,argumentProfiling=org.apache.streampark.plugin.profiling.example.HelloWorldApplication.publicSleepMethod.1,ioProfiling=true",
                "org.apache.streampark.plugin.profiling.example.HelloWorldApplication",
                "2000");

        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();
        process.waitFor();

        File[] files = new File(outputDir).listFiles();
        Assertions.assertEquals(5, files.length);

        List<String> fileNames =
            Arrays.stream(files).map(File::getName).sorted().collect(Collectors.toList());

        Assertions.assertEquals("CpuAndMemory.json", fileNames.get(0));
        String jsonCpuAndMemory =
            new String(Files.readAllBytes(Paths.get(outputDir, fileNames.get(0))));
        System.out.println("-----CpuAndMemory-----");
        System.out.println(jsonCpuAndMemory);
        Assertions.assertTrue(jsonCpuAndMemory.contains("bufferPool"));

        Assertions.assertEquals("IO.json", fileNames.get(1));
        String jsonProcFileSystem =
            new String(Files.readAllBytes(Paths.get(outputDir, fileNames.get(1))));
        System.out.println("-----IO-----");
        System.out.println(jsonProcFileSystem);
        Assertions.assertTrue(jsonProcFileSystem.contains("read_bytes"));
        Assertions.assertTrue(jsonProcFileSystem.contains("write_bytes"));

        Assertions.assertEquals("MethodArgument.json", fileNames.get(2));
        String jsonMethodArgument =
            new String(Files.readAllBytes(Paths.get(outputDir, fileNames.get(2))));
        System.out.println("-----MethodArgument-----");
        System.out.println(jsonMethodArgument);
        Assertions.assertTrue(jsonMethodArgument.contains("arg.1"));

        Assertions.assertEquals("MethodDuration.json", fileNames.get(3));
        String jsonMethodDuration =
            new String(Files.readAllBytes(Paths.get(outputDir, fileNames.get(3))));
        System.out.println("-----MethodDuration-----");
        System.out.println(jsonMethodDuration);
        Assertions.assertTrue(jsonMethodDuration.contains("duration.sum"));

        Assertions.assertEquals("ProcessInfo.json", fileNames.get(4));
        String jsonProcessInfo = new String(Files.readAllBytes(Paths.get(outputDir, fileNames.get(4))));
        System.out.println("-----ProcessInfo-----");
        System.out.println(jsonProcessInfo);
        Assertions.assertTrue(jsonProcessInfo.contains("jvmClassPath"));
        Assertions.assertTrue(jsonProcessInfo.contains(agentJar));
    }

    @Test
    void runAgent_noop() throws InterruptedException, IOException {
        String javaHome = System.getProperty("java.home");
        String javaBin = Paths.get(javaHome, "bin/java").toAbsolutePath().toString();

        String agentJar = getAgentJarPath();

        String outputDir = Files.createTempDirectory("jvm_profiler_test_output").toString();
        System.out.println("outputDir: " + outputDir);

        ProcessBuilder pb =
            new ProcessBuilder(
                javaBin,
                "-cp",
                agentJar,
                "-javaagent:"
                    + agentJar
                    +
                    "=noop=true,configProvider=org.apache.streampark.plugin.profiling.util.DummyConfigProvider,reporter=org.apache.streampark.plugin.profiling.reporter.FileOutputReporter,outputDir="
                    + outputDir
                    +
                    ",tag=mytag,metricInterval=200,durationProfiling=org.apache.streampark.plugin.profiling.example.HelloWorldApplication.publicSleepMethod,argumentProfiling=org.apache.streampark.plugin.profiling.example.HelloWorldApplication.publicSleepMethod.1,ioProfiling=true",
                "org.apache.streampark.plugin.profiling.example.HelloWorldApplication",
                "2000");

        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();
        process.waitFor();

        File[] files = new File(outputDir).listFiles();
        Assertions.assertEquals(0, files.length);
    }

    @Test
    void runAgent_noopConfigProvider() throws InterruptedException, IOException {
        String javaHome = System.getProperty("java.home");
        String javaBin = Paths.get(javaHome, "bin/java").toAbsolutePath().toString();

        String agentJar = getAgentJarPath();

        String outputDir = Files.createTempDirectory("jvm_profiler_test_output").toString();
        System.out.println("outputDir: " + outputDir);

        ProcessBuilder pb =
            new ProcessBuilder(
                javaBin,
                "-cp",
                agentJar,
                "-javaagent:"
                    + agentJar
                    +
                    "=configProvider=org.apache.streampark.plugin.profiling.util.NoopConfigProvider,reporter=org.apache.streampark.plugin.profiling.reporter.FileOutputReporter,outputDir="
                    + outputDir
                    +
                    ",tag=mytag,metricInterval=200,durationProfiling=org.apache.streampark.plugin.profiling.example.HelloWorldApplication.publicSleepMethod,argumentProfiling=org.apache.streampark.plugin.profiling.example.HelloWorldApplication.publicSleepMethod.1,ioProfiling=true",
                "org.apache.streampark.plugin.profiling.example.HelloWorldApplication",
                "2000");

        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();
        process.waitFor();

        File[] files = new File(outputDir).listFiles();
        Assertions.assertEquals(0, files.length);
    }

    @Test
    void runAgent_argumentProfilingZero() throws InterruptedException, IOException {
        String javaHome = System.getProperty("java.home");
        String javaBin = Paths.get(javaHome, "bin/java").toAbsolutePath().toString();

        String agentJar = getAgentJarPath();

        String outputDir = Files.createTempDirectory("jvm_profiler_test_output").toString();
        System.out.println("outputDir: " + outputDir);

        ProcessBuilder pb =
            new ProcessBuilder(
                javaBin,
                "-cp",
                agentJar,
                "-javaagent:"
                    + agentJar
                    +
                    "=configProvider=org.apache.streampark.plugin.profiling.util.DummyConfigProvider,reporter=org.apache.streampark.plugin.profiling.reporter.FileOutputReporter,outputDir="
                    + outputDir
                    +
                    ",tag=mytag,metricInterval=200,durationProfiling=org.apache.streampark.plugin.profiling.example.HelloWorldApplication.publicSleepMethod,argumentProfiling=org.apache.streampark.plugin.profiling.example.HelloWorldApplication.publicSleepMethod.0",
                "org.apache.streampark.plugin.profiling.example.HelloWorldApplication",
                "2000");

        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();
        process.waitFor();

        File[] files = new File(outputDir).listFiles();
        Assertions.assertEquals(4, files.length);

        List<String> fileNames =
            Arrays.stream(files).map(File::getName).sorted().collect(Collectors.toList());

        Assertions.assertEquals("CpuAndMemory.json", fileNames.get(0));
        String jsonCpuAndMemory =
            new String(Files.readAllBytes(Paths.get(outputDir, fileNames.get(0))));
        System.out.println("-----CpuAndMemory-----");
        System.out.println(jsonCpuAndMemory);
        Assertions.assertTrue(jsonCpuAndMemory.contains("bufferPool"));

        Assertions.assertEquals("MethodArgument.json", fileNames.get(1));
        String jsonMethodArgument =
            new String(Files.readAllBytes(Paths.get(outputDir, fileNames.get(1))));
        System.out.println("-----MethodArgument-----");
        System.out.println(jsonMethodArgument);
        Assertions.assertTrue(jsonMethodArgument.contains("arg.0"));

        Assertions.assertEquals("MethodDuration.json", fileNames.get(2));
        String jsonMethodDuration =
            new String(Files.readAllBytes(Paths.get(outputDir, fileNames.get(2))));
        System.out.println("-----MethodDuration-----");
        System.out.println(jsonMethodDuration);
        Assertions.assertTrue(jsonMethodDuration.contains("duration.sum"));

        Assertions.assertEquals("ProcessInfo.json", fileNames.get(3));
        String jsonProcessInfo = new String(Files.readAllBytes(Paths.get(outputDir, fileNames.get(3))));
        System.out.println("-----ProcessInfo-----");
        System.out.println(jsonProcessInfo);
        Assertions.assertTrue(jsonProcessInfo.contains("jvmClassPath"));
        Assertions.assertTrue(jsonProcessInfo.contains(agentJar));
    }

    @Test
    void runAgent_appIdVariable() throws InterruptedException, IOException {
        String javaHome = System.getProperty("java.home");
        String javaBin = Paths.get(javaHome, "bin/java").toAbsolutePath().toString();

        String agentJar = getAgentJarPath();

        String outputDir = Files.createTempDirectory("jvm_profiler_test_output").toString();
        System.out.println("outputDir: " + outputDir);

        ProcessBuilder pb =
            new ProcessBuilder(
                javaBin,
                "-cp",
                agentJar,
                "-javaagent:"
                    + agentJar
                    +
                    "=configProvider=org.apache.streampark.plugin.profiling.util.DummyConfigProvider,reporter=org.apache.streampark.plugin.profiling.reporter.FileOutputReporter,outputDir="
                    + outputDir
                    + ",tag=mytag,appIdVariable=APP_ID",
                "org.apache.streampark.plugin.profiling.example.HelloWorldApplication",
                "2000");

        pb.environment().put("APP_ID", "TEST_APP_ID_123_ABC");

        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = pb.start();
        process.waitFor();

        File[] files = new File(outputDir).listFiles();
        Assertions.assertEquals(2, files.length);

        List<String> fileNames =
            Arrays.stream(files).map(File::getName).sorted().collect(Collectors.toList());

        Assertions.assertEquals("CpuAndMemory.json", fileNames.get(0));
        String jsonCpuAndMemory =
            new String(Files.readAllBytes(Paths.get(outputDir, fileNames.get(0))));
        System.out.println("-----CpuAndMemory-----");
        System.out.println(jsonCpuAndMemory);
        Assertions.assertTrue(jsonCpuAndMemory.contains("TEST_APP_ID_123_ABC"));

        Assertions.assertEquals("ProcessInfo.json", fileNames.get(1));
        String jsonProcessInfo = new String(Files.readAllBytes(Paths.get(outputDir, fileNames.get(1))));
        System.out.println("-----ProcessInfo-----");
        System.out.println(jsonProcessInfo);
        Assertions.assertTrue(jsonProcessInfo.contains("TEST_APP_ID_123_ABC"));
    }

    private String getAgentJarPath() throws IOException {
        // Find jar file with largest size under target directory, which should be the packaged agent
        // jar file
        String agentJar =
            Files.list(Paths.get("target"))
                .max(
                    Comparator.comparingLong(
                        t -> {
                            try {
                                return Files.size(t);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }))
                .map(Path::toString)
                .filter(t -> t.endsWith(".jar"))
                .get();
        System.out.println("agentJar: " + agentJar);
        return agentJar;
    }
}
