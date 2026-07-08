/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.flink.kubernetes.ingress;

import org.apache.streampark.common.util.AutoCloseUtils;

import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.DefaultKubernetesClient;

public class IngressStrategyV1 extends IngressStrategy {

    @Override
    public String getIngressUrl(String nameSpace, String clusterId, ClusterClient<?> clusterClient) {
        return AutoCloseUtils.using(new DefaultKubernetesClient(), client -> {
            try {
                Ingress ingress = client.network().v1().ingresses().inNamespace(nameSpace).withName(clusterId).get();
                if (ingress != null && ingress.getSpec() != null && !ingress.getSpec().getRules().isEmpty()) {
                    var rule = ingress.getSpec().getRules().get(0);
                    String host = rule.getHost();
                    String path = rule.getHttp().getPaths().get(0).getPath();
                    if (path != null && !path.isEmpty()) {
                        path = path.replaceAll("/++$", "");
                    } else {
                        path = "";
                    }
                    return "http://" + host + path;
                }
                return AutoCloseUtils.using(clusterClient, ClusterClient::getWebInterfaceURL);
            } catch (Exception e) {
                throw new RuntimeException("[StreamPark] get ingressUrlAddress error: " + e.getMessage(), e);
            }
        });
    }

    private int touchIngressBackendRestPort(DefaultKubernetesClient client, String clusterId, String nameSpace) {
        var ports = client.services().inNamespace(nameSpace).withName(clusterId + "-" + REST_SERVICE_IDENTIFICATION)
            .get().getSpec().getPorts();
        return ports.stream()
            .filter(servicePort -> REST_SERVICE_IDENTIFICATION.equalsIgnoreCase(servicePort.getName()))
            .mapToInt(servicePort -> servicePort.getTargetPort().getIntVal())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("rest port not found"));
    }

    @Override
    public void configureIngress(String domainName, String clusterId, String nameSpace) {
        AutoCloseUtils.using(new DefaultKubernetesClient(), client -> {
            var ownerReference = getOwnerReference(nameSpace, clusterId, client);
            int ingressBackendRestServicePort = touchIngressBackendRestPort(client, clusterId, nameSpace);
            var ingress = new IngressBuilder()
                .withNewMetadata()
                .withName(clusterId)
                .addToAnnotations(buildIngressAnnotations(clusterId, nameSpace))
                .addToLabels(buildIngressLabels(clusterId))
                .addToOwnerReferences(ownerReference)
                .endMetadata()
                .withNewSpec()
                .withIngressClassName(ingressClass)
                .addNewRule()
                .withHost(domainName)
                .withNewHttp()
                .addNewPath()
                .withPath("/" + nameSpace + "/" + clusterId + "/")
                .withPathType("ImplementationSpecific")
                .withNewBackend()
                .withNewService()
                .withName(clusterId + "-" + REST_SERVICE_IDENTIFICATION)
                .withNewPort().withNumber(ingressBackendRestServicePort).endPort()
                .endService()
                .endBackend()
                .endPath()
                .addNewPath()
                .withPath("/" + nameSpace + "/" + clusterId + "(/|$)(.*)")
                .withPathType("ImplementationSpecific")
                .withNewBackend()
                .withNewService()
                .withName(clusterId + "-" + REST_SERVICE_IDENTIFICATION)
                .withNewPort().withNumber(ingressBackendRestServicePort).endPort()
                .endService()
                .endBackend()
                .endPath()
                .endHttp()
                .endRule()
                .endSpec()
                .build();
            client.network().v1().ingresses().inNamespace(nameSpace).create(ingress);
            return null;
        });
    }
}
