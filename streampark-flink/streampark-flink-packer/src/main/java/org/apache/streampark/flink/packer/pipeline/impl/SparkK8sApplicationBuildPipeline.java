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
import org.apache.streampark.flink.kubernetes.PodTemplateTool;
import org.apache.streampark.flink.packer.docker.DockerConf;
import org.apache.streampark.flink.packer.docker.SparkDockerfileTemplate;
import org.apache.streampark.flink.packer.docker.SparkDockerfileTemplateTrait;
import org.apache.streampark.flink.packer.docker.SparkHadoopDockerfileTemplate;
import org.apache.streampark.flink.packer.pipeline.DockerImageBuildResponse;
import org.apache.streampark.flink.packer.pipeline.K8sDockerBuildSupport;
import org.apache.streampark.flink.packer.pipeline.PipelineTypeEnum;
import org.apache.streampark.flink.packer.pipeline.SparkK8sApplicationBuildRequest;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Building pipeline for Spark kubernetes-native application mode */
public class SparkK8sApplicationBuildPipeline extends AbstractK8sApplicationBuildPipeline {

    private final SparkK8sApplicationBuildRequest request;

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
                });

        Map<String, String> podTemplatePaths =
            preparePodTemplateStep(
                buildWorkspace,
                2,
                request.sparkPodTemplate().isEmpty(),
                () -> PodTemplateTool.preparePodTemplateFiles(buildWorkspace, request.sparkPodTemplate())
                    .tmplFiles(),
                "spark");

        final String mainJarPath =
            execStep(
                3,
                () -> {
                    String mainJarName = Paths.get(request.mainJar()).getFileName().toString();
                    String path = buildWorkspace + "/" + mainJarName;
                    LfsOperator.copy(request.mainJar(), path);
                    logInfo("Prepared spark job jar: " + path);
                    return path;
                });
        final Set<String> extJarLibs = new HashSet<>();

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
                });
        File dockerfile = (File) dockerResult[0];
        SparkDockerfileTemplateTrait dockerFileTemplate = (SparkDockerfileTemplateTrait) dockerResult[1];

        DockerConf dockerConf = request.dockerConfig();
        String baseImageTag = request.sparkBaseImage().trim();
        if (request.k8sNamespace().isEmpty() || request.appName().isEmpty()) {
            throw new IllegalArgumentException("k8sNamespace or appName cannot be empty");
        }
        String pushImageTag =
            K8sDockerBuildSupport.compileTag(
                "streampark-sparkjob-" + request.k8sNamespace() + "-" + request.appName(),
                dockerConf.registerAddress(),
                dockerConf.imageNamespace());

        runDockerBuildSteps(buildWorkspace, dockerfile, dockerConf, baseImageTag, pushImageTag, false);

        return new DockerImageBuildResponse(
            buildWorkspace,
            pushImageTag,
            podTemplatePaths,
            dockerFileTemplate.innerMainJarPath());
    }

    public static SparkK8sApplicationBuildPipeline of(SparkK8sApplicationBuildRequest request) {
        return new SparkK8sApplicationBuildPipeline(request);
    }
}
