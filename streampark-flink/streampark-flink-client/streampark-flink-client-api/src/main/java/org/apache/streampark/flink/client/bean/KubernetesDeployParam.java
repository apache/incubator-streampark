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

import org.apache.streampark.common.enums.FlinkK8sRestExposedType;

import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KubernetesDeployParam {

    private String clusterId;

    @Builder.Default
    private String kubernetesNamespace = KubernetesConfigOptions.NAMESPACE.defaultValue();

    @Builder.Default
    private String kubeConf = "~/.kube/config";

    @Builder.Default
    private String serviceAccount = KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT.defaultValue();

    @Builder.Default
    private String flinkImage = KubernetesConfigOptions.CONTAINER_IMAGE.defaultValue();

    @Nullable
    @Builder.Default
    private FlinkK8sRestExposedType flinkRestExposedType = FlinkK8sRestExposedType.CLUSTER_IP;
}
