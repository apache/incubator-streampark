/*
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
package com.streamxhub.streamx.console.core.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.streamxhub.streamx.common.conf.ConfigConst;
import com.streamxhub.streamx.common.enums.DevelopmentMode;
import com.streamxhub.streamx.common.util.HadoopUtils;
import com.streamxhub.streamx.common.util.HttpClientUtils;
import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.console.base.util.JsonUtils;
import com.streamxhub.streamx.console.base.util.SpringContextUtils;
import com.streamxhub.streamx.console.core.enums.ApplicationType;
import com.streamxhub.streamx.console.core.enums.DeployState;
import com.streamxhub.streamx.console.core.enums.FlinkAppState;
import com.streamxhub.streamx.console.core.metrics.flink.CheckPoints;
import com.streamxhub.streamx.console.core.metrics.flink.JobsOverview;
import com.streamxhub.streamx.console.core.metrics.flink.Overview;
import com.streamxhub.streamx.console.core.metrics.yarn.AppInfo;
import com.streamxhub.streamx.console.core.service.SettingService;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static com.streamxhub.streamx.console.core.enums.FlinkAppState.of;

/**
 * @author benjobs
 */
@Data
@TableName("t_flink_app")
@Slf4j
public class Application implements Serializable {

    private Long id;

    /**
     * 1) custom code
     * 2) flink SQL
     */
    private Integer jobType;

    private Long projectId;
    /**
     * 创建人
     */
    private Long userId;

    /**
     * 前端和程序在yarn中显示的名称
     */
    private String jobName;

    @TableField(strategy = FieldStrategy.IGNORED)
    private String appId;

    @TableField(strategy = FieldStrategy.IGNORED)
    private String jobId;

    private Integer state;
    /**
     * 是否需要重新发布(针对项目已更新,需要重新发布项目.)
     */
    private Integer deploy;

    /**
     * 任务失败后的最大重启次数.
     */
    private Integer restartSize;

    /**
     * 已经重启的次数
     */
    private Integer restartCount;

    private Integer optionState;

    /**
     * 失败告警的通知邮箱
     */
    private String alertEmail;

    private String args;
    /**
     * 应用程序模块
     */
    private String module;

    private String options;
    private Integer resolveOrder;
    private Integer executionMode;
    private String dynamicOptions;
    private Integer appType;
    private Boolean flameGraph;

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

    /**
     * checkpoint最大的失败次数
     */
    private Integer cpMaxFailureInterval;

    /**
     * checkpoint在时间范围内失败(分钟)
     */
    private Integer cpFailureRateInterval;

    /**
     * 在X分钟之后失败Y次,之后触发的操作:
     * 1: 发送告警
     * 2: 重启
     */
    private Integer cpFailureAction;

    /**
     * overview
     */
    @TableField("TOTAL_TM")
    private Integer totalTM;

    private Integer totalSlot;
    private Integer availableSlot;
    private Integer jmMemory;
    private Integer tmMemory;
    private Integer totalTask;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * running job
     */
    private transient JobsOverview.Task overview;

    private transient String dependency;
    private transient Long sqlId;
    private transient String flinkSql;

    private transient Integer[] stateArray;
    private transient Integer[] jobTypeArray;

    private transient Boolean backUp = false;
    private transient Boolean restart = false;
    private transient String userName;
    private transient String config;
    private transient Long configId;
    private transient String confPath;
    private transient Integer format;
    private transient String savePoint;
    private transient Boolean savePointed = false;
    private transient Boolean drain = false;
    private transient Boolean allowNonRestored = false;
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
            case SUCCEEDED:
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
    public boolean cpFailedTrigger() {
        return this.cpMaxFailureInterval != null && this.cpFailureRateInterval != null && this.cpFailureAction != null;
    }

    @JsonIgnore
    public boolean eqFlinkJob(Application other) {
        if (this.isFlinkSqlJob() && other.isFlinkSqlJob()) {
            if (this.getFlinkSql().trim().equals(other.getFlinkSql().trim())) {
                return this.getDependencyObject().eq(other.getDependencyObject());
            }
        }
        return false;
    }

    @JsonIgnore
    public File getLocalAppBase() {
        String localWorkspace = SpringContextUtils.getBean(SettingService.class).getStreamXWorkspace();
        return new File(localWorkspace.concat("/app/").concat(projectId.toString()));
    }

    @JsonIgnore
    public File getLocalFlinkSqlBase() {
        String localWorkspace = SpringContextUtils.getBean(SettingService.class).getStreamXWorkspace();
        File flinkSql = new File(localWorkspace, "flinksql");
        if (!flinkSql.exists()) {
            flinkSql.mkdirs();
        }
        return new File(flinkSql, id.toString());
    }

