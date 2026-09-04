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

package org.apache.streampark.common.util;

import org.apache.streampark.common.configuration.ConfigException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifies version-aware loading of Flink's legacy and standard YAML formats. */
class FlinkConfigurationUtilsTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void loadNestedYamlAndPreserveLists() throws Exception {
        Path configFile = temporaryDirectory.resolve(FlinkConfigurationLoader.FLINK_CONF_FILENAME);
        Files.writeString(
            configFile,
            "env:\n"
                + "  java:\n"
                + "    opts:\n"
                + "      all: --add-opens=java.base/java.util=ALL-UNNAMED\n"
                + "high-availability:\n"
                + "  namespaces: [primary, secondary]\n");

        Map<String, String> configuration =
            FlinkConfigurationLoader.loadConfigurationFromFile(configFile.toFile());

        assertThat(configuration.get("env.java.opts.all"))
            .isEqualTo("--add-opens=java.base/java.util=ALL-UNNAMED");
        assertThat(configuration.get("high-availability.namespaces"))
            .isEqualTo("[primary, secondary]");
    }

    @Test
    void selectOnlyLegacyYamlBeforeFlink119() throws Exception {
        Files.writeString(
            temporaryDirectory.resolve(FlinkConfigurationLoader.LEGACY_FLINK_CONF_FILENAME),
            "pipeline.name: legacy\n");
        Files.writeString(
            temporaryDirectory.resolve(FlinkConfigurationLoader.FLINK_CONF_FILENAME),
            "pipeline:\n  name: standard\n");

        Map<String, String> configuration =
            FlinkConfigurationLoader.loadConfiguration(
                temporaryDirectory.toString(), "1.18.2");

        assertThat(FlinkConfigurationLoader.usesStandardYaml(
            temporaryDirectory.toString(), "1.18.2")).isFalse();
        assertThat(configuration.get("pipeline.name")).isEqualTo("legacy");
    }

    @Test
    void selectFlink119YamlByPrecedence() throws Exception {
        Path legacyFile =
            temporaryDirectory.resolve(FlinkConfigurationLoader.LEGACY_FLINK_CONF_FILENAME);
        Files.writeString(legacyFile, "pipeline.name: legacy\n");
        Files.writeString(
            temporaryDirectory.resolve(FlinkConfigurationLoader.FLINK_CONF_FILENAME),
            "pipeline:\n  name: standard\n");

        assertThat(FlinkConfigurationLoader.usesStandardYaml(
            temporaryDirectory.toString(), "1.19.0")).isFalse();
        assertThat(
            FlinkConfigurationLoader.loadConfiguration(
                temporaryDirectory.toString(), "1.19.0")
                .get("pipeline.name"))
                    .isEqualTo("legacy");

        Files.delete(legacyFile);

        assertThat(FlinkConfigurationLoader.usesStandardYaml(
            temporaryDirectory.toString(), "1.19.1")).isTrue();
        assertThat(
            FlinkConfigurationLoader.loadConfiguration(
                temporaryDirectory.toString(), "1.19.1")
                .get("pipeline.name"))
                    .isEqualTo("standard");
    }

    @Test
    void supportLegacyThroughFlink120() throws Exception {
        Path legacyFile =
            temporaryDirectory.resolve(FlinkConfigurationLoader.LEGACY_FLINK_CONF_FILENAME);
        Files.writeString(legacyFile, "pipeline.name: legacy\n");
        Files.writeString(
            temporaryDirectory.resolve(FlinkConfigurationLoader.FLINK_CONF_FILENAME),
            "pipeline:\n  name: standard\n");

        assertThat(FlinkConfigurationLoader.usesStandardYaml(
            temporaryDirectory.toString(), "1.20.0")).isFalse();
        assertThat(
            FlinkConfigurationLoader.loadConfiguration(
                temporaryDirectory.toString(), "1.20.0")
                .get("pipeline.name"))
                    .isEqualTo("legacy");

        Files.delete(legacyFile);

        assertThat(FlinkConfigurationLoader.usesStandardYaml(
            temporaryDirectory.toString(), "1.20.3")).isTrue();
        assertThat(
            FlinkConfigurationLoader.loadConfiguration(
                temporaryDirectory.toString(), "1.20.3")
                .get("pipeline.name"))
                    .isEqualTo("standard");
    }

    @Test
    void requireConfigYamlFromFlink2() throws Exception {
        Files.writeString(
            temporaryDirectory.resolve(FlinkConfigurationLoader.LEGACY_FLINK_CONF_FILENAME),
            "pipeline.name: legacy\n");

        assertThatThrownBy(
            () -> FlinkConfigurationLoader.loadConfiguration(
                temporaryDirectory.toString(), "2.0.0"))
                    .isInstanceOf(ConfigException.class)
                    .hasMessageContaining(FlinkConfigurationLoader.FLINK_CONF_FILENAME);

        Files.writeString(
            temporaryDirectory.resolve(FlinkConfigurationLoader.FLINK_CONF_FILENAME),
            "pipeline:\n  name: standard\n");

        assertThat(FlinkConfigurationLoader.usesStandardYaml(
            temporaryDirectory.toString(), "2.0.0")).isTrue();
        assertThat(
            FlinkConfigurationLoader.loadConfiguration(
                temporaryDirectory.toString(), "2.0.0")
                .get("pipeline.name"))
                    .isEqualTo("standard");
    }

    @Test
    void readLegacyFileAsUtf8() throws Exception {
        Path file =
            temporaryDirectory.resolve(FlinkConfigurationLoader.LEGACY_FLINK_CONF_FILENAME);
        Files.writeString(
            file,
            "# comment-only lines must be ignored\n"
                + "pipeline.name: \u4e2d\u6587\u4f5c\u4e1a\n",
            StandardCharsets.UTF_8);

        Map<String, String> configuration =
            FlinkConfigurationLoader.loadConfigurationFromFile(file.toFile());

        assertThat(configuration.get("pipeline.name")).isEqualTo("\u4e2d\u6587\u4f5c\u4e1a");
    }

    @Test
    void rejectMissingConfigurationDirectory() {
        assertThatThrownBy(
            () -> FlinkConfigurationLoader.loadConfiguration(
                temporaryDirectory.resolve("missing").toString(), "1.20.0"))
                    .isInstanceOf(ConfigException.class)
                    .hasMessageContaining("does not exist");
    }
}
