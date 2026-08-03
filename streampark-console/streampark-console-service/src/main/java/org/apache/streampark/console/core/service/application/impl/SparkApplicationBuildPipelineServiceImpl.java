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

import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.SparkDeployMode;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.FileUtils;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.core.bean.DockerConfig;
import org.apache.streampark.console.core.entity.ApplicationBuildPipeline;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.entity.SparkApplicationConfig;
import org.apache.streampark.console.core.entity.SparkEnv;
import org.apache.streampark.console.core.entity.SparkSql;
import org.apache.streampark.console.core.enums.CandidateTypeEnum;
import org.apache.streampark.console.core.enums.EngineTypeEnum;
import org.apache.streampark.console.core.enums.OptionStateEnum;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.service.MessageService;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.SettingService;
import org.apache.streampark.console.core.service.SparkEnvService;
import org.apache.streampark.console.core.service.SparkSqlService;
import org.apache.streampark.console.core.service.application.ApplicationLogService;
import org.apache.streampark.console.core.service.application.SparkAplicationBuildPipelineService;
import org.apache.streampark.console.core.service.application.SparkApplicationConfigService;
import org.apache.streampark.console.core.service.application.SparkApplicationInfoService;
import org.apache.streampark.console.core.service.application.SparkApplicationManageService;
import org.apache.streampark.console.core.util.ApplicationBuildPipelineUtils;
import org.apache.streampark.console.core.util.ServiceHelper;
import org.apache.streampark.console.core.watcher.SparkAppHttpWatcher;
import org.apache.streampark.flink.packer.docker.DockerConf;
import org.apache.streampark.flink.packer.maven.DependencyInfo;
import org.apache.streampark.flink.packer.pipeline.BuildPipeline;
import org.apache.streampark.flink.packer.pipeline.BuildResult;
import org.apache.streampark.flink.packer.pipeline.PipeWatcher;
import org.apache.streampark.flink.packer.pipeline.PipelineSnapshot;
import org.apache.streampark.flink.packer.pipeline.SparkK8sApplicationBuildRequest;
import org.apache.streampark.flink.packer.pipeline.SparkYarnBuildRequest;
import org.apache.streampark.flink.packer.pipeline.impl.SparkK8sApplicationBuildPipeline;
import org.apache.streampark.flink.packer.pipeline.impl.SparkYarnBuildPipeline;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;

import java.util.concurrent.ExecutorService;

