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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

@ApiModel(value = "AlertConfig")
@Data
@Slf4j
public class AlertConfigWithParams implements Serializable {

    @ApiModelProperty(name = "id", value = "id")
    private Long id;

    @ApiModelProperty(name = "userId", value = "user id")
    private Long userId;

    @ApiModelProperty(name = "alertName", value = "alert name")
    private String alertName;

    @ApiModelProperty(name = "alertType", value = "alert type")
    private Integer alertType;

    @ApiModelProperty(name = "emailParams", value = "email alert params")
    private AlertEmailParams emailParams;

    @ApiModelProperty(name = "dingTalkParams", value = "ding-talk alert params")
    private AlertDingTalkParams dingTalkParams;

    @ApiModelProperty(name = "weComParams", value = "we-com alert params")
    private AlertWeComParams weComParams;

    @ApiModelProperty(name = "httpCallbackParams", value = "http callback alert params")
    private AlertHttpCallbackParams httpCallbackParams;

    @ApiModelProperty(name = "larkParams", value = "lark alert params")
    private AlertLarkParams larkParams;

    public static AlertConfigWithParams of(AlertConfig config) {
        if (config == null) {
            return null;
        }
        AlertConfigWithParams params = new AlertConfigWithParams();
        BeanUtils.copyProperties(config, params, "emailParams", "dingTalkParams", "weComParams", "httpCallbackParams", "larkParams");
        try {
            if (StringUtils.isNotBlank(config.getEmailParams())) {
                params.setEmailParams(JacksonUtils.read(config.getEmailParams(), AlertEmailParams.class));
            }
            if (StringUtils.isNotBlank(config.getDingTalkParams())) {
                params.setDingTalkParams(JacksonUtils.read(config.getDingTalkParams(), AlertDingTalkParams.class));
            }
            if (StringUtils.isNotBlank(config.getWeComParams())) {
                params.setWeComParams(JacksonUtils.read(config.getWeComParams(), AlertWeComParams.class));
            }
            if (StringUtils.isNotBlank(config.getHttpCallbackParams())) {
                params.setHttpCallbackParams(JacksonUtils.read(config.getHttpCallbackParams(), AlertHttpCallbackParams.class));
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
