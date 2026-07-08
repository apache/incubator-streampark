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

package org.apache.streampark.flink.client.bean;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.annotation.Nullable;

import java.util.Map;

@Data
@Accessors(fluent = true)
@Builder
@NoArgsConstructor
public class SubmitResponse {

    private String clusterId;
    private Map<String, String> flinkConfig;

    @Nullable
    @Builder.Default
    private String jobId = "";

    @Nullable
    @Builder.Default
    private String jobManagerUrl = "";

    public SubmitResponse(
                          String clusterId,
                          Map<String, String> flinkConfig,
                          String jobId,
                          String jobManagerUrl) {
        this.clusterId = clusterId;
        this.flinkConfig = flinkConfig;
        this.jobId = jobId;
        this.jobManagerUrl = jobManagerUrl;
    }
}
