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
import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.SparkDevelopmentMode;
import org.apache.streampark.common.enums.SparkExecutionMode;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.common.util.HadoopUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.ApplicationException;
import org.apache.streampark.console.core.entity.AppBuildPipeline;
import org.apache.streampark.console.core.entity.ApplicationConfig;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.entity.SparkApplicationLog;
import org.apache.streampark.console.core.entity.SparkEnv;
import org.apache.streampark.console.core.enums.ConfigFileTypeEnum;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.enums.SparkAppStateEnum;
import org.apache.streampark.console.core.enums.SparkOperationEnum;
import org.apache.streampark.console.core.enums.SparkOptionStateEnum;
import org.apache.streampark.console.core.mapper.SparkApplicationMapper;
import org.apache.streampark.console.core.service.AppBuildPipeService;
import org.apache.streampark.console.core.service.ApplicationConfigService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.ServiceHelper;
import org.apache.streampark.console.core.service.SparkApplicationLogService;
import org.apache.streampark.console.core.service.SparkEnvService;
import org.apache.streampark.console.core.service.VariableService;
import org.apache.streampark.console.core.service.application.SparkApplicationActionService;
import org.apache.streampark.console.core.service.application.SparkApplicationInfoService;
import org.apache.streampark.console.core.watcher.SparkAppHttpWatcher;
import org.apache.streampark.flink.packer.pipeline.BuildResult;
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse;
import org.apache.streampark.spark.client.SparkClient;
import org.apache.streampark.spark.client.bean.StopRequest;
import org.apache.streampark.spark.client.bean.StopResponse;
import org.apache.streampark.spark.client.bean.SubmitRequest;
import org.apache.streampark.spark.client.bean.SubmitResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.MemorySize;
import org.apache.hadoop.service.Service.STATE;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static org.apache.streampark.console.base.enums.MessageStatus.APP_ACTION_REPEAT_START_ERROR;
import static org.apache.streampark.console.base.enums.MessageStatus.APP_ACTION_SAME_TASK_IN_ALREADY_RUN_ERROR;
import static org.apache.streampark.console.base.enums.MessageStatus.FLINK_ENV_FLINK_VERSION_NOT_FOUND;

import static org.apache.hadoop.service.Service.STATE.STARTED;

