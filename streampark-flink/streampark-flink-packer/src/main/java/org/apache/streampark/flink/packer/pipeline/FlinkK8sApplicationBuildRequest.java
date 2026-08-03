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

import org.apache.streampark.common.conf.FlinkVersion;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.flink.kubernetes.model.K8sPodTemplates;
import org.apache.streampark.flink.packer.docker.DockerConf;
import org.apache.streampark.flink.packer.maven.DependencyInfo;

public class FlinkK8sApplicationBuildRequest implements FlinkK8sBuildParam {

    private final String appName;
    private final String workspace;
    private final String mainClass;
    private final String customFlinkUserJar;
    private final FlinkDeployMode deployMode;
    private final FlinkJobType flinkJobType;
    private final FlinkVersion flinkVersion;
    private final DependencyInfo dependencyInfo;
    private final String clusterId;
    private final String k8sNamespace;
    private final String flinkBaseImage;
    private final K8sPodTemplates flinkPodTemplate;
    private final boolean integrateWithHadoop;
    private final DockerConf dockerConfig;
    private final String ingressTemplate;

    public FlinkK8sApplicationBuildRequest(
                                           String appName,
                                           String workspace,
                                           String mainClass,
                                           String customFlinkUserJar,
                                           FlinkDeployMode deployMode,
                                           FlinkJobType flinkJobType,
                                           FlinkVersion flinkVersion,
                                           DependencyInfo dependencyInfo,
                                           String clusterId,
                                           String k8sNamespace,
                                           String flinkBaseImage,
                                           K8sPodTemplates flinkPodTemplate,
                                           boolean integrateWithHadoop,
                                           DockerConf dockerConfig,
                                           String ingressTemplate) {
        this.appName = appName;
        this.workspace = workspace;
        this.mainClass = mainClass;
        this.customFlinkUserJar = customFlinkUserJar;
        this.deployMode = deployMode;
        this.flinkJobType = flinkJobType;
        this.flinkVersion = flinkVersion;
        this.dependencyInfo = dependencyInfo;
        this.clusterId = clusterId;
        this.k8sNamespace = k8sNamespace;
        this.flinkBaseImage = flinkBaseImage;
        this.flinkPodTemplate = flinkPodTemplate;
        this.integrateWithHadoop = integrateWithHadoop;
        this.dockerConfig = dockerConfig;
        this.ingressTemplate = ingressTemplate;
    }

    @Override
    public String appName() {
        return appName;
    }

    @Override
    public String workspace() {
        return workspace;
    }

    @Override
    public String mainClass() {
        return mainClass;
    }

    @Override
    public String customFlinkUserJar() {
        return customFlinkUserJar;
    }

    @Override
    public FlinkDeployMode deployMode() {
        return deployMode;
    }

    @Override
    public FlinkJobType flinkJobType() {
        return flinkJobType;
    }

    @Override
    public FlinkVersion flinkVersion() {
        return flinkVersion;
    }

    @Override
    public DependencyInfo dependencyInfo() {
        return dependencyInfo;
    }

    @Override
    public String clusterId() {
        return clusterId;
    }

    @Override
    public String k8sNamespace() {
        return k8sNamespace;
    }

    public String flinkBaseImage() {
        return flinkBaseImage;
    }

    public K8sPodTemplates flinkPodTemplate() {
        return flinkPodTemplate;
    }

    public boolean integrateWithHadoop() {
        return integrateWithHadoop;
    }

    public DockerConf dockerConfig() {
        return dockerConfig;
    }

    public String ingressTemplate() {
        return ingressTemplate;
    }
}
