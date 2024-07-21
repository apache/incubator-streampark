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

import org.apache.streampark.common.Constant;
import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.FlinkDevelopmentMode;
import org.apache.streampark.common.enums.SparkExecutionMode;
import org.apache.streampark.common.enums.StorageType;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.console.base.mybatis.entity.BaseEntity;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.bean.AppControl;
import org.apache.streampark.console.core.bean.Dependency;
import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.enums.ResourceFromEnum;
import org.apache.streampark.console.core.enums.SparkAppStateEnum;
import org.apache.streampark.console.core.metrics.flink.JobsOverview;
import org.apache.streampark.console.core.utils.YarnQueueLabelExpression;
import org.apache.streampark.flink.kubernetes.model.K8sPodTemplates;
import org.apache.streampark.flink.packer.maven.DependencyInfo;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Data
@TableName("t_spark_app")
@Slf4j
public class SparkApplication extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teamId;

    /** 1) custom code 2) spark SQL */
    private Integer jobType;

    private Long projectId;
    /** creator */
    private Long userId;

    /** The name of the frontend and program displayed in yarn */
    private String jobName;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String appId;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String jobId;

    /** The address of the jobmanager, that is, the direct access address of the Flink web UI */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String jobManagerUrl;

    /** spark version */
    private Long versionId;

    /** k8s cluster id */
    private String clusterId;

    /** spark docker base image */
    private String sparkImage;

    /** The resource name of the spark job on k8s, equivalent to clusterId in application mode. */
    private String k8sName;

    /** k8s namespace */
    private String k8sNamespace = Constant.DEFAULT;

    /** The exposed type of the rest service of K8s(kubernetes.rest-service.exposed.type) */
    private Integer k8sRestExposedType;
    /** spark kubernetes pod template */
    private String k8sPodTemplate;

    private String k8sJmPodTemplate;
    private String k8sTmPodTemplate;

    @Getter
    private String ingressTemplate;
    private String defaultModeIngress;

    /** spark-hadoop integration on spark-k8s mode */
    private Boolean k8sHadoopIntegration;

    private Integer state;
    /** task release status */
    @TableField("`release`")
    private Integer release;

    /** determine if a task needs to be built */
    private Boolean build;

    /** max restart retries after job failed */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Integer restartSize;

    /** has restart count */
    private Integer restartCount;

    private Integer optionState;

    /** alert id */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long alertId;

    private String args;
    /** application module */
    private String module;

    private String options;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String hotParams;

    private Integer resolveOrder;
    private Integer executionMode;
    private String dynamicProperties;
    private Integer appType;

    /** determine if tracking status */
    private Integer tracking;

    private String jar;

    /**
     * for upload type tasks, checkSum needs to be recorded whether it needs to be republished after
     * the update and modify.
     */
    private Long jarCheckSum;

    private String mainClass;

    private Date startTime;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Date endTime;

    private Long duration;

    /** checkpoint max failure interval */
    private Integer cpMaxFailureInterval;

    /** checkpoint failure rate interval */
    private Integer cpFailureRateInterval;

    /** Actions triggered after X minutes failed Y times: 1: send alert 2: restart */
    private Integer cpFailureAction;

    /** overview */
    @TableField("TOTAL_TM")
    private Integer totalTM;

    @TableField("HADOOP_USER")
    private String hadoopUser;

    private Integer totalSlot;
    private Integer availableSlot;
    private Integer jmMemory;
    private Integer tmMemory;
    private Integer totalTask;

    /** the cluster id bound to the task in remote mode */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long sparkClusterId;

    private String description;

    private Date optionTime;

    /** 1: cicd (build from csv) 2: upload (upload local jar job) */
    private Integer resourceFrom;

    private String tags;

    /** running job */
    private transient JobsOverview.Task overview;

    private transient String teamResource;
    private transient String dependency;
    private transient Long sqlId;
    private transient String sparkSql;

    private transient Integer[] stateArray;
    private transient Integer[] jobTypeArray;
    private transient Boolean backUp = false;
    private transient Boolean restart = false;
    private transient String userName;
    private transient String nickName;
    private transient String config;
    private transient Long configId;
    private transient String sparkVersion;
    private transient String confPath;
    private transient Integer format;
    private transient String savePoint;
    private transient Boolean savePointed = false;
    private transient Boolean drain = false;
    private transient Boolean nativeFormat = false;
    private transient Long savePointTimeout = 60L;
    private transient Boolean allowNonRestored = false;
    private transient Integer restoreMode;
    private transient String socketId;
    private transient String projectName;
    private transient String createTimeFrom;
    private transient String createTimeTo;
    private transient String backUpDescription;
    private transient String yarnQueue;

    /** spark Web UI Url */
    private transient String sparkRestUrl;

    /** refer to {@link org.apache.streampark.flink.packer.pipeline.BuildPipeline} */
    private transient Integer buildStatus;

    private transient AppControl appControl;

    public void setDefaultModeIngress(String defaultModeIngress) {
        this.defaultModeIngress = defaultModeIngress;
    }

    public void setK8sNamespace(String k8sNamespace) {
        this.k8sNamespace = StringUtils.isBlank(k8sNamespace) ? Constant.DEFAULT : k8sNamespace;
    }

    public K8sPodTemplates getK8sPodTemplates() {
        return K8sPodTemplates.of(k8sPodTemplate, k8sJmPodTemplate, k8sTmPodTemplate);
    }

    public void setState(Integer state) {
        this.state = state;
        this.tracking = shouldTracking() ? 1 : 0;
    }

    public void setYarnQueueByHotParams() {
        if (!(SparkExecutionMode.YARN_CLIENT == this.getSparkExecutionMode()
            || SparkExecutionMode.YARN_CLUSTER == this.getSparkExecutionMode())) {
            return;
        }

        Map<String, Object> hotParamsMap = this.getHotParamsMap();
        if (MapUtils.isNotEmpty(hotParamsMap)
            && hotParamsMap.containsKey(ConfigKeys.KEY_YARN_APP_QUEUE())) {
            String yarnQueue = hotParamsMap.get(ConfigKeys.KEY_YARN_APP_QUEUE()).toString();
            String labelExpr = Optional.ofNullable(hotParamsMap.get(ConfigKeys.KEY_YARN_APP_NODE_LABEL()))
                .map(Object::toString)
                .orElse(null);
            this.setYarnQueue(YarnQueueLabelExpression.of(yarnQueue, labelExpr).toString());
        }
    }

    /**
     * Determine if a SparkAppState requires tracking.
     *
     * @return 1: need to be tracked | 0: no need to be tracked.
     */
    public Boolean shouldTracking() {
        switch (getStateEnum()) {
            case ADDED:
            case FINISHED:
            case FAILED:
            case KILLED:
                return false;
            default:
                return true;
        }
    }

    /**
     * Determine whether the application can be started to prevent repeated starts.
     *
     * @return true: can start | false: can not start.
     */
    public boolean isCanBeStart() {
        switch (getStateEnum()) {
            case ADDED:
            case FAILED:
            case FINISHED:
            case LOST:
            case SUCCEEDED:
            case KILLED:
                return true;
            default:
                return false;
        }
    }

    @JsonIgnore
    public ReleaseStateEnum getReleaseState() {
        return ReleaseStateEnum.of(release);
    }

    @JsonIgnore
    public FlinkDevelopmentMode getDevelopmentMode() {
        return FlinkDevelopmentMode.of(jobType);
    }

    @JsonIgnore
    public SparkAppStateEnum getStateEnum() {
        return SparkAppStateEnum.of(state);
    }

    @JsonIgnore
    public SparkExecutionMode getSparkExecutionMode() {
        return SparkExecutionMode.of(executionMode);
    }

    public boolean cpFailedTrigger() {
        return this.cpMaxFailureInterval != null
            && this.cpFailureRateInterval != null
            && this.cpFailureAction != null;
    }

    public boolean eqFlinkJob(SparkApplication other) {
        if (this.isSparkSqlJob()
            && other.isSparkSqlJob()
            && this.getSparkSql().trim().equals(other.getSparkSql().trim())) {
            return this.getDependencyObject().equals(other.getDependencyObject());
        }
        return false;
    }

    /** Local compilation and packaging working directory */
    @JsonIgnore
    public String getDistHome() {
        String path = String.format("%s/%s/%s", Workspace.APP_LOCAL_DIST(), projectId.toString(), getModule());
        log.info("local distHome:{}", path);
        return path;
    }

    @JsonIgnore
    public String getLocalAppHome() {
        String path = String.format("%s/%s", Workspace.local().APP_WORKSPACE(), id.toString());
        log.info("local appHome:{}", path);
        return path;
    }

    @JsonIgnore
    public String getRemoteAppHome() {
        String path = String.format("%s/%s", Workspace.remote().APP_WORKSPACE(), id.toString());
        log.info("remote appHome:{}", path);
        return path;
    }

    /** Automatically identify remoteAppHome or localAppHome based on app SparkExecutionMode */
    @JsonIgnore
    public String getAppHome() {
        switch (this.getSparkExecutionMode()) {
            case REMOTE:
            case LOCAL:
                return getLocalAppHome();
            case YARN_CLIENT:
            case YARN_CLUSTER:
                return getRemoteAppHome();
            default:
                throw new UnsupportedOperationException(
                    "unsupported executionMode ".concat(getSparkExecutionMode().getName()));
        }
    }

    @JsonIgnore
    public String getAppLib() {
        return getAppHome().concat("/lib");
    }

    @JsonIgnore
    public ApplicationType getApplicationType() {
        return ApplicationType.of(appType);
    }

    @JsonIgnore
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public Map<String, String> getOptionMap() {
        if (StringUtils.isBlank(this.options)) {
            return new HashMap<>();
        }
        Map<String, String> optionMap = JacksonUtils.read(this.options, Map.class);
        optionMap.entrySet().removeIf(entry -> entry.getValue() == null);
        return optionMap;
    }

    @JsonIgnore
    public boolean isSparkSqlJob() {
        return FlinkDevelopmentMode.FLINK_SQL.getMode().equals(this.getJobType());
    }

    @JsonIgnore
    public boolean isCustomCodeJob() {
        return FlinkDevelopmentMode.CUSTOM_CODE.getMode().equals(this.getJobType());
    }

    @JsonIgnore
    public boolean isCustomCodeOrPySparkJob() {
        return FlinkDevelopmentMode.CUSTOM_CODE.getMode().equals(this.getJobType())
            || FlinkDevelopmentMode.PYFLINK.getMode().equals(this.getJobType());
    }

    @JsonIgnore
    public boolean isUploadJob() {
        return isCustomCodeOrPySparkJob()
            && ResourceFromEnum.UPLOAD.getValue().equals(this.getResourceFrom());
    }

    @JsonIgnore
    public boolean isCICDJob() {
        return isCustomCodeOrPySparkJob()
            && ResourceFromEnum.CICD.getValue().equals(this.getResourceFrom());
    }

    public boolean isStreamParkJob() {
        return this.getAppType() == ApplicationType.STREAMPARK_FLINK.getType();
    }

    @JsonIgnore
    @SneakyThrows
    public Dependency getDependencyObject() {
        return Dependency.toDependency(this.dependency);
    }

    @JsonIgnore
    public DependencyInfo getDependencyInfo() {
        return Dependency.toDependency(getDependency()).toJarPackDeps();
    }

    @JsonIgnore
    public boolean isRunning() {
        return FlinkAppStateEnum.RUNNING.getValue() == this.getState();
    }

    @JsonIgnore
    public boolean isNeedRollback() {
        return ReleaseStateEnum.NEED_ROLLBACK.get() == this.getRelease();
    }

    @JsonIgnore
    public boolean isNeedRestartOnFailed() {
        if (this.restartSize != null && this.restartCount != null) {
            return this.restartSize > 0 && this.restartCount <= this.restartSize;
        }
        return false;
    }

    @JsonIgnore
    public StorageType getStorageType() {
        return getStorageType(getExecutionMode());
    }

    public static StorageType getStorageType(Integer execMode) {
        SparkExecutionMode executionModeEnum = SparkExecutionMode.of(execMode);
        switch (Objects.requireNonNull(executionModeEnum)) {
            case YARN_CLUSTER:
            case YARN_CLIENT:
                return StorageType.HDFS;
            case REMOTE:
                return StorageType.LFS;
            default:
                throw new UnsupportedOperationException("Unsupported ".concat(executionModeEnum.getName()));
        }
    }

    @JsonIgnore
    public FsOperator getFsOperator() {
        return FsOperator.of(getStorageType());
    }

    @JsonIgnore
    public Workspace getWorkspace() {
        return Workspace.of(getStorageType());
    }

    @JsonIgnore
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public Map<String, Object> getHotParamsMap() {
        if (StringUtils.isNotBlank(this.hotParams)) {
            Map<String, Object> hotParamsMap = JacksonUtils.read(this.hotParams, Map.class);
            hotParamsMap.entrySet().removeIf(entry -> entry.getValue() == null);
            return hotParamsMap;
        }
        return Collections.EMPTY_MAP;
    }

    @SneakyThrows
    public void doSetHotParams() {
        updateHotParams(this);
    }

    @SneakyThrows
    public void updateHotParams(SparkApplication appParam) {
        if (appParam != this) {
            this.hotParams = null;
        }
        SparkExecutionMode executionModeEnum = appParam.getSparkExecutionMode();
        Map<String, String> hotParams = new HashMap<>(0);
        if (needFillYarnQueueLabel(executionModeEnum)) {
            hotParams.putAll(YarnQueueLabelExpression.getQueueLabelMap(appParam.getYarnQueue()));
        }
        if (MapUtils.isNotEmpty(hotParams)) {
            this.setHotParams(JacksonUtils.write(hotParams));
        }
    }

    private boolean needFillYarnQueueLabel(SparkExecutionMode mode) {
        return SparkExecutionMode.YARN_CLUSTER == mode || SparkExecutionMode.YARN_CLIENT == mode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return id.equals(((SparkApplication) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class SFunc {

        public static final SFunction<SparkApplication, Long> ID = SparkApplication::getId;
        public static final SFunction<SparkApplication, String> JOB_ID = SparkApplication::getJobId;
        public static final SFunction<SparkApplication, Date> START_TIME = SparkApplication::getStartTime;
        public static final SFunction<SparkApplication, Date> END_TIME = SparkApplication::getEndTime;
        public static final SFunction<SparkApplication, Long> DURATION = SparkApplication::getDuration;
        public static final SFunction<SparkApplication, Integer> TOTAL_TASK = SparkApplication::getTotalTask;
        public static final SFunction<SparkApplication, Integer> TOTAL_TM = SparkApplication::getTotalTM;
        public static final SFunction<SparkApplication, Integer> TOTAL_SLOT = SparkApplication::getTotalSlot;
        public static final SFunction<SparkApplication, Integer> JM_MEMORY = SparkApplication::getJmMemory;
        public static final SFunction<SparkApplication, Integer> TM_MEMORY = SparkApplication::getTmMemory;
        public static final SFunction<SparkApplication, Integer> STATE = SparkApplication::getState;
        public static final SFunction<SparkApplication, String> OPTIONS = SparkApplication::getOptions;
        public static final SFunction<SparkApplication, Integer> AVAILABLE_SLOT = SparkApplication::getAvailableSlot;
        public static final SFunction<SparkApplication, Integer> EXECUTION_MODE = SparkApplication::getExecutionMode;
        public static final SFunction<SparkApplication, String> JOB_MANAGER_URL = SparkApplication::getJobManagerUrl;
    }
}
