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

import org.apache.streampark.common.conf.ConfigConst;
import org.apache.streampark.common.conf.K8sFlinkConfig;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.enums.DevelopmentMode;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.enums.ResolveOrder;
import org.apache.streampark.common.enums.RestoreMode;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.common.util.CompletableFutureUtils;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.HadoopUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.ApplicationException;
import org.apache.streampark.console.core.entity.AppBuildPipeline;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationConfig;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.entity.SavePoint;
import org.apache.streampark.console.core.enums.CheckPointType;
import org.apache.streampark.console.core.enums.ConfigFileType;
import org.apache.streampark.console.core.enums.FlinkAppState;
import org.apache.streampark.console.core.enums.Operation;
import org.apache.streampark.console.core.enums.OptionState;
import org.apache.streampark.console.core.enums.ReleaseState;
import org.apache.streampark.console.core.mapper.ApplicationMapper;
import org.apache.streampark.console.core.service.AppBuildPipeService;
import org.apache.streampark.console.core.service.ApplicationBackUpService;
import org.apache.streampark.console.core.service.ApplicationConfigService;
import org.apache.streampark.console.core.service.ApplicationLogService;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.SavePointService;
import org.apache.streampark.console.core.service.SettingService;
import org.apache.streampark.console.core.service.VariableService;
import org.apache.streampark.console.core.service.application.ApplicationActionService;
import org.apache.streampark.console.core.service.application.ApplicationInfoService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;
import org.apache.streampark.console.core.task.FlinkAppHttpWatcher;
import org.apache.streampark.console.core.utils.FlinkK8sDataTypeConverterStub;
import org.apache.streampark.flink.client.FlinkClient;
import org.apache.streampark.flink.client.bean.CancelRequest;
import org.apache.streampark.flink.client.bean.CancelResponse;
import org.apache.streampark.flink.client.bean.KubernetesSubmitParam;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.kubernetes.FlinkK8sWatcher;
import org.apache.streampark.flink.kubernetes.helper.KubernetesDeploymentHelper;
import org.apache.streampark.flink.kubernetes.ingress.IngressController;
import org.apache.streampark.flink.kubernetes.model.TrackId;
import org.apache.streampark.flink.packer.pipeline.BuildResult;
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.streampark.console.core.task.FlinkK8sWatcherWrapper.Bridge.toTrackId;
import static org.apache.streampark.console.core.task.FlinkK8sWatcherWrapper.isKubernetesApp;