@Service
@Slf4j
@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
public class SparkApplicationBuildPipelineServiceImpl
    extends
        AbstractApplicationBuildPipelineService
    implements
        SparkAplicationBuildPipelineService {

    @Autowired
    private SparkEnvService sparkEnvService;

    @Autowired
    private SparkSqlService sparkSqlService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private SettingService settingService;

    @Autowired
    private ApplicationLogService applicationLogService;

    @Autowired
    private SparkApplicationManageService applicationManageService;

    @Autowired
    private SparkApplicationInfoService applicationInfoService;

    @Autowired
    private SparkAppHttpWatcher sparkAppHttpWatcher;

    @Autowired
    private SparkApplicationConfigService applicationConfigService;

    @Autowired
    private ResourceService resourceService;

    @Qualifier("streamparkBuildPipelineExecutor")
    @Autowired
    private ExecutorService executorService;

    /**
     * Build application. This is an async call method.
     *
     * @param appId application id
     * @param forceBuild forced start pipeline or not
     * @return Whether the pipeline was successfully started
     */
    @Override
    public boolean buildApplication(@Nonnull Long appId, boolean forceBuild) {
        // check the build environment
        checkBuildEnv(appId, forceBuild);

        SparkApplication app = applicationManageService.getById(appId);
        ApplicationLog applicationLog =
            ApplicationBuildPipelineUtils.createReleaseLog(app.getId(), EngineTypeEnum.SPARK.getCode());

        // check if you need to go through the build process (if the jar and pom have changed,
        // you need to go through the build process, if other common parameters are modified,
        // you don't need to go through the build process)
        boolean needBuild = applicationManageService.checkBuildAndUpdate(app);
        if (!needBuild) {
            applicationLog.setSuccess(true);
            applicationLogService.save(applicationLog);
            return true;
        }

        // 1) spark sql setDependency
        SparkSql newSparkSql = sparkSqlService.getCandidate(app.getId(), CandidateTypeEnum.NEW);
        SparkSql effectiveSparkSql = sparkSqlService.getEffective(app.getId(), false);
        if (app.isSparkSqlJob()) {
            SparkSql sparkSql = newSparkSql == null ? effectiveSparkSql : newSparkSql;
            AssertUtils.notNull(sparkSql);
            app.setDependency(sparkSql.getDependency());
            app.setTeamResource(sparkSql.getTeamResource());
        }

        // create pipeline instance
        BuildPipeline pipeline = createPipelineInstance(app);

        // clear history
        removeByAppId(app.getId());
        // register pipeline progress event watcher.
        // save snapshot of pipeline to db when status of pipeline was changed.
        pipeline.registerWatcher(
            new PipeWatcher() {

                @Override
                public void onStart(PipelineSnapshot snapshot) {
                    saveEntity(ApplicationBuildPipelineUtils.fromSnapshot(snapshot, app.getId()));

                    app.setRelease(ReleaseStateEnum.RELEASING.get());
                    applicationManageService.updateRelease(app);

                    if (sparkAppHttpWatcher.isWatchingApp(app.getId())) {
                        sparkAppHttpWatcher.init();
                    }

                    applicationInfoService.checkEnv(app);

                    String appUploads = app.getWorkspace().APP_UPLOADS();
                    ApplicationBuildPipelineUtils.prepareBuildResources(
                        app.isSparkJarOrPySparkJob(),
                        () -> ApplicationBuildPipelineUtils.prepareJarJobHome(
                            app.getTeamId(),
                            app.getJar(),
                            app.getAppHome(),
                            app.getAppLib(),
                            app.getDistHome(),
                            app.getFsOperator(),
                            appUploads,
                            app.isFromUploadJob(),
                            app.getApplicationType(),
                            resourceService,
                            false),
                        app.getDependencyObject(),
                        resourceService);
                }

                @Override
                public void onStepStateChange(PipelineSnapshot snapshot) {
                    saveEntity(ApplicationBuildPipelineUtils.fromSnapshot(snapshot, app.getId()));
                }

                @Override
                public void onFinish(PipelineSnapshot snapshot, BuildResult result) {
                    saveEntity(ApplicationBuildPipelineUtils.finishedSnapshot(snapshot, result, app.getId()));
                    if (result.pass()) {
                        ApplicationBuildPipelineUtils.applySuccessfulRelease(
                            app,
                            () -> {
                                if (app.isSparkOnYarnJob()) {
                                    applicationManageService.toEffective(app);
                                } else if (app.isStreamParkJob()) {
                                    SparkApplicationConfig config =
                                        applicationConfigService.getLatest(app.getId());
                                    if (config != null) {
                                        config.setToApplication(app);
                                        applicationConfigService.toEffective(
                                            app.getId(), app.getConfigId());
                                    }
                                }
                            });
                        applicationLog.setSuccess(true);
                    } else {
                        ApplicationBuildPipelineUtils.recordReleaseFailure(
                            app.getId(), app.getAppName(), snapshot, applicationLog, messageService);
                        app.setRelease(ReleaseStateEnum.FAILED.get());
                        app.setOptionState(OptionStateEnum.NONE.getValue());
                        app.setBuild(true);
                    }
                    ApplicationBuildPipelineUtils.finalizeRelease(
                        () -> applicationManageService.updateRelease(app),
                        applicationLog,
                        applicationLogService,
                        () -> {
                            if (sparkAppHttpWatcher.isWatchingApp(app.getId())) {
                                sparkAppHttpWatcher.init();
                            }
                        });
                }
            });
        // save pipeline instance snapshot to db before release it.
        ApplicationBuildPipeline buildPipeline =
            ApplicationBuildPipeline.initFromPipeline(pipeline).setAppId(app.getId());
        boolean saved = saveEntity(buildPipeline);
        // async release pipeline
        executorService.submit((Runnable) pipeline::launch);
        return saved;
    }

    /**
     * check the build environment
     *
     * @param appId application id
     * @param forceBuild forced start pipeline or not
     */
    private void checkBuildEnv(Long appId, boolean forceBuild) {
        SparkApplication app = applicationManageService.getById(appId);

        // 1) check spark version
        SparkEnv env = sparkEnvService.getById(app.getVersionId());
        boolean checkVersion = env.getSparkVersion().checkVersion(false);
        ApiAlertException.throwIfFalse(
            checkVersion, "Unsupported spark version:" + env.getSparkVersion().version());

        // 2) check env
        boolean envOk = applicationInfoService.checkEnv(app);
        ApiAlertException.throwIfFalse(
            envOk, "Check spark env failed, please check the spark version of this job");

        // 3) Whether the application can currently start a new building progress
        ApiAlertException.throwIfTrue(
            !forceBuild && !allowToBuildNow(appId),
            "The job is invalid, or the job cannot be built while it is running");
    }

    /** create building pipeline instance */
    private BuildPipeline createPipelineInstance(@Nonnull SparkApplication app) {
        SparkEnv sparkEnv = sparkEnvService.getByIdOrDefault(app.getVersionId());
        String sparkUserJar = retrieveSparkUserJar(sparkEnv, app);

        if (!FileUtils.exists(sparkUserJar)) {
            Resource resource = resourceService.findByResourceName(app.getTeamId(), app.getJar());
            if (resource != null && StringUtils.isNotBlank(resource.getFilePath())) {
                sparkUserJar = resource.getFilePath();
            }
        }

        SparkDeployMode deployModeEnum = app.getDeployModeEnum();
        String mainClass = Constants.STREAMPARK_SPARKSQL_CLIENT_CLASS;
        switch (deployModeEnum) {
            case YARN_CLIENT:
            case YARN_CLUSTER:
                String yarnProvidedPath = app.getAppLib();
                String localWorkspace = app.getLocalAppHome().concat("/lib");
                if (ApplicationType.APACHE_SPARK == app.getApplicationType()) {
                    yarnProvidedPath = app.getAppHome();
                    localWorkspace = app.getLocalAppHome();
                }
                SparkYarnBuildRequest yarnAppRequest = new SparkYarnBuildRequest(
                    app.getAppName(),
                    mainClass,
                    localWorkspace,
                    yarnProvidedPath,
                    app.getJobTypeEnum(),
                    deployModeEnum,
                    getMergedDependencyInfo(app));
                log.info("Submit params to building pipeline : {}", yarnAppRequest);
                return SparkYarnBuildPipeline.of(yarnAppRequest);
            case KUBERNETES_NATIVE_CLUSTER:
            case KUBERNETES_NATIVE_CLIENT:
                DockerConfig dockerConfig = settingService.getDockerConfig();
                SparkK8sApplicationBuildRequest k8sApplicationBuildRequest = buildSparkK8sApplicationBuildRequest(
                    app, mainClass, sparkUserJar, sparkEnv, dockerConfig);
                log.info("Submit params to building pipeline : {}", k8sApplicationBuildRequest);
                return SparkK8sApplicationBuildPipeline.of(k8sApplicationBuildRequest);
            default:
                throw new UnsupportedOperationException(
                    "Unsupported Building Application for DeployMode: " + app.getDeployModeEnum());
        }
    }

    @Nonnull
    private SparkK8sApplicationBuildRequest buildSparkK8sApplicationBuildRequest(
                                                                                 @Nonnull SparkApplication app,
                                                                                 String mainClass,
                                                                                 String mainJar,
                                                                                 SparkEnv sparkEnv,
                                                                                 DockerConfig dockerConfig) {
        SparkK8sApplicationBuildRequest k8sApplicationBuildRequest = new SparkK8sApplicationBuildRequest(
            app.getAppName(),
            app.getAppHome(),
            mainClass,
            mainJar,
            app.getDeployModeEnum(),
            app.getJobTypeEnum(),
            sparkEnv.getSparkVersion(),
            getMergedDependencyInfo(app),
            app.getK8sNamespace(),
            app.getK8sContainerImage(),
            app.getK8sPodTemplates(),
            app.getK8sHadoopIntegration() != null ? app.getK8sHadoopIntegration() : false,
            DockerConf.of(
                dockerConfig.getAddress(),
                dockerConfig.getNamespace(),
                dockerConfig.getUsername(),
                dockerConfig.getPassword()));
        return k8sApplicationBuildRequest;
    }

    private String retrieveSparkUserJar(SparkEnv sparkEnv, SparkApplication app) {
        switch (app.getJobTypeEnum()) {
            case SPARK_JAR:
                switch (app.getApplicationType()) {
                    case STREAMPARK_SPARK:
                        return String.format(
                            "%s/%s", app.getAppLib(), app.getModule().concat(Constants.JAR_SUFFIX));
                    case APACHE_SPARK:
                        return String.format("%s/%s", app.getAppHome(), app.getJar());
                    default:
                        throw new IllegalArgumentException(
                            "[StreamPark] unsupported ApplicationType of FlinkJar: "
                                + app.getApplicationType());
                }
            case PYSPARK:
                return String.format("%s/%s", app.getAppHome(), app.getJar());
            case SPARK_SQL:
                String sqlDistJar = ServiceHelper.getSparkSqlClientJar(sparkEnv);
                if (app.getDeployModeEnum() == SparkDeployMode.YARN_CLUSTER) {
                    String clientPath = Workspace.remote().APP_CLIENT();
                    return String.format("%s/%s", clientPath, sqlDistJar);
                }
                return Workspace.local().APP_CLIENT().concat("/").concat(sqlDistJar);
            default:
                throw new UnsupportedOperationException(
                    "[StreamPark] unsupported JobType: " + app.getJobTypeEnum());
        }
    }

    private DependencyInfo getMergedDependencyInfo(SparkApplication application) {
        return ApplicationBuildPipelineUtils.getMergedDependencyInfo(
            application.getDependencyInfo(),
            application.getTeamResource(),
            application.getTeamId(),
            resourceService,
            log);
    }
}
