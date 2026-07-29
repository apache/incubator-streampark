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

import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.enums.SparkJobType;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.common.fs.HdfsOperator;
import org.apache.streampark.common.fs.LfsOperator;
import org.apache.streampark.common.util.AutoCloseUtils;
import org.apache.streampark.flink.packer.maven.MavenTool;
import org.apache.streampark.flink.packer.pipeline.BuildPipeline;
import org.apache.streampark.flink.packer.pipeline.PipelineTypeEnum;
import org.apache.streampark.flink.packer.pipeline.SimpleBuildResponse;
import org.apache.streampark.flink.packer.pipeline.SparkYarnBuildRequest;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Building pipeline for spark yarn application mode */
public class SparkYarnBuildPipeline extends BuildPipeline {

    private final SparkYarnBuildRequest request;

    public SparkYarnBuildPipeline(SparkYarnBuildRequest request) {
        this.request = request;
    }

    @Override
    public PipelineTypeEnum pipeType() {
        return PipelineTypeEnum.SPARK_CLUSTER;
    }

    @Override
    public SparkYarnBuildRequest offerBuildParam() {
        return request;
    }

    @Override
    public SimpleBuildResponse buildProcess() {
        execStep(
            1,
            () -> {
                if (request.jobType() == SparkJobType.SPARK_SQL) {
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
                    if (request.jobType() == SparkJobType.SPARK_SQL) {
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
                    uploadJarToHdfsOrLfs(FsOperator.lfs(), jar, request.localWorkspace());
                    uploadJarToHdfsOrLfs(FsOperator.hdfs(), jar, request.yarnProvidedPath());
                }
                return null;
            })
                .orElseThrow(() -> {
                    throw pipelineException();
                });

        return new SimpleBuildResponse();
    }

    private void uploadJarToHdfsOrLfs(FsOperator fsOperator, String origin, String target) throws IOException {
        File originFile = new File(origin);
        if (!fsOperator.exists(target)) {
            fsOperator.mkdirs(target);
        }
        if (originFile.isFile()) {
            if (fsOperator == FsOperator.lfs()) {
                fsOperator.copy(originFile.getAbsolutePath(), target);
            } else {
                String uploadFile = Workspace.remote().APP_UPLOADS() + "/" + originFile.getName();
                if (fsOperator.exists(uploadFile)) {
                    AutoCloseUtils.using(
                        new FileInputStream(originFile),
                        inputStream -> {
                            try {
                                if (!DigestUtils.md5Hex(inputStream)
                                    .equals(fsOperator.fileMd5(uploadFile))) {
                                    fsOperator.upload(originFile.getAbsolutePath(), uploadFile);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return null;
                        });
                } else {
                    fsOperator.upload(originFile.getAbsolutePath(), uploadFile);
                }
                fsOperator.copy(uploadFile, target);
            }
        } else if (fsOperator == FsOperator.hdfs()) {
            fsOperator.upload(originFile.getAbsolutePath(), target);
        }
    }

    public static SparkYarnBuildPipeline of(SparkYarnBuildRequest request) {
        return new SparkYarnBuildPipeline(request);
    }
}
