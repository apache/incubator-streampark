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
package com.streamxhub.console.core.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamxhub.common.conf.ConfigConst;
import com.streamxhub.common.util.HttpClientUtils;
import com.streamxhub.console.base.properties.StreamXProperties;
import com.streamxhub.console.base.utils.SpringContextUtil;
import com.streamxhub.console.core.enums.ApplicationType;
import com.streamxhub.console.core.enums.FlinkAppState;
import com.streamxhub.console.core.metrics.flink.JobsOverview;
import com.streamxhub.console.core.metrics.flink.Overview;
import com.streamxhub.console.core.metrics.yarn.AppInfo;
import com.wuwenze.poi.annotation.Excel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Date;
import java.util.Map;

import static com.streamxhub.console.core.enums.FlinkAppState.*;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_app")
@Excel("flink应用实体")
@Slf4j
public class Application implements Serializable {

    private Long id;

    private Long projectId;
    /**
     * 创建人
     */
    private Long userId;
    /**
     * 前端和程序在yarn中显示的名称
     */
    private String jobName;
    private String appId;
    private String jobId;
    private Integer state;
    /**
     * 是否需要重新发布(针对项目已更新,具体影响需要手动发布.)
     */
    private Integer deploy;

    private Integer optionState;

    private String args;
    /**
     * 应用程序模块
     */
    private String module;
    private String options;
    private String dynamicOptions;
    private Integer appType;
    /**
     * 是否需要跟踪监控状态
     */
    private Integer tracking;
    private String jar;
    private String mainClass;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(strategy = FieldStrategy.IGNORED)
    private Date endTime;

    private Long duration;

    // overview...
    @TableField("TOTAL_TM")
    private Integer totalTM;
    private Integer totalSlot;
    private Integer availableSlot;
    private String flinkCommit;
    private String jmMemory;
    private String tmMemory;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    private transient Boolean backUp;
    private transient Boolean restart;
    private transient String userName;
    private transient String config;
    private transient Long configId;
    private transient String confPath;
    private transient Integer format;
    private transient String savePoint;
    private transient Boolean savePointed;
    private transient Boolean drain;
    private transient Boolean allowNonRestored;
    private transient String projectName;
    private transient String createTimeFrom;
    private transient String createTimeTo;
    private transient String backUpDescription;

    public void setState(Integer state) {
        this.state = state;
        FlinkAppState appState = of(this.state);
        switch (appState) {
            case DEPLOYING:
            case DEPLOYED:
            case CREATED:
            case FINISHED:
            case FAILED:
            case CANCELED:
            case LOST:
                this.tracking = 0;
                break;
            default:
                this.tracking = 1;
                break;
        }
    }

    @JsonIgnore
    public File getAppBase() {
        String localWorkspace = SpringContextUtil.getBean(StreamXProperties.class).getAppHome();
        return new File(localWorkspace.concat("/app/").concat(projectId.toString()));
    }

    @JsonIgnore
    public String getWorkspace(boolean withModule) {
        String workspace = ConfigConst.APP_WORKSPACE().concat("/").concat(id.toString());
        if (withModule) {
            String module = getModule().replaceFirst("^.*/", "");
            workspace = workspace.concat("/").concat(module);
        }
        return workspace;
    }

    @JsonIgnore
    public AppInfo getYarnAppInfo() throws Exception {
        String yarn = SpringContextUtil.getBean(StreamXProperties.class).getYarn();
        String url = yarn.concat("/ws/v1/cluster/apps/").concat(appId);
        String result = HttpClientUtils.httpGetRequest(url);
        if (result != null) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new StringReader(result), AppInfo.class);
        }
        return null;
    }

    @JsonIgnore
    public JobsOverview getJobsOverview() throws IOException {
        String yarn = SpringContextUtil.getBean(StreamXProperties.class).getYarn();
        String url = yarn.concat("/proxy/").concat(appId).concat("/jobs/overview");
        try {
            String result = HttpClientUtils.httpGetRequest(url);
            if (result != null) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(new StringReader(result), JobsOverview.class);
            }
        } catch (IOException e) {
            throw e;
        }
        return null;
    }

    @JsonIgnore
    public Overview getOverview() throws IOException {
        String yarn = SpringContextUtil.getBean(StreamXProperties.class).getYarn();
        String url = yarn.concat("/proxy/").concat(appId).concat("/overview");
        try {
            String result = HttpClientUtils.httpGetRequest(url);
            if (result != null) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(new StringReader(result), Overview.class);
            }
        } catch (IOException e) {
            throw e;
        }
        return null;
    }

    @JsonIgnore
    public ApplicationType getApplicationType() {
        return ApplicationType.of(appType);
    }

    @JsonIgnore
    public Map<String, Object> getOptionMap() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = mapper.readValue(getOptions(), Map.class);
        return map;
    }

}
