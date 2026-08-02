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
import org.apache.streampark.common.enums.FlinkDeployMode;

import javax.annotation.Nullable;

import java.util.Map;

public class DeployRequest extends AbstractDeployClientRequest {

    private static final long serialVersionUID = 1L;

    private transient HdfsWorkspace hdfsWorkspace;

    public DeployRequest(
                         FlinkVersion flinkVersion,
                         FlinkDeployMode deployMode,
                         Map<String, Object> properties,
                         String clusterId,
                         long id,
                         @Nullable KubernetesDeployParam k8sParam) {
        super(flinkVersion, deployMode, properties, clusterId, id, k8sParam);
    }

    public HdfsWorkspace hdfsWorkspace() {
        if (hdfsWorkspace == null) {
            hdfsWorkspace = ClientBeanUtils.createHdfsWorkspace(flinkVersion());
        }
        return hdfsWorkspace;
    }
}
