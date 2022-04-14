/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.service.impl;

import com.streamxhub.streamx.common.conf.ConfigConst;
import com.streamxhub.streamx.common.conf.Workspace;
import com.streamxhub.streamx.common.enums.ApplicationType;
import com.streamxhub.streamx.common.enums.DevelopmentMode;
import com.streamxhub.streamx.common.enums.ExecutionMode;
import com.streamxhub.streamx.common.fs.FsOperator;
import com.streamxhub.streamx.common.util.ExceptionUtils;
import com.streamxhub.streamx.common.util.ThreadUtils;
import com.streamxhub.streamx.console.base.util.WebUtils;
import com.streamxhub.streamx.console.core.dao.ApplicationBuildPipelineMapper;
import com.streamxhub.streamx.console.core.entity.AppBuildPipeline;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.ApplicationConfig;
import com.streamxhub.streamx.console.core.entity.FlinkEnv;
import com.streamxhub.streamx.console.core.entity.FlinkSql;
import com.streamxhub.streamx.console.core.entity.Message;
import com.streamxhub.streamx.console.core.enums.CandidateType;
import com.streamxhub.streamx.console.core.enums.LaunchState;
import com.streamxhub.streamx.console.core.enums.NoticeType;
import com.streamxhub.streamx.console.core.enums.OptionState;
import com.streamxhub.streamx.console.core.service.AppBuildPipeService;
import com.streamxhub.streamx.console.core.service.ApplicationBackUpService;
import com.streamxhub.streamx.console.core.service.ApplicationConfigService;
import com.streamxhub.streamx.console.core.service.ApplicationService;
import com.streamxhub.streamx.console.core.service.CommonService;
import com.streamxhub.streamx.console.core.service.FlinkEnvService;
import com.streamxhub.streamx.console.core.service.FlinkSqlService;
import com.streamxhub.streamx.console.core.service.MessageService;
import com.streamxhub.streamx.console.core.service.SettingService;
import com.streamxhub.streamx.flink.packer.docker.DockerAuthConf;
import com.streamxhub.streamx.flink.packer.pipeline.BuildPipeline;
import com.streamxhub.streamx.flink.packer.pipeline.BuildResult;
import com.streamxhub.streamx.flink.packer.pipeline.DockerBuildSnapshot;
import com.streamxhub.streamx.flink.packer.pipeline.DockerProgressWatcher;
import com.streamxhub.streamx.flink.packer.pipeline.DockerPullSnapshot;
import com.streamxhub.streamx.flink.packer.pipeline.DockerPushSnapshot;
import com.streamxhub.streamx.flink.packer.pipeline.DockerResolvedSnapshot;
import com.streamxhub.streamx.flink.packer.pipeline.FlinkK8sApplicationBuildRequest;
import com.streamxhub.streamx.flink.packer.pipeline.FlinkK8sSessionBuildRequest;
import com.streamxhub.streamx.flink.packer.pipeline.FlinkRemotePerJobBuildRequest;
import com.streamxhub.streamx.flink.packer.pipeline.FlinkYarnApplicationBuildRequest;
import com.streamxhub.streamx.flink.packer.pipeline.PipeSnapshot;
import com.streamxhub.streamx.flink.packer.pipeline.PipeWatcher;
import com.streamxhub.streamx.flink.packer.pipeline.PipelineStatus;
import com.streamxhub.streamx.flink.packer.pipeline.PipelineType;
import com.streamxhub.streamx.flink.packer.pipeline.impl.FlinkK8sApplicationBuildPipeline;
import com.streamxhub.streamx.flink.packer.pipeline.impl.FlinkK8sSessionBuildPipeline;
import com.streamxhub.streamx.flink.packer.pipeline.impl.FlinkRemoteBuildPipeline;
import com.streamxhub.streamx.flink.packer.pipeline.impl.FlinkYarnApplicationBuildPipeline;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Al-assad
 */
