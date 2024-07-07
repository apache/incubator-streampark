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

@Schema(name = "AlertLark")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertLarkParams implements Serializable {

    @Schema(description = "lark access token")
    @NotBlank(message = "The access token of Lark must be not empty")
    private String token;

    @Schema(description = "is @all", example = "false", implementation = boolean.class)
    private Boolean isAtAll = false;

    @Schema(description = "is lark robot secret enabled", example = "false", defaultValue = "false", implementation = boolean.class)
    private Boolean secretEnable = false;

    @Schema(description = "lark robot webhook secret token")
    private String secretToken;
}
