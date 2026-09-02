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
import org.apache.streampark.flink.packer.maven.Artifact;
import org.apache.streampark.flink.packer.maven.DependencyInfo;
import org.apache.streampark.flink.packer.maven.MavenTool;
import org.apache.streampark.flink.packer.pipeline.BuildPipeline;
import org.apache.streampark.flink.packer.pipeline.FlinkYarnApplicationBuildRequest;
import org.apache.streampark.flink.packer.pipeline.PipelineTypeEnum;
import org.apache.streampark.flink.packer.pipeline.SimpleBuildResponse;
import org.apache.streampark.flink.packer.pipeline.YarnJarUploader;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
        boolean sqlMode =
            request.flinkJobType() == FlinkJobType.FLINK_SQL
                || request.flinkJobType() == FlinkJobType.PYFLINK;
        DependencyInfo dependencyInfo = request.dependencyInfo();
        if (request.flinkJobType() == FlinkJobType.FLINK_SQL) {
            Set<String> extJarLibs = new HashSet<>(dependencyInfo.extJarLibs());
            String appHome = System.getProperty("app.home", "/streampark");
            File snakeyaml = new File(appHome, "lib/snakeyaml-2.0.jar");
            if (snakeyaml.isFile()) {
                extJarLibs.add(snakeyaml.getAbsolutePath());
            } else {
                try {
                    MavenTool.resolveArtifacts(
                        Collections.singleton(new Artifact("org.yaml", "snakeyaml", "2.0")))
                        .stream()
                        .map(File::getAbsolutePath)
                        .forEach(extJarLibs::add);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to resolve snakeyaml for Flink SQL yarn application", e);
                }
            }
            dependencyInfo = new DependencyInfo(dependencyInfo.mavenArts(), extJarLibs);
            buildAndUploadSqlFatJar();
        }
        runYarnSqlBuildSteps(
            request.localWorkspace(),
            request.yarnProvidedPath(),
            sqlMode,
            dependencyInfo);
        return new SimpleBuildResponse();
    }

    private void buildAndUploadSqlFatJar() {
        try {
            String shadedJarOutputPath = request.getShadedJarPath(request.localAppHome());
            File jar =
                MavenTool.buildFatJar(
                    request.mainClass(),
                    request.providedLibs(),
                    shadedJarOutputPath);
            logInfo("Output shaded flink SQL jar: " + jar.getAbsolutePath());
            YarnJarUploader.uploadJarToHdfsOrLfs(
                FsOperator.hdfs(), jar.getAbsolutePath(), request.remoteAppHome());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build Flink SQL fat jar for yarn application", e);
        }
    }

    public static FlinkYarnApplicationBuildPipeline of(FlinkYarnApplicationBuildRequest request) {
        return new FlinkYarnApplicationBuildPipeline(request);
    }
}
