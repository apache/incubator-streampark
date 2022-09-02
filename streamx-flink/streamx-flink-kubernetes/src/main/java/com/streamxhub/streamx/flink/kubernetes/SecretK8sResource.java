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
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class SecretK8sResource implements K8sResource<Secret> {
    private final KubernetesClient client;
    private final String namespace;
    private final Secret resource;
    @Nullable
    private final Supplier<Map<String, String>> secretDataSupplier;

    public SecretK8sResource(Config config, String namespace, Secret resource, Supplier<Map<String, String>> secretDataSupplier) {
        this.client = new DefaultKubernetesClient(config);
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.resource = Objects.requireNonNull(resource, "resource");
        this.secretDataSupplier = secretDataSupplier;
    }

    public Secret createOrGetResource(UnaryOperator<Secret> reconfiguration) {
        return this.getResource().orElseGet(() -> {
            try {
                Secret reconfigured = reconfiguration.apply(this.resource);
                Secret secret;
                if (this.secretDataSupplier == null) {
                    secret = reconfigured;
                } else {
                    secret = (new SecretBuilder(reconfigured).withData(this.secretDataSupplier.get())).build();
                }

                return this.client.secrets().inNamespace(this.namespace).create(secret);
            } catch (KubernetesClientException e) {
                throw new RuntimeException("Failed to create resource", e);
            }
        });
    }

    public Optional<Secret> getResource() {
        try {
            return Optional.ofNullable(this.client.secrets().inNamespace(this.namespace).withName(this.resource.getMetadata().getName()).get());
        } catch (KubernetesClientException e) {
            return Optional.empty();
        }
    }

    public boolean deleteResource() {
        try {
            this.client.secrets().inNamespace(this.namespace).withName(this.resource.getMetadata().getName()).withPropagationPolicy(DeletionPropagation.valueOf(Objects.requireNonNull(Objects.requireNonNull(DeletionPropagationPolicy.NONE.getDeleteOptions()).getPropagationPolicy()))).delete();
        } catch (KubernetesClientException e) {
            return false;
        }
        return true;
    }

    public Secret getDefinition() {
        return this.resource;
    }
}
