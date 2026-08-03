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

import org.apache.streampark.flink.packer.docker.FlinkDockerfileTemplate;

import org.apache.commons.io.FileUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlinkDockerfileTemplateTest {

    private final File outputDir = new File("FlinkDockerfileTemplateSpec-output/");
    private String mainJarPath;

    @BeforeEach
    void setUp() throws Exception {
        outputDir.mkdir();
        mainJarPath =
            PackerTestUtils
                .createJar(
                    Paths.get(outputDir.getAbsolutePath(), "WordCountSQL.jar"),
                    "org/apache/flink/WordCountSQL.class",
                    new byte[]{1})
                .toString();
    }

    @AfterEach
    void tearDown() throws Exception {
        FileUtils.forceDelete(outputDir);
    }

    @Test
    void buildDockerfileContent() {
        FlinkDockerfileTemplate template =
            new FlinkDockerfileTemplate(
                outputDir.getAbsolutePath(), "1.13-scala_2.11", mainJarPath, Collections.emptySet());
        String expected =
            "FROM 1.13-scala_2.11\n"
                + "RUN mkdir -p $FLINK_HOME/usrlib\n"
                + "COPY lib $FLINK_HOME/lib/\n"
                + "COPY WordCountSQL.jar $FLINK_HOME/usrlib/WordCountSQL.jar\n";
        assertEquals(expected, template.offerDockerfileContent());
    }

    @Test
    void writeDockerfileToFile() throws Exception {
        FlinkDockerfileTemplate template =
            new FlinkDockerfileTemplate(
                outputDir.getAbsolutePath(), "1.13-scala_2.11", mainJarPath, Collections.emptySet());
        String expected =
            "FROM 1.13-scala_2.11\n"
                + "RUN mkdir -p $FLINK_HOME/usrlib\n"
                + "COPY lib $FLINK_HOME/lib/\n"
                + "COPY WordCountSQL.jar $FLINK_HOME/usrlib/WordCountSQL.jar\n";
        File outFile = template.writeDockerfile();
        assertEquals("Dockerfile", outFile.getName());
        assertEquals(expected, FileUtils.readFileToString(outFile, "UTF-8"));
    }

    @Test
    void writeDockerfileWithSpecialName() throws Exception {
        FlinkDockerfileTemplate template =
            new FlinkDockerfileTemplate(
                outputDir.getAbsolutePath(), "1.13-scala_2.11", mainJarPath, Collections.emptySet());
        String expected =
            "FROM 1.13-scala_2.11\n"
                + "RUN mkdir -p $FLINK_HOME/usrlib\n"
                + "COPY lib $FLINK_HOME/lib/\n"
                + "COPY WordCountSQL.jar $FLINK_HOME/usrlib/WordCountSQL.jar\n";
        File outFile = template.writeDockerfile("Dockerfile");
        assertEquals("Dockerfile", outFile.getName());
        assertEquals(expected, FileUtils.readFileToString(outFile, "UTF-8"));
    }
}
