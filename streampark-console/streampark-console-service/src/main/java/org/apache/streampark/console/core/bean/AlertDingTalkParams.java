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

package org.apache.streampark.console.core.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

import java.io.Serializable;

@Schema(name = "AlertDingTalk")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertDingTalkParams implements Serializable {

    @Schema(description = "dink-talk token")
    @NotBlank(message = "The access token of DingTalk must be not empty")
    private String token;

    @Schema(description = "phone numbers, use ',' to split multiple phone numbers")
    private String contacts;

    @Schema(description = "ding-talk url")
    private String alertDingURL;

    @Schema(description = "is @all", example = "false", defaultValue = "false")
    private Boolean isAtAll = false;

    @Schema(description = "is ding-talk robot secret enabled", example = "false", defaultValue = "false")
    private Boolean secretEnable = false;

    @Schema(description = "ding-talk robot webhook secret token")
    private String secretToken;
}
