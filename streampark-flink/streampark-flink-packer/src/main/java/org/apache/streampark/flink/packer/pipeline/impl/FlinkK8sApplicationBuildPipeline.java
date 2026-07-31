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
import org.apache.streampark.flink.kubernetes.model.K8sPodTemplates;
import org.apache.streampark.flink.packer.docker.DockerConf;
import org.apache.streampark.flink.packer.docker.DockerUtils;
import org.apache.streampark.flink.packer.docker.FlinkDockerfileTemplate;
import org.apache.streampark.flink.packer.docker.FlinkDockerfileTemplateTrait;
import org.apache.streampark.flink.packer.docker.FlinkHadoopDockerfileTemplate;
import org.apache.streampark.flink.packer.maven.MavenTool;
import org.apache.streampark.flink.packer.pipeline.BuildPipeline;
import org.apache.streampark.flink.packer.pipeline.DockerBuildProgress;
import org.apache.streampark.flink.packer.pipeline.DockerImageBuildResponse;
import org.apache.streampark.flink.packer.pipeline.DockerProgressWatcher;
import org.apache.streampark.flink.packer.pipeline.DockerPullProgress;
import org.apache.streampark.flink.packer.pipeline.DockerPushProgress;
import org.apache.streampark.flink.packer.pipeline.DockerResolveProgress;
import org.apache.streampark.flink.packer.pipeline.FlinkK8sApplicationBuildRequest;
import org.apache.streampark.flink.packer.pipeline.PipelineTypeEnum;
import org.apache.streampark.flink.packer.pipeline.SilentDockerProgressWatcher;

import org.apache.commons.lang3.StringUtils;

import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.core.command.HackBuildImageCmd;
import com.github.dockerjava.core.command.HackPullImageCmd;
import com.github.dockerjava.core.command.HackPushImageCmd;
import com.google.common.collect.Sets;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Building pipeline for flink kubernetes-native application mode */
public class FlinkK8sApplicationBuildPipeline extends BuildPipeline {

    private static final ExecutorService DOCKER_EXECUTOR =
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

    private final DockerResolveProgress dockerProcess =
        new DockerResolveProgress(
            DockerPullProgress.empty(),
            DockerBuildProgress.empty(),
            DockerPushProgress.empty());

    public FlinkK8sApplicationBuildPipeline(FlinkK8sApplicationBuildRequest request) {
        this.request = request;
    }

    @Override
    public PipelineTypeEnum pipeType() {
        return PipelineTypeEnum.FLINK_NATIVE_K8S_APPLICATION;
    }

    @Override
    public FlinkK8sApplicationBuildRequest offerBuildParam() {
        return request;
    }

    public void registerDockerProgressWatcher(DockerProgressWatcher watcher) {
        this.dockerProcessWatcher = watcher;
    }

