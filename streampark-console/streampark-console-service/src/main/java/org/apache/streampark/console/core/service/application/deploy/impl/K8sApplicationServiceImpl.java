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

package org.apache.streampark.console.core.service.application.deploy.impl;

import org.apache.streampark.common.conf.ConfigConst;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.enums.DevelopmentMode;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.enums.ResolveOrder;
import org.apache.streampark.common.util.CompletableFutureUtils;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.HadoopUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.entity.AppBuildPipeline;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.ApplicationConfig;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.entity.FlinkEnv;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.entity.SavePoint;
import org.apache.streampark.console.core.enums.CheckPointType;
import org.apache.streampark.console.core.enums.ConfigFileType;
import org.apache.streampark.console.core.enums.FlinkAppState;
import org.apache.streampark.console.core.enums.Operation;
import org.apache.streampark.console.core.enums.OptionState;
import org.apache.streampark.console.core.enums.ReleaseState;
import org.apache.streampark.console.core.mapper.ApplicationMapper;
import org.apache.streampark.console.core.service.AppBuildPipeService;
import org.apache.streampark.console.core.service.ApplicationConfigService;
import org.apache.streampark.console.core.service.ApplicationLogService;
import org.apache.streampark.console.core.service.CommonService;
import org.apache.streampark.console.core.service.FlinkEnvService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.LogClientService;
import org.apache.streampark.console.core.service.SavePointService;
import org.apache.streampark.console.core.service.SettingService;
import org.apache.streampark.console.core.service.VariableService;
import org.apache.streampark.console.core.service.application.OpApplicationInfoService;
import org.apache.streampark.console.core.service.application.QueryApplicationInfoService;
import org.apache.streampark.console.core.service.application.deploy.K8sApplicationService;
import org.apache.streampark.console.core.task.FlinkRESTAPIWatcher;
import org.apache.streampark.flink.client.FlinkClient;
import org.apache.streampark.flink.client.bean.CancelRequest;
import org.apache.streampark.flink.client.bean.CancelResponse;
import org.apache.streampark.flink.client.bean.KubernetesSubmitParam;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.kubernetes.FlinkK8sWatcher;
import org.apache.streampark.flink.kubernetes.IngressController;
import org.apache.streampark.flink.kubernetes.helper.KubernetesDeploymentHelper;
import org.apache.streampark.flink.kubernetes.model.TrackId;
import org.apache.streampark.flink.packer.pipeline.BuildResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.JobID;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.streampark.console.core.task.FlinkK8sWatcherWrapper.Bridge.toTrackId;
import static org.apache.streampark.console.core.task.FlinkK8sWatcherWrapper.isKubernetesApp;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class K8sApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application>
    implements K8sApplicationService {

  private final ExecutorService executorService =
      new ThreadPoolExecutor(
          Runtime.getRuntime().availableProcessors() * 5,
          Runtime.getRuntime().availableProcessors() * 10,
          60L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(1024),
          ThreadUtils.threadFactory("streampark-deploy-k8s-executor"),
          new ThreadPoolExecutor.AbortPolicy());
  private static final int DEFAULT_HISTORY_RECORD_LIMIT = 25;

  private static final int DEFAULT_HISTORY_POD_TMPL_RECORD_LIMIT = 5;

  private final LogClientService logClient;
  private final FlinkEnvService flinkEnvService;
  private final CommonService commonService;
  private final SavePointService savePointService;
  private final FlinkK8sWatcher k8SFlinkTrackMonitor;
  private final ApplicationLogService applicationLogService;
  private final OpApplicationInfoService opApplicationInfoService;
  private final AppBuildPipeService appBuildPipeService;
  private final ApplicationConfigService configService;
  private final FlinkSqlService flinkSqlService;
  private final VariableService variableService;
  private final SettingService settingService;
  private final QueryApplicationInfoService queryApplicationInfoService;

  @Override
  public void start(Application appParam, boolean auto) throws Exception {
    Map<Long, CompletableFuture<SubmitResponse>> startFutureMap =
        opApplicationInfoService.getStartFutureMap();
    final Application application = getById(appParam.getId());
    Utils.notNull(application);
    if (!application.isCanBeStart()) {
      throw new ApiAlertException("[StreamPark] The application cannot be started repeatedly.");
    }

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

    opApplicationInfoService.starting(application);
    application.setAllowNonRestored(appParam.getAllowNonRestored());

    String appConf;
    String jobId = new JobID().toHexString();
    ApplicationLog applicationLog = new ApplicationLog();
    applicationLog.setOptionName(Operation.START.getValue());
    applicationLog.setAppId(application.getId());
    applicationLog.setOptionTime(new Date());

    // set the latest to Effective, (it will only become the current effective at this time)
    opApplicationInfoService.toEffective(application);

    ApplicationConfig applicationConfig = configService.getEffective(application.getId());
    ExecutionMode executionMode = ExecutionMode.of(application.getExecutionMode());
    ApiAlertException.throwIfNull(
        executionMode, "ExecutionMode can't be null, start application failed.");
    if (application.isCustomCodeJob()) {
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
                  String.format("%s://%s", fileType.getTypeName(), applicationConfig.getContent());
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

    } else if (application.isFlinkSqlJob()) {
      FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), false);
      Utils.notNull(flinkSql);
      appConf =
          applicationConfig == null
              ? null
              : String.format("yaml://%s", applicationConfig.getContent());
    } else {
      throw new UnsupportedOperationException("Unsupported...");
    }

    Map<String, Object> extraParameter = new HashMap<>(0);
    if (application.isFlinkSqlJob()) {
      FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), true);
      // Get the sql of the replaced placeholder
      String realSql = variableService.replaceVariable(application.getTeamId(), flinkSql.getSql());
      flinkSql.setSql(DeflaterUtils.zipString(realSql));
      extraParameter.put(ConfigConst.KEY_FLINK_SQL(null), flinkSql.getSql());
    }

    KubernetesSubmitParam kubernetesSubmitParam =
        new KubernetesSubmitParam(
            application.getClusterId(),
            application.getK8sNamespace(),
            application.getK8sRestExposedTypeEnum());

    AppBuildPipeline buildPipeline = appBuildPipeService.getById(application.getId());

    Utils.notNull(buildPipeline);

    BuildResult buildResult = buildPipeline.getBuildResult();

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
            queryApplicationInfoService.getSavePointed(appParam),
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
              k8SFlinkTrackMonitor.doWatching(toTrackId(application));

              applicationLog.setSuccess(true);
              // set savepoint to expire
              savePointService.expire(application.getId());
            },
            e -> {
              if (e.getCause() instanceof CancellationException) {
                opApplicationInfoService.updateToStopped(application);
              } else {
                String exception = Utils.stringifyException(e);
                applicationLog.setException(exception);
                applicationLog.setSuccess(false);
                Application app = getById(appParam.getId());
                app.setState(FlinkAppState.FAILED.getValue());
                app.setOptionState(OptionState.NONE.getValue());
                updateById(app);
                k8SFlinkTrackMonitor.unWatching(toTrackId(app));
              }
            })
        .whenComplete(
            (t, e) -> {
              if (ExecutionMode.isKubernetesApplicationMode(application.getExecutionMode())) {
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

  @Override
  public void restart(Application application) throws Exception {
    cancel(application);
    start(application, false);
  }

  @Override
  public void cancel(Application appParam) throws Exception {
    FlinkRESTAPIWatcher.setOptionState(appParam.getId(), OptionState.CANCELLING);
    Application application = getById(appParam.getId());
    application.setState(FlinkAppState.CANCELLING.getValue());

    ApplicationLog applicationLog = new ApplicationLog();
    applicationLog.setOptionName(Operation.CANCEL.getValue());
    applicationLog.setAppId(application.getId());
    applicationLog.setJobManagerUrl(application.getJobManagerUrl());
    applicationLog.setOptionTime(new Date());
    applicationLog.setYarnAppId(application.getClusterId());

    if (appParam.getSavePointed()) {
      FlinkRESTAPIWatcher.addSavepoint(application.getId());
      application.setOptionState(OptionState.SAVEPOINTING.getValue());
    } else {
      application.setOptionState(OptionState.CANCELLING.getValue());
    }

    application.setOptionTime(new Date());
    this.baseMapper.updateById(application);

    Long userId = commonService.getUserId();
    if (!application.getUserId().equals(userId)) {
      FlinkRESTAPIWatcher.addCanceledApp(application.getId(), userId);
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
    clusterId = application.getClusterId();

    Map<String, Object> properties = new HashMap<>();

    CancelRequest cancelRequest =
        new CancelRequest(
            flinkEnv.getFlinkVersion(),
            ExecutionMode.of(application.getExecutionMode()),
            properties,
            clusterId,
            application.getJobId(),
            appParam.getSavePointed(),
            appParam.getDrain(),
            customSavepoint,
            application.getK8sNamespace());

    final Date triggerTime = new Date();
    CompletableFuture<CancelResponse> cancelFuture =
        CompletableFuture.supplyAsync(() -> FlinkClient.cancel(cancelRequest), executorService);

    Map<Long, CompletableFuture<CancelResponse>> cancelFutureMap =
        opApplicationInfoService.getCancelFutureMap();
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
              k8SFlinkTrackMonitor.unWatching(toTrackId(application));
            },
            e -> {
              if (e.getCause() instanceof CancellationException) {
                opApplicationInfoService.updateToStopped(application);
              } else {
                log.error("stop flink job fail.", e);
                application.setOptionState(OptionState.NONE.getValue());
                application.setState(FlinkAppState.FAILED.getValue());
                updateById(application);

                if (appParam.getSavePointed()) {
                  savePointService.expire(application.getId());
                }

                // re-tracking flink job on kubernetes and logging exception
                TrackId id = toTrackId(application);
                k8SFlinkTrackMonitor.unWatching(id);
                k8SFlinkTrackMonitor.doWatching(id);

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
  public List<String> getRecentK8sNamespace() {
    return baseMapper.getRecentK8sNamespace(DEFAULT_HISTORY_RECORD_LIMIT);
  }

  @Override
  public List<String> getRecentK8sClusterId(Integer executionMode) {
    return baseMapper.getRecentK8sClusterId(executionMode, DEFAULT_HISTORY_RECORD_LIMIT);
  }

  @Override
  public List<String> getRecentFlinkBaseImage() {
    return baseMapper.getRecentFlinkBaseImage(DEFAULT_HISTORY_RECORD_LIMIT);
  }

  @Override
  public List<String> getRecentK8sPodTemplate() {
    return baseMapper.getRecentK8sPodTemplate(DEFAULT_HISTORY_POD_TMPL_RECORD_LIMIT);
  }

  @Override
  public List<String> getRecentK8sJmPodTemplate() {
    return baseMapper.getRecentK8sJmPodTemplate(DEFAULT_HISTORY_POD_TMPL_RECORD_LIMIT);
  }

  @Override
  public List<String> getRecentK8sTmPodTemplate() {
    return baseMapper.getRecentK8sTmPodTemplate(DEFAULT_HISTORY_POD_TMPL_RECORD_LIMIT);
  }

  @Override
  public String k8sStartLog(Long id, Integer offset, Integer limit) throws Exception {
    Application application = getById(id);
    ApiAlertException.throwIfNull(
        application, String.format("The application id=%s can't be found.", id));
    if (ExecutionMode.isKubernetesMode(application.getExecutionModeEnum())) {
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
                if (path != null) {
                  return logClient.rollViewLog(path, offset, limit);
                }
                return null;
              })
          .toCompletableFuture()
          .get(5, TimeUnit.SECONDS);
    } else {
      throw new ApiAlertException(
          "Job executionMode must be kubernetes-session|kubernetes-application.");
    }
  }

  private Map<String, Object> getProperties(Application application) {
    Map<String, Object> properties = application.getOptionMap();
    if (ExecutionMode.isKubernetesMode(application.getExecutionModeEnum())) {
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
}
