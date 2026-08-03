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
import org.apache.streampark.flink.packer.maven.MavenTool;
import org.apache.streampark.flink.packer.pipeline.BuildPipeline;
import org.apache.streampark.flink.packer.pipeline.FlinkK8sSessionBuildRequest;
import org.apache.streampark.flink.packer.pipeline.PipelineTypeEnum;
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse;

import java.io.File;

/** Building pipeline for flink kubernetes-native session mode */
public class FlinkK8sSessionBuildPipeline extends BuildPipeline {

    private final FlinkK8sSessionBuildRequest request;

    public FlinkK8sSessionBuildPipeline(FlinkK8sSessionBuildRequest request) {
        this.request = request;
    }

    @Override
    public PipelineTypeEnum pipeType() {
        return PipelineTypeEnum.FLINK_NATIVE_K8S_SESSION;
    }

    @Override
    public FlinkK8sSessionBuildRequest offerBuildParam() {
        return request;
    }

    @Override
    public ShadedBuildResponse buildProcess() {
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

        File shadedJar =
            execStep(
                2,
                () -> {
                    File output =
                        MavenTool.buildFatJar(
                            request.mainClass(),
                            request.providedLibs(),
                            request.getShadedJarPath(buildWorkspace));
                    logInfo("Output shaded flink job jar: " + output.getAbsolutePath());
                    return output;
                })
                    .orElseThrow(() -> {
                        throw pipelineException();
                    });

        return new ShadedBuildResponse(buildWorkspace, shadedJar.getAbsolutePath());
    }

    public static FlinkK8sSessionBuildPipeline of(FlinkK8sSessionBuildRequest request) {
        return new FlinkK8sSessionBuildPipeline(request);
    }
}