    @Override
    public DockerImageBuildResponse buildProcess() {
        String buildWorkspace =
            execStep(
                1,
                () -> {
                    String workspace =
                        request.workspace()
                            + "/"
                            + request.clusterId()
                            + "@"
                            + request.k8sNamespace();
                    LfsOperator.mkCleanDirs(workspace);
                    logInfo("Recreate building workspace: " + workspace);
                    return workspace;
                })
                    .orElseThrow(() -> {
                        throw pipelineException();
                    });

        Map<String, String> podTemplatePaths;
        K8sPodTemplates podTemplate = request.flinkPodTemplate();
        if (podTemplate.isEmpty()) {
            skipStep(2);
            podTemplatePaths = Collections.emptyMap();
        } else {
            podTemplatePaths =
                execStep(
                    2,
                    () -> {
                        Map<String, String> podTemplateFiles =
                            PodTemplateTool.preparePodTemplateFiles(buildWorkspace, podTemplate)
                                .tmplFiles();
                        logInfo(
                            "Export flink podTemplates: "
                                + String.join(",", podTemplateFiles.values()));
                        return podTemplateFiles;
                    })
                        .orElseThrow(() -> {
                            throw pipelineException();
                        });
        }

        final File shadedJar =
            execStep(
                3,
                () -> {
                    String shadedJarOutputPath = request.getShadedJarPath(buildWorkspace);
                    File jar =
                        MavenTool.buildFatJar(
                            request.mainClass(),
                            request.providedLibs(),
                            shadedJarOutputPath);
                    logInfo("Output shaded flink job jar: " + jar.getAbsolutePath());
                    return jar;
                })
                    .orElseThrow(
                        () -> {
                            throw pipelineException();
                        });
        final Set<String> extJarLibs = request.dependencyInfo().extJarLibs();

        File dockerfile;
        FlinkDockerfileTemplateTrait dockerFileTemplate;
        Object[] dockerResult =
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
                    File dockerFile = template.writeDockerfile();
                    logInfo(
                        "Output flink dockerfile: "
                            + dockerFile.getAbsolutePath()
                            + ", content: \n"
                            + template.offerDockerfileContent());
                    return new Object[]{dockerFile, template};
                })
                    .orElseThrow(() -> {
                        throw pipelineException();
                    });
        dockerfile = (File) dockerResult[0];
        dockerFileTemplate = (FlinkDockerfileTemplateTrait) dockerResult[1];

        DockerConf dockerConf = request.dockerConfig();
        String baseImageTag = request.flinkBaseImage().trim();
        if (request.k8sNamespace().isEmpty() || request.clusterId().isEmpty()) {
            throw new IllegalArgumentException("k8sNamespace or clusterId cannot be empty");
        }
        String expectedImageTag =
            "streampark-flinkjob-" + request.k8sNamespace() + "-" + request.clusterId();
        String pushImageTag =
            compileTag(
                expectedImageTag, dockerConf.registerAddress(), dockerConf.imageNamespace());

        execStep(
            5,
            () -> {
                DockerUtils.usingDockerClient(
                    dockerClient -> {
                        boolean imgExists =
                            dockerClient.listImagesCmd().exec().stream()
                                .anyMatch(
                                    image -> image.getRepoTags() != null
                                        && java.util.Arrays.stream(image.getRepoTags())
                                            .anyMatch(tag -> tag.contains(baseImageTag)));
                        if (imgExists) {
                            logInfo(
                                "found local docker image "
                                    + baseImageTag
                                    + ", no need to pull from remote.");
                        } else {
                            HackPullImageCmd pullImageCmd;
                            if (dockerConf.registerAddress() != null
                                && !baseImageTag.startsWith(dockerConf.registerAddress())) {
                                pullImageCmd =
                                    (HackPullImageCmd) dockerClient.pullImageCmd(baseImageTag);
                            } else {
                                pullImageCmd =
                                    (HackPullImageCmd) dockerClient
                                        .pullImageCmd(baseImageTag)
                                        .withAuthConfig(dockerConf.toAuthConf());
                            }
                            try {
                                pullImageCmd
                                    .start(
                                        DockerUtils.watchDockerPullProcess(
                                            pullRsp -> {
                                                dockerProcess.getPull().update(pullRsp);
                                                DOCKER_EXECUTOR.submit(
                                                    () -> dockerProcessWatcher
                                                        .onDockerPullProgressChange(
                                                            dockerProcess.getPull().snapshot()));
                                            }))
                                    .awaitCompletion();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(e);
                            }
                            logInfo(
                                "Already pulled docker image from remote register, imageTag="
                                    + baseImageTag);
                        }
                        return null;
                    },
                    err -> {
                        throw new RuntimeException(
                            "Pull docker image failed, imageTag=" + baseImageTag, err);
                    });
                return null;
            })
                .orElseThrow(() -> {
                    throw pipelineException();
                });

        execStep(
            6,
            () -> {
                DockerUtils.usingDockerClient(
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
                                    DockerUtils.watchDockerBuildStep(
                                        buildStep -> {
                                            dockerProcess.getBuild().update(buildStep);
                                            DOCKER_EXECUTOR.submit(
                                                () -> dockerProcessWatcher
                                                    .onDockerBuildProgressChange(
                                                        dockerProcess.getBuild().snapshot()));
                                        }))
                                .awaitImageId();
                        logInfo(
                            "Built docker image, imageId="
                                + imageId
                                + ", imageTag="
                                + pushImageTag);
                        return null;
                    },
                    err -> {
                        throw new RuntimeException(
                            "Build docker image failed. tag=" + pushImageTag, err);
                    });
                return null;
            })
                .orElseThrow(() -> {
                    throw pipelineException();
                });

        execStep(
            7,
            () -> {
                DockerUtils.usingDockerClient(
                    dockerClient -> {
                        PushImageCmd pushCmd =
                            dockerClient
                                .pushImageCmd(pushImageTag)
                                .withAuthConfig(dockerConf.toAuthConf());
                        try {
                            ((HackPushImageCmd) pushCmd)
                                .start(
                                    DockerUtils.watchDockerPushProcess(
                                        pushRsp -> {
                                            dockerProcess.getPush().update(pushRsp);
                                            DOCKER_EXECUTOR.submit(
                                                () -> dockerProcessWatcher.onDockerPushProgressChange(
                                                    dockerProcess.getPush().snapshot()));
                                        }))
                                .awaitCompletion();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                        logInfo("Already pushed docker image, imageTag=" + pushImageTag);
                        return null;
                    },
                    err -> {
                        throw new RuntimeException(
                            "Push docker image failed. tag=" + pushImageTag, err);
                    });
                return null;
            })
                .orElseThrow(() -> {
                    throw pipelineException();
                });

        if (StringUtils.isBlank(request.ingressTemplate())) {
            skipStep(8);
        } else {
            execStep(
                8,
                () -> {
                    String ingressOutputPath =
                        IngressController.prepareIngressTemplateFiles(
                            buildWorkspace, request.ingressTemplate());
                    logInfo("Export flink ingress: " + ingressOutputPath);
                    return ingressOutputPath;
                })
                    .orElseThrow(() -> {
                        throw pipelineException();
                    });
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

    public static FlinkK8sApplicationBuildPipeline of(FlinkK8sApplicationBuildRequest request) {
        return new FlinkK8sApplicationBuildPipeline(request);
    }
}