@Service
@Slf4j
@Transactional(propagation = Propagation.SUPPORTS, rollbackFor = Exception.class)
@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
public class ApplBuildPipeServiceImpl
        extends ServiceImpl<ApplicationBuildPipelineMapper, AppBuildPipeline> implements AppBuildPipeService {

    @Autowired
    private FlinkEnvService flinkEnvService;

    @Autowired
    private FlinkSqlService flinkSqlService;

    @Autowired
    private ApplicationBackUpService backUpService;

    @Autowired
    private CommonService commonService;

    @Autowired
    private SettingService settingService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationConfigService applicationConfigService;

    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2,
            300,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2048),
            ThreadUtils.threadFactory("streamx-build-pipeline-executor"),
            new ThreadPoolExecutor.AbortPolicy()
    );

    private static final Cache<Long, DockerPullSnapshot> DOCKER_PULL_PG_SNAPSHOTS =
            Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).build();

    private static final Cache<Long, DockerBuildSnapshot> DOCKER_BUILD_PG_SNAPSHOTS =
            Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).build();

    private static final Cache<Long, DockerPushSnapshot> DOCKER_PUSH_PG_SNAPSHOTS =
            Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).build();

    @Override
    public boolean buildApplication(@Nonnull Application app) throws Exception {

        // 1) flink sql setDependency
        FlinkSql newFlinkSql = flinkSqlService.getCandidate(app.getId(), CandidateType.NEW);
        FlinkSql effectiveFlinkSql = flinkSqlService.getEffective(app.getId(), false);
        if (app.isFlinkSqlJob()) {
            FlinkSql flinkSql = newFlinkSql == null ? effectiveFlinkSql : newFlinkSql;
            assert flinkSql != null;
            app.setDependency(flinkSql.getDependency());
        }

        // create pipeline instance
        BuildPipeline pipeline = createPipelineInstance(app);

        // clear history
        removeApp(app.getId());
        // register pipeline progress event watcher.
        // save snapshot of pipeline to db when status of pipeline was changed.
        pipeline.registerWatcher(new PipeWatcher() {
            @Override
            public void onStart(PipeSnapshot snapshot) throws Exception {
                AppBuildPipeline buildPipeline = AppBuildPipeline.fromPipeSnapshot(snapshot).setAppId(app.getId());
                saveEntity(buildPipeline);

                app.setLaunch(LaunchState.LAUNCHING.get());
                applicationService.updateLaunch(app);

                // 1) checkEnv
                applicationService.checkEnv(app);

                // 2) some preparatory work
                String appUploads = app.getWorkspace().APP_UPLOADS();

                if (app.isCustomCodeJob()) {
                    // customCode upload jar to appHome...
                    String appHome = app.getAppHome();
                    FsOperator fsOperator = app.getFsOperator();
                    fsOperator.delete(appHome);
                    if (app.isUploadJob()) {
                        File localJar = new File(WebUtils.getAppTempDir(), app.getJar());
                        // upload jar copy to appHome
                        String uploadJar = appUploads.concat("/").concat(app.getJar());
                        checkOrElseUploadJar(app.getFsOperator(), localJar, uploadJar, appUploads);
                        switch (app.getApplicationType()) {
                            case STREAMX_FLINK:
                                fsOperator.mkdirs(app.getAppLib());
                                fsOperator.copy(uploadJar, app.getAppLib(), false, true);
                                break;
                            case APACHE_FLINK:
                                fsOperator.mkdirs(appHome);
                                fsOperator.copy(uploadJar, appHome, false, true);
                                break;
                            default:
                                throw new IllegalArgumentException("[StreamX] unsupported ApplicationType of custom code: "
                                        + app.getApplicationType());
                        }
                    } else {
                        fsOperator.upload(app.getDistHome(), appHome);
                    }
                } else {
                    if (!app.getDependencyObject().getJar().isEmpty()) {
                        //copy jar to local upload dir
                        for (String jar : app.getDependencyObject().getJar()) {
                            File localJar = new File(WebUtils.getAppTempDir(), jar);
                            assert localJar.exists();
                            String localUploads = Workspace.local().APP_UPLOADS();
                            String uploadJar = localUploads.concat("/").concat(jar);
                            checkOrElseUploadJar(FsOperator.lfs(), localJar, uploadJar, localUploads);
                        }
                    }
                }
            }

            @Override
            public void onStepStateChange(PipeSnapshot snapshot) {
                AppBuildPipeline buildPipeline = AppBuildPipeline.fromPipeSnapshot(snapshot).setAppId(app.getId());
                saveEntity(buildPipeline);
            }

            @Override
            public void onFinish(PipeSnapshot snapshot, BuildResult result) {
                AppBuildPipeline buildPipeline = AppBuildPipeline.fromPipeSnapshot(snapshot).setAppId(app.getId()).setBuildResult(result);
                saveEntity(buildPipeline);
                if (result.pass()) {
                    //running job ...
                    if (app.isRunning()) {
                        app.setLaunch(LaunchState.NEED_RESTART.get());
                    } else {
                        app.setOptionState(OptionState.NONE.getValue());
                        app.setLaunch(LaunchState.DONE.get());
                        //如果当前任务未运行,或者刚刚新增的任务,则直接将候选版本的设置为正式版本
                        if (app.isFlinkSqlJob()) {
                            applicationService.toEffective(app);
                        } else {
                            if (app.isStreamXJob()) {
                                ApplicationConfig config = applicationConfigService.getLatest(app.getId());
                                if (config != null) {
                                    config.setToApplication(app);
                                    applicationConfigService.toEffective(app.getId(), app.getConfigId());
                                }
                            }
                        }
                    }
                    // backup.
                    if (!app.isNeedRollback()) {
                        if (app.isFlinkSqlJob() && newFlinkSql != null) {
                            backUpService.backup(app, newFlinkSql);
                        } else {
                            backUpService.backup(app, null);
                        }
                    }

                    app.setBuild(false);

                } else {
                    Message message = new Message(
                            commonService.getCurrentUser().getUserId(),
                            app.getId(),
                            app.getJobName().concat(" launch failed"),
                            ExceptionUtils.stringifyException(snapshot.error().exception()),
                            NoticeType.EXCEPTION
                    );
                    messageService.push(message);
                    app.setLaunch(LaunchState.FAILED.get());
                    app.setOptionState(OptionState.NONE.getValue());
                    app.setBuild(true);
                }
                applicationService.updateLaunch(app);
            }
        });
        // save docker resolve progress detail to cache, only for flink-k8s application mode.
        if (PipelineType.FLINK_NATIVE_K8S_APPLICATION == pipeline.pipeType()) {
            pipeline.as(FlinkK8sApplicationBuildPipeline.class).registerDockerProgressWatcher(new DockerProgressWatcher() {
                @Override
                public void onDockerPullProgressChange(DockerPullSnapshot snapshot) {
                    DOCKER_PULL_PG_SNAPSHOTS.put(app.getId(), snapshot);
                }

                @Override
                public void onDockerBuildProgressChange(DockerBuildSnapshot snapshot) {
                    DOCKER_BUILD_PG_SNAPSHOTS.put(app.getId(), snapshot);
                }

                @Override
                public void onDockerPushProgressChange(DockerPushSnapshot snapshot) {
                    DOCKER_PUSH_PG_SNAPSHOTS.put(app.getId(), snapshot);
                }
            });
        }
        // save pipeline instance snapshot to db before launch it.
        AppBuildPipeline buildPipeline = AppBuildPipeline.initFromPipeline(pipeline).setAppId(app.getId());
        boolean saved = saveEntity(buildPipeline);
        DOCKER_PULL_PG_SNAPSHOTS.invalidate(app.getId());
        DOCKER_BUILD_PG_SNAPSHOTS.invalidate(app.getId());
        DOCKER_PUSH_PG_SNAPSHOTS.invalidate(app.getId());
        // async launch pipeline
        executorService.submit((Runnable) pipeline::launch);
        return saved;
    }

    /**
     * create building pipeline instance
     */
    private BuildPipeline createPipelineInstance(@Nonnull Application app) {
        FlinkEnv flinkEnv = flinkEnvService.getByIdOrDefault(app.getVersionId());
        String flinkUserJar = retrieveFlinkUserJar(flinkEnv, app);
        ExecutionMode executionMode = app.getExecutionModeEnum();
        String mainClass = ConfigConst.STREAMX_FLINKSQL_CLIENT_CLASS();
        switch (executionMode) {
            case YARN_APPLICATION:
                String yarnProvidedPath = app.getAppLib();
                String localWorkspace = app.getLocalAppHome().concat("/lib");
                if (app.getDevelopmentMode().equals(DevelopmentMode.CUSTOMCODE)
                        && app.getApplicationType().equals(ApplicationType.APACHE_FLINK)) {
                    yarnProvidedPath = app.getAppHome();
                    localWorkspace = app.getLocalAppHome();
                }
                FlinkYarnApplicationBuildRequest yarnAppRequest = new FlinkYarnApplicationBuildRequest(
                        app.getJobName(),
                        mainClass,
                        localWorkspace,
                        yarnProvidedPath,
                        app.getDevelopmentMode(),
                        app.getDependencyInfo()
                );
                log.info("Submit params to building pipeline : {}", yarnAppRequest);
                return FlinkYarnApplicationBuildPipeline.of(yarnAppRequest);
            case YARN_PER_JOB:
            case YARN_SESSION:
            case REMOTE:
                FlinkRemotePerJobBuildRequest buildRequest = new FlinkRemotePerJobBuildRequest(
                        app.getJobName(),
                        app.getLocalAppHome(),
                        mainClass,
                        flinkUserJar,
                        app.isCustomCodeJob(),
                        app.getExecutionModeEnum(),
                        app.getDevelopmentMode(),
                        flinkEnv.getFlinkVersion(),
                        app.getDependencyInfo()
                );
                log.info("Submit params to building pipeline : {}", buildRequest);
                return FlinkRemoteBuildPipeline.of(buildRequest);
            case KUBERNETES_NATIVE_SESSION:
                FlinkK8sSessionBuildRequest k8sSessionBuildRequest = new FlinkK8sSessionBuildRequest(
                        app.getJobName(),
                        app.getLocalAppHome(),
                        mainClass,
                        flinkUserJar,
                        app.getExecutionModeEnum(),
                        app.getDevelopmentMode(),
                        flinkEnv.getFlinkVersion(),
                        app.getDependencyInfo(),
                        app.getClusterId(),
                        app.getK8sNamespace());
                log.info("Submit params to building pipeline : {}", k8sSessionBuildRequest);
                return FlinkK8sSessionBuildPipeline.of(k8sSessionBuildRequest);
            case KUBERNETES_NATIVE_APPLICATION:
                FlinkK8sApplicationBuildRequest k8sApplicationBuildRequest = new FlinkK8sApplicationBuildRequest(
                        app.getJobName(),
                        app.getLocalAppHome(),
                        mainClass,
                        flinkUserJar,
                        app.getExecutionModeEnum(),
                        app.getDevelopmentMode(),
                        flinkEnv.getFlinkVersion(),
                        app.getDependencyInfo(),
                        app.getClusterId(),
                        app.getK8sNamespace(),
                        app.getFlinkImage(),
                        app.getK8sPodTemplates(),
                        app.getK8sHadoopIntegration() != null ? app.getK8sHadoopIntegration() : false,
                        DockerAuthConf.of(
                                settingService.getDockerRegisterAddress(),
                                settingService.getDockerRegisterUser(),
                                settingService.getDockerRegisterPassword()
                        )
                );
                log.info("Submit params to building pipeline : {}", k8sApplicationBuildRequest);
                return FlinkK8sApplicationBuildPipeline.of(k8sApplicationBuildRequest);
            default:
                throw new UnsupportedOperationException("Unsupported Building Application for ExecutionMode: " + app.getExecutionModeEnum());
        }
    }

    /**
     * copy from {@link ApplicationServiceImpl#start(Application, boolean)}
     */
    private String retrieveFlinkUserJar(FlinkEnv flinkEnv, Application app) {
        switch (app.getDevelopmentMode()) {
            case CUSTOMCODE:
                switch (app.getApplicationType()) {
                    case STREAMX_FLINK:
                        return String.format("%s/%s", app.getAppLib(), app.getModule().concat(".jar"));
                    case APACHE_FLINK:
                        return String.format("%s/%s", app.getAppHome(), app.getJar());
                    default:
                        throw new IllegalArgumentException("[StreamX] unsupported ApplicationType of custom code: "
                                + app.getApplicationType());
                }
            case FLINKSQL:
                String sqlDistJar = commonService.getSqlClientJar(flinkEnv);
                if (app.getExecutionModeEnum() == ExecutionMode.YARN_APPLICATION) {
                    String clientPath = Workspace.remote().APP_CLIENT();
                    return String.format("%s/%s", clientPath, sqlDistJar);
                }
                return Workspace.local().APP_CLIENT().concat("/").concat(sqlDistJar);
            default:
                throw new UnsupportedOperationException("[StreamX] unsupported JobType: " + app.getDevelopmentMode());
        }
    }

    @Override
    public Optional<AppBuildPipeline> getCurrentBuildPipeline(@Nonnull Long appId) {
        return Optional.ofNullable(getById(appId));
    }

    @Override
    public DockerResolvedSnapshot getDockerProgressDetailSnapshot(@Nonnull Long appId) {
        return new DockerResolvedSnapshot(
                DOCKER_PULL_PG_SNAPSHOTS.getIfPresent(appId),
                DOCKER_BUILD_PG_SNAPSHOTS.getIfPresent(appId),
                DOCKER_PUSH_PG_SNAPSHOTS.getIfPresent(appId)
        );
    }

    @Override
    public boolean allowToBuildNow(@Nonnull Long appId) {
        return getCurrentBuildPipeline(appId)
                .map(pipeline -> PipelineStatus.running != pipeline.getPipeStatus())
                .orElse(true);
    }

    @Override
    public Map<Long, PipelineStatus> listPipelineStatus(List<Long> appIds) {
        if (CollectionUtils.isEmpty(appIds)) {
            return Maps.newHashMap();
        }
        QueryWrapper<AppBuildPipeline> query = new QueryWrapper<>();
        query.select("app_id", "pipe_status").in("app_id", appIds);
        List<Map<String, Object>> rMaps = baseMapper.selectMaps(query);
        if (CollectionUtils.isEmpty(rMaps)) {
            return Maps.newHashMap();
        }
        return rMaps.stream().collect(Collectors.toMap(
                e -> (Long) e.get("app_id"),
                e -> PipelineStatus.of((Integer) e.get("pipe_status"))));
    }

    @Override
    public void removeApp(Long appId) {
        baseMapper.delete(new QueryWrapper<AppBuildPipeline>().lambda().eq(AppBuildPipeline::getAppId, appId));
    }

    public boolean saveEntity(AppBuildPipeline pipe) {
        AppBuildPipeline old = getById(pipe.getAppId());
        if (old == null) {
            return save(pipe);
        } else {
            return updateById(pipe);
        }
    }

    private void checkOrElseUploadJar(FsOperator fsOperator, File localJar, String targetJar, String targetDir) throws IOException {
        //1)文件不存直接上传
        if (!fsOperator.exists(targetJar)) {
            fsOperator.upload(localJar.getAbsolutePath(), targetDir, false, true);
        } else {
            //2) 文件已经存在则检查md5是否一致.不一致则重新上传
            try (InputStream inputStream = new FileInputStream(localJar)) {
                String md5 = DigestUtils.md5Hex(inputStream);
                //2) md5不一致,则需重新上传.将本地temp/下的文件上传到upload目录下
                if (!md5.equals(fsOperator.fileMd5(targetJar))) {
                    fsOperator.upload(localJar.getAbsolutePath(), targetDir, false, true);
                }
            }
        }
    }

}
