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

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class ServiceAccountK8sResource implements K8sResource<ServiceAccount> {
    private final KubernetesClient client;
    private final String namespace;
    private final ServiceAccount resource;

    public ServiceAccountK8sResource(Config config, String namespace, ServiceAccount resource) {
        this.client = new DefaultKubernetesClient(config);
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.resource = Objects.requireNonNull(resource, "resource");
    }

    public ServiceAccount createOrGetResource(UnaryOperator<ServiceAccount> reconfiguration) {
        return this.getResource().orElseGet(() -> {
            try {
                ServiceAccount reconfigured = reconfiguration.apply(this.resource);
                return this.client.serviceAccounts().inNamespace(this.namespace).create(reconfigured);
            } catch (KubernetesClientException e) {
                throw new RuntimeException("Failed to create resource", e);
            }
        });
    }

    public Optional<ServiceAccount> getResource() {
        try {
            return Optional.ofNullable(this.client.serviceAccounts().inNamespace(this.namespace).withName(this.resource.getMetadata().getName()).get());
        } catch (KubernetesClientException e) {
            return Optional.empty();
        }
    }

    public boolean deleteResource() {
        try {
            this.client.serviceAccounts().inNamespace(this.namespace).withName(this.resource.getMetadata().getName()).withPropagationPolicy(DeletionPropagation.valueOf(Objects.requireNonNull(Objects.requireNonNull(DeletionPropagationPolicy.NONE.getDeleteOptions()).getPropagationPolicy()))).delete();
        } catch (KubernetesClientException var2) {
            return false;
        }
        return true;
    }

    public ServiceAccount getDefinition() {
        return this.resource;
    }
}
