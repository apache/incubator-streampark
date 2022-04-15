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

import static com.streamxhub.streamx.console.core.enums.FlinkAppState.of;

import com.streamxhub.streamx.common.conf.ConfigConst;
import com.streamxhub.streamx.common.conf.K8sFlinkConfig;
import com.streamxhub.streamx.common.conf.Workspace;
import com.streamxhub.streamx.common.enums.ApplicationType;
import com.streamxhub.streamx.common.enums.DevelopmentMode;
import com.streamxhub.streamx.common.enums.ExecutionMode;
import com.streamxhub.streamx.common.enums.FlinkK8sRestExposedType;
import com.streamxhub.streamx.common.enums.StorageType;
import com.streamxhub.streamx.common.fs.FsOperator;
import com.streamxhub.streamx.common.util.HadoopUtils;
import com.streamxhub.streamx.common.util.HttpClientUtils;
import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.console.base.util.JacksonUtils;
import com.streamxhub.streamx.console.base.util.ObjectUtils;
import com.streamxhub.streamx.console.core.enums.FlinkAppState;
import com.streamxhub.streamx.console.core.enums.LaunchState;
import com.streamxhub.streamx.console.core.enums.ResourceFrom;
import com.streamxhub.streamx.console.core.metrics.flink.CheckPoints;
import com.streamxhub.streamx.console.core.metrics.flink.JobsOverview;
import com.streamxhub.streamx.console.core.metrics.flink.Overview;
import com.streamxhub.streamx.console.core.metrics.yarn.AppInfo;
import com.streamxhub.streamx.flink.kubernetes.model.K8sPodTemplates;
import com.streamxhub.streamx.flink.packer.maven.DependencyInfo;
import com.streamxhub.streamx.flink.packer.maven.MavenArtifact;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    /**
     * 对应的flink的版本.
     */
    private Long versionId;

    /**
     * k8s部署下clusterId
     */
    private String clusterId;

    /**
     * flink docker base image
     */
    private String flinkImage;

    /**
     * k8s部署下的namespace
     */
    private String k8sNamespace = K8sFlinkConfig.DEFAULT_KUBERNETES_NAMESPACE();


    private Integer state;
    /**
     * 任务的上线发布状态
     */
    private Integer launch;

    /**
     * 任务实现需要构建
     */
    private Boolean build;

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
    private String hotParams;
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

    /**
     * 针对upload 类型任务,需要记录checkSum,用于判断更新修改之后是否需要重新发布.
     */
    private Long jarCheckSum;

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

    /**
     * remote 模式下与任务绑定的cluster
     */
    private Long flinkClusterId;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * The exposed type of the rest service of K8s(kubernetes.rest-service.exposed.type)
     */
    private Integer k8sRestExposedType;
    /**
     * flink kubernetes pod template
     */
    private String k8sPodTemplate;
    private String k8sJmPodTemplate;
    private String k8sTmPodTemplate;

    /**
     * 1: cicd (build from csv)
     * 2: upload (upload local jar job)
     */
    private Integer resourceFrom;

    /**
     * flink-hadoop integration on flink-k8s mode
     */
    private Boolean k8sHadoopIntegration;

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
    private transient String flinkVersion;
    private transient String confPath;
    private transient Integer format;
    private transient String savePoint;
    private transient Boolean savePointed = false;
    private transient Boolean drain = false;
    private transient Boolean allowNonRestored = false;
    private transient String socketId;
    private transient String projectName;
    private transient String createTimeFrom;
    private transient String createTimeTo;
    private transient String backUpDescription;
    private transient String yarnQueue;
    private transient String yarnSessionClusterId;
    /**
     * Flink Web UI Url
     */
    private transient String flinkRestUrl;

    /**
     * refer to {@link com.streamxhub.streamx.flink.packer.pipeline.BuildPipeline}
     */
    private transient Integer buildStatus;
    private transient AppControl appControl;

    public void setK8sNamespace(String k8sNamespace) {
        this.k8sNamespace = StringUtils.isBlank(k8sNamespace) ? K8sFlinkConfig.DEFAULT_KUBERNETES_NAMESPACE() : k8sNamespace;
    }

    public K8sPodTemplates getK8sPodTemplates() {
        return K8sPodTemplates.of(k8sPodTemplate, k8sJmPodTemplate, k8sTmPodTemplate);
    }

    public void setState(Integer state) {
        this.state = state;
        FlinkAppState appState = of(this.state);
        this.tracking = shouldTracking(appState);
    }

    /**
     * Determine if a FlinkAppState requires tracking.
     *
     * @return 1: need to be tracked | 0: no need to be tracked.
     */
    public static Integer shouldTracking(@Nonnull FlinkAppState state) {
        switch (state) {
            case ADDED:
            case CREATED:
            case FINISHED:
            case FAILED:
            case CANCELED:
            case TERMINATED:
            case POS_TERMINATED:
            case LOST:
                return 0;
            default:
                return 1;
        }
    }

    public boolean shouldBeTrack() {
        return shouldTracking(FlinkAppState.of(getState())) == 1;
    }

    @JsonIgnore
    public LaunchState getLaunchState() {
        return LaunchState.of(state);
    }

    @JsonIgnore
    public void setLaunchState(LaunchState launchState) {
        this.launch = launchState.get();
    }

    @JsonIgnore
    public DevelopmentMode getDevelopmentMode() {
        return DevelopmentMode.of(jobType);
    }

    @JsonIgnore
    public void setDevelopmentMode(DevelopmentMode mode) {
        this.jobType = mode.getValue();
    }

    @JsonIgnore
    public FlinkAppState getFlinkAppStateEnum() {
        return FlinkAppState.of(state);
    }

    @JsonIgnore
    public FlinkK8sRestExposedType getK8sRestExposedTypeEnum() {
        return FlinkK8sRestExposedType.of(this.k8sRestExposedType);
    }

    @JsonIgnore
    public ExecutionMode getExecutionModeEnum() {
        return ExecutionMode.of(executionMode);
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

    /**
     * 本地的编译打包工作目录
     *
     * @return
     */
    @JsonIgnore
    public String getDistHome() {
        String path = String.format("%s/%s/%s",
                Workspace.local().APP_LOCAL_DIST(),
                projectId.toString(),
                getModule()
        );
        log.info("local distHome:{}", path);
        return path;
    }

    @JsonIgnore
    public String getLocalAppHome() {
        String path = String.format("%s/%s",
                Workspace.local().APP_WORKSPACE(),
                id.toString()
        );
        log.info("local appHome:{}", path);
        return path;
    }

    @JsonIgnore
    public String getRemoteAppHome() {
        String path = String.format(
                "%s/%s",
                Workspace.remote().APP_WORKSPACE(),
                id.toString()
        );
        log.info("remote appHome:{}", path);
        return path;
    }

    /**
     * 根据 app ExecutionModeEnum 自动识别remoteAppHome 或 localAppHome
     *
     * @return
     */
    @JsonIgnore
    public String getAppHome() {
        switch (this.getExecutionModeEnum()) {
            case KUBERNETES_NATIVE_APPLICATION:
            case KUBERNETES_NATIVE_SESSION:
            case YARN_PER_JOB:
            case YARN_SESSION:
            case REMOTE:
            case LOCAL:
                return getLocalAppHome();
            case YARN_APPLICATION:
                return getRemoteAppHome();
            default:
                throw new UnsupportedOperationException("unsupported executionMode ".concat(getExecutionModeEnum().getName()));
        }
    }

    @JsonIgnore
    public String getAppLib() {
        return getAppHome().concat("/lib");
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
    public Overview httpOverview(FlinkEnv env, FlinkCluster flinkCluster) throws IOException {
        final String flinkUrl = "overview";
        if (appId != null) {
            if (getExecutionModeEnum().equals(ExecutionMode.YARN_APPLICATION) ||
                    getExecutionModeEnum().equals(ExecutionMode.YARN_PER_JOB)) {
                String format = "%s/proxy/%s/" + flinkUrl;
                try {
                    String url = String.format(format, HadoopUtils.getRMWebAppURL(false), appId);
                    return httpGetDoResult(url, Overview.class);
                } catch (IOException e) {
                    String url = String.format(format, HadoopUtils.getRMWebAppURL(true), appId);
                    return httpGetDoResult(url, Overview.class);
                }
                // TODO: yarn-session
                //String remoteUrl = getFlinkClusterRestUrl(flinkCluster, flinkUrl);
                //return httpGetDoResult(remoteUrl, Overview.class);
            }
        }
        return null;
    }

    @JsonIgnore
    public JobsOverview httpJobsOverview(FlinkEnv env, FlinkCluster flinkCluster) throws Exception {
        final String flinkUrl = "jobs/overview";
        if (ExecutionMode.isYarnMode(executionMode)) {
            if (appId != null) {
                String format = "%s/proxy/%s/" + flinkUrl;
                JobsOverview jobsOverview;
                try {
                    String url = String.format(format, HadoopUtils.getRMWebAppURL(false), appId);
                    jobsOverview = httpGetDoResult(url, JobsOverview.class);
                } catch (IOException e) {
                    String url = String.format(format, HadoopUtils.getRMWebAppURL(true), appId);
                    jobsOverview = httpGetDoResult(url, JobsOverview.class);
                }
                if (jobsOverview != null && ExecutionMode.YARN_SESSION.equals(getExecutionModeEnum())) {
                    //过滤出当前job
                    List<JobsOverview.Job> jobs = jobsOverview.getJobs().stream().filter(x -> x.getId().equals(jobId)).collect(Collectors.toList());
                    jobsOverview.setJobs(jobs);
                }
                return jobsOverview;
            }
        } else if (ExecutionMode.isRemoteMode(executionMode)) {
            if (jobId != null) {
                String remoteUrl = getFlinkClusterRestUrl(flinkCluster, flinkUrl);
                JobsOverview jobsOverview = httpGetDoResult(remoteUrl, JobsOverview.class);
                if (jobsOverview != null) {
                    //过滤出当前job
                    List<JobsOverview.Job> jobs = jobsOverview.getJobs().stream().filter(x -> x.getId().equals(jobId)).collect(Collectors.toList());
                    jobsOverview.setJobs(jobs);
                }
                return jobsOverview;
            }
        }
        return null;
    }

    @JsonIgnore
    public CheckPoints httpCheckpoints(FlinkEnv env, FlinkCluster flinkCluster) throws IOException {
        final String flinkUrl = "jobs/%s/checkpoints";
        if (ExecutionMode.isYarnMode(executionMode)) {
            if (appId != null) {
                String format = "%s/proxy/%s/" + flinkUrl;
                try {
                    String url = String.format(format, HadoopUtils.getRMWebAppURL(false), appId, jobId);
                    return httpGetDoResult(url, CheckPoints.class);
                } catch (IOException e) {
                    String url = String.format(format, HadoopUtils.getRMWebAppURL(true), appId, jobId);
                    return httpGetDoResult(url, CheckPoints.class);
                }
            }
        } else if (ExecutionMode.isRemoteMode(executionMode)) {
            if (jobId != null) {
                String remoteUrl = getFlinkClusterRestUrl(flinkCluster, String.format(flinkUrl, jobId));
                return httpGetDoResult(remoteUrl, CheckPoints.class);
            }
        }
        return null;
    }

    @JsonIgnore
    private <T> T httpGetDoResult(String url, Class<T> clazz) throws IOException {
        String result = HttpClientUtils.httpGetRequest(url, RequestConfig.custom().setConnectTimeout(5000).build());
        if (result != null) {
            return JacksonUtils.read(result, clazz);
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
        Map<String, Object> map = JacksonUtils.read(getOptions(), Map.class);
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

    @JsonIgnore
    public boolean isUploadJob() {
        return isCustomCodeJob() && ResourceFrom.UPLOAD.getValue().equals(this.getResourceFrom());
    }

    @JsonIgnore
    public boolean isCICDJob() {
        return isCustomCodeJob() && ResourceFrom.CICD.getValue().equals(this.getResourceFrom());
    }

    public boolean isStreamXJob() {
        return this.getAppType() == ApplicationType.STREAMX_FLINK.getType();
    }

    @JsonIgnore
    private String getFlinkClusterRestUrl(FlinkCluster cluster, String url) throws MalformedURLException {
        return cluster.getActiveAddress().toURL() + "/" + url;
    }

    @JsonIgnore
    @SneakyThrows
    public Dependency getDependencyObject() {
        return Dependency.jsonToDependency(this.dependency);
    }

    @JsonIgnore
    public DependencyInfo getDependencyInfo() {
        return Application.Dependency.jsonToDependency(getDependency()).toJarPackDeps();
    }

    @JsonIgnore
    public boolean isRunning() {
        return FlinkAppState.RUNNING.getValue() == this.getState();
    }

    @JsonIgnore
    public boolean isNeedRollback() {
        return LaunchState.NEED_ROLLBACK.get() == this.getLaunch();
    }

    @JsonIgnore
    public boolean isNeedRestartOnFailed() {
        if (this.restartSize != null && this.restartCount != null) {
            return this.restartSize > 0 && this.restartCount <= this.restartSize;
        }
        return false;
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
        //2) flink Version是否发生变化
        //3) Execution Mode 是否发生变化
        //4) Parallelism 是否发生变化
        //5) Task Slots 是否发生变化
        //6) Options 是否发生变化
        //7) Dynamic Option 是否发生变化
        //8) Program Args 是否发生变化
        //9) Flink Version  是否发生变化

        if (!ObjectUtils.safeEquals(this.getVersionId(), other.getVersionId())) {
            return false;
        }

        if (!ObjectUtils.safeEquals(this.getResolveOrder(), other.getResolveOrder()) ||
                !ObjectUtils.safeEquals(this.getExecutionMode(), other.getExecutionMode()) ||
                !ObjectUtils.safeEquals(this.getK8sRestExposedType(), other.getK8sRestExposedType())) {
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

    @JsonIgnore
    public StorageType getStorageType() {
        return getStorageType(getExecutionMode());
    }

    public static StorageType getStorageType(Integer execMode) {
        ExecutionMode executionMode = ExecutionMode.of(execMode);
        switch (Objects.requireNonNull(executionMode)) {
            case YARN_APPLICATION:
                return StorageType.HDFS;
            case YARN_PER_JOB:
            case YARN_SESSION:
            case KUBERNETES_NATIVE_SESSION:
            case KUBERNETES_NATIVE_APPLICATION:
            case REMOTE:
                return StorageType.LFS;
            default:
                throw new UnsupportedOperationException("Unsupported ".concat(executionMode.getName()));
        }
    }

    public FsOperator getFsOperator() {
        return FsOperator.of(getStorageType());
    }

    public Workspace getWorkspace() {
        return Workspace.of(getStorageType());
    }

    @JsonIgnore
    @SneakyThrows
    public Map<String, Object> getHotParamsMap() {
        if (this.hotParams != null) {
            Map<String, Object> map = JacksonUtils.read(this.hotParams, Map.class);
            map.entrySet().removeIf(entry -> entry.getValue() == null);
            return map;
        }
        return Collections.EMPTY_MAP;
    }

    @JsonIgnore
    @SneakyThrows
    public void doSetHotParams() {
        Map<String, String> hotParams = new HashMap<>();
        ExecutionMode executionModeEnum = this.getExecutionModeEnum();
        if (ExecutionMode.YARN_APPLICATION.equals(executionModeEnum)) {
            if (StringUtils.isNotEmpty(this.getYarnQueue())) {
                hotParams.put(ConfigConst.KEY_YARN_APP_QUEUE(), this.getYarnQueue());
            }
        }
        if (ExecutionMode.YARN_SESSION.equals(executionModeEnum)) {
            if (StringUtils.isNotEmpty(this.getYarnSessionClusterId())) {
                hotParams.put("yarn.application.id", this.getYarnSessionClusterId());
            }
        }
        if (!hotParams.isEmpty()) {
            this.setHotParams(JacksonUtils.write(hotParams));
        }
    }

    @JsonIgnore
    @SneakyThrows
    public void updateHotParams(Application appParam) {
        ExecutionMode executionModeEnum = appParam.getExecutionModeEnum();
        Map<String, String> hotParams = new HashMap<>(0);
        if (ExecutionMode.YARN_APPLICATION.equals(executionModeEnum)) {
            if (StringUtils.isNotEmpty(appParam.getYarnQueue())) {
                hotParams.put(ConfigConst.KEY_YARN_APP_QUEUE(), appParam.getYarnQueue());
            }
        }
        if (ExecutionMode.YARN_SESSION.equals(executionModeEnum)) {
            if (StringUtils.isNotEmpty(appParam.getYarnSessionClusterId())) {
                hotParams.put(ConfigConst.KEY_YARN_APP_ID(), appParam.getYarnSessionClusterId());
            }
        }
        this.setHotParams(JacksonUtils.write(hotParams));
    }

    @Data
    public static class Dependency {
        private List<Pom> pom = Collections.emptyList();
        private List<String> jar = Collections.emptyList();

        @JsonIgnore
        @SneakyThrows
        public static Dependency jsonToDependency(String dependency) {
            if (Utils.notEmpty(dependency)) {
                return JacksonUtils.read(dependency, new TypeReference<Dependency>() {
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

        @JsonIgnore
        public DependencyInfo toJarPackDeps() {
            List<MavenArtifact> mvnArts = this.pom.stream()
                    .map(pom -> new MavenArtifact(pom.getGroupId(), pom.getArtifactId(), pom.getVersion()))
                    .collect(Collectors.toList());
            List<String> extJars = this.jar.stream()
                    .map(jar -> Workspace.local().APP_UPLOADS() + "/" + jar)
                    .collect(Collectors.toList());
            return new DependencyInfo(mvnArts, extJars);
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
