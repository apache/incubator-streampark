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
import org.apache.streampark.flink.util.FlinkUtils;

import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

public class DeployRequest implements DeployRequestTrait, Serializable {

    private static final long serialVersionUID = 1L;

    private final FlinkVersion flinkVersion;
    private final FlinkDeployMode deployMode;
    private final Map<String, Object> properties;
    private final String clusterId;
    private final long id;
    @Nullable
    private final KubernetesDeployParam k8sParam;

    private transient HdfsWorkspace hdfsWorkspace;

    public DeployRequest(
                         FlinkVersion flinkVersion,
                         FlinkDeployMode deployMode,
                         Map<String, Object> properties,
                         String clusterId,
                         long id,
                         @Nullable KubernetesDeployParam k8sParam) {
        this.flinkVersion = flinkVersion;
        this.deployMode = deployMode;
        this.properties = properties;
        this.clusterId = clusterId;
        this.id = id;
        this.k8sParam = k8sParam;
    }

    @Override
    public FlinkVersion flinkVersion() {
        return flinkVersion;
    }

    @Override
    public FlinkDeployMode deployMode() {
        return deployMode;
    }

    @Override
    public Map<String, Object> properties() {
        return properties;
    }

    @Override
    public String clusterId() {
        return clusterId;
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    @Nullable
    public KubernetesDeployParam k8sParam() {
        return k8sParam;
    }

    public HdfsWorkspace hdfsWorkspace() {
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
                new HdfsWorkspace(
                    flinkName,
                    flinkHome,
                    FlinkUtils.getFlinkDistJar(flinkHome),
                    flinkHdfsHome + "/lib",
                    flinkHdfsHome + "/plugins",
                    workspace.APP_JARS());
        }
        return hdfsWorkspace;
    }
}
