/*
 * Copyright (c) 2021 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.console.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Maps;
import com.streamxhub.streamx.common.conf.Workspace;
import com.streamxhub.streamx.common.enums.ExecutionMode;
import com.streamxhub.streamx.common.util.ThreadUtils;
import com.streamxhub.streamx.console.base.util.WebUtils;
import com.streamxhub.streamx.console.core.dao.ApplicationBuildPipelineMapper;
import com.streamxhub.streamx.console.core.entity.AppBuildPipeline;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.FlinkEnv;
import com.streamxhub.streamx.console.core.service.AppBuildPipeService;
import com.streamxhub.streamx.console.core.service.FlinkEnvService;
import com.streamxhub.streamx.console.core.service.SettingService;
import com.streamxhub.streamx.flink.packer.docker.DockerAuthConf;
import com.streamxhub.streamx.flink.packer.pipeline.*;
import com.streamxhub.streamx.flink.packer.pipeline.impl.FlinkK8sApplicationBuildPipeline;
import com.streamxhub.streamx.flink.packer.pipeline.impl.FlinkK8sSessionBuildPipeline;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.streamxhub.streamx.common.enums.ExecutionMode.KUBERNETES_NATIVE_APPLICATION;
import static com.streamxhub.streamx.common.enums.ExecutionMode.KUBERNETES_NATIVE_SESSION;

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
    private SettingService settingService;

    private final ExecutorService executorService = new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 2,
        300,
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(2048),
        ThreadUtils.threadFactory("streamx-build-pipeline-executor"),
        new ThreadPoolExecutor.AbortPolicy()
    );

    private final static Cache<Long, DockerPullSnapshot> dockerPullPgSnapshots =
        Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).build();

    private final static Cache<Long, DockerBuildSnapshot> dockerBuildPgSnapshots =
        Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).build();

    private final static Cache<Long, DockerPushSnapshot> dockerPushPgSnapshots =
        Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).build();


    @Override
    public boolean buildApplication(@Nonnull Application app) {
        // create pipeline instance
        BuildPipeline pipeline = createPipelineInstance(app);

        // register pipeline progress event watcher.
        // save snapshot of pipeline to db when status of pipeline was changed.
        pipeline.registerWatcher(new PipeWatcher() {
            @Override
            public void onStart(PipeSnapshot snapshot) {
                AppBuildPipeline pipePo = AppBuildPipeline.fromPipeSnapshot(snapshot).setAppId(app.getId());
                saveEntity(pipePo);
            }

            @Override
            public void onStepStateChange(PipeSnapshot snapshot) {
                AppBuildPipeline pipePo = AppBuildPipeline.fromPipeSnapshot(snapshot).setAppId(app.getId());
                saveEntity(pipePo);
            }

            @Override
            public void onFinish(PipeSnapshot snapshot, BuildResult result) {
                AppBuildPipeline pipePo = AppBuildPipeline.fromPipeSnapshot(snapshot)
                    .setAppId(app.getId())
                    .setBuildResult(result);
                saveEntity(pipePo);
            }
        });
        // save docker resolve progress detail to cache, only for flink-k8s application mode.
        if (PipeType.FLINK_NATIVE_K8S_APPLICATION == pipeline.pipeType()) {
            pipeline.as(FlinkK8sApplicationBuildPipeline.class).registerDockerProgressWatcher(new DockerProgressWatcher() {
                @Override
                public void onDockerPullProgressChange(DockerPullSnapshot snapshot) {
                    dockerPullPgSnapshots.put(app.getId(), snapshot);
                }

                @Override
                public void onDockerBuildProgressChange(DockerBuildSnapshot snapshot) {
                    dockerBuildPgSnapshots.put(app.getId(), snapshot);
                }

                @Override
                public void onDockerPushProgressChange(DockerPushSnapshot snapshot) {
                    dockerPushPgSnapshots.put(app.getId(), snapshot);
                }
            });
        }
        // save pipeline instance snapshot to db before launch it.
        AppBuildPipeline pipePo = AppBuildPipeline.initFromPipeline(pipeline).setAppId(app.getId());
        boolean saved = saveEntity(pipePo);
        dockerPullPgSnapshots.invalidate(app.getId());
        dockerBuildPgSnapshots.invalidate(app.getId());
        dockerPushPgSnapshots.invalidate(app.getId());
        // async launch pipeline
        executorService.submit((Runnable) pipeline::launch);
        return saved;
    }

    /**
     * create building pipeline instance
     */
    private BuildPipeline createPipelineInstance(@Nonnull Application app) {
        FlinkEnv flinkEnv = flinkEnvService.getByIdOrDefault(app.getVersionId());
        String flinkUserJar = retrieveFlinkUserJar(app);
        ExecutionMode executionMode = app.getExecutionModeEnum();

        if (KUBERNETES_NATIVE_SESSION.equals(executionMode)) {
            FlinkK8sSessionBuildRequest params = new FlinkK8sSessionBuildRequest(
                app.getJobName(),
                app.getExecutionModeEnum(),
                app.getDevelopmentMode(),
                flinkEnv.getFlinkVersion(),
                app.getJarPackDeps(),
                flinkUserJar,
                app.getClusterId(),
                app.getK8sNamespace());
            log.info("Submit params to building pipeline : {}", params);
            return FlinkK8sSessionBuildPipeline.of(params);

        } else if (KUBERNETES_NATIVE_APPLICATION.equals(executionMode)) {
            FlinkK8sApplicationBuildRequest params = new FlinkK8sApplicationBuildRequest(
                app.getJobName(),
                app.getExecutionModeEnum(),
                app.getDevelopmentMode(),
                flinkEnv.getFlinkVersion(),
                app.getJarPackDeps(),
                flinkUserJar,
                app.getClusterId(),
                app.getK8sNamespace(),
                app.getFlinkImage(),
                app.getK8sPodTemplates(),
                app.getK8sHadoopIntegration() != null ? app.getK8sHadoopIntegration() : false,
                DockerAuthConf.of(
                    settingService.getDockerRegisterAddress(),
                    settingService.getDockerRegisterUser(),
                    settingService.getDockerRegisterPassword()));
            log.info("Submit params to building pipeline : {}", params);
            return FlinkK8sApplicationBuildPipeline.of(params);

        } else {
            throw new UnsupportedOperationException("Unsupported Building Application for ExecutionMode: " + app.getExecutionModeEnum());
        }
    }


    /**
     * copy from {@link ApplicationServiceImpl#start(Application, boolean)}
     * todo needs to be refactored.
     */
    private String retrieveFlinkUserJar(Application application) {
        switch (application.getDevelopmentMode()) {
            case CUSTOMCODE:
                switch (application.getApplicationType()) {
                    case STREAMX_FLINK:
                        return String.format("%s/lib/%s", application.getAppHome(), application.getModule().concat(".jar"));
                    case APACHE_FLINK:
                        return String.format("%s/%s", application.getAppHome(), application.getJar());
                    default:
                        throw new IllegalArgumentException("[streamx] unsupported ApplicationType of custom code: "
                            + application.getApplicationType());
                }
            case FLINKSQL:
                switch (application.getExecutionModeEnum()) {
                    case YARN_APPLICATION:
                        String pluginPath = Workspace.remote().APP_PLUGINS();
                        return String.format("%s/%s", pluginPath, retrieveSqlDistJar());
                    default:
                        return Workspace.local().APP_PLUGINS().concat("/").concat(retrieveSqlDistJar());
                }
            default:
                throw new UnsupportedOperationException("[streamx] unsupported JobType: " + application.getDevelopmentMode());
        }
    }

    /**
     * copy from {@link ApplicationServiceImpl#start(Application, boolean)}
     * todo needs to be refactored.
     */
    private String retrieveSqlDistJar() {
        File localPlugins = new File(WebUtils.getAppDir("plugins"));
        assert localPlugins.exists();
        List<String> jars =
            Arrays.stream(Objects.requireNonNull(localPlugins.list())).filter(x -> x.matches("streamx-flink-sqlclient-.*\\.jar"))
                .collect(Collectors.toList());
        if (jars.isEmpty()) {
            throw new IllegalArgumentException("[streamx] can no found streamx-flink-sqlclient jar in " + localPlugins);
        }
        if (jars.size() > 1) {
            throw new IllegalArgumentException("[streamx] found multiple streamx-flink-sqlclient jar in " + localPlugins);
        }
        return jars.get(0);
    }


    @Override
    public Optional<AppBuildPipeline> getCurrentBuildPipeline(@Nonnull Long appId) {
        return Optional.ofNullable(getById(appId));
    }

    @Override
    public DockerResolvedSnapshot getDockerProgressDetailSnapshot(@Nonnull Long appId) {
        return new DockerResolvedSnapshot(
            dockerPullPgSnapshots.getIfPresent(appId),
            dockerBuildPgSnapshots.getIfPresent(appId),
            dockerPushPgSnapshots.getIfPresent(appId));
    }

    @Override
    public boolean allowToBuildNow(@Nonnull Long appId) {
        return getCurrentBuildPipeline(appId)
            .map(pipeline -> PipeStatus.running != pipeline.getPipeStatus())
            .orElse(true);
    }

    @Override
    public Map<Long, PipeStatus> listPipelineStatus(List<Long> appIds) {
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
            e -> PipeStatus.of((Integer) e.get("pipe_status"))));
    }


    public boolean saveEntity (AppBuildPipeline pipe) {
        AppBuildPipeline old = getById(pipe.getAppId());
        if (old == null) {
            return save(pipe);
        } else {
            return updateById(pipe);
        }
    }

}
