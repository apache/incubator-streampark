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
import org.apache.streampark.flink.packer.maven.DependencyInfo;

public class FlinkYarnApplicationBuildRequest implements FlinkBuildParam {

    private final String appName;
    private final String mainClass;
    private final String localAppHome;
    private final String localWorkspace;
    private final String yarnProvidedPath;
    private final String customFlinkUserJar;
    private final FlinkJobType flinkJobType;
    private final FlinkDeployMode deployMode;
    private final FlinkVersion flinkVersion;
    private final DependencyInfo dependencyInfo;

    public FlinkYarnApplicationBuildRequest(
                                            String appName,
                                            String mainClass,
                                            String localAppHome,
                                            String localWorkspace,
                                            String yarnProvidedPath,
                                            String customFlinkUserJar,
                                            FlinkJobType flinkJobType,
                                            FlinkDeployMode deployMode,
                                            FlinkVersion flinkVersion,
                                            DependencyInfo dependencyInfo) {
        this.appName = appName;
        this.mainClass = mainClass;
        this.localAppHome = localAppHome;
        this.localWorkspace = localWorkspace;
        this.yarnProvidedPath = yarnProvidedPath;
        this.customFlinkUserJar = customFlinkUserJar;
        this.flinkJobType = flinkJobType;
        this.deployMode = deployMode;
        this.flinkVersion = flinkVersion;
        this.dependencyInfo = dependencyInfo;
    }

    @Override
    public String appName() {
        return appName;
    }

    @Override
    public String mainClass() {
        return mainClass;
    }

    @Override
    public String workspace() {
        return localAppHome;
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
    public String customFlinkUserJar() {
        return customFlinkUserJar;
    }

    public String localAppHome() {
        return localAppHome;
    }

    public String localWorkspace() {
        return localWorkspace;
    }

    public String yarnProvidedPath() {
        return yarnProvidedPath;
    }

    public String remoteAppHome() {
        if (yarnProvidedPath.endsWith("/lib")) {
            return yarnProvidedPath.substring(0, yarnProvidedPath.length() - 4);
        }
        return yarnProvidedPath;
    }
}
