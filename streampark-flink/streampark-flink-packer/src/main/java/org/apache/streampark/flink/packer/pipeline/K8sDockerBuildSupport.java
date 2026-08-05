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

package org.apache.streampark.flink.packer.pipeline;

import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.flink.packer.docker.DockerConf;
import org.apache.streampark.flink.packer.docker.DockerUtils;

import org.apache.commons.lang3.StringUtils;

import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.core.command.HackBuildImageCmd;
import com.github.dockerjava.core.command.HackPullImageCmd;
import com.github.dockerjava.core.command.HackPushImageCmd;
import com.google.common.collect.Sets;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Shared Docker pull/build/push steps for Kubernetes application build pipelines. */
public final class K8sDockerBuildSupport {

    static final ExecutorService DOCKER_EXECUTOR =
        new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 5,
            Runtime.getRuntime().availableProcessors() * 10,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2048),
            ThreadUtils.threadFactory("streampark-docker-progress-watcher-executor"),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    private K8sDockerBuildSupport() {
    }

    public static String compileTag(String tag, String registerAddress, String imageNamespace) {
        String tagName = tag.contains("/") ? tag : imageNamespace + "/" + tag;
        if (StringUtils.isNotBlank(registerAddress) && !tagName.startsWith(registerAddress)) {
            tagName = registerAddress + "/" + tagName;
        }
        return tagName.toLowerCase();
    }

    public static void pullImage(
                                 DockerConf dockerConf,
                                 String baseImageTag,
                                 DockerResolveProgress dockerProcess,
                                 DockerProgressWatcher watcher,
                                 Consumer<String> logInfo,
                                 boolean checkLocalImageFirst) {
        DockerUtils.usingDockerClient(
            dockerClient -> {
                if (checkLocalImageFirst) {
                    boolean imgExists =
                        dockerClient.listImagesCmd().exec().stream()
                            .anyMatch(
                                image -> image.getRepoTags() != null
                                    && java.util.Arrays.stream(image.getRepoTags())
                                        .anyMatch(tag -> tag.contains(baseImageTag)));
                    if (imgExists) {
                        logInfo.accept(
                            "found local docker image "
                                + baseImageTag
                                + ", no need to pull from remote.");
                        return null;
                    }
                }
                HackPullImageCmd pullImageCmd =
                    resolvePullCommand(dockerClient, dockerConf, baseImageTag);
                awaitPull(pullImageCmd, dockerProcess, watcher);
                logInfo.accept(
                    "Already pulled docker image from remote register, imageTag=" + baseImageTag);
                return null;
            },
            err -> {
                throw new IllegalStateException(
                    "Pull docker image failed, imageTag=" + baseImageTag, err);
            });
    }

    public static void buildImage(
                                  String buildWorkspace,
                                  File dockerfile,
                                  String pushImageTag,
                                  DockerResolveProgress dockerProcess,
                                  DockerProgressWatcher watcher,
                                  Consumer<String> logInfo) {
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
                                        () -> watcher.onDockerBuildProgressChange(
                                            dockerProcess.getBuild().snapshot()));
                                }))
                        .awaitImageId();
                logInfo.accept(
                    "Built docker image, imageId=" + imageId + ", imageTag=" + pushImageTag);
                return null;
            },
            err -> {
                throw new IllegalStateException(
                    "Build docker image failed. tag=" + pushImageTag, err);
            });
    }

    public static void pushImage(
                                 DockerConf dockerConf,
                                 String pushImageTag,
                                 DockerResolveProgress dockerProcess,
                                 DockerProgressWatcher watcher,
                                 Consumer<String> logInfo) {
        DockerUtils.usingDockerClient(
            dockerClient -> {
                PushImageCmd pushCmd =
                    dockerClient.pushImageCmd(pushImageTag).withAuthConfig(dockerConf.toAuthConf());
                awaitPush((HackPushImageCmd) pushCmd, dockerProcess, watcher);
                logInfo.accept("Already pushed docker image, imageTag=" + pushImageTag);
                return null;
            },
            err -> {
                throw new IllegalStateException(
                    "Push docker image failed. tag=" + pushImageTag, err);
            });
    }

    private static HackPullImageCmd resolvePullCommand(
                                                       com.github.dockerjava.api.DockerClient dockerClient,
                                                       DockerConf dockerConf,
                                                       String baseImageTag) {
        if (dockerConf.registerAddress() != null
            && !baseImageTag.startsWith(dockerConf.registerAddress())) {
            return (HackPullImageCmd) dockerClient.pullImageCmd(baseImageTag);
        }
        return (HackPullImageCmd) dockerClient.pullImageCmd(baseImageTag)
            .withAuthConfig(dockerConf.toAuthConf());
    }

    private static void awaitPull(
                                  HackPullImageCmd pullImageCmd,
                                  DockerResolveProgress dockerProcess,
                                  DockerProgressWatcher watcher) {
        try {
            pullImageCmd
                .start(
                    DockerUtils.watchDockerPullProcess(
                        pullRsp -> {
                            dockerProcess.getPull().update(pullRsp);
                            DOCKER_EXECUTOR.submit(
                                () -> watcher.onDockerPullProgressChange(
                                    dockerProcess.getPull().snapshot()));
                        }))
                .awaitCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void awaitPush(
                                  HackPushImageCmd pushCmd,
                                  DockerResolveProgress dockerProcess,
                                  DockerProgressWatcher watcher) {
        try {
            pushCmd
                .start(
                    DockerUtils.watchDockerPushProcess(
                        pushRsp -> {
                            dockerProcess.getPush().update(pushRsp);
                            DOCKER_EXECUTOR.submit(
                                () -> watcher.onDockerPushProgressChange(
                                    dockerProcess.getPush().snapshot()));
                        }))
                .awaitCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
