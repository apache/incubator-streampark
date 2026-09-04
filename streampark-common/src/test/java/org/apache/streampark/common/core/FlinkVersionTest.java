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

package org.apache.streampark.common.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FlinkVersionTest {

    @Test
    void matchSupportedShimsRange(@TempDir Path temporaryDirectory) throws IOException {
        assertThat(flinkVersion(temporaryDirectory, "1.17.2").checkVersion(false)).isFalse();
        assertThat(flinkVersion(temporaryDirectory, "1.18.0").checkVersion(false)).isTrue();
        assertThat(flinkVersion(temporaryDirectory, "1.20.1").checkVersion(false)).isTrue();
        assertThat(flinkVersion(temporaryDirectory, "2.0.0").checkVersion(false)).isTrue();
        assertThat(flinkVersion(temporaryDirectory, "2.3.0").checkVersion(false)).isTrue();
        assertThat(flinkVersion(temporaryDirectory, "2.4.0").checkVersion(false)).isFalse();
    }

    private static FlinkVersion flinkVersion(Path temporaryDirectory, String version) throws IOException {
        Path flinkHome = temporaryDirectory.resolve("flink-" + version);
        Path libDirectory = Files.createDirectories(flinkHome.resolve("lib"));
        Files.createFile(libDirectory.resolve("flink-dist-" + version + ".jar"));
        return new FlinkVersion(flinkHome.toString());
    }
}
