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

import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.entity.AlertConfig;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

@Schema(name = "AlertConfig")
@Data
@Slf4j
public class AlertConfigParams implements Serializable {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "100000")
    private Long userId;

    @Schema(example = "test-alert")
    private String alertName;

    @Schema(example = "1", description = "base type, email 1, dink-talk 2, we-com 4, http callback 8, lark 16, also choose a combination, e.g. 3 means email + dink-talk")
    private Integer alertType;

    @Schema(description = "email alert parameters")
    private AlertEmailParams emailParams;

    @Schema(description = "ding-talk alert parameters")
    private AlertDingTalkParams dingTalkParams;

    @Schema(description = "we-com alert parameters")
    private AlertWeComParams weComParams;

    @Schema(description = "http callback alert parameters")
    private AlertHttpCallbackParams httpCallbackParams;

    @Schema(description = "lark alert parameters")
    private AlertLarkParams larkParams;

    public static AlertConfigParams of(AlertConfig config) {
        if (config == null) {
            return null;
        }
        AlertConfigParams params = new AlertConfigParams();
        BeanUtils.copyProperties(
            config,
            params,
            "emailParams",
            "dingTalkParams",
            "weComParams",
            "httpCallbackParams",
            "larkParams");
        try {
            if (StringUtils.isNotBlank(config.getEmailParams())) {
                params.setEmailParams(JacksonUtils.read(config.getEmailParams(), AlertEmailParams.class));
            }
            if (StringUtils.isNotBlank(config.getDingTalkParams())) {
                params.setDingTalkParams(
                    JacksonUtils.read(config.getDingTalkParams(), AlertDingTalkParams.class));
            }
            if (StringUtils.isNotBlank(config.getWeComParams())) {
                params.setWeComParams(JacksonUtils.read(config.getWeComParams(), AlertWeComParams.class));
            }
            if (StringUtils.isNotBlank(config.getHttpCallbackParams())) {
                params.setHttpCallbackParams(
                    JacksonUtils.read(config.getHttpCallbackParams(), AlertHttpCallbackParams.class));
            }
            if (StringUtils.isNotBlank(config.getLarkParams())) {
                params.setLarkParams(JacksonUtils.read(config.getLarkParams(), AlertLarkParams.class));
            }
        } catch (JsonProcessingException e) {
            log.error("Json read failed", e);
        }
        return params;
    }
}
