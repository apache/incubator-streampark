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
import org.apache.streampark.common.fs.HdfsOperator;
import org.apache.streampark.common.fs.LfsOperator;
import org.apache.streampark.flink.packer.maven.MavenTool;
import org.apache.streampark.flink.packer.pipeline.BuildPipeline;
import org.apache.streampark.flink.packer.pipeline.FlinkYarnApplicationBuildRequest;
import org.apache.streampark.flink.packer.pipeline.PipelineTypeEnum;
import org.apache.streampark.flink.packer.pipeline.SimpleBuildResponse;
import org.apache.streampark.flink.packer.pipeline.YarnJarUploader;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Building pipeline for flink yarn application mode */
public class FlinkYarnApplicationBuildPipeline extends BuildPipeline {

    private final FlinkYarnApplicationBuildRequest request;

    public FlinkYarnApplicationBuildPipeline(FlinkYarnApplicationBuildRequest request) {
        this.request = request;
    }

    @Override
    public PipelineTypeEnum pipeType() {
        return PipelineTypeEnum.FLINK_YARN_APPLICATION;
    }

    @Override
    public FlinkYarnApplicationBuildRequest offerBuildParam() {
        return request;
    }

    @Override
    public SimpleBuildResponse buildProcess() {
        execStep(
            1,
            () -> {
                if (request.flinkJobType() == FlinkJobType.FLINK_SQL
                    || request.flinkJobType() == FlinkJobType.PYFLINK) {
                    LfsOperator.mkCleanDirs(request.localWorkspace());
                    HdfsOperator.mkCleanDirs(request.yarnProvidedPath());
                }
                logInfo("Recreate building workspace: " + request.yarnProvidedPath());
                return null;
            })
                .orElseThrow(() -> {
                    throw pipelineException();
                });

        List<String> mavenJars =
            execStep(
                2,
                () -> {
                    if (request.flinkJobType() == FlinkJobType.FLINK_SQL
                        || request.flinkJobType() == FlinkJobType.PYFLINK) {
                        List<File> mavenArts =
                            MavenTool.resolveArtifacts(request.dependencyInfo().mavenArts());
                        List<String> paths =
                            mavenArts.stream()
                                .map(File::getAbsolutePath)
                                .collect(Collectors.toList());
                        paths.addAll(request.dependencyInfo().extJarLibs());
                        return paths;
                    }
                    return Collections.<String>emptyList();
                })
                    .orElseThrow(() -> {
                        throw pipelineException();
                    });

        execStep(
            3,
            () -> {
                for (String jar : mavenJars) {
                    YarnJarUploader.uploadJarToHdfsOrLfs(FsOperator.lfs(), jar, request.localWorkspace());
                    YarnJarUploader.uploadJarToHdfsOrLfs(FsOperator.hdfs(), jar, request.yarnProvidedPath());
                }
                return null;
            })
                .orElseThrow(() -> {
                    throw pipelineException();
                });

        return new SimpleBuildResponse();
    }

    public static FlinkYarnApplicationBuildPipeline of(FlinkYarnApplicationBuildRequest request) {
        return new FlinkYarnApplicationBuildPipeline(request);
    }
}
