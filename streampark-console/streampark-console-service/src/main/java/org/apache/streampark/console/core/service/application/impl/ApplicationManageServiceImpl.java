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

import org.apache.streampark.common.conf.K8sFlinkConfig;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.enums.StorageType;
import org.apache.streampark.common.fs.HdfsOperator;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.base.util.CommonUtils;
import org.apache.streampark.console.base.util.ObjectUtils;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.bean.AppControl;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationConfig;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.enums.CandidateType;
import org.apache.streampark.console.core.enums.ChangeTypeEnum;
import org.apache.streampark.console.core.enums.FlinkAppState;
import org.apache.streampark.console.core.enums.OptionState;
import org.apache.streampark.console.core.enums.ReleaseState;
import org.apache.streampark.console.core.mapper.ApplicationMapper;
import org.apache.streampark.console.core.service.AppBuildPipeService;
import org.apache.streampark.console.core.service.ApplicationBackUpService;
import org.apache.streampark.console.core.service.ApplicationConfigService;
import org.apache.streampark.console.core.service.ApplicationLogService;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.core.service.EffectiveService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.ProjectService;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.SavePointService;
import org.apache.streampark.console.core.service.SettingService;
import org.apache.streampark.console.core.service.YarnQueueService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;
import org.apache.streampark.console.core.task.FlinkAppHttpWatcher;
import org.apache.streampark.console.core.task.FlinkK8sObserverStub;
import org.apache.streampark.console.core.utils.FlinkK8sDataTypeConverterStub;
import org.apache.streampark.flink.kubernetes.FlinkK8sWatcher;
import org.apache.streampark.flink.packer.pipeline.PipelineStatus;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.annotations.VisibleForTesting;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.streampark.console.core.task.FlinkK8sWatcherWrapper.Bridge.toTrackId;
import static org.apache.streampark.console.core.task.FlinkK8sWatcherWrapper.isKubernetesApp;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationManageServiceImpl extends ServiceImpl<ApplicationMapper, Application>
    implements ApplicationManageService {

  private static final String ERROR_APP_QUEUE_HINT =
      "Queue label '%s' isn't available for teamId '%d', please add it into the team first.";

  @Autowired private ProjectService projectService;

  @Autowired private ApplicationBackUpService backUpService;

  @Autowired private ApplicationConfigService configService;

  @Autowired private ApplicationLogService applicationLogService;

  @Autowired private FlinkSqlService flinkSqlService;

  @Autowired private SavePointService savePointService;

  @Autowired private EffectiveService effectiveService;

  @Autowired private SettingService settingService;

  @Autowired private CommonService commonService;

  @Autowired private FlinkK8sWatcher k8SFlinkTrackMonitor;

  @Autowired private AppBuildPipeService appBuildPipeService;

  @Autowired private YarnQueueService yarnQueueService;

  @Autowired private ResourceService resourceService;

  @Autowired private FlinkK8sObserverStub flinkK8sObserver;

  @Autowired private FlinkK8sDataTypeConverterStub flinkK8sDataTypeConverter;

  @PostConstruct
  public void resetOptionState() {
    this.baseMapper.resetOptionState();
  }

  @Override
  public void toEffective(Application appParam) {
    // set latest to Effective
    ApplicationConfig config = configService.getLatest(appParam.getId());
    if (config != null) {
      this.configService.toEffective(appParam.getId(), config.getId());
    }
    if (appParam.isFlinkSqlJob()) {
      FlinkSql flinkSql = flinkSqlService.getCandidate(appParam.getId(), null);
      if (flinkSql != null) {
        flinkSqlService.toEffective(appParam.getId(), flinkSql.getId());
        // clean candidate
        flinkSqlService.cleanCandidate(flinkSql.getId());
      }
    }
  }

  @Override
  @Transactional(rollbackFor = {Exception.class})
  public Boolean delete(Application appParam) {

    Application application = getById(appParam.getId());

    // 1) remove flink sql
    flinkSqlService.removeApp(application.getId());

    // 2) remove log
    applicationLogService.removeApp(application.getId());

    // 3) remove config
    configService.removeApp(application.getId());

    // 4) remove effective
    effectiveService.removeApp(application.getId());

    // remove related hdfs
    // 5) remove backup
    backUpService.removeApp(application);

    // 6) remove savepoint
    savePointService.removeApp(application);

    // 7) remove BuildPipeline
    appBuildPipeService.removeApp(application.getId());

    // 8) remove app
    removeApp(application);
    if (isKubernetesApp(application)) {
      k8SFlinkTrackMonitor.unWatching(toTrackId(application));
      if (K8sFlinkConfig.isV2Enabled()) {
        flinkK8sObserver.unWatchById(application.getId());
      }
    } else {
      FlinkAppHttpWatcher.unWatching(appParam.getId());
    }
    return true;
  }

  private void removeApp(Application application) {
    Long appId = application.getId();
    removeById(appId);
    try {
      application
          .getFsOperator()
          .delete(application.getWorkspace().APP_WORKSPACE().concat("/").concat(appId.toString()));
      // try to delete yarn-application, and leave no trouble.
      String path =
          Workspace.of(StorageType.HDFS).APP_WORKSPACE().concat("/").concat(appId.toString());
      if (HdfsOperator.exists(path)) {
        HdfsOperator.delete(path);
      }
    } catch (Exception e) {
      // skip
    }
  }

  @Override
  public IPage<Application> page(Application appParam, RestRequest request) {
    if (appParam.getTeamId() == null) {
      return null;
    }
    Page<Application> page = new MybatisPager<Application>().getDefaultPage(request);
    if (CommonUtils.notEmpty(appParam.getStateArray())) {
      if (Arrays.stream(appParam.getStateArray())
          .anyMatch(x -> x == FlinkAppState.FINISHED.getValue())) {
        Integer[] newArray =
            CommonUtils.arrayInsertIndex(
                appParam.getStateArray(),
                appParam.getStateArray().length,
                FlinkAppState.POS_TERMINATED.getValue());
        appParam.setStateArray(newArray);
      }
    }
    this.baseMapper.page(page, appParam);
    List<Application> records = page.getRecords();
    long now = System.currentTimeMillis();

    List<Long> appIds = records.stream().map(Application::getId).collect(Collectors.toList());
    Map<Long, PipelineStatus> pipeStates = appBuildPipeService.listPipelineStatus(appIds);

    List<Application> newRecords =
        records.stream()
            .peek(
                record -> {
                  // status of flink job on kubernetes mode had been automatically persisted to db
                  // in time.
                  if (isKubernetesApp(record)) {
                    // set duration
                    String restUrl = k8SFlinkTrackMonitor.getRemoteRestUrl(toTrackId(record));
                    record.setFlinkRestUrl(restUrl);
                    if (record.getTracking() == 1
                        && record.getStartTime() != null
                        && record.getStartTime().getTime() > 0) {
                      record.setDuration(now - record.getStartTime().getTime());
                    }
                  }
                  if (pipeStates.containsKey(record.getId())) {
                    record.setBuildStatus(pipeStates.get(record.getId()).getCode());
                  }

                  AppControl appControl =
                      new AppControl()
                          .setAllowBuild(
                              record.getBuildStatus() == null
                                  || !PipelineStatus.running
                                      .getCode()
                                      .equals(record.getBuildStatus()))
                          .setAllowStart(
                              !record.shouldTracking()
                                  && PipelineStatus.success
                                      .getCode()
                                      .equals(record.getBuildStatus()))
                          .setAllowStop(record.isRunning());
                  record.setAppControl(appControl);
                })
            .collect(Collectors.toList());
    page.setRecords(newRecords);
    return page;
  }

  @Override
  public void changeOwnership(Long userId, Long targetUserId) {
    LambdaUpdateWrapper<Application> updateWrapper =
        new LambdaUpdateWrapper<Application>()
            .eq(Application::getUserId, userId)
            .set(Application::getUserId, targetUserId);
    this.baseMapper.update(null, updateWrapper);
  }

  @SneakyThrows
  @Override
  @Transactional(rollbackFor = {Exception.class})
  public boolean create(Application appParam) {
    ApiAlertException.throwIfNull(
        appParam.getTeamId(), "The teamId can't be null. Create application failed.");
    appParam.setUserId(commonService.getUserId());
    appParam.setState(FlinkAppState.ADDED.getValue());
    appParam.setRelease(ReleaseState.NEED_RELEASE.get());
    appParam.setOptionState(OptionState.NONE.getValue());
    appParam.setCreateTime(new Date());
    appParam.setDefaultModeIngress(settingService.getIngressModeDefault());

    boolean success = validateQueueIfNeeded(appParam);
    ApiAlertException.throwIfFalse(
        success,
        String.format(ERROR_APP_QUEUE_HINT, appParam.getYarnQueue(), appParam.getTeamId()));

    appParam.doSetHotParams();
    if (appParam.isUploadJob()) {
      String jarPath =
          String.format(
              "%s/%d/%s", Workspace.local().APP_UPLOADS(), appParam.getTeamId(), appParam.getJar());
      if (!new File(jarPath).exists()) {
        Resource resource =
            resourceService.findByResourceName(appParam.getTeamId(), appParam.getJar());
        if (resource != null && StringUtils.isNotBlank(resource.getFilePath())) {
          jarPath = resource.getFilePath();
        }
      }
      appParam.setJarCheckSum(org.apache.commons.io.FileUtils.checksumCRC32(new File(jarPath)));
    }

    if (shouldHandleK8sName(appParam)) {
      switch (appParam.getExecutionModeEnum()) {
        case KUBERNETES_NATIVE_APPLICATION:
          appParam.setK8sName(appParam.getClusterId());
          break;
        case KUBERNETES_NATIVE_SESSION:
          appParam.setK8sName(
              flinkK8sDataTypeConverter.genSessionJobK8sCRName(appParam.getClusterId()));
          break;
      }
    }

    if (save(appParam)) {
      if (appParam.isFlinkSqlJobOrPyFlinkJob()) {
        FlinkSql flinkSql = new FlinkSql(appParam);
        flinkSqlService.create(flinkSql);
      }
      if (appParam.getConfig() != null) {
        configService.create(appParam, true);
      }
      return true;
    } else {
      throw new ApiAlertException("create application failed");
    }
  }

  private boolean shouldHandleK8sName(Application app) {
    return K8sFlinkConfig.isV2Enabled() && ExecutionMode.isKubernetesMode(app.getExecutionMode());
  }

  private boolean existsByJobName(String jobName) {
    return baseMapper.exists(
        new LambdaQueryWrapper<Application>().eq(Application::getJobName, jobName));
  }

  @SuppressWarnings("checkstyle:WhitespaceAround")
  @Override
  @SneakyThrows
  @Transactional(rollbackFor = {Exception.class})
  public Long copy(Application appParam) {
    boolean existsByJobName = this.existsByJobName(appParam.getJobName());
    ApiAlertException.throwIfFalse(
        !existsByJobName,
        "[StreamPark] Application names can't be repeated, copy application failed.");

    Application oldApp = getById(appParam.getId());
    Application newApp = new Application();
    String jobName = appParam.getJobName();

    newApp.setJobName(jobName);
    newApp.setClusterId(
        ExecutionMode.isSessionMode(oldApp.getExecutionModeEnum())
            ? oldApp.getClusterId()
            : jobName);
    newApp.setArgs(appParam.getArgs() != null ? appParam.getArgs() : oldApp.getArgs());
    newApp.setVersionId(oldApp.getVersionId());

    newApp.setFlinkClusterId(oldApp.getFlinkClusterId());
    newApp.setRestartSize(oldApp.getRestartSize());
    newApp.setJobType(oldApp.getJobType());
    newApp.setOptions(oldApp.getOptions());
    newApp.setDynamicProperties(oldApp.getDynamicProperties());
    newApp.setResolveOrder(oldApp.getResolveOrder());
    newApp.setExecutionMode(oldApp.getExecutionMode());
    newApp.setFlinkImage(oldApp.getFlinkImage());
    newApp.setK8sNamespace(oldApp.getK8sNamespace());
    newApp.setK8sRestExposedType(oldApp.getK8sRestExposedType());
    newApp.setK8sPodTemplate(oldApp.getK8sPodTemplate());
    newApp.setK8sJmPodTemplate(oldApp.getK8sJmPodTemplate());
    newApp.setK8sTmPodTemplate(oldApp.getK8sTmPodTemplate());
    newApp.setK8sHadoopIntegration(oldApp.getK8sHadoopIntegration());
    newApp.setDescription(oldApp.getDescription());
    newApp.setAlertId(oldApp.getAlertId());
    newApp.setCpFailureAction(oldApp.getCpFailureAction());
    newApp.setCpFailureRateInterval(oldApp.getCpFailureRateInterval());
    newApp.setCpMaxFailureInterval(oldApp.getCpMaxFailureInterval());
    newApp.setMainClass(oldApp.getMainClass());
    newApp.setAppType(oldApp.getAppType());
    newApp.setResourceFrom(oldApp.getResourceFrom());
    newApp.setProjectId(oldApp.getProjectId());
    newApp.setModule(oldApp.getModule());
    newApp.setUserId(commonService.getUserId());
    newApp.setState(FlinkAppState.ADDED.getValue());
    newApp.setRelease(ReleaseState.NEED_RELEASE.get());
    newApp.setOptionState(OptionState.NONE.getValue());
    newApp.setCreateTime(new Date());
    newApp.setHotParams(oldApp.getHotParams());

    newApp.setJar(oldApp.getJar());
    newApp.setJarCheckSum(oldApp.getJarCheckSum());
    newApp.setTags(oldApp.getTags());
    newApp.setTeamId(oldApp.getTeamId());

    boolean saved = save(newApp);
    if (saved) {
      if (newApp.isFlinkSqlJob()) {
        FlinkSql copyFlinkSql = flinkSqlService.getLatestFlinkSql(appParam.getId(), true);
        newApp.setFlinkSql(copyFlinkSql.getSql());
        newApp.setTeamResource(copyFlinkSql.getTeamResource());
        newApp.setDependency(copyFlinkSql.getDependency());
        FlinkSql flinkSql = new FlinkSql(newApp);
        flinkSqlService.create(flinkSql);
      }
      ApplicationConfig copyConfig = configService.getEffective(appParam.getId());
      if (copyConfig != null) {
        ApplicationConfig config = new ApplicationConfig();
        config.setAppId(newApp.getId());
        config.setFormat(copyConfig.getFormat());
        config.setContent(copyConfig.getContent());
        config.setCreateTime(new Date());
        config.setVersion(1);
        configService.save(config);
        configService.setLatestOrEffective(true, config.getId(), newApp.getId());
      }
      return newApp.getId();
    } else {
      throw new ApiAlertException(
          "create application from copy failed, copy source app: " + oldApp.getJobName());
    }
  }

  @Override
  @Transactional(rollbackFor = {Exception.class})
  public boolean update(Application appParam) {
    Application application = getById(appParam.getId());

    boolean success = validateQueueIfNeeded(application, appParam);
    ApiAlertException.throwIfFalse(
        success,
        String.format(ERROR_APP_QUEUE_HINT, appParam.getYarnQueue(), appParam.getTeamId()));

    application.setRelease(ReleaseState.NEED_RELEASE.get());

    // 1) jar job jar file changed
    if (application.isUploadJob()) {
      if (!Objects.equals(application.getJar(), appParam.getJar())) {
        application.setBuild(true);
      } else {
        File jarFile = new File(WebUtils.getAppTempDir(), appParam.getJar());
        if (jarFile.exists()) {
          try {
            long checkSum = org.apache.commons.io.FileUtils.checksumCRC32(jarFile);
            if (!Objects.equals(checkSum, application.getJarCheckSum())) {
              application.setBuild(true);
            }
          } catch (IOException e) {
            log.error("Error in checksumCRC32 for {}.", jarFile);
            throw new RuntimeException(e);
          }
        }
      }
    }

    // 2) k8s podTemplate changed.
    if (application.getBuild() && ExecutionMode.isKubernetesMode(appParam.getExecutionMode())) {
      if (ObjectUtils.trimNoEquals(
              application.getK8sRestExposedType(), appParam.getK8sRestExposedType())
          || ObjectUtils.trimNoEquals(
              application.getK8sJmPodTemplate(), appParam.getK8sJmPodTemplate())
          || ObjectUtils.trimNoEquals(
              application.getK8sTmPodTemplate(), appParam.getK8sTmPodTemplate())
          || ObjectUtils.trimNoEquals(
              application.getK8sPodTemplates(), appParam.getK8sPodTemplates())
          || ObjectUtils.trimNoEquals(
              application.getK8sHadoopIntegration(), appParam.getK8sHadoopIntegration())
          || ObjectUtils.trimNoEquals(application.getFlinkImage(), appParam.getFlinkImage())) {
        application.setBuild(true);
      }
    }

    // 3) flink version changed
    if (!application.getBuild()
        && !Objects.equals(application.getVersionId(), appParam.getVersionId())) {
      application.setBuild(true);
    }

    // 4) yarn application mode change
    if (!application.getBuild()) {
      if (!application.getExecutionMode().equals(appParam.getExecutionMode())) {
        if (ExecutionMode.YARN_APPLICATION == appParam.getExecutionModeEnum()
            || ExecutionMode.YARN_APPLICATION == application.getExecutionModeEnum()) {
          application.setBuild(true);
        }
      }
    }

    appParam.setJobType(application.getJobType());
    // changes to the following parameters need to be re-release to take effect
    application.setJobName(appParam.getJobName());
    application.setVersionId(appParam.getVersionId());
    application.setArgs(appParam.getArgs());
    application.setOptions(appParam.getOptions());
    application.setDynamicProperties(appParam.getDynamicProperties());
    application.setResolveOrder(appParam.getResolveOrder());
    application.setExecutionMode(appParam.getExecutionMode());
    application.setClusterId(appParam.getClusterId());
    application.setFlinkImage(appParam.getFlinkImage());
    application.setK8sNamespace(appParam.getK8sNamespace());
    application.updateHotParams(appParam);
    application.setK8sRestExposedType(appParam.getK8sRestExposedType());
    application.setK8sPodTemplate(appParam.getK8sPodTemplate());
    application.setK8sJmPodTemplate(appParam.getK8sJmPodTemplate());
    application.setK8sTmPodTemplate(appParam.getK8sTmPodTemplate());
    application.setK8sHadoopIntegration(appParam.getK8sHadoopIntegration());

    // changes to the following parameters do not affect running tasks
    application.setModifyTime(new Date());
    application.setDescription(appParam.getDescription());
    application.setAlertId(appParam.getAlertId());
    application.setRestartSize(appParam.getRestartSize());
    application.setCpFailureAction(appParam.getCpFailureAction());
    application.setCpFailureRateInterval(appParam.getCpFailureRateInterval());
    application.setCpMaxFailureInterval(appParam.getCpMaxFailureInterval());
    application.setTags(appParam.getTags());

    switch (appParam.getExecutionModeEnum()) {
      case YARN_APPLICATION:
      case YARN_PER_JOB:
      case KUBERNETES_NATIVE_APPLICATION:
        application.setFlinkClusterId(null);
        break;
      case REMOTE:
      case YARN_SESSION:
      case KUBERNETES_NATIVE_SESSION:
        application.setFlinkClusterId(appParam.getFlinkClusterId());
        break;
      default:
        break;
    }

    if (shouldHandleK8sName(appParam)) {
      switch (appParam.getExecutionModeEnum()) {
        case KUBERNETES_NATIVE_APPLICATION:
          application.setK8sName(appParam.getClusterId());
          break;
        case KUBERNETES_NATIVE_SESSION:
          if (!Objects.equals(application.getClusterId(), appParam.getClusterId())) {
            application.setK8sName(
                flinkK8sDataTypeConverter.genSessionJobK8sCRName(appParam.getClusterId()));
          }
          break;
      }
    }

    // Flink Sql job...
    if (application.isFlinkSqlJob()) {
      updateFlinkSqlJob(application, appParam);
      return true;
    }

    if (application.isStreamParkJob()) {
      configService.update(appParam, application.isRunning());
    } else {
      application.setJar(appParam.getJar());
      application.setMainClass(appParam.getMainClass());
    }
    this.updateById(application);
    return true;
  }

  /**
   * update FlinkSql type jobs, there are 3 aspects to consider<br>
   * 1. flink sql has changed <br>
   * 2. dependency has changed<br>
   * 3. parameter has changed<br>
   *
   * @param application
   * @param appParam
   */
  private void updateFlinkSqlJob(Application application, Application appParam) {
    FlinkSql effectiveFlinkSql = flinkSqlService.getEffective(application.getId(), true);
    if (effectiveFlinkSql == null) {
      effectiveFlinkSql = flinkSqlService.getCandidate(application.getId(), CandidateType.NEW);
      flinkSqlService.removeById(effectiveFlinkSql.getId());
      FlinkSql sql = new FlinkSql(appParam);
      flinkSqlService.create(sql);
      application.setBuild(true);
    } else {
      // get previous flink sql and decode
      FlinkSql copySourceFlinkSql = flinkSqlService.getById(appParam.getSqlId());
      ApiAlertException.throwIfNull(
          copySourceFlinkSql, "Flink sql is null, update flink sql job failed.");
      copySourceFlinkSql.decode();

      // get submit flink sql
      FlinkSql targetFlinkSql = new FlinkSql(appParam);

      // judge sql and dependency has changed
      ChangeTypeEnum changeTypeEnum = copySourceFlinkSql.checkChange(targetFlinkSql);

      log.info("updateFlinkSqlJob changeTypeEnum: {}", changeTypeEnum);

      // if has been changed
      if (changeTypeEnum.hasChanged()) {
        // check if there is a candidate version for the newly added record
        FlinkSql newFlinkSql = flinkSqlService.getCandidate(application.getId(), CandidateType.NEW);
        // If the candidate version of the new record exists, it will be deleted directly,
        // and only one candidate version will be retained. If the new candidate version is not
        // effective,
        // if it is edited again and the next record comes in, the previous candidate version will
        // be deleted.
        if (newFlinkSql != null) {
          // delete all records about candidates
          flinkSqlService.removeById(newFlinkSql.getId());
        }
        FlinkSql historyFlinkSql =
            flinkSqlService.getCandidate(application.getId(), CandidateType.HISTORY);
        // remove candidate flags that already exist but are set as candidates
        if (historyFlinkSql != null) {
          flinkSqlService.cleanCandidate(historyFlinkSql.getId());
        }
        FlinkSql sql = new FlinkSql(appParam);
        flinkSqlService.create(sql);
        if (changeTypeEnum.isDependencyChanged()) {
          application.setBuild(true);
        }
      } else {
        // judge version has changed
        boolean versionChanged = !effectiveFlinkSql.getId().equals(appParam.getSqlId());
        if (versionChanged) {
          // sql and dependency not changed, but version changed, means that rollback to the version
          CandidateType type = CandidateType.HISTORY;
          flinkSqlService.setCandidate(type, appParam.getId(), appParam.getSqlId());
          application.setRelease(ReleaseState.NEED_ROLLBACK.get());
          application.setBuild(true);
        }
      }
    }
    this.updateById(application);
    this.configService.update(appParam, application.isRunning());
  }

  @Override
  public void updateRelease(Application appParam) {
    LambdaUpdateWrapper<Application> updateWrapper = Wrappers.lambdaUpdate();
    updateWrapper.eq(Application::getId, appParam.getId());
    updateWrapper.set(Application::getRelease, appParam.getRelease());
    updateWrapper.set(Application::getBuild, appParam.getBuild());
    if (appParam.getOptionState() != null) {
      updateWrapper.set(Application::getOptionState, appParam.getOptionState());
    }
    this.update(updateWrapper);
  }

  @Override
  public List<Application> getByProjectId(Long id) {
    return baseMapper.getByProjectId(id);
  }

  @Override
  public List<Application> getByTeamId(Long teamId) {
    return baseMapper.getByTeamId(teamId);
  }

  @Override
  public List<Application> getByTeamIdAndExecutionModes(
      Long teamId, @Nonnull Collection<ExecutionMode> executionModes) {
    return getBaseMapper()
        .selectList(
            new LambdaQueryWrapper<Application>()
                .eq((SFunction<Application, Long>) Application::getTeamId, teamId)
                .in(
                    Application::getExecutionMode,
                    executionModes.stream()
                        .map(ExecutionMode::getMode)
                        .collect(Collectors.toSet())));
  }

  public List<Application> getProbeApps() {
    return this.baseMapper.getProbeApps();
  }

  @Override
  public boolean checkBuildAndUpdate(Application appParam) {
    boolean build = appParam.getBuild();
    if (!build) {
      LambdaUpdateWrapper<Application> updateWrapper = Wrappers.lambdaUpdate();
      updateWrapper.eq(Application::getId, appParam.getId());
      if (appParam.isRunning()) {
        updateWrapper.set(Application::getRelease, ReleaseState.NEED_RESTART.get());
      } else {
        updateWrapper.set(Application::getRelease, ReleaseState.DONE.get());
        updateWrapper.set(Application::getOptionState, OptionState.NONE.getValue());
      }
      this.update(updateWrapper);

      // backup
      if (appParam.isFlinkSqlJob()) {
        FlinkSql newFlinkSql = flinkSqlService.getCandidate(appParam.getId(), CandidateType.NEW);
        if (!appParam.isNeedRollback() && newFlinkSql != null) {
          backUpService.backup(appParam, newFlinkSql);
        }
      }

      // If the current task is not running, or the task has just been added,
      // directly set the candidate version to the official version
      FlinkSql flinkSql = flinkSqlService.getEffective(appParam.getId(), false);
      if (!appParam.isRunning() || flinkSql == null) {
        this.toEffective(appParam);
      }
    }
    return build;
  }

  @Override
  public void clean(Application appParam) {
    appParam.setRelease(ReleaseState.DONE.get());
    this.updateRelease(appParam);
  }

  @Override
  public Application getApp(Application appParam) {
    Application application = this.baseMapper.getApp(appParam);
    ApplicationConfig config = configService.getEffective(appParam.getId());
    config = config == null ? configService.getLatest(appParam.getId()) : config;
    if (config != null) {
      config.setToApplication(application);
    }
    if (application.isFlinkSqlJob()) {
      FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), true);
      if (flinkSql == null) {
        flinkSql = flinkSqlService.getCandidate(application.getId(), CandidateType.NEW);
        flinkSql.setSql(DeflaterUtils.unzipString(flinkSql.getSql()));
      }
      flinkSql.setToApplication(application);
    } else {
      if (application.isCICDJob()) {
        String path =
            this.projectService.getAppConfPath(application.getProjectId(), application.getModule());
        application.setConfPath(path);
      }
    }
    // add flink web url info for k8s-mode
    if (isKubernetesApp(application)) {
      String restUrl = k8SFlinkTrackMonitor.getRemoteRestUrl(toTrackId(application));
      application.setFlinkRestUrl(restUrl);

      // set duration
      long now = System.currentTimeMillis();
      if (application.getTracking() == 1
          && application.getStartTime() != null
          && application.getStartTime().getTime() > 0) {
        application.setDuration(now - application.getStartTime().getTime());
      }
    }

    application.setYarnQueueByHotParams();

    return application;
  }

  /**
   * Check queue label validation when create the application if needed.
   *
   * @param appParam the app to create.
   * @return <code>true</code> if validate it successfully, <code>false</code> else.
   */
  @VisibleForTesting
  public boolean validateQueueIfNeeded(Application appParam) {
    yarnQueueService.checkQueueLabel(appParam.getExecutionModeEnum(), appParam.getYarnQueue());
    if (!isYarnNotDefaultQueue(appParam)) {
      return true;
    }
    return yarnQueueService.existByTeamIdQueueLabel(appParam.getTeamId(), appParam.getYarnQueue());
  }

  /**
   * Check queue label validation when update the application if needed.
   *
   * @param oldApp the old app to update.
   * @param newApp the new app payload.
   * @return <code>true</code> if validate it successfully, <code>false</code> else.
   */
  @VisibleForTesting
  public boolean validateQueueIfNeeded(Application oldApp, Application newApp) {
    yarnQueueService.checkQueueLabel(newApp.getExecutionModeEnum(), newApp.getYarnQueue());
    if (!isYarnNotDefaultQueue(newApp)) {
      return true;
    }

    oldApp.setYarnQueueByHotParams();
    if (ExecutionMode.isYarnPerJobOrAppMode(newApp.getExecutionModeEnum())
        && StringUtils.equals(oldApp.getYarnQueue(), newApp.getYarnQueue())) {
      return true;
    }
    return yarnQueueService.existByTeamIdQueueLabel(newApp.getTeamId(), newApp.getYarnQueue());
  }

  /**
   * Judge the execution mode whether is the Yarn PerJob or Application mode with not default or
   * empty queue label.
   *
   * @param application application entity.
   * @return If the executionMode is (Yarn PerJob or application mode) and the queue label is not
   *     (empty or default), return true, false else.
   */
  private boolean isYarnNotDefaultQueue(Application application) {
    return ExecutionMode.isYarnPerJobOrAppMode(application.getExecutionModeEnum())
        && !yarnQueueService.isDefaultQueue(application.getYarnQueue());
  }
}
