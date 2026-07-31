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

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

/** Updates a provider-managed Flink environment registration. */
@Getter
@Setter
public class ManagedFlinkEnvironmentUpdateRequest {

    @NotNull
    @Positive
    private Long teamId;

    @NotNull
    @Positive
    private Long clusterId;

    @NotNull
    @Min(0)
    private Integer version;

    @NotBlank
    @Size(max = 128)
    private String clusterName;

    @Size(max = 255)
    private String description;

    @NotNull
    @Positive
    private Long cloudAccountId;

    @NotBlank
    @Size(max = 128)
    private String projectId;

    @Size(max = 128)
    private String projectName;

    @NotBlank
    @Size(max = 128)
    private String resourcePoolId;

    @Size(max = 128)
    private String resourcePoolName;

    @NotNull
    @Positive
    private Long draftDirectoryId;
}
