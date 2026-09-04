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

package org.apache.streampark.common.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurationParserTest {

    @Test
    void flattenYamlAndPreserveLists() {
        Configuration configuration =
            ConfigurationParser.parse(
                "server:\n  port: 8080\n  hosts: [host-a, host-b]\nempty: null\n",
                ConfigurationFormat.YAML,
                "test YAML");

        assertThat(configuration.toMap())
            .containsEntry("server.port", "8080")
            .containsEntry("server.hosts", "host-a,host-b")
            .doesNotContainKey("empty");
    }

    @Test
    void rejectDuplicateAndFlattenedYamlKeys() {
        assertThatThrownBy(
            () -> ConfigurationParser.parse(
                "server:\n  port: 8080\nserver.port: 9090\n",
                ConfigurationFormat.YAML,
                "ambiguous YAML"))
                    .isInstanceOf(ConfigException.class)
                    .hasMessageContaining("Duplicate configuration key");
    }

    @Test
    void hoconShouldResolveSubstitutions() {
        Configuration configuration =
            ConfigurationParser.parse(
                "base.port = 8080\nserver.port = ${base.port}\n",
                ConfigurationFormat.HOCON,
                "test HOCON");

        assertThat(configuration.getString("server.port")).isEqualTo("8080");
    }

    @Test
    void propertiesShouldPreserveExactKeys() {
        Configuration configuration =
            ConfigurationParser.parse(
                "server.port=8080\nfeature.enabled=true\n",
                ConfigurationFormat.PROPERTIES,
                "test properties");

        assertThat(configuration.toMap())
            .containsEntry("server.port", "8080")
            .containsEntry("feature.enabled", "true");
    }

    @Test
    void fileFormatShouldBeInferredFromExtension(@TempDir Path temporaryDirectory) throws IOException {
        Path file = temporaryDirectory.resolve("application.yaml");
        Files.writeString(file, "server:\n  port: 8080\n");

        Configuration configuration = ConfigurationParser.parse(file);

        assertThat(configuration.getString("server.port")).isEqualTo("8080");
        assertThat(configuration.origin("server.port").orElseThrow().name())
            .isEqualTo(file.toAbsolutePath().normalize().toString());
    }
}
