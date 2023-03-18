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

import org.apache.streampark.common.conf.ConfigConst;
import org.apache.streampark.common.conf.K8sFlinkConfig;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.enums.FlinkK8sRestExposedType;
import org.apache.streampark.common.enums.JobType;
import org.apache.streampark.common.enums.StorageType;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.common.util.FileUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.bean.AppControl;
import org.apache.streampark.console.core.enums.ConfigFileType;
import org.apache.streampark.console.core.enums.FlinkAppState;
import org.apache.streampark.console.core.enums.OptionState;
import org.apache.streampark.console.core.enums.ReleaseState;
import org.apache.streampark.console.core.enums.ResourceFrom;
import org.apache.streampark.console.core.metrics.flink.JobsOverview;
import org.apache.streampark.console.core.utils.YarnQueueLabelExpression;
import org.apache.streampark.flink.kubernetes.model.K8sPodTemplates;
import org.apache.streampark.flink.packer.maven.Artifact;
import org.apache.streampark.flink.packer.maven.DependencyInfo;
import org.apache.streampark.flink.packer.pipeline.PipelineStatus;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@TableName("t_flink_app")
@Slf4j
public class Application implements Serializable {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long teamId;

  /** 1) custom code 2) flink SQL */
  private JobType jobType;

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

  /** flink version */
  private Long versionId;

  /** k8s cluster id */
  private String clusterId;

  /** flink docker base image */
  private String flinkImage;

  /** k8s namespace */
  private String k8sNamespace = K8sFlinkConfig.DEFAULT_KUBERNETES_NAMESPACE();

  private FlinkAppState state;
  /** task release status */
  @TableField("`release`")
  private ReleaseState release;

  /** determine if a task needs to be built */
  private Boolean build;

  /** max restart retries after job failed */
  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private Integer restartSize;

  /** has restart count */
  private Integer restartCount;

  private OptionState optionState;

  /** alert id */
  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private Integer alertId;

  private String args;
  /** application module */
  private String module;

  private String options;

  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private String hotParams;

  private Integer resolveOrder;
  private ExecutionMode executionMode;
  private String dynamicProperties;
  private ApplicationType appType;

  /** determine if tracking status */
  private Integer tracking;

  private String jar;

  /**
   * for upload type tasks, checkSum needs to be recorded whether it needs to be republished after
   * the update and modify.
   */
  private Long jarCheckSum;

  private String mainClass;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
  private Date startTime;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
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

  private Integer totalSlot;
  private Integer availableSlot;
  private Integer jmMemory;
  private Integer tmMemory;
  private Integer totalTask;

  /** the cluster id bound to the task in remote mode */
  @TableField(updateStrategy = FieldStrategy.IGNORED)
  private Long flinkClusterId;

  private String description;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
  private Date createTime;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
  private Date optionTime;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
  private Date modifyTime;

  /** The exposed type of the rest service of K8s(kubernetes.rest-service.exposed.type) */
  private FlinkK8sRestExposedType k8sRestExposedType;
  /** flink kubernetes pod template */
  private String k8sPodTemplate;

  private String k8sJmPodTemplate;
  private String k8sTmPodTemplate;

  private String ingressTemplate;
  private String defaultModeIngress;

  /** 1: cicd (build from csv) 2: upload (upload local jar job) */
  private ResourceFrom resourceFrom;

  /** flink-hadoop integration on flink-k8s mode */
  private Boolean k8sHadoopIntegration;

  private String tags;

  /** running job */
  private transient JobsOverview.Task overview;

  private transient String dependency;
  private transient Long sqlId;
  private transient String flinkSql;

  private transient FlinkAppState[] stateArray;
  private transient Integer[] jobTypeArray;
  private transient Boolean backUp = false;
  private transient Boolean restart = false;
  private transient String userName;
  private transient String nickName;
  private transient String config;
  private transient Long configId;
  private transient String flinkVersion;
  private transient String confPath;
  private transient ConfigFileType format;
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

