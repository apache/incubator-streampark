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

package org.apache.streampark.flink.client.request;

import org.apache.streampark.common.core.FlinkVersion;
import org.apache.streampark.common.enums.FlinkDeployMode;

import javax.annotation.Nullable;

import java.util.Map;

/** Request to deploy a Flink session cluster. */
public final class DeployRequest extends ClusterRequest {

    private static final long serialVersionUID = 1L;

    public DeployRequest(
                         FlinkVersion flinkVersion,
                         FlinkDeployMode deployMode,
                         Map<String, Object> properties,
                         String clusterId,
                         long id,
                         @Nullable KubernetesDeploySpec kubernetesDeploySpec) {
        super(flinkVersion, deployMode, properties, clusterId, id, kubernetesDeploySpec);
    }

}
