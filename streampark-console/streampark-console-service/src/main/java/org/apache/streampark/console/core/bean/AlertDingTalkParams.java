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
import lombok.Data;

import javax.validation.constraints.NotBlank;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertDingTalkParams implements Serializable {
    @NotBlank(message = "The access token of DingTalk must not be empty")
    private String token;

    /**
     * 报警人手机号, 多个用','分割
     */
    private String contacts;

    /**
     * 钉钉告警url
     */
    private String alertDingURL;

    /**
     * 是否@所有人
     */
    private Boolean isAtAll = false;

    /**
     * 钉钉机器人是否启用加签，默认 false，启用加签需设置 secret_token
     */
    private Boolean secretEnable = false;

    /**
     * 钉钉机器人 WebHook 地址的 secret_token,群机器人加签用
     */
    private String secretToken;
}
