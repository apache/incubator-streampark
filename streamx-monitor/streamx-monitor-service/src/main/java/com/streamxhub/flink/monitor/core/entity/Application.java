/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.flink.monitor.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.streamxhub.common.conf.ConfigConst;
import com.streamxhub.flink.monitor.base.properties.StreamXProperties;
import com.streamxhub.flink.monitor.base.utils.SpringContextUtil;
import com.wuwenze.poi.annotation.Excel;
import lombok.Data;

import java.io.File;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_flink_app")
@Excel("flink应用实体")
public class Application implements Serializable {
    /**
     * what fuck。。。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long projectId;
    /**
     * 创建人
     */
    private Long userId;
    private String config;
    /**
     * 仅前端页面显示的任务名称
     */
    private String appName;
    /**
     * 程序在yarn中的名称
     */
    private String yarnName;
    private String appId;
    private Integer state;
    /**
     * 是否需要重新发布(针对项目已更新,具体影响需要手动发布.)
     */
    private Integer deploy;
    private String args;
    private String module;//应用程序模块
    private String options;
    private String shortOptions;

    private String description;
    private Date createTime;
    private String workspace;
    private transient String userName;
    private transient String projectName;
    private transient String createTimeFrom;
    private transient String createTimeTo;

    public File getAppBase() {
        String localWorkspace = SpringContextUtil.getBean(StreamXProperties.class).getAppHome();
        return new File(localWorkspace.concat("/app/").concat(projectId.toString()));
    }

    public String backupPath() {
        return ConfigConst.APP_HISTORY()
                .concat("/")
                .concat(id.toString())
                .concat("/")
                .concat(System.currentTimeMillis()+"");
    }
}
