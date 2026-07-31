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

package org.apache.streampark.console.core.managed.api;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Provider capabilities used to drive validation and UI options. */
@Value
@Builder
public class ManagedFlinkCapability {

    ManagedFlinkProviderType providerType;

    String apiVersion;

    @Singular
    List<String> engineVersions;

    @Singular
    List<String> jobTypes;

    @Singular
    List<String> executionModes;

    @Singular
    List<String> startModes;

    @Singular
    List<String> schedulingStrategies;

    boolean supportsProjectList;

    boolean supportsResourcePoolList;

    boolean supportsSqlDeepCheck;

    boolean supportsSkipPrecheck;

    boolean supportsStopWithSnapshot;

    boolean supportsCreateSnapshot;

    boolean supportsJarDirectUpload;

    boolean supportsCustomEndpoint;

    BigDecimal minCpu;

    BigDecimal cpuStep;

    BigDecimal memoryPerCpuGiB;

    Long maxArtifactBytes;

    @Singular("customParameterRule")
    Map<String, String> customParameterRules;

    String capabilityRevision;

    Instant expireAt;
}
