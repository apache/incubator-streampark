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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurationTest {

    private static final ConfigOption<Integer> PORT =
        ConfigOptions.key("server.port")
            .intType()
            .defaultValue(8080)
            .check(value -> value > 0, "port must be positive")
            .withDescription("Server port.")
            .build();

    @Test
    void preferHigherPrioritySource() {
        Configuration configuration =
            Configuration.builder()
                .add("command line", ConfigSource.COMMAND_LINE, Map.of(PORT.key(), "9090"))
                .add("file", ConfigSource.FILE, Map.of(PORT.key(), "8081"))
                .build();

        assertThat(configuration.get(PORT)).isEqualTo(9090);
        assertThat(configuration.origin(PORT.key()))
            .contains(ConfigOrigin.of(ConfigSource.COMMAND_LINE, "command line"));
    }

    @Test
    void laterValueWithinSameSourceShouldWin() {
        Configuration configuration =
            Configuration.builder()
                .add("base file", ConfigSource.FILE, Map.of(PORT.key(), "8081"))
                .add("override file", ConfigSource.FILE, Map.of(PORT.key(), "8082"))
                .build();

        assertThat(configuration.get(PORT)).isEqualTo(8082);
    }

    @Test
    void convertStructuredAndTextValues() {
        ConfigOption<Duration> timeout =
            ConfigOptions.key("request.timeout")
                .durationType(ChronoUnit.SECONDS)
                .noDefaultValue()
                .withDescription("Request timeout.")
                .build();
        ConfigOption<DataSize> size =
            ConfigOptions.key("buffer.size")
                .dataSizeType()
                .noDefaultValue()
                .withDescription("Buffer size.")
                .build();
        ConfigOption<List<String>> hosts =
            ConfigOptions.key("hosts")
                .stringListType()
                .noDefaultValue()
                .withDescription("Target hosts.")
                .build();
        Configuration configuration =
            Configuration.builder()
                .add(
                    "test",
                    ConfigSource.FILE,
                    Map.of(
                        timeout.key(), "15",
                        size.key(), "2 MiB",
                        hosts.key(), Arrays.asList("host-a", "host-b")))
                .build();

        assertThat(configuration.get(timeout)).isEqualTo(Duration.ofSeconds(15));
        assertThat(configuration.get(size)).isEqualTo(DataSize.ofMebiBytes(2));
        assertThat(configuration.get(hosts)).containsExactly("host-a", "host-b");
    }

    @Test
    void preferCanonicalKey() {
        ConfigOption<String> option =
            ConfigOptions.key("current.key")
                .stringType()
                .noDefaultValue()
                .withFallbackKeys("legacy.key")
                .withDescription("Migrated option.")
                .build();
        Configuration fallbackOnly =
            Configuration.builder()
                .add("test", ConfigSource.FILE, Map.of("legacy.key", "legacy"))
                .build();
        Configuration both =
            Configuration.builder(fallbackOnly)
                .add("test", ConfigSource.FILE, Map.of("current.key", "current"))
                .build();

        assertThat(fallbackOnly.get(option)).isEqualTo("legacy");
        assertThat(both.get(option)).isEqualTo("current");
    }

    @Test
    void invalidSensitiveValueShouldBeRedacted() {
        ConfigOption<Integer> secret =
            ConfigOptions.key("service.secret")
                .intType()
                .noDefaultValue()
                .sensitive()
                .withDescription("Sensitive numeric value.")
                .build();
        Configuration configuration =
            Configuration.builder()
                .add("test", ConfigSource.FILE, Map.of(secret.key(), "clear-text-value"))
                .build();

        assertThatThrownBy(() -> configuration.get(secret))
            .isInstanceOf(ConfigException.class)
            .hasMessageContaining(SensitiveKeys.MASK)
            .hasMessageNotContaining("clear-text-value");
        assertThat(configuration.toRedactedMap()).containsEntry(secret.key(), SensitiveKeys.MASK);
    }

    @Test
    void redactFlinkSecrets() {
        assertThat(
            Arrays.asList(
                "fs.azure.account.key.storage",
                "security.apikey",
                "security.auth-params",
                "security.basic-auth",
                "security.jaas.config",
                "security.http-headers"))
                    .allMatch(SensitiveKeys::isSensitive);
    }

    @Test
    void snapshotsAndViewsShouldBeImmutable() {
        Configuration configuration =
            Configuration.builder()
                .add("test", ConfigSource.FILE, Map.of("app.name", "orders"))
                .build();

        assertThat(configuration.subset("app.").toMap()).containsExactly(Map.entry("name", "orders"));
        assertThatThrownBy(() -> configuration.toMap().put("new", "value"))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThat(Configuration.empty().toMap()).isEqualTo(Collections.emptyMap());
    }

    @Test
    void copyMutableValues() {
        ConfigOption<List<String>> hosts =
            ConfigOptions.key("hosts")
                .stringListType()
                .noDefaultValue()
                .withDescription("Target hosts.")
                .build();
        List<String> source = new ArrayList<>(Collections.singletonList("host-a"));
        Configuration configuration =
            Configuration.builder()
                .add("test", ConfigSource.FILE, Map.of(hosts.key(), source))
                .build();

        source.add("host-b");

        assertThat(configuration.get(hosts)).containsExactly("host-a");
        assertThatThrownBy(() -> configuration.get(hosts).add("host-c"))
            .isInstanceOf(UnsupportedOperationException.class);

        List<String> defaultHosts = new ArrayList<>(Collections.singletonList("default-a"));
        ConfigOption<List<String>> optionWithDefault =
            ConfigOptions.key("default.hosts")
                .stringListType()
                .defaultValue(defaultHosts)
                .withDescription("Default hosts.")
                .build();
        defaultHosts.add("default-b");

        assertThat(optionWithDefault.defaultValue()).containsExactly("default-a");
        assertThatThrownBy(() -> optionWithDefault.defaultValue().add("default-c"))
            .isInstanceOf(UnsupportedOperationException.class);

        List<String> pending = new ArrayList<>(Collections.singletonList("pending-a"));
        ConfigurationLoader loader =
            new ConfigurationLoader()
                .add(ConfigSource.FILE, "test", Map.of(hosts.key(), pending));
        pending.add("pending-b");

        assertThat(loader.load().get(hosts)).containsExactly("pending-a");
    }

    @Test
    void rejectInvalidAndMissingValues() {
        Configuration invalid =
            Configuration.builder()
                .add("application.yaml", ConfigSource.FILE, Map.of(PORT.key(), "zero"))
                .build();

        assertThatThrownBy(() -> invalid.get(PORT))
            .isInstanceOf(ConfigException.class)
            .hasMessageContaining(PORT.key())
            .hasMessageContaining("application.yaml");
        assertThatThrownBy(
            () -> Configuration.empty()
                .get(
                    ConfigOptions.key("required")
                        .stringType()
                        .noDefaultValue()
                        .withDescription("Required value.")
                        .build()))
                            .isInstanceOf(ConfigException.class)
                            .hasMessageContaining("required");
    }

    @Test
    void rejectNegativeDurations() {
        ConfigOption<Duration> timeout =
            ConfigOptions.key("request.timeout")
                .durationType()
                .noDefaultValue()
                .withDescription("Request timeout.")
                .build();
        Configuration durationValue =
            Configuration.builder()
                .add("test", ConfigSource.FILE, Map.of(timeout.key(), Duration.ofSeconds(-1)))
                .build();
        Configuration isoValue =
            Configuration.builder()
                .add("test", ConfigSource.FILE, Map.of(timeout.key(), "-PT1S"))
                .build();

        assertThatThrownBy(() -> durationValue.get(timeout))
            .isInstanceOf(ConfigException.class)
            .hasRootCauseMessage("duration must not be negative");
        assertThatThrownBy(() -> isoValue.get(timeout))
            .isInstanceOf(ConfigException.class)
            .hasRootCauseMessage("duration must not be negative");
    }
}
