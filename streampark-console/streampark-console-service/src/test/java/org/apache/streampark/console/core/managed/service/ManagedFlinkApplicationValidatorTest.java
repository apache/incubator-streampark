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

package org.apache.streampark.console.core.managed.service;

import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationSaveRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseConfig;
import org.apache.streampark.console.core.managed.model.ManagedFlinkRuntimeConfig;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ManagedFlinkApplicationValidatorTest {

    private final ManagedFlinkApplicationValidator validator =
        new ManagedFlinkApplicationValidator();

    @Test
    void shouldEstimatePrdExampleAsNinetySevenCu() {
        assertThat(validator.validate(request(), capability()))
            .isEqualByComparingTo("97");
    }

    @Test
    void shouldRejectCpuOutsideCapabilityStep() {
        ManagedFlinkApplicationSaveRequest request = request();
        request.getRuntimeConfig().getResource().setTaskManagerCpu(new BigDecimal("0.75"));

        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> validator.validate(request, capability()))
            .withMessageContaining("increments of 0.5");
    }

    @Test
    void shouldRejectCustomPropertyThatOverridesStructuredField() {
        ManagedFlinkApplicationSaveRequest request = request();
        request.getRuntimeConfig().getCustomProperties().put("parallelism", "1");

        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> validator.validate(request, capability()))
            .withMessageContaining("cannot override");
    }

    @Test
    void shouldRejectDependenciesForSqlApplication() {
        ManagedFlinkApplicationSaveRequest request = request();
        request.getReleaseConfig()
            .setDependencyResourceNames(Collections.singletonList("dependency.jar"));

        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> validator.validate(request, capability()))
            .withMessageContaining("cannot define JAR dependencies");
    }

    @Test
    void shouldRejectMoreThanTwentyJarDependencies() {
        ManagedFlinkApplicationSaveRequest request = request();
        request.setJobType("STREAMING_JAR");
        request.setSql(null);
        request.setJar("main.jar");
        request.setMainClass("org.example.Main");
        request.getReleaseConfig()
            .setDependencyResourceNames(
                java.util.stream.IntStream.range(0, 21)
                    .mapToObj(index -> "dependency-" + index + ".jar")
                    .collect(java.util.stream.Collectors.toList()));

        assertThatExceptionOfType(ApiAlertException.class)
            .isThrownBy(() -> validator.validate(request, capability()))
            .withMessageContaining("more than 20");
    }

    static ManagedFlinkApplicationSaveRequest request() {
        ManagedFlinkApplicationSaveRequest request =
            new ManagedFlinkApplicationSaveRequest();
        request.setTeamId(100000L);
        request.setJobName("managed-validator");
        request.setManagedEnvironmentId(100000L);
        request.setJobType("STREAMING_SQL");
        request.setSql("SELECT 1");

        ManagedFlinkRuntimeConfig runtime = new ManagedFlinkRuntimeConfig();
        runtime.setEngineVersion("1.20");
        ManagedFlinkRuntimeConfig.ResourceConfig resource =
            new ManagedFlinkRuntimeConfig.ResourceConfig();
        resource.setParallelism(96);
        resource.setTaskManagerCpu(new BigDecimal("8"));
        resource.setTaskManagerMemoryGiB(new BigDecimal("32"));
        resource.setTaskManagerSlots(8);
        resource.setJobManagerCpu(BigDecimal.ONE);
        resource.setJobManagerMemoryGiB(new BigDecimal("4"));
        runtime.setResource(resource);
        runtime.setCustomProperties(new LinkedHashMap<>());
        request.setRuntimeConfig(runtime);
        request.setReleaseConfig(new ManagedFlinkReleaseConfig());
        return request;
    }

    static Map<String, Object> capability() {
        Map<String, Object> capability = new LinkedHashMap<>();
        capability.put("engineVersions", Arrays.asList("1.20"));
        capability.put("jobTypes", Arrays.asList("STREAMING_SQL", "STREAMING_JAR"));
        capability.put("schedulingStrategies", Arrays.asList("DEFAULT"));
        capability.put("minCpu", "0.5");
        capability.put("cpuStep", "0.5");
        capability.put("memoryPerCpuGiB", "4");
        return capability;
    }
}
