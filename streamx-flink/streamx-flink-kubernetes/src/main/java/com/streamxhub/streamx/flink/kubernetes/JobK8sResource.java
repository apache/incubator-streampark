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
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class JobK8sResource implements K8sResourceWithPods<Job> {

    private final KubernetesClient client;
    private final String namespace;
    private final Job resource;
    private final PodK8sResourceCollection pods;
    @VisibleForTesting
    public final DeletionPropagationPolicy deletionPolicy;

    public JobK8sResource(Config config, String namespace, Job resource, DeletionPropagationPolicy deletionPolicy) {
        this.client = new DefaultKubernetesClient(config);
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.resource = Objects.requireNonNull(resource, "resource");
        this.pods = new PodK8sResourceCollection(config, namespace, Optional.ofNullable(resource.getSpec()).map((spec) -> {
            return spec.getTemplate().getMetadata();
        }).map(ObjectMeta::getLabels).orElseThrow(() -> {
            return new IllegalArgumentException("Must provide Pod template labels");
        }));
        this.deletionPolicy = deletionPolicy;
    }

    public Job createOrGetResource(UnaryOperator<Job> reconfiguration) {
        return this.getResource().orElseGet(() -> {
            try {
                Job reconfigured = reconfiguration.apply(this.resource);
                return this.client.batch().v1().jobs().inNamespace(this.namespace).create(reconfigured);
            } catch (KubernetesClientException e) {
                throw new RuntimeException("Failed to create resource", e);
            }
        });
    }

    public Optional<Job> getResource() {
        try {
            return Optional.ofNullable(this.client.batch().v1().jobs().inNamespace(this.namespace).withName(this.resource.getMetadata().getName()).get());
        } catch (KubernetesClientException e) {
            return Optional.empty();
        }
    }

    public boolean deleteResource() {
        try {
            this.client.batch().v1().jobs().inNamespace(this.namespace).withName(this.resource.getMetadata().getName()).withPropagationPolicy(DeletionPropagation.valueOf(Objects.requireNonNull(this.deletionPolicy.getDeleteOptions()).getPropagationPolicy())).delete();
        } catch (KubernetesClientException e) {
            return false;
        }
        return true;
    }

    public Job getDefinition() {
        return this.resource;
    }

    public Iterable<Pod> listPods() {
        return this.pods.listResources();
    }
}
