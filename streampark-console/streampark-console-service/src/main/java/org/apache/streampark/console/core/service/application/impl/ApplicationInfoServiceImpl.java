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

package org.apache.streampark.console.core.service.application.impl;

import org.apache.streampark.common.Constant;
import org.apache.streampark.common.conf.K8sFlinkConfig;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.FlinkExecutionMode;
import org.apache.streampark.common.fs.LfsOperator;
import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.common.util.HadoopUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.ApiDetailException;
import org.apache.streampark.console.base.exception.ApplicationException;
import org.apache.streampark.console.base.util.PremisesUtils;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.enums.AppExistsStateEnum;
import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.mapper.ApplicationMapper;
import org.apache.streampark.console.core.metrics.flink.JobsOverview;
import org.apache.streampark.console.core.runner.EnvInitializer;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.SavePointService;
import org.apache.streampark.console.core.service.application.ApplicationInfoService;
import org.apache.streampark.console.core.watcher.FlinkAppHttpWatcher;
import org.apache.streampark.console.core.watcher.FlinkClusterWatcher;
import org.apache.streampark.console.core.watcher.FlinkK8sObserverStub;
import org.apache.streampark.flink.core.conf.ParameterCli;
import org.apache.streampark.flink.kubernetes.FlinkK8sWatcher;
import org.apache.streampark.flink.kubernetes.helper.KubernetesDeploymentHelper;
import org.apache.streampark.flink.kubernetes.model.FlinkMetricCV;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.streampark.common.enums.StorageType.LFS;
import static org.apache.streampark.console.core.watcher.FlinkK8sWatcherWrapper.Bridge.toTrackId;

