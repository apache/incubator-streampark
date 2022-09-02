/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.kubernetes;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Map;
import java.util.Objects;

public class PodKubernetesResourceCollection {
    private final KubernetesClient client;
    private final String namespace;
    private final String labelSelector;

    public PodKubernetesResourceCollection(Config config, String namespace, Map<String, String> labels) {
        this.client = new DefaultKubernetesClient(config);
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.labelSelector = K8sDeploymentRelated.toLabelSelector(Objects.requireNonNull(labels, "labels"));
    }

    public Iterable<Pod> listResources() {
        try {
            PodList resourceList = this.client.pods().inNamespace(this.namespace).withLabel(labelSelector).list();
            return resourceList.getItems();
        } catch (Exception e) {
            throw new RuntimeException("Failed to list resources", e);
        }
    }
}
