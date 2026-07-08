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

import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.common.fs.LfsOperator;
import org.apache.streampark.flink.packer.maven.MavenTool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlinkRemoteBuildPipeline extends BuildPipeline {

    private final FlinkRemotePerJobBuildRequest request;

    public FlinkRemoteBuildPipeline(FlinkRemotePerJobBuildRequest request) {
        this.request = request;
    }
    public static FlinkRemoteBuildPipeline of(FlinkRemotePerJobBuildRequest request) {
        return new FlinkRemoteBuildPipeline(request);
    }

    @Override
    public PipelineTypeEnum getPipeType() {
        return PipelineTypeEnum.FLINK_STANDALONE;
    }

    @Override
    protected BuildParam offerBuildParam() {
        return request;
    }

    @Override
    protected BuildResult buildProcess() throws Throwable {
        if (request.skipBuild()) {
            return new ShadedBuildResponse(request.workspace(), request.customFlinkUserJar());
        }
        execStep(1, () -> {
            LfsOperator.getInstance().mkCleanDirs(request.workspace());
            return null;
        })
            .orElseThrow(() -> getError().exception());
        File shadedJar = execStep(2, () -> {
            if (request.flinkJobType() == FlinkJobType.FLINK_SQL) {
                return MavenTool.buildFatJar(request.mainClass(), request.providedLibs(),
                    request.getShadedJarPath(request.workspace()));
            }
            return new File(request.customFlinkUserJar());
        }).orElseThrow(() -> getError().exception());
        List<String> mavenJars = execStep(3, () -> {
            if (request.flinkJobType() == FlinkJobType.PYFLINK) {
                List<String> paths = new ArrayList<>();
                MavenTool.resolveArtifacts(request.dependencyInfo().mavenArts())
                    .forEach(f -> paths.add(f.getAbsolutePath()));
                paths.addAll(request.dependencyInfo().extJarLibs());
                return paths;
            }
            return Collections.<String>emptyList();
        }).orElseThrow(() -> getError().exception());
        execStep(4, () -> {
            if (request.flinkJobType() == FlinkJobType.PYFLINK) {
                for (String jar : mavenJars) {
                    FsOperator lfs = FsOperator.lfs();
                    String lib = request.workspace() + "/lib";
                    lfs.mkdirsIfNotExists(lib);
                    File originFile = new File(jar);
                    if (originFile.isFile())
                        lfs.copy(originFile.getAbsolutePath(), lib);
                }
            }
            return null;
        }).orElseThrow(() -> getError().exception());
        return new ShadedBuildResponse(request.workspace(), shadedJar.getAbsolutePath());
    }
}
