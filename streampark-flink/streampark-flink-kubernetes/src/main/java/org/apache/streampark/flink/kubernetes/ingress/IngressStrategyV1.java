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
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.api.model.ServicePort;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.DefaultKubernetesClient;

import java.util.List;

public class IngressStrategyV1 implements IngressStrategy {

    @Override
    public String getIngressUrl(String nameSpace, String clusterId, ClusterClient<?> clusterClient) {
        try (DefaultKubernetesClient client = new DefaultKubernetesClient()) {
            Ingress ingress = null;
            try {
                ingress = client.network().v1().ingresses().inNamespace(nameSpace).withName(clusterId).get();
            } catch (Exception ignored) {
                // fall through
            }
            if (ingress != null
                && ingress.getSpec() != null
                && !ingress.getSpec().getRules().isEmpty()
                && ingress.getSpec().getRules().get(0).getHttp() != null
                && !ingress.getSpec().getRules().get(0).getHttp().getPaths().isEmpty()) {
                String host = ingress.getSpec().getRules().get(0).getHost();
                String path = ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getPath();
                if (host != null) {
                    String newPath = path == null || path.isEmpty() ? "" : path.replaceAll("/+$", "");
                    return "http://" + host + newPath;
                }
            }
            return clusterClient.getWebInterfaceURL();
        } catch (Exception e) {
            throw new RuntimeException("[StreamPark] get ingressUrlAddress error: " + e, e);
        }
    }

    private int touchIngressBackendRestPort(DefaultKubernetesClient client, String clusterId, String nameSpace) {
        List<ServicePort> ports =
            client.services()
                .inNamespace(nameSpace)
                .withName(clusterId + "-" + REST_SERVICE_IDENTIFICATION)
                .get()
                .getSpec()
                .getPorts();
        for (ServicePort servicePort : ports) {
            if (REST_SERVICE_IDENTIFICATION.equalsIgnoreCase(servicePort.getName())) {
                return servicePort.getTargetPort().getIntVal();
            }
        }
        throw new IllegalStateException("REST service port not found for cluster " + clusterId);
    }

    @Override
    public void configureIngress(String domainName, String clusterId, String nameSpace) {
        AutoCloseUtils.using(
            new DefaultKubernetesClient(),
            client -> {
                var ownerReference = getOwnerReference(nameSpace, clusterId, client);
                int ingressBackendRestServicePort = touchIngressBackendRestPort(client, clusterId, nameSpace);
                Ingress ingress =
                    new IngressBuilder()
                        .withNewMetadata()
                        .withName(clusterId)
                        .addToAnnotations(buildIngressAnnotations(clusterId, nameSpace))
                        .addToLabels(buildIngressLabels(clusterId))
                        .addToOwnerReferences(ownerReference)
                        .endMetadata()
                        .withNewSpec()
                        .withIngressClassName(ingressClass())
                        .addNewRule()
                        .withHost(domainName)
                        .withNewHttp()
                        .addNewPath()
                        .withPath("/" + nameSpace + "/" + clusterId + "/")
                        .withPathType("ImplementationSpecific")
                        .withNewBackend()
                        .withNewService()
                        .withName(clusterId + "-" + REST_SERVICE_IDENTIFICATION)
                        .withNewPort()
                        .withNumber(ingressBackendRestServicePort)
                        .endPort()
                        .endService()
                        .endBackend()
                        .endPath()
                        .addNewPath()
                        .withPath("/" + nameSpace + "/" + clusterId + "(/|$)(.*)")
                        .withPathType("ImplementationSpecific")
                        .withNewBackend()
                        .withNewService()
                        .withName(clusterId + "-" + REST_SERVICE_IDENTIFICATION)
                        .withNewPort()
                        .withNumber(ingressBackendRestServicePort)
                        .endPort()
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
