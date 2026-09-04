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

import org.apache.streampark.common.enums.FlinkKubernetesRestExposedType;

import javax.annotation.Nullable;

import java.io.Serializable;

/**
 * Kubernetes-specific settings for session-cluster deployment.
 *
 * <p>A missing REST exposure type is normalized to {@code ClusterIP} at construction, allowing
 * deployment clients to consume this object without repeating a platform default.
 */
public final class KubernetesDeploySpec implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String clusterId;
    private final String kubernetesNamespace;
    private final String kubeConf;
    private final String serviceAccount;
    private final String flinkImage;
    private final FlinkKubernetesRestExposedType flinkRestExposedType;

    public KubernetesDeploySpec(
                                String clusterId,
                                String kubernetesNamespace,
                                String kubeConf,
                                String serviceAccount,
                                String flinkImage,
                                @Nullable FlinkKubernetesRestExposedType flinkRestExposedType) {
        this.clusterId = clusterId;
        this.kubernetesNamespace = kubernetesNamespace;
        this.kubeConf = kubeConf;
        this.serviceAccount = serviceAccount;
        this.flinkImage = flinkImage;
        this.flinkRestExposedType =
            flinkRestExposedType != null ? flinkRestExposedType : FlinkKubernetesRestExposedType.CLUSTER_IP;
    }

    public String clusterId() {
        return clusterId;
    }

    public String kubernetesNamespace() {
        return kubernetesNamespace;
    }

    public String kubeConf() {
        return kubeConf;
    }

    public String serviceAccount() {
        return serviceAccount;
    }

    public String flinkImage() {
        return flinkImage;
    }

    public FlinkKubernetesRestExposedType flinkRestExposedType() {
        return flinkRestExposedType;
    }
}