@Slf4j
@Service
public class SparkApplicationActionServiceImpl
    extends
        ServiceImpl<SparkApplicationMapper, SparkApplication>
    implements
        SparkApplicationActionService {

    @Qualifier("streamparkDeployExecutor")
    @Autowired
    private Executor executorService;

    @Autowired
    private SparkApplicationInfoService applicationInfoService;

    @Autowired
    private ApplicationConfigService configService;

    @Autowired
    private SparkApplicationLogService applicationLogService;

    @Autowired
    private SparkEnvService sparkEnvService;

    @Autowired
    private FlinkSqlService flinkSqlService;

    @Autowired
    private ServiceHelper serviceHelper;

    @Autowired
    private AppBuildPipeService appBuildPipeService;

    @Autowired
    private VariableService variableService;

    @Autowired
    private ResourceService resourceService;

    private final Map<Long, CompletableFuture<SubmitResponse>> startFutureMap = new ConcurrentHashMap<>();

    private final Map<Long, CompletableFuture<StopResponse>> stopFutureMap = new ConcurrentHashMap<>();

    @Override
    public void revoke(Long appId) throws ApplicationException {
        SparkApplication application = getById(appId);
        ApiAlertException.throwIfNull(
            application, String.format("The application id=%s not found, revoke failed.", appId));

        // 1) delete files that have been published to workspace
        application.getFsOperator().delete(application.getAppHome());

        // 2) rollback the files to the workspace
        // backUpService.revoke(application);

        // 3) restore related status
        LambdaUpdateWrapper<SparkApplication> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(SparkApplication::getId, application.getId());
        if (application.isSparkSqlJob()) {
            updateWrapper.set(SparkApplication::getRelease, ReleaseStateEnum.FAILED.get());
        } else {
            updateWrapper.set(SparkApplication::getRelease, ReleaseStateEnum.NEED_RELEASE.get());
        }
        if (!application.isRunning()) {
            updateWrapper.set(SparkApplication::getState, SparkAppStateEnum.REVOKED.getValue());
        }
        baseMapper.update(null, updateWrapper);
    }

    @Override
    public void restart(SparkApplication appParam) throws Exception {
        this.stop(appParam);
        this.start(appParam, false);
    }

    @Override
    public void forcedStop(Long id) {
        CompletableFuture<SubmitResponse> startFuture = startFutureMap.remove(id);
        CompletableFuture<StopResponse> stopFuture = stopFutureMap.remove(id);
        SparkApplication application = this.baseMapper.selectApp(id);
        if (startFuture != null) {
            startFuture.cancel(true);
        }
        if (stopFuture != null) {
            stopFuture.cancel(true);
        }
        if (startFuture == null && stopFuture == null) {
            this.doStopped(id);
        }
    }

    @Override
    public void stop(SparkApplication appParam) throws Exception {
        SparkAppHttpWatcher.setOptionState(appParam.getId(), SparkOptionStateEnum.STOPPING);
        SparkApplication application = getById(appParam.getId());
        application.setState(SparkAppStateEnum.STOPPING.getValue());

        SparkApplicationLog applicationLog = new SparkApplicationLog();
        applicationLog.setOptionName(SparkOperationEnum.STOP.getValue());
        applicationLog.setAppId(application.getId());
        applicationLog.setTrackUrl(application.getJobManagerUrl());
        applicationLog.setOptionTime(new Date());
        applicationLog.setSparkAppId(application.getJobId());
        applicationLog.setUserId(serviceHelper.getUserId());
        application.setOptionTime(new Date());
        this.baseMapper.updateById(application);

        Long userId = serviceHelper.getUserId();
        if (!application.getUserId().equals(userId)) {
            SparkAppHttpWatcher.addCanceledApp(application.getId(), userId);
        }

        SparkEnv sparkEnv = sparkEnvService.getById(application.getVersionId());

        Map<String, String> stopProper = new HashMap<>();

        StopRequest stopRequest =
            new StopRequest(
                application.getId(),
                sparkEnv.getSparkVersion(),
                SparkExecutionMode.of(application.getExecutionMode()),
                stopProper,
                application.getJobId());

        CompletableFuture<StopResponse> stopFuture =
            CompletableFuture.supplyAsync(() -> SparkClient.stop(stopRequest), executorService);

        stopFutureMap.put(application.getId(), stopFuture);
        stopFuture.whenComplete(
            (cancelResponse, throwable) -> {
                stopFutureMap.remove(application.getId());
                if (throwable != null) {
                    String exception = ExceptionUtils.stringifyException(throwable);
                    applicationLog.setException(exception);
                    applicationLog.setSuccess(false);
                    applicationLogService.save(applicationLog);

                    if (throwable instanceof CancellationException) {
                        doStopped(application.getId());
                    } else {
                        log.error("stop spark job failed.", throwable);
                        application.setOptionState(SparkOptionStateEnum.NONE.getValue());
                        application.setState(SparkAppStateEnum.FAILED.getValue());
                        updateById(application);
                        SparkAppHttpWatcher.unWatching(application.getId());
                    }
                    return;
                }
                applicationLog.setSuccess(true);
                // save log...
                applicationLogService.save(applicationLog);
            });
    }

    @Override
    public void start(SparkApplication appParam, boolean auto) throws Exception {
        // 1) check application
        final SparkApplication application = getById(appParam.getId());
        AssertUtils.notNull(application);
        ApiAlertException.throwIfTrue(
            !application.isCanBeStart(), APP_ACTION_REPEAT_START_ERROR);

        SparkEnv sparkEnv = sparkEnvService.getByIdOrDefault(application.getVersionId());
        ApiAlertException.throwIfNull(sparkEnv, "[StreamPark] can no found spark version");

        if (SparkExecutionMode.isYarnMode(application.getSparkExecutionMode())) {
            checkYarnBeforeStart(application);
        }

        AppBuildPipeline buildPipeline = appBuildPipeService.getById(application.getId());
        AssertUtils.notNull(buildPipeline);

        // if manually started, clear the restart flag
        if (!auto) {
            application.setRestartCount(0);
        } else {
            if (!application.isNeedRestartOnFailed()) {
                return;
            }
            application.setRestartCount(application.getRestartCount() + 1);
        }

        // 2) update app state to starting...
        starting(application);

        String jobId = new JobID().toHexString();
        SparkApplicationLog applicationLog = new SparkApplicationLog();
        applicationLog.setOptionName(SparkOperationEnum.START.getValue());
        applicationLog.setAppId(application.getId());
        applicationLog.setOptionTime(new Date());
        applicationLog.setUserId(serviceHelper.getUserId());

        // set the latest to Effective, (it will only become the current effective at this time)
        // applicationManageService.toEffective(application);

        Map<String, Object> extraParameter = new HashMap<>(0);
        if (application.isSparkSqlJob()) {
            FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), true);
            // Get the sql of the replaced placeholder
            String realSql = variableService.replaceVariable(application.getTeamId(), flinkSql.getSql());
            flinkSql.setSql(DeflaterUtils.zipString(realSql));
            extraParameter.put(ConfigKeys.KEY_FLINK_SQL(null), flinkSql.getSql());
        }

        Tuple2<String, String> userJarAndAppConf = getUserJarAndAppConf(sparkEnv, application);
        String flinkUserJar = userJarAndAppConf.f0;
        String appConf = userJarAndAppConf.f1;

        BuildResult buildResult = buildPipeline.getBuildResult();
        if (SparkExecutionMode.YARN_CLUSTER == application.getSparkExecutionMode()
            || SparkExecutionMode.YARN_CLIENT == application.getSparkExecutionMode()) {
            buildResult = new ShadedBuildResponse(null, flinkUserJar, true);
            application.setJobManagerUrl(YarnUtils.getRMWebAppURL(true));
        }

        // Get the args after placeholder replacement
        String applicationArgs = variableService.replaceVariable(application.getTeamId(), application.getArgs());

        SubmitRequest submitRequest = new SubmitRequest(
            sparkEnv.getSparkVersion(),
            SparkExecutionMode.of(application.getExecutionMode()),
            getProperties(application),
            sparkEnv.getSparkConf(),
            SparkDevelopmentMode.valueOf(application.getJobType()),
            application.getId(),
            jobId,
            application.getJobName(),
            appConf,
            application.getApplicationType(),
            applicationArgs,
            application.getHadoopUser(),
            buildResult,
            extraParameter);

        CompletableFuture<SubmitResponse> future = CompletableFuture
            .supplyAsync(() -> SparkClient.submit(submitRequest), executorService);

        startFutureMap.put(application.getId(), future);
        future.whenComplete(
            (response, throwable) -> {
                // 1) remove Future
                startFutureMap.remove(application.getId());

                // 2) exception
                if (throwable != null) {
                    String exception = ExceptionUtils.stringifyException(throwable);
                    applicationLog.setException(exception);
                    applicationLog.setSuccess(false);
                    applicationLogService.save(applicationLog);
                    if (throwable instanceof CancellationException) {
                        doStopped(application.getId());
                    } else {
                        SparkApplication app = getById(appParam.getId());
                        app.setState(SparkAppStateEnum.FAILED.getValue());
                        app.setOptionState(SparkOptionStateEnum.NONE.getValue());
                        updateById(app);
                        SparkAppHttpWatcher.unWatching(appParam.getId());
                    }
                    return;
                }

                // 3) success
                applicationLog.setSuccess(true);
                // TODO：修改为spark对应的参数
                if (response.sparkConfig() != null) {
                    String jmMemory = response.sparkConfig().get(ConfigKeys.KEY_FLINK_JM_PROCESS_MEMORY());
                    if (jmMemory != null) {
                        application.setJmMemory(MemorySize.parse(jmMemory).getMebiBytes());
                    }
                    String tmMemory = response.sparkConfig().get(ConfigKeys.KEY_FLINK_TM_PROCESS_MEMORY());
                    if (tmMemory != null) {
                        application.setTmMemory(MemorySize.parse(tmMemory).getMebiBytes());
                    }
                }
                application.setAppId(response.clusterId());
                if (StringUtils.isNoneEmpty(response.clusterId())) {
                    application.setJobId(response.clusterId());
                }
                applicationLog.setSparkAppId(response.clusterId());
                application.setStartTime(new Date());
                application.setEndTime(null);

                // if start completed, will be added task to tracking queue
                SparkAppHttpWatcher.setOptionState(appParam.getId(), SparkOptionStateEnum.STARTING);
                SparkAppHttpWatcher.doWatching(application);

                // update app
                updateById(application);
                // save log
                applicationLogService.save(applicationLog);
            });
    }

    /**
     * Check whether a job with the same name is running in the yarn queue
     *
     * @param jobName
     * @return
     */
    private boolean checkAppRepeatInYarn(String jobName) {
        try {
            YarnClient yarnClient = HadoopUtils.yarnClient();
            Set<String> types = Sets.newHashSet(
                ApplicationType.STREAMPARK_FLINK.getName(), ApplicationType.APACHE_FLINK.getName());
            EnumSet<YarnApplicationState> states = EnumSet.of(YarnApplicationState.RUNNING,
                YarnApplicationState.ACCEPTED);
            List<ApplicationReport> applications = yarnClient.getApplications(types, states);
            for (ApplicationReport report : applications) {
                if (report.getName().equals(jobName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("The yarn api is abnormal. Ensure that yarn is running properly.");
        }
    }

    private void starting(SparkApplication application) {
        application.setState(SparkAppStateEnum.STARTING.getValue());
        application.setOptionTime(new Date());
        updateById(application);
    }

    private Tuple2<String, String> getUserJarAndAppConf(
                                                        SparkEnv sparkEnv, SparkApplication application) {
        SparkExecutionMode executionModeEnum = application.getSparkExecutionMode();
        ApplicationConfig applicationConfig = configService.getEffective(application.getId());

        ApiAlertException.throwIfNull(
            executionModeEnum, "ExecutionMode can't be null, start application failed.");

        String flinkUserJar = null;
        String appConf = null;

        switch (application.getDevelopmentMode()) {
            case FLINK_SQL:
                FlinkSql flinkSql = flinkSqlService.getEffective(application.getId(), false);
                AssertUtils.notNull(flinkSql);
                // 1) dist_userJar
                String sqlDistJar = serviceHelper.getSparkSqlClientJar(sparkEnv);
                // 2) appConfig
                appConf = applicationConfig == null
                    ? null
                    : String.format("yaml://%s", applicationConfig.getContent());
                // 3) client
                if (SparkExecutionMode.YARN_CLUSTER == executionModeEnum) {
                    String clientPath = Workspace.remote().APP_CLIENT();
                    flinkUserJar = String.format("%s/%s", clientPath, sqlDistJar);
                }
                break;

            case PYFLINK:
                Resource resource = resourceService.findByResourceName(application.getTeamId(), application.getJar());

                ApiAlertException.throwIfNull(
                    resource, "pyflink file can't be null, start application failed.");

                ApiAlertException.throwIfNull(
                    resource.getFilePath(), "pyflink file can't be null, start application failed.");

                ApiAlertException.throwIfFalse(
                    resource.getFilePath().endsWith(Constant.PYTHON_SUFFIX),
                    "pyflink format error, must be a \".py\" suffix, start application failed.");

                flinkUserJar = resource.getFilePath();
                break;

            case CUSTOM_CODE:
                if (application.isUploadJob()) {
                    appConf = String.format(
                        "json://{\"%s\":\"%s\"}",
                        ConfigKeys.KEY_FLINK_APPLICATION_MAIN_CLASS(), application.getMainClass());
                } else {
                    switch (application.getApplicationType()) {
                        case STREAMPARK_SPARK:
                            ConfigFileTypeEnum fileType = ConfigFileTypeEnum.of(applicationConfig.getFormat());
                            if (fileType != null && ConfigFileTypeEnum.UNKNOWN != fileType) {
                                appConf = String.format(
                                    "%s://%s", fileType.getTypeName(), applicationConfig.getContent());
                            } else {
                                throw new IllegalArgumentException(
                                    "application' config type error,must be ( yaml| properties| hocon )");
                            }
                            break;
                        case APACHE_SPARK:
                            appConf = String.format(
                                "json://{\"%s\":\"%s\"}",
                                ConfigKeys.KEY_FLINK_APPLICATION_MAIN_CLASS(), application.getMainClass());
                            break;
                        default:
                            throw new IllegalArgumentException(
                                "[StreamPark] ApplicationType must be (StreamPark flink | Apache flink)... ");
                    }
                }

                if (SparkExecutionMode.YARN_CLUSTER == executionModeEnum) {
                    switch (application.getApplicationType()) {
                        case STREAMPARK_SPARK:
                            flinkUserJar = String.format(
                                "%s/%s",
                                application.getAppLib(),
                                application.getModule().concat(Constant.JAR_SUFFIX));
                            break;
                        case APACHE_SPARK:
                            flinkUserJar = String.format("%s/%s", application.getAppHome(), application.getJar());
                            if (!FsOperator.hdfs().exists(flinkUserJar)) {
                                resource = resourceService.findByResourceName(
                                    application.getTeamId(), application.getJar());
                                if (resource != null && StringUtils.isNotBlank(resource.getFilePath())) {
                                    flinkUserJar = String.format(
                                        "%s/%s",
                                        application.getAppHome(),
                                        new File(resource.getFilePath()).getName());
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

    private Map<String, String> getProperties(SparkApplication application) {
        Map<String, String> properties = new HashMap<>(application.getOptionMap());
        if (SparkExecutionMode.isYarnMode(application.getSparkExecutionMode())) {
            String yarnQueue = (String) application.getHotParamsMap().get(ConfigKeys.KEY_YARN_APP_QUEUE());
            String yarnLabelExpr = (String) application.getHotParamsMap().get(ConfigKeys.KEY_YARN_APP_NODE_LABEL());
            Optional.ofNullable(yarnQueue)
                .ifPresent(yq -> properties.put(ConfigKeys.KEY_YARN_APP_QUEUE(), yq));
            Optional.ofNullable(yarnLabelExpr)
                .ifPresent(yLabel -> properties.put(ConfigKeys.KEY_YARN_APP_NODE_LABEL(), yLabel));
        }

        Map<String, String> dynamicProperties = PropertiesUtils
            .extractDynamicPropertiesAsJava(application.getDynamicProperties());
        properties.putAll(dynamicProperties);
        return properties;
    }

    private void doStopped(Long id) {
        SparkApplication application = getById(id);
        application.setOptionState(SparkOptionStateEnum.NONE.getValue());
        application.setState(SparkAppStateEnum.KILLED.getValue());
        application.setOptionTime(new Date());
        updateById(application);
        SparkAppHttpWatcher.unWatching(application.getId());
        // kill application
        if (SparkExecutionMode.isYarnMode(application.getSparkExecutionMode())) {
            try {
                List<ApplicationReport> applications = applicationInfoService
                    .getYarnAppReport(application.getJobName());
                if (!applications.isEmpty()) {
                    YarnClient yarnClient = HadoopUtils.yarnClient();
                    yarnClient.killApplication(applications.get(0).getApplicationId());
                }
            } catch (Exception ignored) {
            }
        }
    }

    /* check yarn cluster before job start job */
    private void checkYarnBeforeStart(SparkApplication application) {
        STATE yarnState = HadoopUtils.yarnClient().getServiceState();
        ApiAlertException.throwIfFalse(
            yarnState == STARTED,
            "[StreamPark] The yarn cluster service state is " + yarnState.name() + ", please check it");
        ApiAlertException.throwIfTrue(
            !applicationInfoService.getYarnAppReport(application.getJobName()).isEmpty(),
            "[StreamPark] The same task name is already running in the yarn queue");
    }
}
