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
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.flink.packer.maven.DependencyInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface FlinkBuildParam extends BuildParam {

    String workspace();

    FlinkDeployMode deployMode();

    FlinkJobType flinkJobType();

    FlinkVersion flinkVersion();

    DependencyInfo dependencyInfo();

    String customFlinkUserJar();

    default DependencyInfo providedLibs() {
        Set<String> libs = new HashSet<>(Arrays.asList(
            Workspace.local().APP_JARS(),
            customFlinkUserJar()));
        if (flinkJobType() == FlinkJobType.FLINK_SQL) {
            libs.add(
                Workspace.local().APP_SHIMS()
                    + "/flink-"
                    + flinkVersion().majorVersion());
        }
        return dependencyInfo().merge(libs);
    }

    default String getShadedJarPath(String rootWorkspace) {
        String safeAppName = appName().replaceAll("\\s+", "_");
        return rootWorkspace + "/streampark-flinkjob_" + safeAppName + ".jar";
    }
}
