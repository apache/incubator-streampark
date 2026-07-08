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

package org.apache.streampark.flink.packer.pipeline.impl;

import org.apache.streampark.common.fs.LfsOperator;
import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.flink.kubernetes.PodTemplateTool;
import org.apache.streampark.flink.kubernetes.ingress.IngressController;
import org.apache.streampark.flink.packer.docker.DockerClients;
import org.apache.streampark.flink.packer.docker.FlinkDockerfileTemplate;
import org.apache.streampark.flink.packer.docker.FlinkDockerfileTemplateTrait;
import org.apache.streampark.flink.packer.docker.FlinkHadoopDockerfileTemplate;
import org.apache.streampark.flink.packer.maven.MavenTool;
import org.apache.streampark.flink.packer.pipeline.BuildParam;
import org.apache.streampark.flink.packer.pipeline.BuildPipeline;
import org.apache.streampark.flink.packer.pipeline.BuildResult;
import org.apache.streampark.flink.packer.pipeline.DockerImageBuildResponse;
import org.apache.streampark.flink.packer.pipeline.DockerProgressWatcher;
import org.apache.streampark.flink.packer.pipeline.DockerResolveProgress;
import org.apache.streampark.flink.packer.pipeline.FlinkK8sApplicationBuildRequest;
import org.apache.streampark.flink.packer.pipeline.PipelineTypeEnum;
import org.apache.streampark.flink.packer.pipeline.SilentDockerProgressWatcher;

import org.apache.commons.lang3.StringUtils;

import com.github.dockerjava.core.command.HackBuildImageCmd;
import com.github.dockerjava.core.command.HackPullImageCmd;
import com.github.dockerjava.core.command.HackPushImageCmd;
import com.google.common.collect.Sets;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Building pipeline for flink kubernetes-native application mode. */
public class FlinkK8sApplicationBuildPipeline extends BuildPipeline {

