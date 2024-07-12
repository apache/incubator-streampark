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
import org.apache.streampark.common.enums.ClusterState;
import org.apache.streampark.common.enums.FlinkDevelopmentMode;
import org.apache.streampark.common.enums.ResolveOrder;
import org.apache.streampark.common.enums.SparkExecutionMode;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.common.util.HadoopUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.ApplicationException;
import org.apache.streampark.console.core.entity.AppBuildPipeline;
import org.apache.streampark.console.core.entity.ApplicationConfig;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.entity.SavePoint;
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.entity.SparkEnv;
import org.apache.streampark.console.core.enums.CheckPointTypeEnum;
import org.apache.streampark.console.core.enums.ConfigFileTypeEnum;
import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.enums.OperationEnum;
import org.apache.streampark.console.core.enums.OptionStateEnum;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.mapper.SparkApplicationMapper;
import org.apache.streampark.console.core.service.AppBuildPipeService;
import org.apache.streampark.console.core.service.ApplicationConfigService;
import org.apache.streampark.console.core.service.ApplicationLogService;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.ServiceHelper;
import org.apache.streampark.console.core.service.SparkEnvService;
import org.apache.streampark.console.core.service.VariableService;
import org.apache.streampark.console.core.service.application.SparkApplicationActionService;
import org.apache.streampark.console.core.service.application.SparkApplicationInfoService;
import org.apache.streampark.console.core.watcher.FlinkAppHttpWatcher;
import org.apache.streampark.console.core.watcher.FlinkClusterWatcher;
import org.apache.streampark.flink.packer.pipeline.BuildResult;
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse;
import org.apache.streampark.spark.client.SparkClient;
import org.apache.streampark.spark.client.bean.CancelRequest;
import org.apache.streampark.spark.client.bean.CancelResponse;
import org.apache.streampark.spark.client.bean.SubmitRequest;
import org.apache.streampark.spark.client.bean.SubmitResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions;
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
import java.net.URI;
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
    private ApplicationLogService applicationLogService;

    @Autowired
    private SparkEnvService sparkEnvService;

    @Autowired
    private FlinkSqlService flinkSqlService;

    @Autowired
    private ServiceHelper serviceHelper;

    @Autowired
    private AppBuildPipeService appBuildPipeService;

    @Autowired
    private FlinkClusterService flinkClusterService;

    @Autowired
    private VariableService variableService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private FlinkClusterWatcher flinkClusterWatcher;

    private final Map<Long, CompletableFuture<SubmitResponse>> startFutureMap = new ConcurrentHashMap<>();

    private final Map<Long, CompletableFuture<CancelResponse>> cancelFutureMap = new ConcurrentHashMap<>();

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
            updateWrapper.set(SparkApplication::getState, FlinkAppStateEnum.REVOKED.getValue());
        }
        baseMapper.update(null, updateWrapper);
    }

    @Override
    public void restart(SparkApplication appParam) throws Exception {
        this.cancel(appParam);
        this.start(appParam, false);
    }

    @Override
    public void forcedStop(Long id) {
        CompletableFuture<SubmitResponse> startFuture = startFutureMap.remove(id);
        CompletableFuture<CancelResponse> cancelFuture = cancelFutureMap.remove(id);
        SparkApplication application = this.baseMapper.selectApp(id);
        if (startFuture != null) {
            startFuture.cancel(true);
        }
        if (cancelFuture != null) {
            cancelFuture.cancel(true);
        }
        if (startFuture == null && cancelFuture == null) {
            this.doStopped(id);
        }
    }

    @Override
    public void cancel(SparkApplication appParam) throws Exception {
        FlinkAppHttpWatcher.setOptionState(appParam.getId(), OptionStateEnum.CANCELLING);
        SparkApplication application = getById(appParam.getId());
        application.setState(FlinkAppStateEnum.CANCELLING.getValue());

        ApplicationLog applicationLog = new ApplicationLog();
        applicationLog.setOptionName(OperationEnum.CANCEL.getValue());
        applicationLog.setAppId(application.getId());
        applicationLog.setJobManagerUrl(application.getJobManagerUrl());
        applicationLog.setOptionTime(new Date());
        applicationLog.setYarnAppId(application.getClusterId());
        applicationLog.setUserId(serviceHelper.getUserId());

        if (appParam.getSavePointed()) {
            FlinkAppHttpWatcher.addSavepoint(application.getId());
            application.setOptionState(OptionStateEnum.SAVEPOINTING.getValue());
        } else {
            application.setOptionState(OptionStateEnum.CANCELLING.getValue());
        }

        application.setOptionTime(new Date());
        this.baseMapper.updateById(application);

        Long userId = serviceHelper.getUserId();
        if (!application.getUserId().equals(userId)) {
            FlinkAppHttpWatcher.addCanceledApp(application.getId(), userId);
        }

        SparkEnv sparkEnv = sparkEnvService.getById(application.getVersionId());

        String clusterId = null;
        if (SparkExecutionMode.isYarnMode(application.getExecutionMode())) {
            clusterId = application.getAppId();
        }

        Map<String, Object> properties = new HashMap<>();

        if (SparkExecutionMode.isRemoteMode(application.getSparkExecutionMode())) {
            FlinkCluster cluster = flinkClusterService.getById(application.getSparkClusterId());
            ApiAlertException.throwIfNull(
                cluster,
                String.format(
                    "The clusterId=%s cannot be find, maybe the clusterId is wrong or "
                        + "the cluster has been deleted. Please contact the Admin.",
                    application.getSparkClusterId()));
            URI activeAddress = cluster.getRemoteURI();
            properties.put(RestOptions.ADDRESS.key(), activeAddress.getHost());
            properties.put(RestOptions.PORT.key(), activeAddress.getPort());
        }

        CancelRequest cancelRequest = new CancelRequest(
            application.getId(),
            sparkEnv.getSparkVersion(),
            SparkExecutionMode.of(application.getExecutionMode()),
            properties,
            clusterId,
            application.getJobId(),
            appParam.getDrain(),
            appParam.getNativeFormat());

        final Date triggerTime = new Date();
        CompletableFuture<CancelResponse> cancelFuture = CompletableFuture
            .supplyAsync(() -> SparkClient.cancel(cancelRequest), executorService);

        cancelFutureMap.put(application.getId(), cancelFuture);

        cancelFuture.whenComplete(
            (cancelResponse, throwable) -> {
                cancelFutureMap.remove(application.getId());

                if (throwable != null) {
                    String exception = ExceptionUtils.stringifyException(throwable);
                    applicationLog.setException(exception);
                    applicationLog.setSuccess(false);
                    applicationLogService.save(applicationLog);

                    if (throwable instanceof CancellationException) {
                        doStopped(application.getId());
                    } else {
                        log.error("stop flink job failed.", throwable);
                        application.setOptionState(OptionStateEnum.NONE.getValue());
                        application.setState(FlinkAppStateEnum.FAILED.getValue());
                        updateById(application);

                        FlinkAppHttpWatcher.unWatching(application.getId());
                    }
                    return;
                }

                applicationLog.setSuccess(true);
                // save log...
                applicationLogService.save(applicationLog);

                if (cancelResponse != null && cancelResponse.savePointDir() != null) {
                    String savePointDir = cancelResponse.savePointDir();
                    log.info("savePoint path: {}", savePointDir);
                    SavePoint savePoint = new SavePoint();
                    savePoint.setPath(savePointDir);
                    savePoint.setAppId(application.getId());
                    savePoint.setLatest(true);
                    savePoint.setType(CheckPointTypeEnum.SAVEPOINT.get());
                    savePoint.setCreateTime(new Date());
                    savePoint.setTriggerTime(triggerTime);
                }
            });
    }

    @Override
    public void start(SparkApplication appParam, boolean auto) throws Exception {
        // 1) check application
        final SparkApplication application = getById(appParam.getId());
        AssertUtils.notNull(application);
        ApiAlertException.throwIfTrue(
            !application.isCanBeStart(), "[StreamPark] The application cannot be started repeatedly.");

        if (SparkExecutionMode.isRemoteMode(application.getSparkExecutionMode())) {
            checkBeforeStart(application);
        }

        if (SparkExecutionMode.isYarnMode(application.getSparkExecutionMode())) {

            ApiAlertException.throwIfTrue(
                !applicationInfoService.getYarnAppReport(application.getJobName()).isEmpty(),
                "[StreamPark] The same task name is already running in the yarn queue");
        }

        AppBuildPipeline buildPipeline = appBuildPipeService.getById(application.getId());
        AssertUtils.notNull(buildPipeline);

        SparkEnv sparkEnv = sparkEnvService.getByIdOrDefault(application.getVersionId());

        ApiAlertException.throwIfNull(sparkEnv, "[StreamPark] can no found flink version");

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
        applicationLog.setOptionName(OperationEnum.START.getValue());
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
        }

        // Get the args after placeholder replacement
        String applicationArgs = variableService.replaceVariable(application.getTeamId(), application.getArgs());

        SubmitRequest submitRequest = new SubmitRequest(
            sparkEnv.getSparkVersion(),
            SparkExecutionMode.of(application.getExecutionMode()),
            getProperties(application),
            sparkEnv.getSparkConf(),
            FlinkDevelopmentMode.of(application.getJobType()),
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
                        app.setState(FlinkAppStateEnum.FAILED.getValue());
                        app.setOptionState(OptionStateEnum.NONE.getValue());
                        updateById(app);
                        FlinkAppHttpWatcher.unWatching(appParam.getId());
                    }
                    return;
                }

                // 3) success
                applicationLog.setSuccess(true);
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
                if (StringUtils.isNoneEmpty(response.jobId())) {
                    application.setJobId(response.jobId());
                }

                if (StringUtils.isNoneEmpty(response.jobManagerUrl())) {
                    application.setJobManagerUrl(response.jobManagerUrl());
                    applicationLog.setJobManagerUrl(response.jobManagerUrl());
                }
                applicationLog.setYarnAppId(response.clusterId());
                application.setStartTime(new Date());
                application.setEndTime(null);

                // if start completed, will be added task to tracking queue
                FlinkAppHttpWatcher.setOptionState(appParam.getId(), OptionStateEnum.STARTING);
                // FlinkAppHttpWatcher.doWatching(application);

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
        application.setState(FlinkAppStateEnum.STARTING.getValue());
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
                // todo
                String sqlDistJar = serviceHelper.getFlinkSqlClientJar(null);
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

    private Map<String, Object> getProperties(SparkApplication application) {
        Map<String, Object> properties = new HashMap<>(application.getOptionMap());
        if (SparkExecutionMode.isRemoteMode(application.getSparkExecutionMode())) {
            FlinkCluster cluster = flinkClusterService.getById(application.getSparkClusterId());
            ApiAlertException.throwIfNull(
                cluster,
                String.format(
                    "The clusterId=%s can't be find, maybe the clusterId is wrong or "
                        + "the cluster has been deleted. Please contact the Admin.",
                    application.getSparkClusterId()));
            URI activeAddress = cluster.getRemoteURI();
            properties.put(RestOptions.ADDRESS.key(), activeAddress.getHost());
            properties.put(RestOptions.PORT.key(), activeAddress.getPort());
        } else if (SparkExecutionMode.isYarnMode(application.getSparkExecutionMode())) {
            String yarnQueue = (String) application.getHotParamsMap().get(ConfigKeys.KEY_YARN_APP_QUEUE());
            String yarnLabelExpr = (String) application.getHotParamsMap().get(ConfigKeys.KEY_YARN_APP_NODE_LABEL());
            Optional.ofNullable(yarnQueue)
                .ifPresent(yq -> properties.put(ConfigKeys.KEY_YARN_APP_QUEUE(), yq));
            Optional.ofNullable(yarnLabelExpr)
                .ifPresent(yLabel -> properties.put(ConfigKeys.KEY_YARN_APP_NODE_LABEL(), yLabel));
        }

        if (application.getAllowNonRestored()) {
            properties.put(SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE.key(), true);
        }

        Map<String, String> dynamicProperties = PropertiesUtils
            .extractDynamicPropertiesAsJava(application.getDynamicProperties());
        properties.putAll(dynamicProperties);
        ResolveOrder resolveOrder = ResolveOrder.of(application.getResolveOrder());
        if (resolveOrder != null) {
            properties.put(CoreOptions.CLASSLOADER_RESOLVE_ORDER.key(), resolveOrder.getName());
        }

        return properties;
    }

    private void doStopped(Long id) {
        SparkApplication application = getById(id);
        application.setOptionState(OptionStateEnum.NONE.getValue());
        application.setState(FlinkAppStateEnum.CANCELED.getValue());
        application.setOptionTime(new Date());
        updateById(application);
        // re-tracking flink job on kubernetes and logging exception
        FlinkAppHttpWatcher.unWatching(application.getId());
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

    /* check flink cluster before job start job */
    private void checkBeforeStart(SparkApplication application) {
        SparkEnv sparkEnv = sparkEnvService.getByAppId(application.getId());
        ApiAlertException.throwIfNull(sparkEnv, "[StreamPark] can no found flink version");

        ApiAlertException.throwIfFalse(
            flinkClusterService.existsByFlinkEnvId(sparkEnv.getId()),
            "[StreamPark] The flink cluster don't exist, please check it");

        FlinkCluster flinkCluster = flinkClusterService.getById(application.getSparkClusterId());
        ApiAlertException.throwIfFalse(
            flinkClusterWatcher.getClusterState(flinkCluster) == ClusterState.RUNNING,
            "[StreamPark] The flink cluster not running, please start it");
    }
}
