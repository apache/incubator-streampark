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

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.List;
import java.util.Map;

public class K8sDeploymentRelated {

    public static Boolean getDeploymentStatusChanges(String nameSpce, String deploymentName){
        try (KubernetesClient client = new DefaultKubernetesClient()){
            Map<String, String> matchLabels = client.apps()
                .deployments()
                .inNamespace(nameSpce)
                .withName(deploymentName)
                .get()
                .getSpec()
                .getSelector()
                .getMatchLabels();
            List<Pod> items = client.pods().inNamespace(nameSpce).withLabels(matchLabels).list().getItems();
            return items.get(0).getStatus().getContainerStatuses().get(0).getLastState().getTerminated() != null;
        }
    }

    public static void deleteTaskDeployment(String nameSpce, String deploymentName){
        try (KubernetesClient client = new DefaultKubernetesClient()){
            client.apps().deployments().inNamespace(nameSpce).withName(deploymentName).delete();
        }
    }
}
