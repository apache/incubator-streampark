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
import com.streamxhub.streamx.flink.packer.pipeline.BuildPipeline;
import com.streamxhub.streamx.flink.packer.pipeline.BuildResult;
import com.streamxhub.streamx.flink.packer.pipeline.DockerBuildSnapshot;
import com.streamxhub.streamx.flink.packer.pipeline.DockerProgressWatcher;
import com.streamxhub.streamx.flink.packer.pipeline.DockerPullSnapshot;
import com.streamxhub.streamx.flink.packer.pipeline.DockerPushSnapshot;
import com.streamxhub.streamx.flink.packer.pipeline.DockerResolvedSnapshot;
import com.streamxhub.streamx.flink.packer.pipeline.FlinkK8sApplicationBuildRequest;
import com.streamxhub.streamx.flink.packer.pipeline.FlinkK8sSessionBuildRequest;
import com.streamxhub.streamx.flink.packer.pipeline.PipeSnapshot;
import com.streamxhub.streamx.flink.packer.pipeline.PipeStatus;
import com.streamxhub.streamx.flink.packer.pipeline.PipeType;
import com.streamxhub.streamx.flink.packer.pipeline.PipeWatcher;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    private static final Cache<Long, DockerPullSnapshot> DOCKER_PULL_PG_SNAPSHOTS =
        Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).build();

    private static final Cache<Long, DockerBuildSnapshot> DOCKER_BUILD_PG_SNAPSHOTS =
        Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).build();

    private static final Cache<Long, DockerPushSnapshot> DOCKER_PUSH_PG_SNAPSHOTS =
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
        AppBuildPipeline pipePo = AppBuildPipeline.initFromPipeline(pipeline).setAppId(app.getId());
        boolean saved = saveEntity(pipePo);
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
            DOCKER_PULL_PG_SNAPSHOTS.getIfPresent(appId),
            DOCKER_BUILD_PG_SNAPSHOTS.getIfPresent(appId),
            DOCKER_PUSH_PG_SNAPSHOTS.getIfPresent(appId));
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

    public boolean saveEntity(AppBuildPipeline pipe) {
        AppBuildPipeline old = getById(pipe.getAppId());
        if (old == null) {
            return save(pipe);
        } else {
            return updateById(pipe);
        }
    }

}
