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

package org.apache.streampark.console.base.config;

import org.apache.streampark.common.configuration.ConfigException;
import org.apache.streampark.common.configuration.ConfigOrigin;
import org.apache.streampark.common.configuration.ConfigSource;
import org.apache.streampark.common.configuration.Configuration;
import org.apache.streampark.common.configuration.DataSize;
import org.apache.streampark.common.configuration.GlobalConfiguration;
import org.apache.streampark.common.configuration.option.DockerOptions;
import org.apache.streampark.common.configuration.option.HadoopOptions;
import org.apache.streampark.common.configuration.option.YarnOptions;
import org.apache.streampark.flink.kubernetes.configuration.KubernetesOptions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests the explicit integration boundary between Spring and typed server configuration. */
class SpringConfigurationInitializerTest {

    private Configuration previousConfiguration;
    private String previousHadoopUser;

    @BeforeEach
    void captureGlobalConfiguration() {
        previousConfiguration = GlobalConfiguration.current();
        previousHadoopUser = System.getProperty(HadoopOptions.HADOOP_USER_NAME_PROPERTY);
    }

    @AfterEach
    void restoreGlobalConfiguration() {
        GlobalConfiguration.update(previousConfiguration);
        if (previousHadoopUser == null) {
            System.clearProperty(HadoopOptions.HADOOP_USER_NAME_PROPERTY);
        } else {
            System.setProperty(HadoopOptions.HADOOP_USER_NAME_PROPERTY, previousHadoopUser);
        }
    }

    @Test
    void publishDeclaredSpringOverrides() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty(HadoopOptions.HADOOP_USER_NAME_PROPERTY, "spring-hadoop-user")
            .withProperty(HadoopOptions.KERBEROS_ENABLED.key(), "true")
            .withProperty(YarnOptions.PROXY_URL.key(), "https://rm.example.test")
            .withProperty(DockerOptions.MAX_CONNECTIONS.key(), "37")
            .withProperty(ConsoleOptions.BUILD_LOG_READ_MAX_SIZE.key(), "2 MiB")
            .withProperty(KubernetesOptions.INGRESS_CLASS.key(), "internal-nginx")
            .withProperty("spring.unrelated.property", "must-not-leak");

        Configuration configuration = new SpringConfigurationInitializer().initialize(environment);

        assertThat(configuration.get(HadoopOptions.KERBEROS_ENABLED)).isTrue();
        assertThat(System.getProperty(HadoopOptions.HADOOP_USER_NAME_PROPERTY))
            .isEqualTo("spring-hadoop-user");
        assertThat(configuration.get(YarnOptions.PROXY_URL)).isEqualTo("https://rm.example.test");
        assertThat(configuration.get(DockerOptions.MAX_CONNECTIONS)).isEqualTo(37);
        assertThat(configuration.get(ConsoleOptions.BUILD_LOG_READ_MAX_SIZE))
            .isEqualTo(DataSize.ofMebiBytes(2));
        assertThat(configuration.get(KubernetesOptions.INGRESS_CLASS)).isEqualTo("internal-nginx");
        assertThat(configuration.containsKey("spring.unrelated.property")).isFalse();
        assertThat(configuration.origin(DockerOptions.MAX_CONNECTIONS.key()))
            .contains(ConfigOrigin.of(ConfigSource.RUNTIME, "resolved Spring environment"));
    }

    @Test
    void rejectInvalidSpringOverrides() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty(DockerOptions.MAX_CONNECTIONS.key(), "0");

        assertThatThrownBy(() -> new SpringConfigurationInitializer().initialize(environment))
            .isInstanceOf(ConfigException.class)
            .hasMessageContaining(DockerOptions.MAX_CONNECTIONS.key());
        assertThat(GlobalConfiguration.current()).isSameAs(previousConfiguration);
    }
}
