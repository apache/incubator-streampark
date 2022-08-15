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

import com.google.common.annotations.VisibleForTesting;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Patch;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class DeploymentK8sResource implements PatchableK8SResourceWithPods<Deployment> {
    private final KubernetesClient client;
    private final String namespace;
    private final Deployment resource;
    private final PodKubernetesResourceCollection pods;
    @VisibleForTesting
    public final DeletionPropagationPolicy deletionPolicy;

    public DeploymentK8sResource(Config config, String namespace, Deployment resource, DeletionPropagationPolicy deletionPolicy) {
        this.client = new DefaultKubernetesClient(config);
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.resource = Objects.requireNonNull(resource, "resource");
        this.pods = new PodKubernetesResourceCollection(config, namespace, Optional.ofNullable(resource.getSpec()).map((spec) -> {
            return spec.getTemplate().getMetadata();
        }).map(ObjectMeta::getLabels).orElseThrow(() -> {
            return new IllegalArgumentException("Must provide Pod template labels");
        }));
        this.deletionPolicy = deletionPolicy;
    }

    public Deployment createOrGetResource(UnaryOperator<Deployment> reconfiguration) {
        return this.getResource().orElseGet(() -> {
            try {
                Deployment reconfigured = reconfiguration.apply(this.resource);
                return this.client.apps().deployments().inNamespace(this.namespace).create(reconfigured);
            } catch (KubernetesClientException e) {
                throw new RuntimeException("Failed to create resource", e);
            }
        });
    }

    public Optional<Deployment> getResource() {
        try {
            return Optional.ofNullable(this.client.apps().deployments().inNamespace(this.namespace).withName(this.resource.getMetadata().getName()).get());
        } catch (KubernetesClientException e){
            return Optional.empty();
        }
    }

    public boolean deleteResource() {
        try {
            return this.client.apps().deployments().inNamespace(this.namespace).withName(this.resource.getMetadata().getName()).delete();
        } catch (KubernetesClientException e) {
            throw new RuntimeException("Failed to delete resource", e);
        }
    }

    public Deployment getDefinition() {
        return this.resource;
    }

    public Iterable<Pod> listPods() {
        return this.pods.listResources();
    }

    public Deployment patchResource(Patch patch) {
        try {
            return this.client.apps().deployments().inNamespace(this.namespace).withName(this.resource.getMetadata().getName()).patch(patch.toString());
        } catch (KubernetesClientException e) {
            throw new RuntimeException("Failed to patch resource", e);
        }
    }

}
