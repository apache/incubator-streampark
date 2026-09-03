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
import org.apache.streampark.flink.client.bean.SubmitRequestResolver;
import org.apache.streampark.flink.client.request.SubmitApplicationSpec;
import org.apache.streampark.flink.client.request.SubmitRequest;

import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SubmitRequestResolverTest {

    private static final FlinkVersion FLINK_VERSION = new FlinkVersion("/tmp/flink-home");

    @Test
    void appMainForFlinkSqlJob() {
        SubmitRequest request = createRequest(FlinkJobType.FLINK_SQL, null);
        assertThat(SubmitRequestResolver.applicationMain(request))
            .isEqualTo(Constants.STREAMPARK_FLINKSQL_CLIENT_CLASS);
    }

    @Test
    void appMainForPyFlinkJob() {
        SubmitRequest request = createRequest(FlinkJobType.PYFLINK, null);
        assertThat(SubmitRequestResolver.applicationMain(request))
            .isEqualTo(Constants.PYTHON_FLINK_DRIVER_CLASS_NAME);
    }

    @Test
    void appMainForFlinkJarFromJsonConf() {
        String mainClass = "org.apache.flink.streaming.examples.windowing.TopSpeedWindowing";
        String appConf =
            String.format(
                "json://{\"%s\":\"%s\"}",
                FlinkOptions.APPLICATION_MAIN_CLASS.key(), mainClass);
        SubmitRequest request = createRequest(FlinkJobType.FLINK_JAR, appConf);
        assertThat(SubmitRequestResolver.applicationMain(request)).isEqualTo(mainClass);
    }

    @Test
    void savepointRestoreSettingsWithoutSavepoint() {
        SubmitRequest request = createRequest(FlinkJobType.FLINK_JAR, null);
        assertThat(SubmitRequestResolver.savepointRestoreSettings(request))
            .isEqualTo(SavepointRestoreSettings.none());
    }

    @Test
    void appPropertiesFromJsonConf() {
        String propertyKey = FlinkOptions.PROPERTY_PREFIX.value() + "parallelism.default";
        String appConf = "json://{\"" + propertyKey + "\":\"8\"}";
        SubmitApplicationSpec application =
            SubmitApplicationSpec.builder()
                .jobType(FlinkJobType.FLINK_JAR)
                .appConf(appConf)
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

        assertThat(SubmitRequestResolver.applicationProperties(request))
            .containsEntry("parallelism.default", "8");
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
    void returnNullWithoutFlinkSql() {
        SubmitRequest request = createRequest(FlinkJobType.FLINK_SQL, null);
        assertThat(SubmitRequestResolver.flinkSql(request)).isNull();
    }

    @Test
    void flinkSqlShouldReadFromExtraParameter() {
        Map<String, Object> extra = new HashMap<>();
        extra.put(ApplicationOptions.SQL.key(), "select 1");
        SubmitRequest request =
            new SubmitRequest(
                FLINK_VERSION,
                FlinkDeployMode.YARN_APPLICATION,
                Collections.emptyMap(),
                SubmitApplicationSpec.builder().jobType(FlinkJobType.FLINK_SQL).build(),
                null,
                null,
                extra);
        assertThat(SubmitRequestResolver.flinkSql(request)).isEqualTo("select 1");
    }

    @Test
    void extraParametersShouldDefaultToEmptyMap() {
        SubmitRequest request = createRequest(FlinkJobType.FLINK_JAR, null);
        assertThat(request.extraParameter()).isEmpty();
    }

    @Test
    void defaultAllowNonRestoredState() {
        SubmitRequest request = createRequest(FlinkJobType.FLINK_JAR, null);
        assertThat(SubmitRequestResolver.allowNonRestoredState(request)).isFalse();
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

    private static SubmitRequest createRequest(FlinkJobType jobType, String appConf) {
        SubmitApplicationSpec application =
            SubmitApplicationSpec.builder()
                .jobType(jobType)
                .applicationType(ApplicationType.APACHE_FLINK)
                .appConf(appConf)
                .build();
        return new SubmitRequest(
            FLINK_VERSION,
            FlinkDeployMode.YARN_APPLICATION,
            Collections.emptyMap(),
            application,
            null,
            null,
            null);
    }
}
