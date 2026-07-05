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

package org.apache.streampark.flink.client.bean;

import org.apache.streampark.common.conf.FlinkVersion;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.enums.FlinkK8sRestExposedType;
import org.apache.streampark.flink.util.FlinkUtils;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;

import java.io.File;
import java.util.Map;

@Data
@NoArgsConstructor
public class DeployRequest implements DeployRequestTrait {

    private FlinkVersion flinkVersion;
    private FlinkDeployMode deployMode;
    private Map<String, Object> properties;
    private String clusterId;
    private long id;

    @Nullable private KubernetesDeployParam k8sParam;

    private transient HdfsWorkspace hdfsWorkspace;

    public DeployRequest(
            FlinkVersion flinkVersion,
            FlinkDeployMode deployMode,
            Map<String, Object> properties,
            String clusterId,
            long id,
            KubernetesDeployParam k8sParam) {
        this.flinkVersion = flinkVersion;
        this.deployMode = deployMode;
        this.properties = properties;
        this.clusterId = clusterId;
        this.id = id;
        this.k8sParam = k8sParam;
    }

    public HdfsWorkspace getHdfsWorkspace() {
        if (hdfsWorkspace == null) {
            Workspace workspace = Workspace.remote();
            String flinkHome = flinkVersion.flinkHome;
            File flinkHomeDir = new File(flinkHome);
            String flinkName;
            try {
                flinkName =
                        FileUtils.isSymlink(flinkHomeDir)
                                ? flinkHomeDir.getCanonicalFile().getName()
                                : flinkHomeDir.getName();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            String flinkHdfsHome = workspace.APP_FLINK() + "/" + flinkName;
            hdfsWorkspace =
                    HdfsWorkspace.builder()
                            .flinkName(flinkName)
                            .flinkHome(flinkHome)
                            .flinkLib(flinkHdfsHome + "/lib")
                            .flinkPlugins(flinkHdfsHome + "/plugins")
                            .flinkDistJar(FlinkUtils.getFlinkDistJar(flinkHome))
                            .appJars(workspace.APP_JARS())
                            .build();
        }
        return hdfsWorkspace;
    }
}
