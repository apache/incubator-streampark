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

package org.apache.streampark.common.conf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SparkVersionTest {

    @TempDir
    Path tempDir;

    @Test
    void parseSparkVersionFromJarWithoutRunningSparkSubmit() throws Exception {
        File sparkHome = tempDir.resolve("spark-4.1.2").toFile();
        File jarsDir = new File(sparkHome, "jars");
        assertThat(jarsDir.mkdirs()).isTrue();
        assertThat(new File(jarsDir, "spark-core_2.13-4.1.2.jar").createNewFile()).isTrue();
        Files.writeString(
            new File(sparkHome, "RELEASE").toPath(),
            "Spark 4.1.2 (git revision f0bb2e6a47d) built for Hadoop 3.4.2\n",
            StandardCharsets.UTF_8);

        SparkVersion sparkVersion = new SparkVersion(sparkHome.getAbsolutePath());

        assertThat(sparkVersion.getVersion()).isEqualTo("4.1.2");
        assertThat(sparkVersion.getScalaVersion()).isEqualTo("2.13");
        assertThat(sparkVersion.getMajorVersion()).isEqualTo("4.1");
        assertThat(sparkVersion.checkVersion(false)).isTrue();
    }

    @Test
    void parseSpark35FromJar() throws Exception {
        File sparkHome = tempDir.resolve("spark-3.5.4").toFile();
        File jarsDir = new File(sparkHome, "jars");
        assertThat(jarsDir.mkdirs()).isTrue();
        assertThat(new File(jarsDir, "spark-core_2.12-3.5.4.jar").createNewFile()).isTrue();

        SparkVersion sparkVersion = new SparkVersion(sparkHome.getAbsolutePath());

        assertThat(sparkVersion.getVersion()).isEqualTo("3.5.4");
        assertThat(sparkVersion.getScalaVersion()).isEqualTo("2.12");
        assertThat(sparkVersion.checkVersion(false)).isTrue();
    }

    @Test
    void rejectSpark34() throws Exception {
        File sparkHome = tempDir.resolve("spark-3.4.0").toFile();
        File jarsDir = new File(sparkHome, "jars");
        assertThat(jarsDir.mkdirs()).isTrue();
        assertThat(new File(jarsDir, "spark-core_2.12-3.4.0.jar").createNewFile()).isTrue();

        SparkVersion sparkVersion = new SparkVersion(sparkHome.getAbsolutePath());

        assertThat(sparkVersion.getVersion()).isEqualTo("3.4.0");
        assertThat(sparkVersion.checkVersion(false)).isFalse();
    }
}
