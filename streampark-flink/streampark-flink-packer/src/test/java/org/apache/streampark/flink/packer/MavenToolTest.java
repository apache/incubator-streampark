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

package org.apache.streampark.flink.packer;

import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.flink.packer.maven.Artifact;
import org.apache.streampark.flink.packer.maven.DependencyInfo;
import org.apache.streampark.flink.packer.maven.MavenTool;

import org.apache.commons.io.FileUtils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenToolTest {

    private static final String OUTPUT_DIR = "MavenToolSpec-output/";
    private static final Path JAR_DIR = Paths.get(OUTPUT_DIR, "jars");
    private static String jar1Path;
    private static String jar2Path;
    private static String jar3Path;

    @BeforeAll
    static void beforeAll() throws Exception {
        FileUtils.forceMkdir(new File(Workspace.MAVEN_LOCAL_PATH()));
        File output = new File(OUTPUT_DIR);
        FileUtils.deleteDirectory(output);
        FileUtils.forceMkdir(output);
        FileUtils.forceMkdir(JAR_DIR.toFile());

        jar1Path =
            PackerTestUtils
                .createJar(
                    JAR_DIR.resolve("commons-cli-1.4.jar"),
                    "org/apache/commons/cli/DefaultParser.class",
                    new byte[]{1, 2, 3})
                .toString();
        jar2Path =
            PackerTestUtils
                .createJar(
                    JAR_DIR.resolve("commons-dbutils-1.7.jar"),
                    "org/apache/commons/dbutils/DbUtils.class",
                    new byte[]{4, 5, 6})
                .toString();
        jar3Path =
            PackerTestUtils
                .createJar(
                    JAR_DIR.resolve("commons-logging-1.2.jar"),
                    "org/apache/commons/logging/Log.class",
                    new byte[]{7, 8, 9})
                .toString();
    }

    @AfterAll
    static void afterAll() throws Exception {
        FileUtils.deleteDirectory(new File(OUTPUT_DIR));
    }

    @Test
    void buildFatJarWithJarLibs() throws Exception {
        String fatJarPath = OUTPUT_DIR.concat("fat-1.jar");
        File fatJar =
            MavenTool.buildFatJar(null, new HashSet<>(Arrays.asList(jar1Path, jar2Path)), fatJarPath);
        assertTrue(fatJar.exists());
        assertTrue(
            PackerTestUtils.jarEquals(
                new JarFile(fatJarPath), new JarFile(jar1Path), "org/apache/commons/cli/DefaultParser.class"));
        assertTrue(
            PackerTestUtils.jarEquals(
                new JarFile(fatJarPath), new JarFile(jar2Path), "org/apache/commons/dbutils/DbUtils.class"));
    }

    @Test
    void buildFatJarWithJarLibsUnderDirectory() throws Exception {
        String fatJarPath = OUTPUT_DIR.concat("fat-2.jar");
        File fatJar = MavenTool.buildFatJar(null, Collections.singleton(JAR_DIR.toString()), fatJarPath);
        assertTrue(fatJar.exists());
        assertTrue(
            PackerTestUtils.jarEquals(
                new JarFile(fatJarPath), new JarFile(jar1Path), "org/apache/commons/cli/DefaultParser.class"));
        assertTrue(
            PackerTestUtils.jarEquals(
                new JarFile(fatJarPath), new JarFile(jar2Path), "org/apache/commons/dbutils/DbUtils.class"));
        assertTrue(
            PackerTestUtils.jarEquals(
                new JarFile(fatJarPath), new JarFile(jar3Path), "org/apache/commons/logging/Log.class"));
    }

    @Test
    void buildFatJarWithJarLibsAndMavenArtifacts() throws Exception {
        String fatJarPath = OUTPUT_DIR.concat("fat-3.jar");
        File fatJar =
            MavenTool.buildFatJar(
                null,
                new DependencyInfo(
                    Collections.singleton(
                        Artifact.of("org.apache.flink:flink-connector-kafka_2.11:1.13.0")),
                    Collections.singleton(jar2Path)),
                fatJarPath);
        assertTrue(fatJar.exists());
        assertTrue(
            PackerTestUtils.jarEquals(
                new JarFile(fatJarPath), new JarFile(jar2Path), "org/apache/commons/dbutils/DbUtils.class"));
        assertNotNull(new JarFile(fatJarPath).getJarEntry("org/apache/kafka/clients/ClientUtils.class"));
        assertNotNull(
            new JarFile(fatJarPath)
                .getJarEntry("org/apache/flink/connector/base/source/reader/SourceReaderBase.class"));
    }

    @Test
    void resolveSingleArtifact() throws Exception {
        List<File> jars =
            MavenTool.resolveArtifacts(
                Collections.singleton(
                    Artifact.of("org.apache.flink:flink-connector-kafka_2.11:1.13.0")));
        String[] expectJars = {
                "flink-connector-kafka_2.11-1.13.0.jar",
                "flink-connector-base-1.13.0.jar",
                "kafka-clients-2.4.1.jar"
        };
        assertNotEquals(0, jars.size());
        assertTrue(jars.stream().allMatch(File::exists));
        assertEquals(
            new HashSet<>(Arrays.asList(expectJars)),
            jars.stream().map(File::getName).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void resolveMultipleArtifacts() throws Exception {
        List<File> jars =
            MavenTool.resolveArtifacts(
                new HashSet<>(
                    Arrays.asList(
                        Artifact.of("org.apache.flink:flink-connector-kafka_2.11:1.13.0"),
                        Artifact.of("org.apache.flink:flink-connector-base:1.13.0"))));
        String[] expectJars = {
                "flink-connector-kafka_2.11-1.13.0.jar",
                "flink-core-1.13.0.jar",
                "flink-connector-base-1.13.0.jar",
                "kafka-clients-2.4.1.jar"
        };
        assertTrue(jars.stream().allMatch(File::exists));
        assertEquals(
            new HashSet<>(Arrays.asList(expectJars)),
            jars.stream().map(File::getName).collect(java.util.stream.Collectors.toSet()));
    }
}