    @JsonIgnore
    public File getAppHome() {
        String workspace = ConfigConst.APP_WORKSPACE();
        return new File(workspace.concat("/").concat(id.toString()));
    }

    @JsonIgnore
    public AppInfo httpYarnAppInfo() throws Exception {
        if (appId != null) {
            String format = "%s/ws/v1/cluster/apps/%s";
            try {
                String url = String.format(format, HadoopUtils.getRMWebAppURL(false), appId);
                return httpGetDoResult(url, AppInfo.class);
            } catch (IOException e) {
                String url = String.format(format, HadoopUtils.getRMWebAppURL(true), appId);
                return httpGetDoResult(url, AppInfo.class);
            }
        }
        return null;
    }

    @JsonIgnore
    public JobsOverview httpJobsOverview() throws Exception {
        if (appId != null) {
            String format = "%s/proxy/%s/jobs/overview";
            try {
                String url = String.format(format, HadoopUtils.getRMWebAppURL(false), appId);
                return httpGetDoResult(url, JobsOverview.class);
            } catch (IOException e) {
                String url = String.format(format, HadoopUtils.getRMWebAppURL(true), appId);
                return httpGetDoResult(url, JobsOverview.class);
            }
        }
        return null;
    }

    @JsonIgnore
    public Overview httpOverview() throws IOException {
        String format = "%s/proxy/%s/overview";
        try {
            String url = String.format(format, HadoopUtils.getRMWebAppURL(false), appId);
            return httpGetDoResult(url, Overview.class);
        } catch (IOException e) {
            String url = String.format(format, HadoopUtils.getRMWebAppURL(true), appId);
            return httpGetDoResult(url, Overview.class);
        }
    }

    @JsonIgnore
    public CheckPoints httpCheckpoints() throws IOException {
        String format = "%s/proxy/%s/jobs/%s/checkpoints";
        try {
            String url = String.format(format, HadoopUtils.getRMWebAppURL(false), appId, jobId);
            return httpGetDoResult(url, CheckPoints.class);
        } catch (IOException e) {
            String url = String.format(format, HadoopUtils.getRMWebAppURL(true), appId, jobId);
            return httpGetDoResult(url, CheckPoints.class);
        }
    }

    @JsonIgnore
    private <T> T httpGetDoResult(String url, Class<T> clazz) throws IOException {
        String result = HttpClientUtils.httpGetRequest(url);
        if (result != null) {
            return JsonUtils.read(result, clazz);
        }
        return null;
    }

    @JsonIgnore
    public ApplicationType getApplicationType() {
        return ApplicationType.of(appType);
    }

    @JsonIgnore
    @SneakyThrows
    public Map<String, Object> getOptionMap() {
        Map<String, Object> map = JsonUtils.read(getOptions(), Map.class);
        map.entrySet().removeIf(entry -> entry.getValue() == null);
        return map;
    }

    @JsonIgnore
    public boolean isFlinkSqlJob() {
        return DevelopmentMode.FLINKSQL.getValue().equals(this.getJobType());
    }

    @JsonIgnore
    public boolean isCustomCodeJob() {
        return DevelopmentMode.CUSTOMCODE.getValue().equals(this.getJobType());
    }

    public boolean isStreamXJob() {
        return this.getAppType() == ApplicationType.STREAMX_FLINK.getType();
    }

    @JsonIgnore
    @SneakyThrows
    public Dependency getDependencyObject() {
        return Dependency.jsonToDependency(this.dependency);
    }

    @JsonIgnore
    public boolean isRunning() {
        return FlinkAppState.RUNNING.getValue() == this.getState();
    }

    @JsonIgnore
    public boolean isNeedRollback() {
        return DeployState.NEED_ROLLBACK.get() == this.getDeploy();
    }

    @JsonIgnore
    public boolean isNeedRestartOnFailed() {
        return this.restartSize != null && this.restartSize > 0 && this.restartCount <= this.restartSize;
    }