@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class ApplicationActionServiceImpl extends ServiceImpl<ApplicationMapper, Application>
    implements ApplicationActionService {

  private final ExecutorService executorService =
      new ThreadPoolExecutor(
          Runtime.getRuntime().availableProcessors() * 5,
          Runtime.getRuntime().availableProcessors() * 10,
          60L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(1024),
          ThreadUtils.threadFactory("streampark-deploy-executor"),
          new ThreadPoolExecutor.AbortPolicy());

  @Autowired private ApplicationBackUpService backUpService;
  @Autowired private ApplicationManageService applicationManageService;

  @Autowired private ApplicationInfoService applicationInfoService;

  @Autowired private ApplicationConfigService configService;

  @Autowired private ApplicationLogService applicationLogService;

  @Autowired private FlinkEnvService flinkEnvService;

  @Autowired private FlinkSqlService flinkSqlService;

  @Autowired private SavePointService savePointService;

  @Autowired private SettingService settingService;

  @Autowired private CommonService commonService;

  @Autowired private FlinkK8sWatcher k8SFlinkTrackMonitor;

  @Autowired private AppBuildPipeService appBuildPipeService;

  @Autowired private FlinkClusterService flinkClusterService;

  @Autowired private VariableService variableService;

  @Autowired private ResourceService resourceService;

  @Autowired private FlinkK8sDataTypeConverterStub flinkK8sDataTypeConverter;

  private final Map<Long, CompletableFuture<SubmitResponse>> startFutureMap =
      new ConcurrentHashMap<>();

  private final Map<Long, CompletableFuture<CancelResponse>> cancelFutureMap =
      new ConcurrentHashMap<>();

  @Override
  public void revoke(Application appParam) throws ApplicationException {
    Application application = getById(appParam.getId());
    ApiAlertException.throwIfNull(
        application,
        String.format("The application id=%s not found, revoke failed.", appParam.getId()));

    // 1) delete files that have been published to workspace
    application.getFsOperator().delete(application.getAppHome());

    // 2) rollback the files to the workspace
    backUpService.revoke(application);

    // 3) restore related status
    LambdaUpdateWrapper<Application> updateWrapper = Wrappers.lambdaUpdate();
    updateWrapper.eq(Application::getId, application.getId());
    if (application.isFlinkSqlJob()) {
      updateWrapper.set(Application::getRelease, ReleaseState.FAILED.get());
    } else {
      updateWrapper.set(Application::getRelease, ReleaseState.NEED_RELEASE.get());
    }
    if (!application.isRunning()) {
      updateWrapper.set(Application::getState, FlinkAppState.REVOKED.getValue());
    }
    baseMapper.update(null, updateWrapper);
  }

  @Override
  public void restart(Application appParam) throws Exception {
    this.cancel(appParam);
    this.start(appParam, false);
  }

  @Override
  public void forcedStop(Application appParam) {
    CompletableFuture<SubmitResponse> startFuture = startFutureMap.remove(appParam.getId());
    CompletableFuture<CancelResponse> cancelFuture = cancelFutureMap.remove(appParam.getId());
    Application application = this.baseMapper.getApp(appParam);
    if (isKubernetesApp(application)) {
      KubernetesDeploymentHelper.watchPodTerminatedLog(
          application.getK8sNamespace(), application.getJobName(), application.getJobId());
      KubernetesDeploymentHelper.deleteTaskDeployment(
          application.getK8sNamespace(), application.getJobName());
      KubernetesDeploymentHelper.deleteTaskConfigMap(
          application.getK8sNamespace(), application.getJobName());
    }
    if (startFuture != null) {
      startFuture.cancel(true);
    }
    if (cancelFuture != null) {
      cancelFuture.cancel(true);
    }
    if (startFuture == null && cancelFuture == null) {
      this.updateToStopped(appParam);
    }
  }

  @Override
  public void cancel(Application appParam) throws Exception {
    FlinkAppHttpWatcher.setOptionState(appParam.getId(), OptionState.CANCELLING);
    Application application = getById(appParam.getId());
    application.setState(FlinkAppState.CANCELLING.getValue());

    ApplicationLog applicationLog = new ApplicationLog();
    applicationLog.setOptionName(Operation.CANCEL.getValue());
    applicationLog.setAppId(application.getId());
    applicationLog.setJobManagerUrl(application.getJobManagerUrl());
    applicationLog.setOptionTime(new Date());
    applicationLog.setYarnAppId(application.getClusterId());

    if (appParam.getSavePointed()) {
      FlinkAppHttpWatcher.addSavepoint(application.getId());
      application.setOptionState(OptionState.SAVEPOINTING.getValue());
    } else {
      application.setOptionState(OptionState.CANCELLING.getValue());
    }

    application.setOptionTime(new Date());
    this.baseMapper.updateById(application);

    Long userId = commonService.getUserId();
    if (!application.getUserId().equals(userId)) {
      FlinkAppHttpWatcher.addCanceledApp(application.getId(), userId);
    }

    FlinkEnv flinkEnv = flinkEnvService.getById(application.getVersionId());

    // infer savepoint
    String customSavepoint = null;
    if (appParam.getSavePointed()) {
      customSavepoint = appParam.getSavePoint();
      if (StringUtils.isBlank(customSavepoint)) {
        customSavepoint = savePointService.getSavePointPath(appParam);
      }
    }

    String clusterId = null;
    if (ExecutionMode.isKubernetesMode(application.getExecutionMode())) {
      clusterId = application.getClusterId();
    } else if (ExecutionMode.isYarnMode(application.getExecutionMode())) {
      if (ExecutionMode.YARN_SESSION.equals(application.getExecutionModeEnum())) {
        FlinkCluster cluster = flinkClusterService.getById(application.getFlinkClusterId());
        ApiAlertException.throwIfNull(
            cluster,
            String.format(
                "The yarn session clusterId=%s can't found, maybe the clusterId is wrong or the cluster has been deleted. Please contact the Admin.",
                application.getFlinkClusterId()));
        clusterId = cluster.getClusterId();
      } else {
        clusterId = application.getAppId();
      }
    }

    Map<String, Object> properties = new HashMap<>();

    if (ExecutionMode.isRemoteMode(application.getExecutionModeEnum())) {
      FlinkCluster cluster = flinkClusterService.getById(application.getFlinkClusterId());
      ApiAlertException.throwIfNull(
          cluster,
          String.format(
              "The clusterId=%s cannot be find, maybe the clusterId is wrong or "
                  + "the cluster has been deleted. Please contact the Admin.",
              application.getFlinkClusterId()));
      URI activeAddress = cluster.getRemoteURI();
      properties.put(RestOptions.ADDRESS.key(), activeAddress.getHost());
      properties.put(RestOptions.PORT.key(), activeAddress.getPort());
    }

    CancelRequest cancelRequest =
        new CancelRequest(
            application.getId(),
            flinkEnv.getFlinkVersion(),
            ExecutionMode.of(application.getExecutionMode()),
            properties,
            clusterId,
            application.getJobId(),
            appParam.getSavePointed(),
            appParam.getDrain(),
            customSavepoint,
            appParam.getNativeFormat(),
            application.getK8sNamespace());

    final Date triggerTime = new Date();
    CompletableFuture<CancelResponse> cancelFuture =
        CompletableFuture.supplyAsync(() -> FlinkClient.cancel(cancelRequest), executorService);

    cancelFutureMap.put(application.getId(), cancelFuture);

    CompletableFutureUtils.runTimeout(
            cancelFuture,
            10L,
            TimeUnit.MINUTES,
            cancelResponse -> {
              applicationLog.setSuccess(true);
              if (cancelResponse != null && cancelResponse.savePointDir() != null) {
                String savePointDir = cancelResponse.savePointDir();
                log.info("savePoint path: {}", savePointDir);
                SavePoint savePoint = new SavePoint();
                savePoint.setPath(savePointDir);
                savePoint.setAppId(application.getId());
                savePoint.setLatest(true);
                savePoint.setType(CheckPointType.SAVEPOINT.get());
                savePoint.setCreateTime(new Date());
                savePoint.setTriggerTime(triggerTime);
                savePointService.save(savePoint);
              }
              if (isKubernetesApp(application)) {
                k8SFlinkTrackMonitor.unWatching(toTrackId(application));
              }
            },
            e -> {
              if (e.getCause() instanceof CancellationException) {
                updateToStopped(application);
              } else {
                log.error("stop flink job fail.", e);
                application.setOptionState(OptionState.NONE.getValue());
                application.setState(FlinkAppState.FAILED.getValue());
                updateById(application);

                if (appParam.getSavePointed()) {
                  savePointService.expire(application.getId());
                }

                // re-tracking flink job on kubernetes and logging exception
                if (isKubernetesApp(application)) {
                  TrackId id = toTrackId(application);
                  k8SFlinkTrackMonitor.unWatching(id);
                  k8SFlinkTrackMonitor.doWatching(id);
                } else {
                  FlinkAppHttpWatcher.unWatching(application.getId());
                }

                String exception = Utils.stringifyException(e);
                applicationLog.setException(exception);
                applicationLog.setSuccess(false);
              }
            })
        .whenComplete(
            (t, e) -> {
              cancelFutureMap.remove(application.getId());
              applicationLogService.save(applicationLog);
            });
  }

  @Override
  @Transactional(rollbackFor = {Exception.class})
  public void start(Application appParam, boolean auto) throws Exception {
    // 1) check application
    final Application application = getById(appParam.getId());
    Utils.notNull(application);
    if (!application.isCanBeStart()) {
      throw new ApiAlertException("[StreamPark] The application cannot be started repeatedly.");
    }

    AppBuildPipeline buildPipeline = appBuildPipeService.getById(application.getId());
    Utils.notNull(buildPipeline);

    FlinkEnv flinkEnv = flinkEnvService.getByIdOrDefault(application.getVersionId());
    if (flinkEnv == null) {
      throw new ApiAlertException("[StreamPark] can no found flink version");
    }

    // if manually started, clear the restart flag
    if (!auto) {
      application.setRestartCount(0);
    } else {
      if (!application.isNeedRestartOnFailed()) {
        return;
      }
      appParam.setSavePointed(true);
      application.setRestartCount(application.getRestartCount() + 1);
    }

    // 2) update app state to starting...
    starting(application);

    String jobId = new JobID().toHexString();
    ApplicationLog applicationLog = new ApplicationLog();
    applicationLog.setOptionName(Operation.START.getValue());
    applicationLog.setAppId(application.getId());
    applicationLog.setOptionTime(new Date());

    // set the latest to Effective, (it will only become the current effective at this time)
    applicationManageService.toEffective(application);

    Map<String, Object> extraParameter = new HashMap<>(0);
    if (application.isFlinkSqlJob()) {
      FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), true);
      // Get the sql of the replaced placeholder
      String realSql = variableService.replaceVariable(application.getTeamId(), flinkSql.getSql());
      flinkSql.setSql(DeflaterUtils.zipString(realSql));
      extraParameter.put(ConfigConst.KEY_FLINK_SQL(null), flinkSql.getSql());
    }

    // TODO Need to display more K8s submission parameters in the front-end UI.
    //      See: org.apache.streampark.flink.client.bean.KubernetesSubmitParam
    KubernetesSubmitParam kubernetesSubmitParam =
        KubernetesSubmitParam.apply(
            application.getClusterId(),
            application.getK8sName(),
            application.getK8sNamespace(),
            application.getFlinkImage(),
            application.getK8sRestExposedTypeEnum(),
            flinkK8sDataTypeConverter.genDefaultFlinkDeploymentIngressDef());

    Tuple2<String, String> userJarAndAppConf = getUserJarAndAppConf(flinkEnv, application);
    String flinkUserJar = userJarAndAppConf.f0;
    String appConf = userJarAndAppConf.f1;

    BuildResult buildResult = buildPipeline.getBuildResult();
    if (ExecutionMode.YARN_APPLICATION.equals(application.getExecutionModeEnum())) {
      buildResult = new ShadedBuildResponse(null, flinkUserJar, true);
    }

    // Get the args after placeholder replacement
    String applicationArgs =
        variableService.replaceVariable(application.getTeamId(), application.getArgs());

    SubmitRequest submitRequest =
        new SubmitRequest(
            flinkEnv.getFlinkVersion(),
            ExecutionMode.of(application.getExecutionMode()),
            getProperties(application),
            flinkEnv.getFlinkConf(),
            DevelopmentMode.of(application.getJobType()),
            application.getId(),
            jobId,
            application.getJobName(),
            appConf,
            application.getApplicationType(),
            getSavePointed(appParam),
            appParam.getRestoreMode() == null ? null : RestoreMode.of(appParam.getRestoreMode()),
            applicationArgs,
            buildResult,
            kubernetesSubmitParam,
            extraParameter);

    CompletableFuture<SubmitResponse> future =
        CompletableFuture.supplyAsync(() -> FlinkClient.submit(submitRequest), executorService);

    startFutureMap.put(application.getId(), future);

    CompletableFutureUtils.runTimeout(
            future,
            2L,
            TimeUnit.MINUTES,
            submitResponse -> {
              if (submitResponse.flinkConfig() != null) {
                String jmMemory =
                    submitResponse.flinkConfig().get(ConfigConst.KEY_FLINK_JM_PROCESS_MEMORY());
                if (jmMemory != null) {
                  application.setJmMemory(MemorySize.parse(jmMemory).getMebiBytes());
                }
                String tmMemory =
                    submitResponse.flinkConfig().get(ConfigConst.KEY_FLINK_TM_PROCESS_MEMORY());
                if (tmMemory != null) {
                  application.setTmMemory(MemorySize.parse(tmMemory).getMebiBytes());
                }
              }
              application.setAppId(submitResponse.clusterId());
              if (StringUtils.isNoneEmpty(submitResponse.jobId())) {
                application.setJobId(submitResponse.jobId());
              }

              if (StringUtils.isNoneEmpty(submitResponse.jobManagerUrl())) {
                application.setJobManagerUrl(submitResponse.jobManagerUrl());
                applicationLog.setJobManagerUrl(submitResponse.jobManagerUrl());
              }
              applicationLog.setYarnAppId(submitResponse.clusterId());
              application.setStartTime(new Date());
              application.setEndTime(null);
              if (isKubernetesApp(application)) {
                application.setRelease(ReleaseState.DONE.get());
              }
              updateById(application);

              // if start completed, will be added task to tracking queue
              if (isKubernetesApp(application)) {
                k8SFlinkTrackMonitor.doWatching(toTrackId(application));
              } else {
                FlinkAppHttpWatcher.setOptionState(appParam.getId(), OptionState.STARTING);
                FlinkAppHttpWatcher.doWatching(application);
              }

              applicationLog.setSuccess(true);
              // set savepoint to expire
              savePointService.expire(application.getId());
            },
            e -> {
              if (e.getCause() instanceof CancellationException) {
                updateToStopped(application);
              } else {
                String exception = Utils.stringifyException(e);
                applicationLog.setException(exception);
                applicationLog.setSuccess(false);
                Application app = getById(appParam.getId());
                app.setState(FlinkAppState.FAILED.getValue());
                app.setOptionState(OptionState.NONE.getValue());
                updateById(app);
                if (isKubernetesApp(app)) {
                  k8SFlinkTrackMonitor.unWatching(toTrackId(app));
                } else {
                  FlinkAppHttpWatcher.unWatching(appParam.getId());
                }
              }
            })
        .whenComplete(
            (t, e) -> {
              if (!K8sFlinkConfig.isV2Enabled()
                  && ExecutionMode.isKubernetesApplicationMode(application.getExecutionMode())) {
                String domainName = settingService.getIngressModeDefault();
                if (StringUtils.isNotBlank(domainName)) {
                  try {
                    IngressController.configureIngress(
                        domainName, application.getClusterId(), application.getK8sNamespace());
                  } catch (KubernetesClientException kubernetesClientException) {
                    log.info(
                        "Failed to create ingress, stack info:{}",
                        kubernetesClientException.getMessage());
                    applicationLog.setException(e.getMessage());
                    applicationLog.setSuccess(false);
                    applicationLogService.save(applicationLog);
                    application.setState(FlinkAppState.FAILED.getValue());
                    application.setOptionState(OptionState.NONE.getValue());
                    updateById(application);
                    return;
                  }
                }
              }

              applicationLogService.save(applicationLog);
              startFutureMap.remove(application.getId());
            });
  }

  private void starting(Application application) {
    application.setState(FlinkAppState.STARTING.getValue());
    application.setOptionTime(new Date());
    updateById(application);
  }

  private Tuple2<String, String> getUserJarAndAppConf(FlinkEnv flinkEnv, Application application) {
    ExecutionMode executionMode = application.getExecutionModeEnum();
    ApplicationConfig applicationConfig = configService.getEffective(application.getId());

    ApiAlertException.throwIfNull(
        executionMode, "ExecutionMode can't be null, start application failed.");

    String flinkUserJar = null;
    String appConf = null;

    switch (application.getDevelopmentMode()) {
      case FLINK_SQL:
        FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), false);
        Utils.notNull(flinkSql);
        // 1) dist_userJar
        String sqlDistJar = commonService.getSqlClientJar(flinkEnv);
        // 2) appConfig
        appConf =
            applicationConfig == null
                ? null
                : String.format("yaml://%s", applicationConfig.getContent());
        // 3) client
        if (ExecutionMode.YARN_APPLICATION.equals(executionMode)) {
          String clientPath = Workspace.remote().APP_CLIENT();
          flinkUserJar = String.format("%s/%s", clientPath, sqlDistJar);
        }
        break;

      case PYFLINK:
        Resource resource =
            resourceService.findByResourceName(application.getTeamId(), application.getJar());

        ApiAlertException.throwIfNull(
            resource, "pyflink file can't be null, start application failed.");

        ApiAlertException.throwIfNull(
            resource.getFilePath(), "pyflink file can't be null, start application failed.");

        ApiAlertException.throwIfFalse(
            resource.getFilePath().endsWith(ConfigConst.PYTHON_SUFFIX()),
            "pyflink format error, must be a \".py\" suffix, start application failed.");

        flinkUserJar = resource.getFilePath();
        break;

      case CUSTOM_CODE:
        if (application.isUploadJob()) {
          appConf =
              String.format(
                  "json://{\"%s\":\"%s\"}",
                  ConfigConst.KEY_FLINK_APPLICATION_MAIN_CLASS(), application.getMainClass());
        } else {
          switch (application.getApplicationType()) {
            case STREAMPARK_FLINK:
              ConfigFileType fileType = ConfigFileType.of(applicationConfig.getFormat());
              if (fileType != null && !fileType.equals(ConfigFileType.UNKNOWN)) {
                appConf =
                    String.format(
                        "%s://%s", fileType.getTypeName(), applicationConfig.getContent());
              } else {
                throw new IllegalArgumentException(
                    "application' config type error,must be ( yaml| properties| hocon )");
              }
              break;
            case APACHE_FLINK:
              appConf =
                  String.format(
                      "json://{\"%s\":\"%s\"}",
                      ConfigConst.KEY_FLINK_APPLICATION_MAIN_CLASS(), application.getMainClass());
              break;
            default:
              throw new IllegalArgumentException(
                  "[StreamPark] ApplicationType must be (StreamPark flink | Apache flink)... ");
          }
        }

        if (ExecutionMode.YARN_APPLICATION.equals(executionMode)) {
          switch (application.getApplicationType()) {
            case STREAMPARK_FLINK:
              flinkUserJar =
                  String.format(
                      "%s/%s", application.getAppLib(), application.getModule().concat(".jar"));
              break;
            case APACHE_FLINK:
              flinkUserJar = String.format("%s/%s", application.getAppHome(), application.getJar());
              if (!FsOperator.hdfs().exists(flinkUserJar)) {
                resource =
                    resourceService.findByResourceName(
                        application.getTeamId(), application.getJar());
                if (resource != null && StringUtils.isNotBlank(resource.getFilePath())) {
                  flinkUserJar =
                      String.format(
                          "%s/%s",
                          application.getAppHome(), new File(resource.getFilePath()).getName());
                }
              }
              break;
            default:
              throw new IllegalArgumentException(
                  "[StreamPark] ApplicationType must be (StreamPark flink | Apache flink)... ");
          }
        }
        break;
    }
    return Tuple2.of(flinkUserJar, appConf);
  }

  private Map<String, Object> getProperties(Application application) {
    Map<String, Object> properties = new HashMap<>(application.getOptionMap());
    if (ExecutionMode.isRemoteMode(application.getExecutionModeEnum())) {
      FlinkCluster cluster = flinkClusterService.getById(application.getFlinkClusterId());
      ApiAlertException.throwIfNull(
          cluster,
          String.format(
              "The clusterId=%s can't be find, maybe the clusterId is wrong or "
                  + "the cluster has been deleted. Please contact the Admin.",
              application.getFlinkClusterId()));
      URI activeAddress = cluster.getRemoteURI();
      properties.put(RestOptions.ADDRESS.key(), activeAddress.getHost());
      properties.put(RestOptions.PORT.key(), activeAddress.getPort());
    } else if (ExecutionMode.isYarnMode(application.getExecutionModeEnum())) {
      if (ExecutionMode.YARN_SESSION.equals(application.getExecutionModeEnum())) {
        FlinkCluster cluster = flinkClusterService.getById(application.getFlinkClusterId());
        ApiAlertException.throwIfNull(
            cluster,
            String.format(
                "The yarn session clusterId=%s cannot be find, maybe the clusterId is wrong or "
                    + "the cluster has been deleted. Please contact the Admin.",
                application.getFlinkClusterId()));
        properties.put(ConfigConst.KEY_YARN_APP_ID(), cluster.getClusterId());
      } else {
        String yarnQueue =
            (String) application.getHotParamsMap().get(ConfigConst.KEY_YARN_APP_QUEUE());
        String yarnLabelExpr =
            (String) application.getHotParamsMap().get(ConfigConst.KEY_YARN_APP_NODE_LABEL());
        Optional.ofNullable(yarnQueue)
            .ifPresent(yq -> properties.put(ConfigConst.KEY_YARN_APP_QUEUE(), yq));
        Optional.ofNullable(yarnLabelExpr)
            .ifPresent(yLabel -> properties.put(ConfigConst.KEY_YARN_APP_NODE_LABEL(), yLabel));
      }
    } else if (ExecutionMode.isKubernetesMode(application.getExecutionModeEnum())) {
      properties.put(ConfigConst.KEY_K8S_IMAGE_PULL_POLICY(), "Always");
    }

    if (ExecutionMode.isKubernetesApplicationMode(application.getExecutionMode())) {
      try {
        HadoopUtils.yarnClient();
        properties.put(JobManagerOptions.ARCHIVE_DIR.key(), Workspace.ARCHIVES_FILE_PATH());
      } catch (Exception e) {
        // skip
      }
    }

    if (application.getAllowNonRestored()) {
      properties.put(SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE.key(), true);
    }

    Map<String, String> dynamicProperties =
        PropertiesUtils.extractDynamicPropertiesAsJava(application.getDynamicProperties());
    properties.putAll(dynamicProperties);
    ResolveOrder resolveOrder = ResolveOrder.of(application.getResolveOrder());
    if (resolveOrder != null) {
      properties.put(CoreOptions.CLASSLOADER_RESOLVE_ORDER.key(), resolveOrder.getName());
    }

    return properties;
  }

  private void updateToStopped(Application app) {
    Application application = getById(app);
    application.setOptionState(OptionState.NONE.getValue());
    application.setState(FlinkAppState.CANCELED.getValue());
    application.setOptionTime(new Date());
    updateById(application);
    savePointService.expire(application.getId());
    // re-tracking flink job on kubernetes and logging exception
    if (isKubernetesApp(application)) {
      TrackId id = toTrackId(application);
      k8SFlinkTrackMonitor.unWatching(id);
      k8SFlinkTrackMonitor.doWatching(id);
    } else {
      FlinkAppHttpWatcher.unWatching(application.getId());
    }
  }

  private String getSavePointed(Application appParam) {
    if (appParam.getSavePointed()) {
      if (appParam.getSavePoint() == null) {
        SavePoint savePoint = savePointService.getLatest(appParam.getId());
        if (savePoint != null) {
          return savePoint.getPath();
        }
      } else {
        return appParam.getSavePoint();
      }
    }
    return null;
  }
}
