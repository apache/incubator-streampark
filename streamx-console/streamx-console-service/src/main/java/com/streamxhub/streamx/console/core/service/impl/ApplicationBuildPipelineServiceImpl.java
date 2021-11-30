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

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.streamxhub.streamx.common.conf.Workspace;
import com.streamxhub.streamx.common.enums.ExecutionMode;
import com.streamxhub.streamx.common.util.ThreadUtils;
import com.streamxhub.streamx.console.base.util.WebUtils;
import com.streamxhub.streamx.console.core.dao.ApplicationBuildPipelineMapper;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.ApplicationBuildPipeline;
import com.streamxhub.streamx.console.core.entity.FlinkEnv;
import com.streamxhub.streamx.console.core.service.ApplicationBuildPipelineService;
import com.streamxhub.streamx.console.core.service.FlinkEnvService;
import com.streamxhub.streamx.console.core.service.SettingService;
import com.streamxhub.streamx.flink.packer.docker.DockerAuthConf;
import com.streamxhub.streamx.flink.packer.pipeline.*;
import com.streamxhub.streamx.flink.packer.pipeline.impl.FlinkK8sApplicationBuildPipeline;
import com.streamxhub.streamx.flink.packer.pipeline.impl.FlinkK8sSessionBuildPipeline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
public class ApplicationBuildPipelineServiceImpl
    extends ServiceImpl<ApplicationBuildPipelineMapper, ApplicationBuildPipeline> implements ApplicationBuildPipelineService {

    private final ExecutorService executorService = new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 2,
        200,
        60L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1024),
        ThreadUtils.threadFactory("streamx-deploy-executor"),
        new ThreadPoolExecutor.AbortPolicy()
    );

    @Autowired
    private FlinkEnvService flinkEnvService;

    @Autowired
    private SettingService settingService;


    @Override
    public void buildApplication(@Nonnull Application app) {
        // create pipeline instance
        BuildPipeline pipeline = createPipelineInstance(app);

        // register pipeline progress event watcher.
        // save snapshot of pipeline to db when status of pipeline was changed.
        pipeline.registerWatcher(new BuildPipelineWatcher() {
            @Override
            public void onStart(PipeSnapshot snapshot) {
                ApplicationBuildPipeline pipePo = ApplicationBuildPipeline.fromPipeSnapshot(snapshot).setAppId(app.getId());
                save(pipePo);
            }

            @Override
            public void onStepStateChange(PipeSnapshot snapshot) {
                ApplicationBuildPipeline pipePo = ApplicationBuildPipeline.fromPipeSnapshot(snapshot).setAppId(app.getId());
                save(pipePo);
            }

            @Override
            public void onFinish(PipeSnapshot snapshot, BuildResult result) {
                ApplicationBuildPipeline pipePo = ApplicationBuildPipeline.fromPipeSnapshot(snapshot)
                    .setAppId(app.getId())
                    .setBuildResult(result);
                save(pipePo);
            }
        });
        // save pipeline instance snapshot to db before launch it.
        ApplicationBuildPipeline pipePo = ApplicationBuildPipeline.initFromPipeline(pipeline).setAppId(app.getId());
        save(pipePo);
        // async launch pipeline
        executorService.submit((Runnable) pipeline::launch);
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


}
