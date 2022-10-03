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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

import java.io.Serializable;

@ApiModel(value = "AlertDingTalk")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertDingTalkParams implements Serializable {

    @ApiModelProperty(name = "token", value = "token")
    @NotBlank(message = "The access token of DingTalk must be not empty")
    private String token;

    @ApiModelProperty(name = "contacts", value = "phone, multiple use ',' delimiter")
    private String contacts;

    @ApiModelProperty(name = "alertDingUrl", value = "ding url")
    private String alertDingURL;

    @ApiModelProperty(name = "isAtAll", value = "is @all", example = "false")
    private Boolean isAtAll = false;

    @ApiModelProperty(name = "secretEnable", value = "is ding robot secret enabled", example = "false")
    private Boolean secretEnable = false;

    @ApiModelProperty(name = "secretToken", value = "ding robot webhook secret token")
    private String secretToken;
}
