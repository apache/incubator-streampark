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
import org.apache.streampark.flink.packer.docker.DockerConf;
import org.apache.streampark.flink.packer.docker.DockerUtils;
import org.apache.streampark.flink.packer.docker.SparkDockerfileTemplate;
import org.apache.streampark.flink.packer.docker.SparkDockerfileTemplateTrait;
import org.apache.streampark.flink.packer.docker.SparkHadoopDockerfileTemplate;
import org.apache.streampark.flink.packer.pipeline.BuildPipeline;
import org.apache.streampark.flink.packer.pipeline.DockerBuildProgress;
import org.apache.streampark.flink.packer.pipeline.DockerImageBuildResponse;
import org.apache.streampark.flink.packer.pipeline.DockerProgressWatcher;
import org.apache.streampark.flink.packer.pipeline.DockerPullProgress;
import org.apache.streampark.flink.packer.pipeline.DockerPushProgress;
import org.apache.streampark.flink.packer.pipeline.DockerResolveProgress;
import org.apache.streampark.flink.packer.pipeline.PipelineTypeEnum;
import org.apache.streampark.flink.packer.pipeline.SilentDockerProgressWatcher;
import org.apache.streampark.flink.packer.pipeline.SparkK8sApplicationBuildRequest;
import org.apache.streampark.spark.kubernetes.model.SparkK8sPodTemplates;

import org.apache.commons.lang3.StringUtils;

import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.core.command.HackBuildImageCmd;
import com.github.dockerjava.core.command.HackPullImageCmd;
import com.github.dockerjava.core.command.HackPushImageCmd;
import com.google.common.collect.Sets;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import scala.collection.JavaConverters;

/** Building pipeline for Spark kubernetes-native application mode */
public class SparkK8sApplicationBuildPipeline extends BuildPipeline {

    private static final ExecutorService DOCKER_EXECUTOR =
        new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 5,
            Runtime.getRuntime().availableProcessors() * 10,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2048),
            ThreadUtils.threadFactory("streampark-docker-progress-watcher-executor"),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    private final SparkK8sApplicationBuildRequest request;

    private DockerProgressWatcher dockerProcessWatcher = new SilentDockerProgressWatcher();

    private final DockerResolveProgress dockerProcess =
        new DockerResolveProgress(
            DockerPullProgress.empty(),
            DockerBuildProgress.empty(),
            DockerPushProgress.empty());

    public SparkK8sApplicationBuildPipeline(SparkK8sApplicationBuildRequest request) {
        this.request = request;
    }

    @Override
    public PipelineTypeEnum pipeType() {
        return PipelineTypeEnum.SPARK_NATIVE_K8S_APPLICATION;
    }

    @Override
    public SparkK8sApplicationBuildRequest offerBuildParam() {
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
                    String workspace = request.workspace() + "/" + request.k8sNamespace();
                    LfsOperator.mkCleanDirs(workspace);
                    logInfo("Recreate building workspace: " + workspace);
                    return workspace;
                })
                    .orElseThrow(() -> {
                        throw pipelineException();
                    });

        Map<String, String> podTemplatePaths;
        SparkK8sPodTemplates podTemplate = request.sparkPodTemplate();
        if (podTemplate.isEmpty()) {
            skipStep(2);
            podTemplatePaths = Collections.emptyMap();
        } else {
            podTemplatePaths =
                execStep(
                    2,
                    () -> {
                        Map<String, String> podTemplateFiles =
                            JavaConverters.mapAsJavaMap(
                                PodTemplateTool.preparePodTemplateFiles(
                                    buildWorkspace, podTemplate)
                                    .tmplFiles());
                        logInfo(
                            "Export spark podTemplates: "
                                + String.join(",", podTemplateFiles.values()));
                        return podTemplateFiles;
                    })
                        .orElseThrow(() -> {
                            throw pipelineException();
                        });
        }

        final String mainJarPath =
            execStep(
                3,
                () -> {
                    String mainJarName =
                        Paths.get(request.mainJar()).getFileName().toString();
                    String path = buildWorkspace + "/" + mainJarName;
                    LfsOperator.copy(request.mainJar(), path);
                    logInfo("Prepared spark job jar: " + path);
                    return path;
                })
                    .orElseThrow(
                        () -> {
                            throw pipelineException();
                        });
        final Set<String> extJarLibs = new HashSet<>();

        File dockerfile;
        SparkDockerfileTemplateTrait dockerFileTemplate;
        Object[] dockerResult =
            execStep(
                4,
                () -> {
                    SparkDockerfileTemplateTrait template;
                    if (request.integrateWithHadoop()) {
                        template =
                            SparkHadoopDockerfileTemplate.fromSystemHadoopConf(
                                buildWorkspace,
                                request.sparkBaseImage(),
                                mainJarPath,
                                extJarLibs);
                    } else {
                        template =
                            new SparkDockerfileTemplate(
                                buildWorkspace,
                                request.sparkBaseImage(),
                                mainJarPath,
                                extJarLibs);
                    }
                    File dockerFile = template.writeDockerfile();
                    logInfo(
                        "Output spark dockerfile: "
                            + dockerFile.getAbsolutePath()
                            + ", content: \n"
                            + template.offerDockerfileContent());
                    return new Object[]{dockerFile, template};
                })
                    .orElseThrow(() -> {
                        throw pipelineException();
                    });
        dockerfile = (File) dockerResult[0];
        dockerFileTemplate = (SparkDockerfileTemplateTrait) dockerResult[1];

        DockerConf dockerConf = request.dockerConfig();
        String baseImageTag = request.sparkBaseImage().trim();
        if (request.k8sNamespace().isEmpty() || request.appName().isEmpty()) {
            throw new IllegalArgumentException("k8sNamespace or appName cannot be empty");
        }
        String expectedImageTag =
            "streampark-sparkjob-" + request.k8sNamespace() + "-" + request.appName();
        String pushImageTag =
            compileTag(
                expectedImageTag, dockerConf.registerAddress(), dockerConf.imageNamespace());

        execStep(
            5,
            () -> {
                DockerUtils.usingDockerClient(
                    dockerClient -> {
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
                                                () -> dockerProcessWatcher.onDockerPullProgressChange(
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

    public static SparkK8sApplicationBuildPipeline of(SparkK8sApplicationBuildRequest request) {
        return new SparkK8sApplicationBuildPipeline(request);
    }
}
