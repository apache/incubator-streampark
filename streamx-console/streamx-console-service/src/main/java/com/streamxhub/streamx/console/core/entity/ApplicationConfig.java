/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.entity;

import com.streamxhub.streamx.common.util.DeflaterUtils;
import com.streamxhub.streamx.common.util.PropertiesUtils;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.Date;
import java.util.Map;

import scala.collection.JavaConversions;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_config")
@Slf4j
public class ApplicationConfig {

    private Long id;

    private Long appId;

    /**
     * 1)yaml <br>
     * 2)prop
     */
    private Integer format;

    /**
     * 默认版本号:1
     */
    private Integer version = 1;

    @TableField(strategy = FieldStrategy.IGNORED)
    private String content;

    private Date createTime;

    /**
     * 记录要设置的目标要生效的配置
     */
    private Boolean latest;

    private transient boolean effective = false;

    public void setToApplication(Application application) {
        String unzipString = DeflaterUtils.unzipString(content);
        String encode = Base64.getEncoder().encodeToString(unzipString.getBytes());
        application.setConfig(encode);
        application.setConfigId(this.id);
        application.setFormat(this.format);
    }

    @JsonIgnore
    public Map<String, String> readConfig() {
        switch (this.getFormat()) {
            case 1:
                return JavaConversions.mapAsJavaMap(PropertiesUtils.fromYamlText(DeflaterUtils.unzipString(this.content)));
            case 2:
                return JavaConversions.mapAsJavaMap(PropertiesUtils.fromPropertiesText(DeflaterUtils.unzipString(this.content)));
            default:
                break;
        }
        return null;
    }
}
