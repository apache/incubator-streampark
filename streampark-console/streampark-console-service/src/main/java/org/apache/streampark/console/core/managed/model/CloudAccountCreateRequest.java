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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Request used to create a managed Flink cloud account. */
@Getter
@Setter
public class CloudAccountCreateRequest {

    @NotBlank
    @Size(max = 128)
    private String accountName;

    @NotBlank
    @Size(max = 32)
    private String providerType;

    @NotBlank
    @Size(max = 64)
    private String region;

    @Size(max = 255)
    private String endpoint;

    @NotBlank
    @Size(max = 1024)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String accessKey;

    @NotBlank
    @Size(max = 1024)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String secretKey;

    @Size(max = 255)
    private String description;
}
