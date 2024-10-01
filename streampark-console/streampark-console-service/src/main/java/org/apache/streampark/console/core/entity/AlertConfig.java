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

package org.apache.streampark.console.core.entity;

import org.apache.streampark.console.base.mybatis.entity.BaseEntity;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.bean.AlertConfigParams;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@TableName("t_alert_config")
@Slf4j
public class AlertConfig extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** alert name */
    private String alertName;

    /** alert type */
    private Integer alertType;

    /** email alert parameters */
    private String emailParams;

    /** ding alert parameters */
    private String dingTalkParams;

    /** wecom alert parameters */
    private String weComParams;

    /** alert http callback parameters */
    private String httpCallbackParams;

    /** lark alert parameters */
    private String larkParams;

    public static AlertConfig of(AlertConfigParams params) {
        if (params == null) {
            return null;
        }
        AlertConfig alertConfig = new AlertConfig();
        BeanUtils.copyProperties(
            params,
            alertConfig,
            "emailParams",
            "dingTalkParams",
            "weComParams",
            "httpCallbackParams",
            "larkParams");
        try {
            if (params.getEmailParams() != null) {
                alertConfig.setEmailParams(JacksonUtils.write(params.getEmailParams()));
            }
            if (params.getDingTalkParams() != null) {
                alertConfig.setDingTalkParams(JacksonUtils.write(params.getDingTalkParams()));
            }
            if (params.getWeComParams() != null) {
                alertConfig.setWeComParams(JacksonUtils.write(params.getWeComParams()));
            }
            if (params.getHttpCallbackParams() != null) {
                alertConfig.setHttpCallbackParams(JacksonUtils.write(params.getHttpCallbackParams()));
            }
            if (params.getLarkParams() != null) {
                alertConfig.setLarkParams(JacksonUtils.write(params.getLarkParams()));
            }
        } catch (JsonProcessingException e) {
            log.error("Json write failed", e);
        }
        return alertConfig;
    }
}
