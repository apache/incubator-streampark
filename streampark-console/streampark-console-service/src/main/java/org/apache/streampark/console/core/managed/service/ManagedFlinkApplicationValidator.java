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

import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Capability-driven validation and CU estimation for managed Flink candidates. */
@Component
public class ManagedFlinkApplicationValidator {

    private static final Set<String> RESERVED_CUSTOM_KEYS =
        Set.of(
            "parallelism",
            "taskManagerCpu",
            "taskManagerMemoryGiB",
            "taskManagerSlots",
            "jobManagerCpu",
            "jobManagerMemoryGiB",
            "checkpoint.enabled",
            "checkpoint.intervalMs",
            "checkpoint.timeoutMs",
            "restartStrategy");

    public BigDecimal validate(
                               ManagedFlinkApplicationSaveRequest request,
                               Map<String, Object> capability) {
        validateDefinition(request);
        ManagedFlinkRuntimeConfig runtime = request.getRuntimeConfig();
        ApiAlertException.throwIfTrue(
            runtime == null || runtime.getResource() == null,
            "Managed Flink resource configuration is required.");
        ApiAlertException.throwIfTrue(
            StringUtils.isBlank(runtime.getEngineVersion()),
            "Managed Flink engine version is required.");
        ApiAlertException.throwIfFalse(
            "STREAMING".equals(runtime.getExecutionMode()),
            "Only STREAMING execution mode is supported.");

        validateCapabilityValue(
            strings(capability.get("engineVersions")),
            runtime.getEngineVersion(),
            "Managed Flink engine version is not supported by the environment.");
        validateCapabilityValue(
            strings(capability.get("jobTypes")),
            request.getJobType(),
            "Managed Flink job type is not supported by the environment.");

        ManagedFlinkRuntimeConfig.ResourceConfig resource = runtime.getResource();
        positive(resource.getParallelism(), "Parallelism must be greater than zero.");
        positive(resource.getTaskManagerSlots(), "TaskManager slots must be greater than zero.");
        BigDecimal minimumCpu = decimal(capability.get("minCpu"), new BigDecimal("0.5"));
        BigDecimal cpuStep = decimal(capability.get("cpuStep"), new BigDecimal("0.5"));
        BigDecimal memoryPerCpu =
            decimal(capability.get("memoryPerCpuGiB"), new BigDecimal("4"));
        validateCpu(resource.getTaskManagerCpu(), minimumCpu, cpuStep, "TaskManager CPU");
        validateCpu(resource.getJobManagerCpu(), minimumCpu, cpuStep, "JobManager CPU");
        validateMemory(
            resource.getTaskManagerMemoryGiB(),
            resource.getTaskManagerCpu(),
            memoryPerCpu,
            "TaskManager memory");
        validateMemory(
            resource.getJobManagerMemoryGiB(),
            resource.getJobManagerCpu(),
            memoryPerCpu,
            "JobManager memory");
        validateCheckpoint(runtime.getCheckpoint());
        validateCustomProperties(runtime.getCustomProperties());
        validateRelease(request.getReleaseConfig(), capability);
        return estimateCu(resource, memoryPerCpu);
    }

    private static void validateDefinition(ManagedFlinkApplicationSaveRequest request) {
        ApiAlertException.throwIfTrue(
            request.getJobName().contains(" "),
            "Managed Flink application name cannot contain spaces.");
        if ("STREAMING_SQL".equals(request.getJobType())) {
            ApiAlertException.throwIfTrue(
                StringUtils.isBlank(request.getSql()),
                "Managed Flink SQL definition is required.");
            ApiAlertException.throwIfTrue(
                StringUtils.isNotBlank(request.getJar())
                    || StringUtils.isNotBlank(request.getMainClass()),
                "Managed Flink SQL applications cannot define a JAR or main class.");
        } else if ("STREAMING_JAR".equals(request.getJobType())) {
            ApiAlertException.throwIfTrue(
                StringUtils.isBlank(request.getJar())
                    || StringUtils.isBlank(request.getMainClass()),
                "Managed Flink JAR and main class are required.");
            ApiAlertException.throwIfTrue(
                StringUtils.isNotBlank(request.getSql()),
                "Managed Flink JAR applications cannot define SQL.");
        } else {
            throw new ApiAlertException("Unsupported managed Flink job type.");
        }
    }

    private static void validateCpu(
                                    BigDecimal value,
                                    BigDecimal minimum,
                                    BigDecimal step,
                                    String name) {
        ApiAlertException.throwIfTrue(
            value == null || value.compareTo(minimum) < 0,
            name + " must be at least " + minimum.toPlainString() + ".");
        ApiAlertException.throwIfTrue(
            step.signum() <= 0 || value.remainder(step).signum() != 0,
            name + " must use increments of " + step.toPlainString() + ".");
    }

