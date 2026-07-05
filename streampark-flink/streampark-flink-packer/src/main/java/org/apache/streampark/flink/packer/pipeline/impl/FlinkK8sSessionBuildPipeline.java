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
import org.apache.streampark.flink.packer.pipeline.*;

import java.io.File;

public class FlinkK8sSessionBuildPipeline extends BuildPipeline {
    private final FlinkK8sSessionBuildRequest request;
    public FlinkK8sSessionBuildPipeline(FlinkK8sSessionBuildRequest request) { this.request = request; }
    public static FlinkK8sSessionBuildPipeline of(FlinkK8sSessionBuildRequest request) { return new FlinkK8sSessionBuildPipeline(request); }
    @Override public PipelineTypeEnum getPipeType() { return PipelineTypeEnum.FLINK_NATIVE_K8S_SESSION; }
    @Override protected BuildParam offerBuildParam() { return request; }
    @Override protected BuildResult buildProcess() throws Throwable {
        String buildWorkspace = execStep(1, () -> {
            String ws = request.workspace() + "/" + request.clusterId() + "@" + request.k8sNamespace();
            LfsOperator.getInstance().mkCleanDirs(ws);
            return ws;
        }).orElseThrow(() -> getError().exception());
        File shadedJar = execStep(2, () -> MavenTool.buildFatJar(request.mainClass(), request.providedLibs(), request.getShadedJarPath(buildWorkspace)))
            .orElseThrow(() -> getError().exception());
        return new ShadedBuildResponse(buildWorkspace, shadedJar.getAbsolutePath());
    }
}
