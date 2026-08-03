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

public class FlinkRemotePerJobBuildRequest implements FlinkBuildParam {

    private final String appName;
    private final String workspace;
    private final String mainClass;
    private final String customFlinkUserJar;
    private final boolean skipBuild;
    private final FlinkDeployMode deployMode;
    private final FlinkJobType flinkJobType;
    private final FlinkVersion flinkVersion;
    private final DependencyInfo dependencyInfo;

    public FlinkRemotePerJobBuildRequest(
                                         String appName,
                                         String workspace,
                                         String mainClass,
                                         String customFlinkUserJar,
                                         boolean skipBuild,
                                         FlinkDeployMode deployMode,
                                         FlinkJobType flinkJobType,
                                         FlinkVersion flinkVersion,
                                         DependencyInfo dependencyInfo) {
        this.appName = appName;
        this.workspace = workspace;
        this.mainClass = mainClass;
        this.customFlinkUserJar = customFlinkUserJar;
        this.skipBuild = skipBuild;
        this.deployMode = deployMode;
        this.flinkJobType = flinkJobType;
        this.flinkVersion = flinkVersion;
        this.dependencyInfo = dependencyInfo;
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

    public boolean skipBuild() {
        return skipBuild;
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
}