    private static final ThreadPoolExecutor DOCKER_EXEC_POOL =
        new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 5,
            Runtime.getRuntime().availableProcessors() * 10,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2048),
            ThreadUtils.threadFactory("streampark-docker-progress-watcher-executor"),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    private final FlinkK8sApplicationBuildRequest request;
    private DockerProgressWatcher dockerProcessWatcher = new SilentDockerProgressWatcher();
    private final DockerResolveProgress dockerProcess = DockerResolveProgress.createEmpty();

    public FlinkK8sApplicationBuildPipeline(FlinkK8sApplicationBuildRequest request) {
        this.request = request;
    }

    public static FlinkK8sApplicationBuildPipeline of(FlinkK8sApplicationBuildRequest request) {
        return new FlinkK8sApplicationBuildPipeline(request);
    }

    public void registerDockerProgressWatcher(DockerProgressWatcher watcher) {
        this.dockerProcessWatcher = watcher;
    }

    @Override
    public PipelineTypeEnum getPipeType() {
        return PipelineTypeEnum.FLINK_NATIVE_K8S_APPLICATION;
    }

    @Override
    protected BuildParam offerBuildParam() {
        return request;
    }

    @Override
    protected BuildResult buildProcess() throws Throwable {
        String buildWorkspace =
            execStep(
                1,
                () -> {
                    String ws =
                        request.workspace()
                            + "/"
                            + request.clusterId()
                            + "@"
                            + request.k8sNamespace();
                    LfsOperator.getInstance().mkCleanDirs(ws);
                    log.info("Recreate building workspace: {}", ws);
                    return ws;
                })
                    .orElseThrow(() -> getError().exception());

        Map<String, String> podTemplatePaths;
        if (request.flinkPodTemplate() == null || request.flinkPodTemplate().isEmpty()) {
            skipStep(2);
            podTemplatePaths = Collections.emptyMap();
        } else {
            podTemplatePaths =
                execStep(
                    2,
                    () -> {
                        Map<String, String> files =
                            PodTemplateTool.preparePodTemplateFiles(
                                buildWorkspace,
                                request.flinkPodTemplate())
                                .tmplFiles();
                        log.info(
                            "Export flink podTemplates: {}",
                            String.join(",", files.values()));
                        return files;
                    })
                        .orElseThrow(() -> getError().exception());
        }

        File shadedJar;
        java.util.Set<String> extJarLibs;
        var step3 =
            execStep(
                3,
                () -> {
                    String shadedJarOutputPath = request.getShadedJarPath(buildWorkspace);
                    File jar =
                        MavenTool.buildFatJar(
                            request.mainClass(),
                            request.providedLibs(),
                            shadedJarOutputPath);
                    log.info("Output shaded flink job jar: {}", jar.getAbsolutePath());
                    return Map.entry(jar, request.dependencyInfo().extJarLibs());
                });
        if (step3.isEmpty()) {
            throw getError().exception();
        }
        shadedJar = step3.get().getKey();
        extJarLibs = step3.get().getValue();

        File dockerfile;
        FlinkDockerfileTemplateTrait dockerFileTemplate;
        var step4 =
            execStep(
                4,
                () -> {
                    FlinkDockerfileTemplateTrait template;
                    if (request.integrateWithHadoop()) {
                        template =
                            FlinkHadoopDockerfileTemplate.fromSystemHadoopConf(
                                buildWorkspace,
                                request.flinkBaseImage(),
                                shadedJar.getAbsolutePath(),
                                extJarLibs);
                    } else {
                        template =
                            new FlinkDockerfileTemplate(
                                buildWorkspace,
                                request.flinkBaseImage(),
                                shadedJar.getAbsolutePath(),
                                extJarLibs);
                    }
                    File file = template.writeDockerfile();
                    log.info(
                        "Output flink dockerfile: {}, content: \n{}",
                        file.getAbsolutePath(),
                        template.offerDockerfileContent());
                    return Map.entry(file, template);
                });
        if (step4.isEmpty()) {
            throw getError().exception();
        }
        dockerfile = step4.get().getKey();
        dockerFileTemplate = step4.get().getValue();

        var dockerConf = request.dockerConfig();
        String baseImageTag = request.flinkBaseImage().trim();
        if (request.k8sNamespace().isEmpty() || request.clusterId().isEmpty()) {
            throw new IllegalArgumentException("k8sNamespace or clusterId cannot be empty");
        }
        String pushImageTag =
            compileTag(
                "streampark-flinkjob-" + request.k8sNamespace() + "-" + request.clusterId(),
                dockerConf.registerAddress(),
                dockerConf.imageNamespace());

        execStep(
            5,
            () -> {
                DockerClients.usingDockerClient(
                    dockerClient -> {
                        boolean imgExists =
                            dockerClient.listImagesCmd().exec().stream()
                                .anyMatch(
                                    image -> image.getRepoTags() != null
                                        && java.util.Arrays
                                            .stream(
                                                image
                                                    .getRepoTags())
                                            .anyMatch(
                                                tag -> tag.contains(
                                                    baseImageTag)));
                        if (imgExists) {
                            log.info(
                                "found local docker image {}, no need to pull from remote.",
                                baseImageTag);
                            return null;
                        }
                        HackPullImageCmd pullImageCmd =
                            (HackPullImageCmd) dockerClient.pullImageCmd(baseImageTag);
                        if (dockerConf.registerAddress() == null
                            || baseImageTag.startsWith(dockerConf.registerAddress())) {
                            pullImageCmd.withAuthConfig(dockerConf.toAuthConf());
                        }
                        try {
                            pullImageCmd
                                .start(
                                    DockerClients.watchDockerPullProcess(
                                        pullRsp -> {
                                            dockerProcess.getPull().update(pullRsp);
                                            DOCKER_EXEC_POOL.execute(
                                                () -> dockerProcessWatcher
                                                    .onDockerPullProgressChange(
                                                        dockerProcess
                                                            .getPull()
                                                            .snapshot()));
                                        }))
                                .awaitCompletion();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                        log.info(
                            "Already pulled docker image from remote register, imageTag={}",
                            baseImageTag);
                        return null;
                    },
                    err -> {
                        throw new RuntimeException(
                            "Pull docker image failed, imageTag=" + baseImageTag, err);
                    });
                return null;
            })
                .orElseThrow(() -> getError().exception());

        execStep(
            6,
            () -> {
                DockerClients.usingDockerClient(
                    dockerClient -> {
                        HackBuildImageCmd buildImageCmd =
                            (HackBuildImageCmd) dockerClient
                                .buildImageCmd()
                                .withBaseDirectory(new File(buildWorkspace))
                                .withDockerfile(dockerfile)
                                .withTags(Sets.newHashSet(pushImageTag));
                        String imageId =
                            buildImageCmd
                                .start(
                                    DockerClients.watchDockerBuildStep(
                                        buildStep -> {
                                            dockerProcess.getBuild().update(buildStep);
                                            DOCKER_EXEC_POOL.execute(
                                                () -> dockerProcessWatcher
                                                    .onDockerBuildProgressChange(
                                                        dockerProcess
                                                            .getBuild()
                                                            .snapshot()));
                                        }))
                                .awaitImageId();
                        log.info(
                            "Built docker image, imageId={}, imageTag={}",
                            imageId,
                            pushImageTag);
                        return null;
                    },
                    err -> {
                        throw new RuntimeException(
                            "Build docker image failed. tag=" + pushImageTag, err);
                    });
                return null;
            })
                .orElseThrow(() -> getError().exception());

        execStep(
            7,
            () -> {
                DockerClients.usingDockerClient(
                    dockerClient -> {
                        HackPushImageCmd pushCmd =
                            (HackPushImageCmd) dockerClient
                                .pushImageCmd(pushImageTag)
                                .withAuthConfig(dockerConf.toAuthConf());
                        try {
                            pushCmd.start(
                                DockerClients.watchDockerPushProcess(
                                    pushRsp -> {
                                        dockerProcess.getPush().update(pushRsp);
                                        DOCKER_EXEC_POOL.execute(
                                            () -> dockerProcessWatcher
                                                .onDockerPushProgressChange(
                                                    dockerProcess
                                                        .getPush()
                                                        .snapshot()));
                                    }))
                                .awaitCompletion();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                        log.info(
                            "Already pushed docker image, imageTag={}",
                            pushImageTag);
                        return null;
                    },
                    err -> {
                        throw new RuntimeException(
                            "Push docker image failed. tag=" + pushImageTag, err);
                    });
                return null;
            })
                .orElseThrow(() -> getError().exception());

        if (StringUtils.isBlank(request.ingressTemplate())) {
            skipStep(8);
        } else {
            execStep(
                8,
                () -> {
                    String ingressOutputPath =
                        IngressController.prepareIngressTemplateFiles(
                            buildWorkspace, request.ingressTemplate());
                    log.info("Export flink ingress: {}", ingressOutputPath);
                    return ingressOutputPath;
                })
                    .orElseThrow(() -> getError().exception());
        }

        return new DockerImageBuildResponse(
            buildWorkspace,
            pushImageTag,
            podTemplatePaths,
            dockerFileTemplate.innerMainJarPath());
    }

    private String compileTag(String tag, String registerAddress, String imageNamespace) {
        String tagName = tag.contains("/") ? tag : imageNamespace + "/" + tag;
        if (StringUtils.isNotBlank(registerAddress) && !tagName.startsWith(registerAddress)) {
            tagName = registerAddress + "/" + tagName;
        }
        return tagName.toLowerCase();
    }
}
