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

package org.apache.streampark.console.core.service.impl;

import org.apache.streampark.common.Constant;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.FlinkDevelopmentMode;
import org.apache.streampark.common.enums.SparkExecutionMode;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.common.util.FileUtils;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.base.util.WebUtils;
import org.apache.streampark.console.core.bean.Dependency;
import org.apache.streampark.console.core.entity.AppBuildPipeline;
import org.apache.streampark.console.core.entity.ApplicationConfig;
import org.apache.streampark.console.core.entity.ApplicationLog;
import org.apache.streampark.console.core.entity.FlinkSql;
import org.apache.streampark.console.core.entity.Message;
import org.apache.streampark.console.core.entity.Resource;
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.entity.SparkEnv;
import org.apache.streampark.console.core.enums.CandidateTypeEnum;
import org.apache.streampark.console.core.enums.NoticeTypeEnum;
import org.apache.streampark.console.core.enums.OptionStateEnum;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.enums.ResourceTypeEnum;
import org.apache.streampark.console.core.mapper.ApplicationBuildPipelineMapper;
import org.apache.streampark.console.core.service.ApplicationBackUpService;
import org.apache.streampark.console.core.service.ApplicationConfigService;
import org.apache.streampark.console.core.service.ApplicationLogService;
import org.apache.streampark.console.core.service.FlinkSqlService;
import org.apache.streampark.console.core.service.MessageService;
import org.apache.streampark.console.core.service.ResourceService;
import org.apache.streampark.console.core.service.ServiceHelper;
import org.apache.streampark.console.core.service.SettingService;
import org.apache.streampark.console.core.service.SparkAppBuildPipeService;
import org.apache.streampark.console.core.service.SparkEnvService;
import org.apache.streampark.console.core.service.application.SparkApplicationInfoService;
import org.apache.streampark.console.core.service.application.SparkApplicationManageService;
import org.apache.streampark.console.core.watcher.FlinkAppHttpWatcher;
import org.apache.streampark.flink.packer.maven.Artifact;
import org.apache.streampark.flink.packer.maven.DependencyInfo;
import org.apache.streampark.flink.packer.pipeline.BuildPipeline;
import org.apache.streampark.flink.packer.pipeline.BuildResult;
import org.apache.streampark.flink.packer.pipeline.PipeWatcher;
import org.apache.streampark.flink.packer.pipeline.PipelineSnapshot;
import org.apache.streampark.flink.packer.pipeline.PipelineStatusEnum;
import org.apache.streampark.flink.packer.pipeline.SparkYarnApplicationBuildRequest;
import org.apache.streampark.flink.packer.pipeline.impl.SparkYarnApplicationBuildPipeline;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static org.apache.streampark.console.core.enums.OperationEnum.RELEASE;

