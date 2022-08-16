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

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

import java.util.Objects;
import java.util.Optional;

public class ReadOnlyConfigMapResource implements ReadOnlyK8sResource<ConfigMap> {
    private final KubernetesClient client;
    private final String name;
    private final String namespace;

    public ReadOnlyConfigMapResource(Config config, String name, String namespace) {
        this.client = new DefaultKubernetesClient(config);
        this.name = Objects.requireNonNull(name, "name");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    public Optional<ConfigMap> getResource() {
        try {
            ConfigMap configMap = this.client.configMaps().inNamespace(this.namespace).withName(this.name).get();
            return Optional.ofNullable(configMap);
        } catch (KubernetesClientException e) {
            return Optional.empty();
        }
    }

    public String getName() {
        return this.name;
    }

    public String getNamespace() {
        return this.namespace;
    }
}
