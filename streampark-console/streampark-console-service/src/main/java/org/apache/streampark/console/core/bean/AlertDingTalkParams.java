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
     * alert phone, separated by ','
     */
    private String contacts;

    /**
     * ding alert url
     */
    private String alertDingURL;

    /**
     * at all
     */
    private Boolean isAtAll = false;

    /**
     * ding robot secret enabled, default false, if enable, need set secret token
     */
    private Boolean secretEnable = false;

    /**
     * ding robot webhook secret_token
     */
    private String secretToken;
}
