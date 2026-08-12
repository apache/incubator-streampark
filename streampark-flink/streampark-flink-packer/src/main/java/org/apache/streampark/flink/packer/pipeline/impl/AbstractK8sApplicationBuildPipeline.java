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

import org.apache.streampark.flink.packer.docker.DockerConf;
import org.apache.streampark.flink.packer.pipeline.BuildPipeline;
import org.apache.streampark.flink.packer.pipeline.DockerBuildProgress;
import org.apache.streampark.flink.packer.pipeline.DockerProgressWatcher;
import org.apache.streampark.flink.packer.pipeline.DockerPullProgress;
import org.apache.streampark.flink.packer.pipeline.DockerPushProgress;
import org.apache.streampark.flink.packer.pipeline.DockerResolveProgress;
import org.apache.streampark.flink.packer.pipeline.K8sDockerBuildSupport;
import org.apache.streampark.flink.packer.pipeline.SilentDockerProgressWatcher;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

/** Shared docker build steps for Kubernetes application build pipelines. */
abstract class AbstractK8sApplicationBuildPipeline extends BuildPipeline {

    protected DockerProgressWatcher dockerProcessWatcher = new SilentDockerProgressWatcher();

    protected final DockerResolveProgress dockerProcess =
        new DockerResolveProgress(
            DockerPullProgress.empty(),
            DockerBuildProgress.empty(),
            DockerPushProgress.empty());

    public void registerDockerProgressWatcher(DockerProgressWatcher watcher) {
        this.dockerProcessWatcher = watcher;
    }

    protected void runDockerBuildSteps(
                                       String buildWorkspace,
                                       File dockerfile,
                                       DockerConf dockerConf,
                                       String baseImageTag,
                                       String pushImageTag,
                                       boolean checkLocalImageFirst) {
        execStep(
            5,
            () -> {
                K8sDockerBuildSupport.pullImage(
                    dockerConf,
                    baseImageTag,
                    dockerProcess,
                    dockerProcessWatcher,
                    this::logInfo,
                    checkLocalImageFirst);
                return null;
            });

        execStep(
            6,
            () -> {
                K8sDockerBuildSupport.buildImage(
                    buildWorkspace,
                    dockerfile,
                    pushImageTag,
                    dockerProcess,
                    dockerProcessWatcher,
                    this::logInfo);
                return null;
            });

        execStep(
            7,
            () -> {
                K8sDockerBuildSupport.pushImage(
                    dockerConf, pushImageTag, dockerProcess, dockerProcessWatcher, this::logInfo);
                return null;
            });
    }

    protected Map<String, String> preparePodTemplateStep(
                                                         String buildWorkspace,
                                                         int stepId,
                                                         boolean empty,
                                                         Callable<Map<String, String>> exporter,
                                                         String engineLabel) {
        if (empty) {
            skipStep(stepId);
            return Collections.emptyMap();
        }
        return execStep(
            stepId,
            () -> {
                Map<String, String> podTemplateFiles = exporter.call();
                logInfo(
                    "Export "
                        + engineLabel
                        + " podTemplates: "
                        + String.join(",", podTemplateFiles.values()));
                return podTemplateFiles;
            });
    }
}
