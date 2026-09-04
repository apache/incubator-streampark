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

package org.apache.streampark.flink.client.test;

import org.apache.streampark.common.configuration.Constants;
import org.apache.streampark.common.configuration.FlinkOptions;
import org.apache.streampark.common.configuration.option.ApplicationOptions;
import org.apache.streampark.common.core.FlinkVersion;
import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.flink.client.bean.ResolvedSubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitRequestResolver;
import org.apache.streampark.flink.client.request.SubmitApplicationSpec;
import org.apache.streampark.flink.client.request.SubmitRequest;
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse;

import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubmitRequestResolverTest {

    private static final FlinkVersion FLINK_VERSION = new FlinkVersion("/tmp/flink-home");

    @Test
    void resolveFlinkSqlMainClass() {
        SubmitRequest request = createRequest(FlinkJobType.FLINK_SQL, null);
        assertThat(SubmitRequestResolver.resolve(request).getJobMainClass())
            .isEqualTo(Constants.STREAMPARK_FLINKSQL_CLIENT_CLASS);
    }

    @Test
    void resolvePyFlinkMainClass() {
        SubmitRequest request = createRequest(FlinkJobType.PYFLINK, null);
        assertThat(SubmitRequestResolver.resolve(request).getJobMainClass())
            .isEqualTo(Constants.PYTHON_FLINK_DRIVER_CLASS_NAME);
    }

    @Test
    void resolveJarMainClassFromJson() {
        String mainClass = "org.apache.flink.streaming.examples.windowing.TopSpeedWindowing";
        String appConf =
            String.format(
                "json://{\"%s\":\"%s\"}",
                FlinkOptions.APPLICATION_MAIN_CLASS.key(), mainClass);
        SubmitRequest request = createRequest(FlinkJobType.FLINK_JAR, appConf);
        assertThat(SubmitRequestResolver.resolve(request).getJobMainClass()).isEqualTo(mainClass);
    }

    @Test
    void returnNoRestoreSettings() {
        SubmitRequest request = createRequest(FlinkJobType.FLINK_JAR, null);
        assertThat(SubmitRequestResolver.resolve(request).savepointRestoreSettings())
            .isEqualTo(SavepointRestoreSettings.none());
    }

    @Test
    void extractPropertiesFromJson() {
        String propertyKey = FlinkOptions.PROPERTY_PREFIX.value() + "parallelism.default";
        String appConf = "json://{\"" + propertyKey + "\":\"8\"}";
        SubmitApplicationSpec application =
            SubmitApplicationSpec.builder()
                .jobType(FlinkJobType.FLINK_JAR)
                .appName("test-job")
                .appConf(appConf)
                .build();
        SubmitRequest request =
            new SubmitRequest(
                FLINK_VERSION,
                FlinkDeployMode.YARN_APPLICATION,
                Collections.emptyMap(),
                application,
                null,
                buildResult(),
                null);

        assertThat(SubmitRequestResolver.resolve(request).jobProperties())
            .containsEntry("parallelism.default", "8");
    }

    @Test
    void keepResolvedConfigImmutable() {
        String propertyKey = FlinkOptions.PROPERTY_PREFIX.value() + "parallelism.default";
        String optionKey = FlinkOptions.OPTION_PREFIX.value() + "detached";
        SubmitRequest request =
            createRequest(
                FlinkJobType.FLINK_JAR,
                "json://{\""
                    + propertyKey
                    + "\":\"8\",\""
                    + optionKey
                    + "\":\"true\"}");

        ResolvedSubmitRequest resolved = SubmitRequestResolver.resolve(request);

        assertThat(resolved.jobProperties())
            .containsExactly(Map.entry("parallelism.default", "8"));
        assertThat(resolved.jobOptions())
            .containsExactly(Map.entry("detached", "true"));
        assertThatThrownBy(() -> resolved.jobProperties().put("new.key", "value"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void copySerializableIntegers() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("key", 42);
        SubmitApplicationSpec application =
            SubmitApplicationSpec.builder().jobType(FlinkJobType.FLINK_JAR).build();
        SubmitRequest request =
            new SubmitRequest(
                FLINK_VERSION,
                FlinkDeployMode.YARN_APPLICATION,
                properties,
                application,
                null,
                null,
                null);

        assertThat(request.properties()).containsEntry("key", 42);
    }

    @Test
    void returnNullWithoutSql() {
        SubmitRequest request = createRequest(FlinkJobType.FLINK_SQL, null);
        assertThat(SubmitRequestResolver.resolve(request).getFlinkSqlContent()).isNull();
    }

    @Test
    void readSqlFromExtraParameters() {
        Map<String, Object> extra = new HashMap<>();
        extra.put(ApplicationOptions.SQL.key(), "select 1");
        SubmitRequest request =
            new SubmitRequest(
                FLINK_VERSION,
                FlinkDeployMode.YARN_APPLICATION,
                Collections.emptyMap(),
                SubmitApplicationSpec.builder()
                    .jobType(FlinkJobType.FLINK_SQL)
                    .appName("test-job")
                    .build(),
                null,
                buildResult(),
                extra);
        assertThat(SubmitRequestResolver.resolve(request).getFlinkSqlContent()).isEqualTo("select 1");
    }

    @Test
    void defaultExtraParametersToEmptyMap() {
        SubmitRequest request = createRequest(FlinkJobType.FLINK_JAR, null);
        assertThat(request.extraParameter()).isEmpty();
    }

    @Test
    void disallowNonRestoredStateByDefault() {
        SubmitRequest request = createRequest(FlinkJobType.FLINK_JAR, null);
        assertThat(SubmitRequestResolver.resolve(request).isNonRestoredStateAllowed()).isFalse();
    }

    @Test
    void preserveYamlFormat() {
        SubmitApplicationSpec application =
            SubmitApplicationSpec.builder()
                .jobType(FlinkJobType.FLINK_JAR)
                .standardYaml(true)
                .build();
        SubmitRequest request =
            new SubmitRequest(
                FLINK_VERSION,
                FlinkDeployMode.YARN_APPLICATION,
                Collections.emptyMap(),
                application,
                null,
                null,
                null);

        assertThat(request.standardYaml()).isTrue();
    }

    @Test
    void rejectMissingJobName() {
        SubmitApplicationSpec application =
            SubmitApplicationSpec.builder().jobType(FlinkJobType.FLINK_JAR).build();
        SubmitRequest request =
            new SubmitRequest(
                FLINK_VERSION,
                FlinkDeployMode.YARN_APPLICATION,
                Collections.emptyMap(),
                application,
                null,
                buildResult(),
                null);

        assertThatThrownBy(() -> SubmitRequestResolver.resolve(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Flink job name must not be blank");
    }

    @Test
    void rejectMissingUserJar() {
        SubmitApplicationSpec job =
            SubmitApplicationSpec.builder()
                .jobType(FlinkJobType.FLINK_JAR)
                .appName("test-job")
                .build();
        SubmitRequest request =
            new SubmitRequest(
                FLINK_VERSION,
                FlinkDeployMode.YARN_APPLICATION,
                Collections.emptyMap(),
                job,
                null,
                new ShadedBuildResponse("/tmp", null),
                null);

        assertThatThrownBy(() -> SubmitRequestResolver.resolve(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Built user JAR path must not be blank");
    }

    private static SubmitRequest createRequest(FlinkJobType jobType, String appConf) {
        SubmitApplicationSpec application =
            SubmitApplicationSpec.builder()
                .jobType(jobType)
                .appName("test-job")
                .applicationType(ApplicationType.APACHE_FLINK)
                .appConf(appConf)
                .build();
        return new SubmitRequest(
            FLINK_VERSION,
            FlinkDeployMode.YARN_APPLICATION,
            Collections.emptyMap(),
            application,
            null,
            buildResult(),
            null);
    }

    private static ShadedBuildResponse buildResult() {
        return new ShadedBuildResponse("/tmp", "/tmp/job.jar");
    }
}