@Service
@Slf4j
@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
public class SparkAppBuildPipeServiceImpl
    extends
        ServiceImpl<ApplicationBuildPipelineMapper, AppBuildPipeline>
    implements
        SparkAppBuildPipeService {

    @Autowired
    private SparkEnvService sparkEnvService;

    @Autowired
    private FlinkSqlService flinkSqlService;

    @Autowired
    private ApplicationBackUpService backUpService;

    @Autowired
    private ServiceHelper serviceHelper;

    @Autowired
    private SettingService settingService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private SparkApplicationManageService applicationManageService;

    @Autowired
    private SparkApplicationInfoService applicationInfoService;

    @Autowired
    private ApplicationLogService applicationLogService;

    @Autowired
    private FlinkAppHttpWatcher flinkAppHttpWatcher;

    @Autowired
    private ApplicationConfigService applicationConfigService;

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
        ApplicationLog applicationLog = new ApplicationLog();
        applicationLog.setOptionName(RELEASE.getValue());
        applicationLog.setAppId(app.getId());
        applicationLog.setOptionTime(new Date());
        applicationLog.setUserId(serviceHelper.getUserId());

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
        FlinkSql newFlinkSql = flinkSqlService.getCandidate(app.getId(), CandidateTypeEnum.NEW);
        FlinkSql effectiveFlinkSql = flinkSqlService.getEffective(app.getId(), false);
        if (app.isSparkSqlJob()) {
            FlinkSql flinkSql = newFlinkSql == null ? effectiveFlinkSql : newFlinkSql;
            AssertUtils.notNull(flinkSql);
            app.setDependency(flinkSql.getDependency());
            app.setTeamResource(flinkSql.getTeamResource());
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
                    AppBuildPipeline buildPipeline = AppBuildPipeline.fromPipeSnapshot(snapshot)
                        .setAppId(app.getId());
                    saveEntity(buildPipeline);

                    app.setRelease(ReleaseStateEnum.RELEASING.get());
                    applicationManageService.updateRelease(app);

                    if (flinkAppHttpWatcher.isWatchingApp(app.getId())) {
                        flinkAppHttpWatcher.init();
                    }

                    // 1) checkEnv
                    applicationInfoService.checkEnv(app);

                    // 2) some preparatory work
                    String appUploads = app.getWorkspace().APP_UPLOADS();

                    if (app.isCustomCodeOrPySparkJob()) {
                        // customCode upload jar to appHome...
                        String appHome = app.getAppHome();
                        FsOperator fsOperator = app.getFsOperator();
                        fsOperator.delete(appHome);
                        if (app.isUploadJob()) {
                            String uploadJar = appUploads.concat("/").concat(app.getJar());
                            File localJar = new File(
                                String.format(
                                    "%s/%d/%s",
                                    Workspace.local().APP_UPLOADS(),
                                    app.getTeamId(),
                                    app.getJar()));
                            if (!localJar.exists()) {
                                Resource resource = resourceService.findByResourceName(app.getTeamId(),
                                    app.getJar());
                                if (resource != null && StringUtils.isNotBlank(resource.getFilePath())) {
                                    localJar = new File(resource.getFilePath());
                                    uploadJar = appUploads.concat("/").concat(localJar.getName());
                                }
                            }
                            // upload jar copy to appHome
                            checkOrElseUploadJar(app.getFsOperator(), localJar, uploadJar, appUploads);

                            switch (app.getApplicationType()) {
                                case STREAMPARK_SPARK:
                                    fsOperator.mkdirs(app.getAppLib());
                                    fsOperator.copy(uploadJar, app.getAppLib(), false, true);
                                    break;
                                case APACHE_SPARK:
                                    fsOperator.mkdirs(appHome);
                                    fsOperator.copy(uploadJar, appHome, false, true);
                                    break;
                                default:
                                    throw new IllegalArgumentException(
                                        "[StreamPark] unsupported ApplicationType of custom code: "
                                            + app.getApplicationType());
                            }
                        } else {
                            fsOperator.upload(app.getDistHome(), appHome);
                        }
                    } else {
                        if (!app.getDependencyObject().getJar().isEmpty()) {
                            String localUploads = Workspace.local().APP_UPLOADS();
                            // copy jar to local upload dir
                            for (String jar : app.getDependencyObject().getJar()) {
                                File localJar = new File(WebUtils.getAppTempDir(), jar);
                                File uploadJar = new File(localUploads, jar);
                                if (!localJar.exists() && !uploadJar.exists()) {
                                    throw new ApiAlertException(
                                        "Missing file: " + jar + ", please upload again");
                                }
                                if (localJar.exists()) {
                                    checkOrElseUploadJar(
                                        FsOperator.lfs(), localJar, uploadJar.getAbsolutePath(),
                                        localUploads);
                                }
                            }
                        }
                    }
                }

                @Override
                public void onStepStateChange(PipelineSnapshot snapshot) {
                    AppBuildPipeline buildPipeline = AppBuildPipeline.fromPipeSnapshot(snapshot)
                        .setAppId(app.getId());
                    saveEntity(buildPipeline);
                }

                @Override
                public void onFinish(PipelineSnapshot snapshot, BuildResult result) {
                    AppBuildPipeline buildPipeline = AppBuildPipeline.fromPipeSnapshot(snapshot)
                        .setAppId(app.getId())
                        .setBuildResult(result);
                    saveEntity(buildPipeline);
                    if (result.pass()) {
                        // running job ...
                        if (app.isRunning()) {
                            app.setRelease(ReleaseStateEnum.NEED_RESTART.get());
                        } else {
                            app.setOptionState(OptionStateEnum.NONE.getValue());
                            app.setRelease(ReleaseStateEnum.DONE.get());
                            // If the current task is not running, or the task has just been added, directly
                            // set
                            // the candidate version to the official version
                            if (app.isSparkSqlJob()) {
                                applicationManageService.toEffective(app);
                            } else {
                                if (app.isStreamParkJob()) {
                                    ApplicationConfig config =
                                        applicationConfigService.getLatest(app.getId());
                                    if (config != null) {
                                        config.setToApplication(app);
                                        applicationConfigService.toEffective(app.getId(),
                                            app.getConfigId());
                                    }
                                }
                            }
                        }
                        applicationLog.setSuccess(true);
                        app.setBuild(false);

                    } else {
                        Message message = new Message(
                            serviceHelper.getUserId(),
                            app.getId(),
                            app.getJobName().concat(" release failed"),
                            ExceptionUtils.stringifyException(snapshot.error().exception()),
                            NoticeTypeEnum.EXCEPTION);
                        messageService.push(message);
                        app.setRelease(ReleaseStateEnum.FAILED.get());
                        app.setOptionState(OptionStateEnum.NONE.getValue());
                        app.setBuild(true);
                        applicationLog.setException(
                            ExceptionUtils.stringifyException(snapshot.error().exception()));
                        applicationLog.setSuccess(false);
                    }
                    applicationManageService.updateRelease(app);
                    applicationLogService.save(applicationLog);
                    if (flinkAppHttpWatcher.isWatchingApp(app.getId())) {
                        flinkAppHttpWatcher.init();
                    }
                }
            });
        // save pipeline instance snapshot to db before release it.
        AppBuildPipeline buildPipeline = AppBuildPipeline.initFromPipeline(pipeline).setAppId(app.getId());
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

        // 1) check flink version
        SparkEnv env = sparkEnvService.getById(app.getVersionId());
        boolean checkVersion = env.getSparkVersion().checkVersion(false);
        ApiAlertException.throwIfFalse(
            checkVersion, "Unsupported flink version:" + env.getSparkVersion().version());

        // 2) check env
        boolean envOk = applicationInfoService.checkEnv(app);
        ApiAlertException.throwIfFalse(
            envOk, "Check flink env failed, please check the flink version of this job");

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

        SparkExecutionMode executionModeEnum = app.getSparkExecutionMode();
        String mainClass = Constant.STREAMPARK_SPARKSQL_CLIENT_CLASS;
        switch (executionModeEnum) {
            case YARN_CLUSTER:
            case YARN_CLIENT:
                String yarnProvidedPath = app.getAppLib();
                String localWorkspace = app.getLocalAppHome().concat("/lib");
                if (FlinkDevelopmentMode.CUSTOM_CODE == app.getDevelopmentMode()
                    && ApplicationType.APACHE_FLINK == app.getApplicationType()) {
                    yarnProvidedPath = app.getAppHome();
                    localWorkspace = app.getLocalAppHome();
                }
                SparkYarnApplicationBuildRequest yarnAppRequest = new SparkYarnApplicationBuildRequest(
                    app.getJobName(),
                    mainClass,
                    localWorkspace,
                    yarnProvidedPath,
                    app.getDevelopmentMode(),
                    getMergedDependencyInfo(app));
                log.info("Submit params to building pipeline : {}", yarnAppRequest);
                return SparkYarnApplicationBuildPipeline.of(yarnAppRequest);
            default:
                throw new UnsupportedOperationException(
                    "Unsupported Building Application for ExecutionMode: " + app.getSparkExecutionMode());
        }
    }

    private String retrieveSparkUserJar(SparkEnv sparkEnv, SparkApplication app) {
        switch (app.getDevelopmentMode()) {
            case CUSTOM_CODE:
                switch (app.getApplicationType()) {
                    case STREAMPARK_SPARK:
                        return String.format(
                            "%s/%s", app.getAppLib(), app.getModule().concat(Constant.JAR_SUFFIX));
                    case APACHE_SPARK:
                        return String.format("%s/%s", app.getAppHome(), app.getJar());
                    default:
                        throw new IllegalArgumentException(
                            "[StreamPark] unsupported ApplicationType of custom code: "
                                + app.getApplicationType());
                }
            case PYFLINK:
                return String.format("%s/%s", app.getAppHome(), app.getJar());
            case FLINK_SQL:
                String sqlDistJar = serviceHelper.getSparkSqlClientJar(sparkEnv);
                if (app.getSparkExecutionMode() == SparkExecutionMode.YARN_CLUSTER) {
                    String clientPath = Workspace.remote().APP_CLIENT();
                    return String.format("%s/%s", clientPath, sqlDistJar);
                }
                return Workspace.local().APP_CLIENT().concat("/").concat(sqlDistJar);
            default:
                throw new UnsupportedOperationException(
                    "[StreamPark] unsupported JobType: " + app.getDevelopmentMode());
        }
    }

    @Override
    public Optional<AppBuildPipeline> getCurrentBuildPipeline(@Nonnull Long appId) {
        return Optional.ofNullable(getById(appId));
    }

    @Override
    public boolean allowToBuildNow(@Nonnull Long appId) {
        return getCurrentBuildPipeline(appId)
            .map(pipeline -> PipelineStatusEnum.running != pipeline.getPipelineStatus())
            .orElse(true);
    }

    @Override
    public Map<Long, PipelineStatusEnum> listAppIdPipelineStatusMap(List<Long> appIds) {
        if (CollectionUtils.isEmpty(appIds)) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<AppBuildPipeline> queryWrapper = new LambdaQueryWrapper<AppBuildPipeline>()
            .in(AppBuildPipeline::getAppId, appIds);

        List<AppBuildPipeline> appBuildPipelines = baseMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(appBuildPipelines)) {
            return new HashMap<>();
        }
        return appBuildPipelines.stream()
            .collect(Collectors.toMap(AppBuildPipeline::getAppId, AppBuildPipeline::getPipelineStatus));
    }

    @Override
    public void removeByAppId(Long appId) {
        baseMapper.delete(
            new LambdaQueryWrapper<AppBuildPipeline>().eq(AppBuildPipeline::getAppId, appId));
    }

    /**
     * save or update build pipeline
     *
     * @param pipe application build pipeline
     * @return value after the save or update
     */
    public boolean saveEntity(AppBuildPipeline pipe) {
        AppBuildPipeline old = getById(pipe.getAppId());
        if (old == null) {
            return save(pipe);
        }
        return updateById(pipe);
    }

    /**
     * Check if the jar exists, and upload a copy if it does not exist
     *
     * @param fsOperator
     * @param localJar
     * @param targetJar
     * @param targetDir
     */
    private void checkOrElseUploadJar(
                                      FsOperator fsOperator, File localJar, String targetJar, String targetDir) {
        if (!fsOperator.exists(targetJar)) {
            fsOperator.upload(localJar.getAbsolutePath(), targetDir, false, true);
        } else {
            // The file exists to check whether it is consistent, and if it is inconsistent, re-upload it
            if (!FileUtils.equals(localJar, new File(targetJar))) {
                fsOperator.upload(localJar.getAbsolutePath(), targetDir, false, true);
            }
        }
    }

    /**
     * Gets and parses dependencies on the application
     *
     * @param application
     * @return DependencyInfo
     */
    private DependencyInfo getMergedDependencyInfo(SparkApplication application) {
        DependencyInfo dependencyInfo = application.getDependencyInfo();
        if (StringUtils.isBlank(application.getTeamResource())) {
            return dependencyInfo;
        }

        try {
            String[] resourceIds = JacksonUtils.read(application.getTeamResource(), String[].class);

            List<Artifact> mvnArtifacts = new ArrayList<Artifact>();
            List<String> jarLibs = new ArrayList<String>();

            Arrays.stream(resourceIds)
                .forEach(
                    resourceId -> {
                        Resource resource = resourceService.getById(resourceId);

                        if (resource.getResourceType() != ResourceTypeEnum.GROUP) {
                            mergeDependency(application, mvnArtifacts, jarLibs, resource);
                        } else {
                            try {
                                String[] groupElements =
                                    JacksonUtils.read(resource.getResource(),
                                        String[].class);
                                Arrays.stream(groupElements)
                                    .forEach(
                                        resourceIdInGroup -> mergeDependency(
                                            application,
                                            mvnArtifacts,
                                            jarLibs,
                                            resourceService.getById(
                                                resourceIdInGroup)));
                            } catch (JsonProcessingException e) {
                                throw new ApiAlertException("Parse resource group failed.", e);
                            }
                        }
                    });
            return dependencyInfo.merge(mvnArtifacts, jarLibs);
        } catch (Exception e) {
            log.warn("Merge team dependency failed.", e);
            return dependencyInfo;
        }
    }

    private static void mergeDependency(
                                        SparkApplication application,
                                        List<Artifact> mvnArtifacts,
                                        List<String> jarLibs,
                                        Resource resource) {
        Dependency dependency = Dependency.toDependency(resource.getResource());
        dependency
            .getPom()
            .forEach(
                pom -> mvnArtifacts.add(
                    new Artifact(
                        pom.getGroupId(),
                        pom.getArtifactId(),
                        pom.getVersion(),
                        pom.getClassifier())));
        dependency
            .getJar()
            .forEach(
                jar -> jarLibs.add(
                    String.format(
                        "%s/%d/%s",
                        Workspace.local().APP_UPLOADS(),
                        application.getTeamId(), jar)));
    }
}
