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

package com.streamxhub.streamx.flink.kubernetes.network;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * sdsd
 */
public class FlinkJobIngress {
    public static void configureIngress(String domainName, String clusterId, String nameSpace)
            throws FileNotFoundException {

        try (final KubernetesClient client = new DefaultKubernetesClient()) {
            Map<String, String> map = new HashMap<>();

            map.put("nginx.ingress.kubernetes.io/rewrite-target", "/$2");
            map.put("nginx.ingress.kubernetes.io/proxy-body-size", "1024m");
            map.put("nginx.ingress.kubernetes.io/configuration-snippet",
                    "rewrite ^(/"+clusterId+")$ $1/ permanent;");
            Map<String, String> map1 = new HashMap<>();
            map1.put("app", clusterId);
            map1.put("type", "flink-native-kubernetes");
            map1.put("component", "ingress");

            io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress ingress =
                    new IngressBuilder()
                            .withNewMetadata()
                            .withName(clusterId)
                            .addToAnnotations(map)
                            .addToLabels(map1)
                            .endMetadata()
                            .withNewSpec()
                            .addNewRule()
                            .withHost(domainName)
                            .withNewHttp()
                            .addNewPath()
                            .withPath("/native-flink/" + clusterId + "/")
                            .withNewBackend()
                            .withServiceName(clusterId + "-rest")
                            .withServicePort(new IntOrString("rest"))
                            .endBackend()
                            .endPath()
                            .addNewPath()
                            .withPath("/native-flink/" + clusterId + "(/|$)(.*)")
                            .withNewBackend()
                            .withServiceName(clusterId + "-rest")
                            .withServicePort(new IntOrString("rest"))
                            .endBackend()
                            .endPath()
                            .endHttp()
                            .endRule()
                            .endSpec()
                            .build();

            client.network().ingress().inNamespace(nameSpace).create(ingress);
        }
        }

    public static void configureIngress(String ingressOutput) throws FileNotFoundException {
        try (final KubernetesClient client = new DefaultKubernetesClient()) {
            client.network().ingress().load(Files.newInputStream(Paths.get(ingressOutput))).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteIngress(String ingressName, String nameSpace) {
        KubernetesClient client = new DefaultKubernetesClient();
        if (determineThePodSurvivalStatus(ingressName,nameSpace)){
            client.network().ingress().inNamespace(nameSpace).withName(ingressName).delete();

        }
    }

    private static Boolean determineThePodSurvivalStatus(String name, String nameSpace){
        // getpod by deploymentName
        KubernetesClient client = new DefaultKubernetesClient();
        Map<String, String> matchLabels = client.apps()
            .deployments()
            .inNamespace(nameSpace)
            .withName(name)
            .get()
            .getSpec()
            .getSelector()
            .getMatchLabels();
        List<Pod> items = client.pods().inNamespace(nameSpace).withLabels(matchLabels).list().getItems();
        Iterator<Pod> iterator = items.iterator();
        int cut = 0;
        while (iterator.hasNext()) {
            Pod it = iterator.next();
            if ("Running".equals(it.getStatus().getPhase())) {
                cut++;
            } else {
                System.out.println(it.getStatus().getPhase());
            }
        }
        if (cut>0){
            return false;
        }else{
            return true;
        }
    }
}