    private static void validateMemory(
                                       BigDecimal memory,
                                       BigDecimal cpu,
                                       BigDecimal memoryPerCpu,
                                       String name) {
        BigDecimal minimum = cpu.multiply(memoryPerCpu);
        ApiAlertException.throwIfTrue(
            memory == null || memory.compareTo(minimum) < 0,
            name + " must be at least " + minimum.toPlainString() + " GiB.");
    }

    private static void validateCheckpoint(ManagedFlinkRuntimeConfig.CheckpointConfig checkpoint) {
        if (checkpoint == null || !Boolean.TRUE.equals(checkpoint.getEnabled())) {
            return;
        }
        positive(checkpoint.getIntervalMs(), "Checkpoint interval must be greater than zero.");
        positive(checkpoint.getTimeoutMs(), "Checkpoint timeout must be greater than zero.");
        ApiAlertException.throwIfTrue(
            checkpoint.getTimeoutMs() < checkpoint.getIntervalMs(),
            "Checkpoint timeout must be greater than or equal to its interval.");
        if (checkpoint.getStateTtlMs() != null) {
            positive(checkpoint.getStateTtlMs(), "State TTL must be greater than zero.");
        }
    }

    private static void validateRelease(
                                        ManagedFlinkReleaseConfig release,
                                        Map<String, Object> capability) {
        ApiAlertException.throwIfNull(
            release, "Managed Flink release configuration is required.");
        ApiAlertException.throwIfTrue(
            release.getDependencyResourceNames() != null
                && release.getDependencyResourceNames().size() > 20,
            "Managed Flink applications cannot define more than 20 JAR dependencies.");
        if (release.getPriority() != null) {
            ApiAlertException.throwIfTrue(
                release.getPriority() < 1 || release.getPriority() > 100,
                "Managed Flink release priority must be between 1 and 100.");
        }
        validateCapabilityValue(
            strings(capability.get("schedulingStrategies")),
            release.getSchedulingStrategy(),
            "Managed Flink scheduling strategy is not supported by the environment.");
        validateCustomProperties(release.getCustomProperties());
    }

    private static void validateCustomProperties(Map<String, String> properties) {
        if (properties == null) {
            return;
        }
        ApiAlertException.throwIfTrue(
            properties.size() > 100,
            "Managed Flink custom properties cannot contain more than 100 entries.");
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            ApiAlertException.throwIfTrue(
                StringUtils.isBlank(entry.getKey())
                    || entry.getKey().length() > 128
                    || entry.getValue() == null
                    || entry.getValue().length() > 1024,
                "Managed Flink custom property key or value is invalid.");
            ApiAlertException.throwIfTrue(
                RESERVED_CUSTOM_KEYS.contains(entry.getKey()),
                "Managed Flink custom properties cannot override structured fields.");
        }
    }

    private static BigDecimal estimateCu(
                                         ManagedFlinkRuntimeConfig.ResourceConfig resource,
                                         BigDecimal memoryPerCpu) {
        int taskManagerCount =
            BigDecimal.valueOf(resource.getParallelism())
                .divide(
                    BigDecimal.valueOf(resource.getTaskManagerSlots()),
                    0,
                    RoundingMode.CEILING)
                .intValueExact();
        BigDecimal totalCpu =
            resource.getTaskManagerCpu()
                .multiply(BigDecimal.valueOf(taskManagerCount))
                .add(resource.getJobManagerCpu());
        BigDecimal totalMemory =
            resource.getTaskManagerMemoryGiB()
                .multiply(BigDecimal.valueOf(taskManagerCount))
                .add(resource.getJobManagerMemoryGiB());
        return totalCpu.max(totalMemory.divide(memoryPerCpu, 8, RoundingMode.CEILING))
            .stripTrailingZeros();
    }

    private static void validateCapabilityValue(
                                                List<String> supported,
                                                String value,
                                                String message) {
        ApiAlertException.throwIfTrue(
            StringUtils.isBlank(value) || !supported.contains(value), message);
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        return ((List<?>) value).stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .collect(java.util.stream.Collectors.toList());
    }

    private static BigDecimal decimal(Object value, BigDecimal defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new ApiAlertException("Managed Flink environment capability is invalid.");
        }
    }

    private static void positive(Number value, String message) {
        ApiAlertException.throwIfTrue(value == null || value.longValue() <= 0, message);
    }
}
