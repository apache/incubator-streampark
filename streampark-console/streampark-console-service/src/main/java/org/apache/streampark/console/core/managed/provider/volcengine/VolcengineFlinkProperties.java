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

package org.apache.streampark.console.core.managed.provider.volcengine;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Runtime limits for the Volcengine managed Flink OpenAPI client. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "streampark.managed-flink.providers.volcengine.client")
public class VolcengineFlinkProperties {

    private String endpoint = "https://open.volcengineapi.com";

    private String iamEndpoint = "https://iam.volcengineapi.com";

    private int connectTimeoutMs = 3000;

    private int requestTimeoutMs = 10000;

    private int maxReadRetries = 2;

    private long initialBackoffMs = 200;

    private long maxBackoffMs = 2000;

    private int maxConcurrentRequestsPerAccount = 4;

    private long minRequestIntervalMs = 50;

    private long capabilityTtlMinutes = 10;
}