@Slf4j
@Service
public class ApplicationInfoServiceImpl extends ServiceImpl<ApplicationMapper, Application>
    implements ApplicationInfoService {

  private static final int DEFAULT_HISTORY_RECORD_LIMIT = 25;

  private static final int DEFAULT_HISTORY_POD_TMPL_RECORD_LIMIT = 5;

  private static final Pattern JOB_NAME_PATTERN =
      Pattern.compile("^[.\\x{4e00}-\\x{9fa5}A-Za-z\\d_\\-\\s]+$");

  private static final Pattern SINGLE_SPACE_PATTERN = Pattern.compile("^\\S+(\\s\\S+)*$");

  @Autowired private FlinkEnvService flinkEnvService;

  @Autowired private SavePointService savePointService;

  @Autowired private EnvInitializer envInitializer;

  @Autowired private FlinkK8sWatcher k8SFlinkTrackMonitor;

  @Autowired private FlinkK8sObserverStub flinkK8sObserver;

  @Autowired private FlinkClusterService flinkClusterService;

  @Autowired private FlinkClusterWatcher flinkClusterWatcher;

  @Override
  public Map<String, Serializable> getDashboardDataMap(Long teamId) {
    JobsOverview.Task overview = new JobsOverview.Task();
    Integer totalJmMemory = 0;
    Integer totalTmMemory = 0;
    Integer totalTm = 0;
    Integer totalSlot = 0;
    Integer availableSlot = 0;
    Integer runningJob = 0;

    // stat metrics from other than kubernetes mode
    for (Application app : FlinkAppHttpWatcher.getWatchingApps()) {
      if (!teamId.equals(app.getTeamId())) {
        continue;
      }
      if (app.getJmMemory() != null) {
        totalJmMemory += app.getJmMemory();
      }
      if (app.getTmMemory() != null) {
        totalTmMemory += app.getTmMemory() * (app.getTotalTM() == null ? 1 : app.getTotalTM());
      }
      if (app.getTotalTM() != null) {
        totalTm += app.getTotalTM();
      }
      if (app.getTotalSlot() != null) {
        totalSlot += app.getTotalSlot();
      }
      if (app.getAvailableSlot() != null) {
        availableSlot += app.getAvailableSlot();
      }
      if (app.getState() == FlinkAppStateEnum.RUNNING.getValue()) {
        runningJob++;
      }
      JobsOverview.Task task = app.getOverview();
      if (task != null) {
        renderJobsOverviewTaskByTask(overview, task);
      }
    }

    // merge metrics from flink kubernetes cluster
    FlinkMetricCV k8sMetric =
        K8sFlinkConfig.isV2Enabled()
            ? flinkK8sObserver.getAggClusterMetricCV(teamId)
            : k8SFlinkTrackMonitor.getAccGroupMetrics(teamId.toString());
    if (k8sMetric != null) {
      totalJmMemory += k8sMetric.totalJmMemory();
      totalTmMemory += k8sMetric.totalTmMemory();
      totalTm += k8sMetric.totalTm();
      totalSlot += k8sMetric.totalSlot();
      availableSlot += k8sMetric.availableSlot();
      runningJob += k8sMetric.runningJob();
      renderJobsOverviewTaskByK8sMetric(overview, k8sMetric);
    }

    // result json
    return constructDashboardMap(
        overview, totalJmMemory, totalTmMemory, totalTm, availableSlot, totalSlot, runningJob);
  }

  private void renderJobsOverviewTaskByTask(JobsOverview.Task overview, JobsOverview.Task task) {
    overview.setTotal(overview.getTotal() + task.getTotal());
    overview.setCreated(overview.getCreated() + task.getCreated());
    overview.setScheduled(overview.getScheduled() + task.getScheduled());
    overview.setDeploying(overview.getDeploying() + task.getDeploying());
    overview.setRunning(overview.getRunning() + task.getRunning());
    overview.setFinished(overview.getFinished() + task.getFinished());
    overview.setCanceling(overview.getCanceling() + task.getCanceling());
    overview.setCanceled(overview.getCanceled() + task.getCanceled());
    overview.setFailed(overview.getFailed() + task.getFailed());
    overview.setReconciling(overview.getReconciling() + task.getReconciling());
  }

  private void renderJobsOverviewTaskByK8sMetric(
      JobsOverview.Task overview, FlinkMetricCV k8sMetric) {
    overview.setTotal(overview.getTotal() + k8sMetric.totalJob());
    overview.setRunning(overview.getRunning() + k8sMetric.runningJob());
    overview.setFinished(overview.getFinished() + k8sMetric.finishedJob());
    overview.setCanceled(overview.getCanceled() + k8sMetric.cancelledJob());
    overview.setFailed(overview.getFailed() + k8sMetric.failedJob());
  }

  @Nonnull
  private Map<String, Serializable> constructDashboardMap(
      JobsOverview.Task overview,
      Integer totalJmMemory,
      Integer totalTmMemory,
      Integer totalTm,
      Integer availableSlot,
      Integer totalSlot,
      Integer runningJob) {
    Map<String, Serializable> dashboardDataMap = new HashMap<>(8);
    dashboardDataMap.put("task", overview);
    dashboardDataMap.put("jmMemory", totalJmMemory);
    dashboardDataMap.put("tmMemory", totalTmMemory);
    dashboardDataMap.put("totalTM", totalTm);
    dashboardDataMap.put("availableSlot", availableSlot);
    dashboardDataMap.put("totalSlot", totalSlot);
    dashboardDataMap.put("runningJob", runningJob);

    return dashboardDataMap;
  }

  @Override
  public boolean checkEnv(Application appParam) throws ApplicationException {
    Application application = getById(appParam.getId());
    try {
      FlinkEnv flinkEnv;
      if (application.getVersionId() != null) {
        flinkEnv = flinkEnvService.getByIdOrDefault(application.getVersionId());
      } else {
        flinkEnv = flinkEnvService.getDefault();
      }
      if (flinkEnv == null) {
        return false;
      }
      envInitializer.checkFlinkEnv(application.getStorageType(), flinkEnv);
      envInitializer.storageInitialize(application.getStorageType());

      if (FlinkExecutionMode.YARN_SESSION == application.getFlinkExecutionMode()
          || FlinkExecutionMode.REMOTE == application.getFlinkExecutionMode()) {
        FlinkCluster flinkCluster = flinkClusterService.getById(application.getFlinkClusterId());
        boolean conned = flinkClusterWatcher.verifyClusterConnection(flinkCluster);
        PremisesUtils.throwIfFalse(
            conned, "the target cluster is unavailable, please check!", ApiAlertException.class);
      }
      return true;
    } catch (Exception e) {
      log.error(ExceptionUtils.stringifyException(e));
      throw new ApiDetailException(e);
    }
  }

  @Override
  public boolean checkAlter(Application appParam) {
    Long appId = appParam.getId();
    if (FlinkAppStateEnum.CANCELED != appParam.getStateEnum()) {
      return false;
    }
    long cancelUserId = FlinkAppHttpWatcher.getCanceledJobUserId(appId);
    long appUserId = appParam.getUserId();
    return cancelUserId != -1 && cancelUserId != appUserId;
  }

  @Override
  public boolean existsByTeamId(Long teamId) {
    return baseMapper.exists(
        new LambdaQueryWrapper<Application>().eq(Application::getTeamId, teamId));
  }

  @Override
  public boolean existsByUserId(Long userId) {
    return baseMapper.exists(
        new LambdaQueryWrapper<Application>().eq(Application::getUserId, userId));
  }

  @Override
  public boolean existsRunningByClusterId(Long clusterId) {
    return baseMapper.existsRunningJobByClusterId(clusterId)
        || FlinkAppHttpWatcher.getWatchingApps().stream()
            .anyMatch(
                application ->
                    clusterId.equals(application.getFlinkClusterId())
                        && FlinkAppStateEnum.RUNNING == application.getStateEnum());
  }

  @Override
  public boolean existsByClusterId(Long clusterId) {
    return baseMapper.exists(
        new LambdaQueryWrapper<Application>().eq(Application::getFlinkClusterId, clusterId));
  }

  @Override
  public Integer countByClusterId(Long clusterId) {
    return baseMapper
        .selectCount(
            new LambdaQueryWrapper<Application>().eq(Application::getFlinkClusterId, clusterId))
        .intValue();
  }

  @Override
  public Integer countAffectedByClusterId(Long clusterId, String dbType) {
    return baseMapper.countAffectedByClusterId(clusterId, dbType);
  }

  @Override
  public boolean existsByFlinkEnvId(Long flinkEnvId) {
    return baseMapper.exists(
        new LambdaQueryWrapper<Application>().eq(Application::getVersionId, flinkEnvId));
  }

  @Override
  public List<String> listRecentK8sNamespace() {
    return baseMapper.selectRecentK8sNamespaces(DEFAULT_HISTORY_RECORD_LIMIT);
  }

  @Override
  public List<String> listRecentK8sClusterId(Integer executionMode) {
    return baseMapper.selectRecentK8sClusterIds(executionMode, DEFAULT_HISTORY_RECORD_LIMIT);
  }

  @Override
  public List<String> listRecentFlinkBaseImage() {
    return baseMapper.selectRecentFlinkBaseImages(DEFAULT_HISTORY_RECORD_LIMIT);
  }

  @Override
  public List<String> listRecentK8sPodTemplate() {
    return baseMapper.selectRecentK8sPodTemplates(DEFAULT_HISTORY_POD_TMPL_RECORD_LIMIT);
  }

  @Override
  public List<String> listRecentK8sJmPodTemplate() {
    return baseMapper.selectRecentK8sJmPodTemplates(DEFAULT_HISTORY_POD_TMPL_RECORD_LIMIT);
  }

  @Override
  public List<String> listRecentK8sTmPodTemplate() {
    return baseMapper.selectRecentK8sTmPodTemplates(DEFAULT_HISTORY_POD_TMPL_RECORD_LIMIT);
  }

  @Override
  public List<String> listHistoryUploadJars() {
    return Arrays.stream(LfsOperator.listDir(Workspace.of(LFS).APP_UPLOADS()))
        .filter(File::isFile)
        .sorted(Comparator.comparingLong(File::lastModified).reversed())
        .map(File::getName)
        .filter(fn -> fn.endsWith(Constant.JAR_SUFFIX))
        .limit(DEFAULT_HISTORY_RECORD_LIMIT)
        .collect(Collectors.toList());
  }

  @Override
  public AppExistsStateEnum checkStart(Long id) {
    Application application = getById(id);
    if (application == null) {
      return AppExistsStateEnum.INVALID;
    }
    if (FlinkExecutionMode.isYarnMode(application.getExecutionMode())) {
      boolean exists = !getYarnAppReport(application.getJobName()).isEmpty();
      return exists ? AppExistsStateEnum.IN_YARN : AppExistsStateEnum.NO;
    }
    // todo on k8s check...
    return AppExistsStateEnum.NO;
  }

  @Override
  public List<ApplicationReport> getYarnAppReport(String appName) {
    try {
      YarnClient yarnClient = HadoopUtils.yarnClient();
      Set<String> types =
          Sets.newHashSet(
              ApplicationType.STREAMPARK_FLINK.getName(), ApplicationType.APACHE_FLINK.getName());
      EnumSet<YarnApplicationState> states =
          EnumSet.of(
              YarnApplicationState.NEW,
              YarnApplicationState.NEW_SAVING,
              YarnApplicationState.SUBMITTED,
              YarnApplicationState.ACCEPTED,
              YarnApplicationState.RUNNING);
      Set<String> yarnTag = Sets.newHashSet("streampark");
      List<ApplicationReport> applications = yarnClient.getApplications(types, states, yarnTag);
      return applications.stream()
          .filter(report -> report.getName().equals(appName))
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException(
          "getYarnAppReport failed. Ensure that yarn is running properly. ", e);
    }
  }

  @Override
  public String k8sStartLog(Long id, Integer offset, Integer limit) throws Exception {
    Application application = getById(id);
    PremisesUtils.throwIfNull(
        application,
        String.format("The application id=%s can't be found.", id),
        ApiAlertException.class);
    PremisesUtils.throwIfFalse(
        FlinkExecutionMode.isKubernetesMode(application.getFlinkExecutionMode()),
        "Job executionMode must be kubernetes-session|kubernetes-application.",
        ApiAlertException.class);

    CompletableFuture<String> future =
        CompletableFuture.supplyAsync(
            () ->
                KubernetesDeploymentHelper.watchDeploymentLog(
                    application.getK8sNamespace(),
                    application.getJobName(),
                    application.getJobId()));

    return future
        .exceptionally(
            e -> {
              String errorLog =
                  String.format(
                      "%s/%s_err.log",
                      WebUtils.getAppTempDir().getAbsolutePath(), application.getJobId());
              File file = new File(errorLog);
              if (file.exists() && file.isFile()) {
                return file.getAbsolutePath();
              }
              return null;
            })
        .thenApply(
            path -> {
              if (!future.isDone()) {
                future.cancel(true);
              }
              if (org.apache.streampark.common.util.FileUtils.exists(path)) {
                return org.apache.streampark.common.util.FileUtils.tailOf(path, offset, limit);
              }
              return null;
            })
        .toCompletableFuture()
        .get(5, TimeUnit.SECONDS);
  }

  @Override
  public String getYarnName(String appConfig) {
    String[] args = new String[2];
    args[0] = "--name";
    args[1] = appConfig;
    return ParameterCli.read(args);
  }

  /**
   * Check if the current jobName and other key identifiers already exist in the database and
   * yarn/k8s.
   *
   * @param appParam The application to check for existence.
   * @return The state of the application's existence.
   */
  @Override
  public AppExistsStateEnum checkExists(Application appParam) {

    if (!checkJobName(appParam.getJobName())) {
      return AppExistsStateEnum.INVALID;
    }

    boolean existsByJobName = this.existsByJobName(appParam.getJobName());

    if (appParam.getId() != null) {
      Application app = getById(appParam.getId());
      if (app.getJobName().equals(appParam.getJobName())) {
        return AppExistsStateEnum.NO;
      }

      if (existsByJobName) {
        return AppExistsStateEnum.IN_DB;
      }

      // has stopped status
      if (FlinkAppStateEnum.isEndState(app.getState())) {
        // check whether jobName exists on yarn
        if (FlinkExecutionMode.isYarnMode(appParam.getExecutionMode())
            && YarnUtils.isContains(appParam.getJobName())) {
          return AppExistsStateEnum.IN_YARN;
        }
        // check whether clusterId, namespace, jobId on kubernetes
        if (FlinkExecutionMode.isKubernetesMode(appParam.getExecutionMode())
            && k8SFlinkTrackMonitor.checkIsInRemoteCluster(toTrackId(appParam))) {
          return AppExistsStateEnum.IN_KUBERNETES;
        }
      }
    } else {
      if (existsByJobName) {
        return AppExistsStateEnum.IN_DB;
      }

      // check whether jobName exists on yarn
      if (FlinkExecutionMode.isYarnMode(appParam.getExecutionMode())
          && YarnUtils.isContains(appParam.getJobName())) {
        return AppExistsStateEnum.IN_YARN;
      }
      // check whether clusterId, namespace, jobId on kubernetes
      if (FlinkExecutionMode.isKubernetesMode(appParam.getExecutionMode())
          && k8SFlinkTrackMonitor.checkIsInRemoteCluster(toTrackId(appParam))) {
        return AppExistsStateEnum.IN_KUBERNETES;
      }
    }
    return AppExistsStateEnum.NO;
  }

  private boolean existsByJobName(String jobName) {
    return baseMapper.exists(
        new LambdaQueryWrapper<Application>().eq(Application::getJobName, jobName));
  }

  @Override
  public String readConf(String appConfig) throws IOException {
    File file = new File(appConfig);
    String conf = org.apache.streampark.common.util.FileUtils.readFile(file);
    return Base64.getEncoder().encodeToString(conf.getBytes());
  }

  @Override
  public String getMain(Application appParam) {
    File jarFile;
    if (appParam.getProjectId() == null) {
      jarFile = new File(appParam.getJar());
    } else {
      Project project = new Project();
      project.setId(appParam.getProjectId());
      String modulePath =
          project.getDistHome().getAbsolutePath().concat("/").concat(appParam.getModule());
      jarFile = new File(modulePath, appParam.getJar());
    }
    return Utils.getJarManClass(jarFile);
  }

  @Override
  public String checkSavepointPath(Application appParam) throws Exception {
    String savepointPath = appParam.getSavePoint();
    if (StringUtils.isBlank(savepointPath)) {
      savepointPath = savePointService.getSavePointPath(appParam);
    }

    if (StringUtils.isNotBlank(savepointPath)) {
      final URI uri = URI.create(savepointPath);
      final String scheme = uri.getScheme();
      final String pathPart = uri.getPath();
      String error = null;
      if (scheme == null) {
        error =
            "This state.savepoints.dir value "
                + savepointPath
                + " scheme (hdfs://, file://, etc) of  is null. Please specify the file system scheme explicitly in the URI.";
      } else if (pathPart == null) {
        error =
            "This state.savepoints.dir value "
                + savepointPath
                + " path part to store the checkpoint data in is null. Please specify a directory path for the checkpoint data.";
      } else if (pathPart.isEmpty() || "/".equals(pathPart)) {
        error =
            "This state.savepoints.dir value "
                + savepointPath
                + " Cannot use the root directory for checkpoints.";
      }
      return error;
    } else {
      return "When custom savepoint is not set, state.savepoints.dir needs to be set in properties or flink-conf.yaml of application";
    }
  }

  private Boolean checkJobName(String jobName) {
    if (!StringUtils.isBlank(jobName.trim())) {
      return JOB_NAME_PATTERN.matcher(jobName).matches()
          && SINGLE_SPACE_PATTERN.matcher(jobName).matches();
    }
    return false;
  }
}
