/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.kubernetes;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class RoleK8sResource implements K8sResource<Role> {
    private final KubernetesClient client;
    private final String namespace;
    private final Role resource;

    public RoleK8sResource(Config config, String namespace, Role resource) {
        this.client = new DefaultKubernetesClient(config);
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.resource = Objects.requireNonNull(resource, "resource");
    }

    public Role createOrGetResource(UnaryOperator<Role> reconfiguration) {
        return this.getResource().orElseGet(() -> {
            try {
                Role reconfigured = reconfiguration.apply(this.resource);
                return this.client.rbac().roles().inNamespace(this.namespace).create(reconfigured);
            } catch (KubernetesClientException e) {
                throw new RuntimeException("Failed to create resource", e);
            }
        });
    }

    public Optional<Role> getResource() {
        try {
            return Optional.ofNullable(this.client.rbac().roles().inNamespace(this.namespace).withName(this.resource.getMetadata().getName()).get());
        } catch (KubernetesClientException e) {
            return Optional.empty();
        }
    }

    public boolean deleteResource() {
        try {
            this.client.rbac().roles().inNamespace(this.namespace).withName(this.resource.getMetadata().getName()).withPropagationPolicy(DeletionPropagation.valueOf(Objects.requireNonNull(Objects.requireNonNull(DeletionPropagationPolicy.NONE.getDeleteOptions()).getPropagationPolicy()))).delete();
        } catch (KubernetesClientException e) {
            return false;
        }
        return true;
    }

    public Role getDefinition() {
        return this.resource;
    }
}