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

import org.apache.streampark.common.conf.SparkVersion;
import org.apache.streampark.common.enums.SparkDeployMode;
import org.apache.streampark.common.enums.SparkJobType;
import org.apache.streampark.flink.packer.docker.DockerConf;
import org.apache.streampark.flink.packer.maven.DependencyInfo;
import org.apache.streampark.spark.kubernetes.model.SparkK8sPodTemplates;

public class SparkK8sApplicationBuildRequest implements BuildParam {

    private final String appName;
    private final String workspace;
    private final String mainClass;
    private final String mainJar;
    private final SparkDeployMode deployMode;
    private final SparkJobType jobType;
    private final SparkVersion sparkVersion;
    private final DependencyInfo dependencyInfo;
    private final String k8sNamespace;
    private final String sparkBaseImage;
    private final SparkK8sPodTemplates sparkPodTemplate;
    private final boolean integrateWithHadoop;
    private final DockerConf dockerConfig;

    public SparkK8sApplicationBuildRequest(
                                           String appName,
                                           String workspace,
                                           String mainClass,
                                           String mainJar,
                                           SparkDeployMode deployMode,
                                           SparkJobType jobType,
                                           SparkVersion sparkVersion,
                                           DependencyInfo dependencyInfo,
                                           String k8sNamespace,
                                           String sparkBaseImage,
                                           SparkK8sPodTemplates sparkPodTemplate,
                                           boolean integrateWithHadoop,
                                           DockerConf dockerConfig) {
        this.appName = appName;
        this.workspace = workspace;
        this.mainClass = mainClass;
        this.mainJar = mainJar;
        this.deployMode = deployMode;
        this.jobType = jobType;
        this.sparkVersion = sparkVersion;
        this.dependencyInfo = dependencyInfo;
        this.k8sNamespace = k8sNamespace;
        this.sparkBaseImage = sparkBaseImage;
        this.sparkPodTemplate = sparkPodTemplate;
        this.integrateWithHadoop = integrateWithHadoop;
        this.dockerConfig = dockerConfig;
    }

    @Override
    public String appName() {
        return appName;
    }

    @Override
    public String mainClass() {
        return mainClass;
    }

    public String workspace() {
        return workspace;
    }

    public String mainJar() {
        return mainJar;
    }

    public SparkDeployMode deployMode() {
        return deployMode;
    }

    public SparkJobType jobType() {
        return jobType;
    }

    public SparkVersion sparkVersion() {
        return sparkVersion;
    }

    public DependencyInfo dependencyInfo() {
        return dependencyInfo;
    }

    public String k8sNamespace() {
        return k8sNamespace;
    }

    public String sparkBaseImage() {
        return sparkBaseImage;
    }

    public SparkK8sPodTemplates sparkPodTemplate() {
        return sparkPodTemplate;
    }

    public boolean integrateWithHadoop() {
        return integrateWithHadoop;
    }

    public DockerConf dockerConfig() {
        return dockerConfig;
    }
}
