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

package org.apache.streampark.console.core.managed.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Provider-neutral runtime configuration for a managed Flink application. */
@Getter
@Setter
public class ManagedFlinkRuntimeConfig {

    private String engineVersion;

    private String executionMode = "STREAMING";

    private ResourceConfig resource;

    private CheckpointConfig checkpoint;

    private RestartStrategyConfig restartStrategy;

    private Boolean retryOnFailure = false;

    private Integer retryIntervalMin;

    private Integer retryMaxCount;

    private Map<String, String> customProperties = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class ResourceConfig {

        private Integer parallelism;

        private BigDecimal taskManagerCpu;

        private BigDecimal taskManagerMemoryGiB;

        private Integer taskManagerSlots;

        private BigDecimal jobManagerCpu;

        private BigDecimal jobManagerMemoryGiB;
    }

    @Getter
    @Setter
    public static class CheckpointConfig {

        private Boolean enabled = false;

        private Long intervalMs;

        private Long timeoutMs;

        private Long stateTtlMs;

        private String backend;
    }

    @Getter
    @Setter
    public static class RestartStrategyConfig {

        private String type;

        private Map<String, String> parameters = new LinkedHashMap<>();
    }
}