    /**
     * 参数对比,主要是对比Flink运行时相关的参数是否发生了变化
     *
     * @param other
     * @return
     */
    @JsonIgnore
    public boolean eqJobParam(Application other) {
        //1) Resolve Order 是否发生变化
        //2) Execution Mode 是否发生变化
        //3) Parallelism 是否发生变化
        //4) Task Slots 是否发生变化
        //5) Options 是否发生变化
        //6) Dynamic Option 是否发生变化
        //7) Program Args 是否发生变化
        if (!this.getResolveOrder().equals(other.getResolveOrder()) ||
                !this.getExecutionMode().equals(other.getExecutionMode())) {
            return false;
        }

        if (this.getOptions() != null) {
            if (other.getOptions() != null) {
                if (!this.getOptions().trim().equals(other.getOptions().trim())) {
                    Map<String, Object> optMap = this.getOptionMap();
                    Map<String, Object> otherMap = other.getOptionMap();
                    if (optMap.size() != otherMap.size()) {
                        return false;
                    }
                    for (Map.Entry<String, Object> entry : optMap.entrySet()) {
                        if (!entry.getValue().equals(otherMap.get(entry.getKey()))) {
                            return false;
                        }
                    }
                }
            } else {
                return false;
            }
        } else if (other.getOptions() != null) {
            return false;
        }

        if (this.getDynamicOptions() != null) {
            if (other.getDynamicOptions() != null) {
                if (!this.getDynamicOptions().trim().equals(other.getDynamicOptions().trim())) {
                    return false;
                }
            } else {
                return false;
            }
        } else if (other.getDynamicOptions() != null) {
            return false;
        }

        if (this.getArgs() != null) {
            if (other.getArgs() != null) {
                return this.getArgs().trim().equals(other.getArgs().trim());
            } else {
                return false;
            }
        } else {
            return other.getArgs() == null;
        }
    }

    @Data
    public static class Dependency {
        private List<Pom> pom = Collections.emptyList();
        private List<String> jar = Collections.emptyList();

        @JsonIgnore
        @SneakyThrows
        public static Dependency jsonToDependency(String dependency) {
            if (Utils.notEmpty(dependency)) {
                return JsonUtils.read(dependency, new TypeReference<Dependency>() {
                });
            }
            return new Dependency();
        }

        public boolean isEmpty() {
            return pom.isEmpty() && jar.isEmpty();
        }

        public boolean eq(Dependency other) {
            if (other == null) {
                return false;
            }
            if (this.isEmpty() && other.isEmpty()) {
                return true;
            }

            if (this.pom.size() != other.pom.size() || this.jar.size() != other.jar.size()) {
                return false;
            }

            Map<String, String> jarMap = new HashMap<>(jar.size());
            jar.forEach(x -> jarMap.put(x, x));

            Map<String, String> jarMap2 = new HashMap<>(other.jar.size());
            other.jar.forEach(x -> jarMap2.put(x, x));

            for (Map.Entry<String, String> entry : jarMap.entrySet()) {
                if (!jarMap2.containsKey(entry.getKey())) {
                    return false;
                }
            }

            Map<String, Pom> pomMap = new HashMap<>(pom.size());
            pom.forEach(x -> pomMap.put(x.getGav(), x));

            Map<String, Pom> pomMap2 = new HashMap<>(other.pom.size());
            other.pom.forEach(x -> pomMap2.put(x.getGav(), x));
            return Pom.checkPom(pomMap, pomMap2);
        }

    }

    @Data
    public static class Pom {
        private String groupId;
        private String artifactId;
        private String version;
        private List<Pom> exclusions = Collections.emptyList();

        @Override
        public String toString() {
            return "{" +
                    "groupId='" + groupId + '\'' +
                    ", artifactId='" + artifactId + '\'' +
                    ", version='" + version + '\'' +
                    '}';
        }

        private String getGav() {
            return this.groupId + ":" + this.artifactId + ":" + this.version;
        }


        private String getGa() {
            return this.groupId + ":" + this.artifactId;
        }

        public boolean eq(Pom other) {
            if (other == null) {
                return false;
            }
            if (!this.getGav().equals(other.getGav())) {
                return false;
            }

            if (exclusions.size() != other.exclusions.size()) {
                return false;
            }

            Map<String, Pom> pomMap = new HashMap<>(exclusions.size());
            exclusions.forEach(x -> pomMap.put(x.getGa(), x));

            Map<String, Pom> pomMap2 = new HashMap<>(other.exclusions.size());
            other.exclusions.forEach(x -> pomMap2.put(x.getGa(), x));

            return checkPom(pomMap, pomMap2);
        }

        public static boolean checkPom(Map<String, Pom> pomMap, Map<String, Pom> pomMap2) {
            for (Map.Entry<String, Pom> entry : pomMap.entrySet()) {
                Pom pom = pomMap2.get(entry.getKey());
                if (pom == null) {
                    return false;
                }
                if (!entry.getValue().eq(pom)) {
                    return false;
                }
            }
            return true;
        }

        @JsonIgnore
        public String getPath() {
            return getGroupId() + "_" + getArtifactId() + "-" + getVersion() + ".jar";
        }
    }

}
