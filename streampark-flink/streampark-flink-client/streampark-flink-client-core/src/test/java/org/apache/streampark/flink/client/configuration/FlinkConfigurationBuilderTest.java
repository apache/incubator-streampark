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

package org.apache.streampark.flink.client.configuration;

import org.apache.streampark.common.core.FlinkVersion;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.flink.client.bean.ResolvedSubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitRequestResolver;
import org.apache.streampark.flink.client.request.SubmitApplicationSpec;
import org.apache.streampark.flink.client.request.SubmitRequest;
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse;

import org.apache.flink.configuration.Configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlinkConfigurationBuilderTest {

    private static final String TEMPLATE =
        "/logs/${jobName}/${jobname}/$jobName/$jobname/"
            + "${jobId}/${jobid}/$jobId/$jobid";

    @TempDir
    Path tempDir;

    @Test
    void expandJobVariables() {
        Configuration configuration = new Configuration();
        configuration.setString("job.log.path", TEMPLATE);

        FlinkConfigurationBuilder.expandJobPlaceholders(
            configuration, resolve(FlinkDeployMode.YARN_APPLICATION));

        assertThat(configuration.getString("job.log.path", null))
            .isEqualTo("/logs/orders/orders/orders/orders/42/42/42/42");
    }

    @Test
    void keepSharedVariables() {
        Configuration configuration = new Configuration();
        configuration.setString("job.log.path", TEMPLATE);

        FlinkConfigurationBuilder.expandJobPlaceholders(
            configuration, resolve(FlinkDeployMode.KUBERNETES_NATIVE_SESSION));

        assertThat(configuration.getString("job.log.path", null)).isEqualTo(TEMPLATE);
    }

    @Test
    void dynamicPropertiesOverrideDefaults() throws Exception {
        Path flinkHome = tempDir.resolve("flink");
        Files.createDirectories(flinkHome.resolve("lib"));
        Files.createDirectories(flinkHome.resolve("conf"));
        Files.createFile(flinkHome.resolve("lib/flink-dist_2.12-1.18.1.jar"));
        Files.writeString(
            flinkHome.resolve("conf/flink-conf.yaml"), "parallelism.default: 1\n");

        Configuration configuration =
            FlinkConfigurationBuilder.extract(
                flinkHome.toString(), Map.of("parallelism.default", 4));

        assertThat(configuration.getInteger("parallelism.default", -1)).isEqualTo(4);
    }

    private static ResolvedSubmitRequest resolve(FlinkDeployMode deployMode) {
        SubmitApplicationSpec job =
            SubmitApplicationSpec.builder()
                .jobType(FlinkJobType.FLINK_JAR)
                .id(42L)
                .appName("orders")
                .build();
        SubmitRequest request =
            new SubmitRequest(
                new FlinkVersion("/tmp/flink-home"),
                deployMode,
                Collections.emptyMap(),
                job,
                null,
                new ShadedBuildResponse("/tmp", "/tmp/job.jar"),
                null);
        return SubmitRequestResolver.resolve(request);
    }
}
