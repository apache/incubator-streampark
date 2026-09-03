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

package org.apache.streampark.console.core.util;

import org.apache.streampark.common.util.FlinkConfigurationUtils;
import org.apache.streampark.console.core.entity.FlinkEnv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies version-aware YAML handling at the console persistence boundary. */
class FlinkEnvUtilsTest {

    @TempDir
    Path flinkHome;

    @Test
    void preserveFlink120YamlSelection() throws Exception {
        Path configurationDirectory = Files.createDirectories(flinkHome.resolve("conf"));
        Path legacyFile =
            configurationDirectory.resolve(
                FlinkConfigurationUtils.LEGACY_FLINK_CONF_FILENAME);
        Files.writeString(legacyFile, "pipeline.name: legacy\n");
        Files.writeString(
            configurationDirectory.resolve(FlinkConfigurationUtils.FLINK_CONF_FILENAME),
            "pipeline:\n  name: standard\n");
        FlinkEnv environment = environment("1.20.0");

        FlinkEnvUtils.sync(environment);

        assertThat(FlinkEnvUtils.isStandardYaml(environment)).isFalse();
        assertThat(FlinkEnvUtils.configuration(environment))
            .containsEntry("pipeline.name", "legacy");

        Files.delete(legacyFile);
        assertThat(FlinkEnvUtils.isStandardYaml(environment)).isFalse();
        assertThat(FlinkEnvUtils.configuration(environment))
            .containsEntry("pipeline.name", "legacy");

        FlinkEnvUtils.sync(environment);

        assertThat(FlinkEnvUtils.isStandardYaml(environment)).isTrue();
        assertThat(FlinkEnvUtils.configuration(environment))
            .containsEntry("pipeline.name", "standard");
    }

    @Test
    void refreshChangesYamlFormat() throws Exception {
        Path legacyHome = createFlinkHome("flink-1.18", "1.18.2", "flink-conf.yaml");
        Path standardHome = createFlinkHome("flink-2.3", "2.3.0", "config.yaml");
        FlinkEnv environment = new FlinkEnv();

        FlinkEnvUtils.refresh(environment, legacyHome.toString());
        assertThat(environment.getVersion()).isEqualTo("1.18.2");
        assertThat(FlinkEnvUtils.isStandardYaml(environment)).isFalse();

        FlinkEnvUtils.refresh(environment, standardHome.toString());
        assertThat(environment.getVersion()).isEqualTo("2.3.0");
        assertThat(FlinkEnvUtils.isStandardYaml(environment)).isTrue();
    }

    private Path createFlinkHome(String directory, String version, String configFile) throws Exception {
        Path home = Files.createDirectories(flinkHome.resolve(directory));
        Path lib = Files.createDirectories(home.resolve("lib"));
        Path conf = Files.createDirectories(home.resolve("conf"));
        Files.createFile(lib.resolve("flink-dist-" + version + ".jar"));
        String yaml =
            FlinkConfigurationUtils.LEGACY_FLINK_CONF_FILENAME.equals(configFile)
                ? "parallelism.default: 1\n"
                : "parallelism:\n  default: 1\n";
        Files.writeString(conf.resolve(configFile), yaml);
        return home;
    }

    private FlinkEnv environment(String version) {
        FlinkEnv environment = new FlinkEnv();
        environment.setFlinkHome(flinkHome.toString());
        environment.setVersion(version);
        return environment;
    }
}
