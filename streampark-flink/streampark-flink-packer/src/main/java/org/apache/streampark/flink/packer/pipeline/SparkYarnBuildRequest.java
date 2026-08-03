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

import org.apache.streampark.common.enums.SparkDeployMode;
import org.apache.streampark.common.enums.SparkJobType;
import org.apache.streampark.flink.packer.maven.DependencyInfo;

public class SparkYarnBuildRequest implements BuildParam {

    private final String appName;
    private final String mainClass;
    private final String localWorkspace;
    private final String yarnProvidedPath;
    private final SparkJobType jobType;
    private final SparkDeployMode deployMode;
    private final DependencyInfo dependencyInfo;

    public SparkYarnBuildRequest(
                                 String appName,
                                 String mainClass,
                                 String localWorkspace,
                                 String yarnProvidedPath,
                                 SparkJobType jobType,
                                 SparkDeployMode deployMode,
                                 DependencyInfo dependencyInfo) {
        this.appName = appName;
        this.mainClass = mainClass;
        this.localWorkspace = localWorkspace;
        this.yarnProvidedPath = yarnProvidedPath;
        this.jobType = jobType;
        this.deployMode = deployMode;
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

    public String localWorkspace() {
        return localWorkspace;
    }

    public String yarnProvidedPath() {
        return yarnProvidedPath;
    }

    public SparkJobType jobType() {
        return jobType;
    }

    public SparkDeployMode deployMode() {
        return deployMode;
    }

    public DependencyInfo dependencyInfo() {
        return dependencyInfo;
    }
}
