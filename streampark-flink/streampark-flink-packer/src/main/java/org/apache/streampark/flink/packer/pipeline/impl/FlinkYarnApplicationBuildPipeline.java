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
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.common.fs.HdfsOperator;
import org.apache.streampark.common.fs.LfsOperator;
import org.apache.streampark.flink.packer.maven.MavenTool;
import org.apache.streampark.flink.packer.pipeline.*;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlinkYarnApplicationBuildPipeline extends BuildPipeline {
    private final FlinkYarnApplicationBuildRequest request;
    public FlinkYarnApplicationBuildPipeline(FlinkYarnApplicationBuildRequest request) { this.request = request; }
    public static FlinkYarnApplicationBuildPipeline of(FlinkYarnApplicationBuildRequest request) { return new FlinkYarnApplicationBuildPipeline(request); }
    @Override public PipelineTypeEnum getPipeType() { return PipelineTypeEnum.FLINK_YARN_APPLICATION; }
    @Override protected BuildParam offerBuildParam() { return request; }
    @Override protected BuildResult buildProcess() throws Throwable {
        execStep(1, () -> {
            if (request.flinkJobType() == FlinkJobType.FLINK_SQL || request.flinkJobType() == FlinkJobType.PYFLINK) {
                LfsOperator.getInstance().mkCleanDirs(request.localWorkspace());
                HdfsOperator.getInstance().mkCleanDirs(request.yarnProvidedPath());
            }
            return null;
        }).orElseThrow(() -> getError().exception());
        List<String> mavenJars = execStep(2, () -> {
            if (request.flinkJobType() == FlinkJobType.FLINK_SQL || request.flinkJobType() == FlinkJobType.PYFLINK) {
                List<String> paths = new ArrayList<>();
                MavenTool.resolveArtifacts(request.dependencyInfo().mavenArts()).forEach(f -> paths.add(f.getAbsolutePath()));
                paths.addAll(request.dependencyInfo().extJarLibs());
                return paths;
            }
            return Collections.<String>emptyList();
        }).orElseThrow(() -> getError().exception());
        execStep(3, () -> {
            for (String jar : mavenJars) {
                uploadJarToHdfsOrLfs(FsOperator.lfs(), jar, request.localWorkspace());
                uploadJarToHdfsOrLfs(FsOperator.hdfs(), jar, request.yarnProvidedPath());
            }
            return null;
        }).orElseThrow(() -> getError().exception());
        return new SimpleBuildResponse();
    }

    @SuppressWarnings("java:S4790")
    private void uploadJarToHdfsOrLfs(FsOperator fsOperator, String origin, String target) throws Exception {
        File originFile = new File(origin);
        if (!fsOperator.exists(target)) fsOperator.mkdirs(target);
        if (originFile.isFile()) {
            if (fsOperator == FsOperator.lfs()) {
                fsOperator.copy(originFile.getAbsolutePath(), target);
            } else {
                String uploadFile = Workspace.remote().APP_UPLOADS() + "/" + originFile.getName();
                if (fsOperator.exists(uploadFile)) {
                    try (FileInputStream in = new FileInputStream(originFile)) {
                        if (!DigestUtils.md5Hex(in).equals(fsOperator.fileMd5(uploadFile))) {
                            fsOperator.upload(originFile.getAbsolutePath(), uploadFile);
                        }
                    }
                } else {
                    fsOperator.upload(originFile.getAbsolutePath(), uploadFile);
                }
                fsOperator.copy(uploadFile, target);
            }
        } else if (fsOperator == FsOperator.hdfs()) {
            fsOperator.upload(originFile.getAbsolutePath(), target);
        }
    }
}
