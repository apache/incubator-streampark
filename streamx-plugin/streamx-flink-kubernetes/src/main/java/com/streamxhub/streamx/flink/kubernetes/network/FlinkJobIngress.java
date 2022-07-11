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
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.io.FileUtils;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.kubernetes.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.kubernetes.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.kubernetes.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlinkJobIngress {
    public static void configureIngress(String domainName, String clusterId, String nameSpace)
            throws FileNotFoundException {
        try (final KubernetesClient client = new DefaultKubernetesClient()) {
            Map<String, String> map = new HashMap<>();
            map.put("nginx.ingress.kubernetes.io/rewrite-target", "/$2");
            map.put("nginx.ingress.kubernetes.io/proxy-body-size", "1024m");
            map.put("nginx.ingress.kubernetes.io/configuration-snippet",
                    "rewrite ^(/" + clusterId + ")$ $1/ permanent;");
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
                            .withPath("/"+nameSpace+"/" + clusterId + "/")
                            .withNewBackend()
                            .withServiceName(clusterId + "-rest")
                            .withServicePort(new IntOrString("rest"))
                            .endBackend()
                            .endPath()
                            .addNewPath()
                            .withPath("/"+nameSpace+"/" + clusterId + "(/|$)(.*)")
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
        if (determineThePodSurvivalStatus(ingressName, nameSpace)){
            try (KubernetesClient client = new DefaultKubernetesClient()) {
                client.network().ingress().inNamespace(nameSpace).withName(ingressName).delete();
            }
        }
    }

    private static Boolean determineThePodSurvivalStatus(String name, String nameSpace){
        // getpod by deploymentName
        try (KubernetesClient client = new DefaultKubernetesClient()){
            Map<String, String> matchLabels = client.apps()
                .deployments()
                .inNamespace(nameSpace)
                .withName(name)
                .get()
                .getSpec()
                .getSelector()
                .getMatchLabels();
        } catch (NullPointerException e){
            return true;
        }
        return false;
    }

    public static String ingressUrlAddress(String nameSpace, String clusterId, ClusterClient clusterClient) throws JsonProcessingException {
        if (determineIfIngressExists(nameSpace, clusterId)){
            KubernetesClient client = new DefaultKubernetesClient();
            Ingress ingress = client.network().ingress().inNamespace(nameSpace).withName(clusterId).get();
            String str = ingress.getMetadata().getAnnotations().get("field.cattle.io/publicEndpoints");

            ObjectMapper objectMapper = new ObjectMapper();
            List<IgsMeta> ingressMetas = objectMapper.readValue(str, new TypeReference<List<IgsMeta>>(){});
            String hostname = ingressMetas.get(0).hostname;
            String path = ingressMetas.get(0).path;
            String url = "https://" + hostname + path;
            return url;
        } else {
            return clusterClient.getWebInterfaceURL();
        }
    }

    public static Boolean determineIfIngressExists(String nameSpace, String clusterId){
        try (KubernetesClient client = new DefaultKubernetesClient()){
            client.extensions().ingresses()
                .inNamespace(nameSpace)
                .withName(clusterId)
                .get().getMetadata().getName();
        } catch (NullPointerException e){
            return false;
        }
        return true;
    }

    public static String prepareIngressTemplateFiles(String buildWorkspace, String ingressTemplates) throws IOException {
        File workspaceDir = new File(buildWorkspace);
        if (!workspaceDir.exists()) {
            workspaceDir.mkdir();
        }
        String outputPath = null;
        if (!ingressTemplates.isEmpty()) {
            outputPath = buildWorkspace + "/ingress.yaml";
            File outputFile = new File(outputPath);
            FileUtils.write(outputFile, ingressTemplates, "UTF-8");
        }
        return outputPath;
    }

}
