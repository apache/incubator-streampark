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

package org.apache.streampark.flink.core;

import org.apache.flink.kubernetes.kubeclient.FlinkKubeClient;
import org.apache.flink.kubernetes.kubeclient.resources.KubernetesService;

import java.util.Optional;

/** Flink Kubernetes client operations. */
public abstract class FlinkKubernetesClientTrait {

    protected final FlinkKubeClient kubeClient;

    protected FlinkKubernetesClientTrait(FlinkKubeClient kubeClient) {
        this.kubeClient = kubeClient;
    }

    /**
     * Get the kubernetes service of the given flink clusterId.
     *
     * @param serviceName the name of the service
     * @return Return the optional kubernetes service of the specified name.
     */
    public Optional<KubernetesService> getService(String serviceName) {
        return kubeClient.getService(serviceName);
    }
}