  /** Flink Web UI Url */
  private transient String flinkRestUrl;

  /** refer to {@link org.apache.streampark.flink.packer.pipeline.BuildPipeline} */
  private transient PipelineStatus buildStatus;

  private transient AppControl appControl;

  public void setK8sNamespace(String k8sNamespace) {
    this.k8sNamespace =
        StringUtils.isBlank(k8sNamespace)
            ? K8sFlinkConfig.DEFAULT_KUBERNETES_NAMESPACE()
            : k8sNamespace;
  }

  public K8sPodTemplates getK8sPodTemplates() {
    return K8sPodTemplates.of(k8sPodTemplate, k8sJmPodTemplate, k8sTmPodTemplate);
  }

  public void setState(FlinkAppState state) {
    this.state = state;
    this.tracking = shouldTracking(state);
  }

  public void setYarnQueueByHotParams() {
    if (!(ExecutionMode.YARN_APPLICATION == this.getExecutionMode()
        || ExecutionMode.YARN_PER_JOB == this.getExecutionMode())) {
      return;
    }

    Map<String, Object> hotParamsMap = this.getHotParamsMap();
    if (!hotParamsMap.isEmpty() && hotParamsMap.containsKey(ConfigConst.KEY_YARN_APP_QUEUE())) {
      String yarnQueueName = hotParamsMap.get(ConfigConst.KEY_YARN_APP_QUEUE()).toString();
      String labelExpr =
          Optional.ofNullable(hotParamsMap.get(ConfigConst.KEY_YARN_APP_NODE_LABEL()))
              .map(Object::toString)
              .orElse(null);
      this.setYarnQueue(YarnQueueLabelExpression.of(yarnQueueName, labelExpr).toString());
    }
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

  /**
   * Determine whether the application can be started to prevent repeated starts.
   *
   * @return true: can start | false: can not start.
   */
  public boolean isCanBeStart() {
    FlinkAppState state = getState();
    switch (state) {
      case ADDED:
      case CREATED:
      case FAILED:
      case CANCELED:
      case FINISHED:
      case LOST:
      case TERMINATED:
      case SUCCEEDED:
      case KILLED:
      case POS_TERMINATED:
        return true;
      default:
        return false;
    }
  }

  public boolean shouldBeTrack() {
    return shouldTracking(getState()) == 1;
  }

  public boolean cpFailedTrigger() {
    return this.cpMaxFailureInterval != null
        && this.cpFailureRateInterval != null
        && this.cpFailureAction != null;
  }

  /** Local compilation and packaging working directory */
  @JsonIgnore
  public String getDistHome() {
    String path =
        String.format("%s/%s/%s", Workspace.APP_LOCAL_DIST(), projectId.toString(), getModule());
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

  /** Automatically identify remoteAppHome or localAppHome based on app ExecutionModeEnum */
  @JsonIgnore
  public String getAppHome() {
    switch (this.getExecutionMode()) {
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
        throw new UnsupportedOperationException(
            "unsupported executionMode ".concat(getExecutionMode().getName()));
    }
  }

  @JsonIgnore
  public String getAppLib() {
    return getAppHome().concat("/lib");
  }

  @JsonIgnore
  @SneakyThrows
  @SuppressWarnings("unchecked")
  public Map<String, Object> getOptionMap() {
    if (StringUtils.isBlank(this.options)) {
      return Collections.emptyMap();
    }
    Map<String, Object> map = JacksonUtils.read(this.options, Map.class);
    map.entrySet().removeIf(entry -> entry.getValue() == null);
    return map;
  }

  @JsonIgnore
  public boolean isFlinkSqlJob() {
    return JobType.FLINK_SQL.equals(this.getJobType());
  }

  @JsonIgnore
  public boolean isCustomCodeJob() {
    return JobType.CUSTOM_CODE.equals(this.getJobType());
  }

  @JsonIgnore
  public boolean isUploadJob() {
    return isCustomCodeJob() && ResourceFrom.UPLOAD.equals(this.getResourceFrom());
  }

  @JsonIgnore
  public boolean isCICDJob() {
    return isCustomCodeJob() && ResourceFrom.CICD.equals(this.getResourceFrom());
  }

  public boolean isStreamParkJob() {
    return this.getAppType() == ApplicationType.STREAMPARK_FLINK;
  }

  @JsonIgnore
  @SneakyThrows
  public Dependency getDependencyObject() {
    return Dependency.toDependency(this.dependency);
  }

  @JsonIgnore
  public DependencyInfo getDependencyInfo() {
    return Application.Dependency.toDependency(getDependency()).toJarPackDeps();
  }

  @JsonIgnore
  public boolean isRunning() {
    return FlinkAppState.RUNNING == this.state;
  }

  @JsonIgnore
  public boolean isNeedRollback() {
    return ReleaseState.NEED_ROLLBACK == this.release;
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

  public static StorageType getStorageType(ExecutionMode executionMode) {
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
    if (this.hotParams != null) {
      Map<String, Object> map = JacksonUtils.read(this.hotParams, Map.class);
      map.entrySet().removeIf(entry -> entry.getValue() == null);
      return map;
    }
    return Collections.EMPTY_MAP;
  }

  @SneakyThrows
  public void doSetHotParams() {
    updateHotParams(this);
  }

  @SneakyThrows
  public void updateHotParams(Application appParam) {
    if (appParam != this) {
      this.hotParams = null;
    }
    ExecutionMode executionModeEnum = appParam.getExecutionMode();
    Map<String, String> hotParams = new HashMap<>(0);
    if (needFillYarnQueueLabel(executionModeEnum)) {
      hotParams.putAll(YarnQueueLabelExpression.getQueueLabelMap(appParam.getYarnQueue()));
    }
    if (!hotParams.isEmpty()) {
      this.setHotParams(JacksonUtils.write(hotParams));
    }
  }

  private boolean needFillYarnQueueLabel(ExecutionMode mode) {
    return ExecutionMode.YARN_PER_JOB.equals(mode) || ExecutionMode.YARN_APPLICATION.equals(mode);
  }

  @Data
  public static class Dependency {
    private List<Pom> pom = Collections.emptyList();
    private List<String> jar = Collections.emptyList();

    @SneakyThrows
    public static Dependency toDependency(String dependency) {
      if (Utils.notEmpty(dependency)) {
        return JacksonUtils.read(dependency, new TypeReference<Dependency>() {});
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
      File localJar = WebUtils.getAppTempDir();
      File localUploads = new File(Workspace.local().APP_UPLOADS());
      HashSet<String> otherJars = new HashSet<>(other.jar);
      for (String jarName : jar) {
        if (!otherJars.contains(jarName)
            || !FileUtils.equals(new File(localJar, jarName), new File(localUploads, jarName))) {
          return false;
        }
      }
      return new HashSet<>(pom).containsAll(other.pom);
    }

    public DependencyInfo toJarPackDeps() {
      List<Artifact> mvnArts =
          this.pom.stream()
              .map(
                  pom ->
                      new Artifact(
                          pom.getGroupId(),
                          pom.getArtifactId(),
                          pom.getVersion(),
                          pom.getClassifier()))
              .collect(Collectors.toList());
      List<String> extJars =
          this.jar.stream()
              .map(jar -> Workspace.local().APP_UPLOADS() + "/" + jar)
              .collect(Collectors.toList());
      return new DependencyInfo(mvnArts, extJars);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return id.equals(((Application) o).id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Data
  public static class Pom {
    private String groupId;
    private String artifactId;
    private String version;
    private String classifier;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      return this.toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
      return Objects.hash(groupId, artifactId, version, classifier);
    }

    @Override
    public String toString() {
      return groupId + ":" + artifactId + ":" + version + getClassifier(":");
    }

    private String getClassifier(String joiner) {
      return StringUtils.isEmpty(classifier) ? "" : joiner + classifier;
    }
  }
}
