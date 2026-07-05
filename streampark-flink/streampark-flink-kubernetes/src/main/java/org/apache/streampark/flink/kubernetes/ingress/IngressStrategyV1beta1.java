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

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.api.model.IntOrString;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.DefaultKubernetesClient;

import java.util.HashMap;
import java.util.Map;

public class IngressStrategyV1beta1 extends IngressStrategy {

    @Override
    public String getIngressUrl(String nameSpace, String clusterId, ClusterClient<?> clusterClient) {
        return AutoCloseUtils.using(new DefaultKubernetesClient(), client -> {
            try {
                Ingress ingress = client.network().v1beta1().ingresses().inNamespace(nameSpace).withName(clusterId).get();
                if (ingress != null && ingress.getSpec() != null && !ingress.getSpec().getRules().isEmpty()) {
                    var rule = ingress.getSpec().getRules().get(0);
                    return "http://" + rule.getHost() + rule.getHttp().getPaths().get(0).getPath();
                }
                return AutoCloseUtils.using(clusterClient, ClusterClient::getWebInterfaceURL);
            } catch (Exception e) {
                throw new RuntimeException("[StreamPark] get ingressUrlAddress error: " + e.getMessage(), e);
            }
        });
    }

    @Override
    protected Map<String, String> buildIngressAnnotations(String clusterId, String namespace) {
        Map<String, String> map = new HashMap<>(super.buildIngressAnnotations(clusterId, namespace));
        if (StringUtils.isNotBlank(ingressClass)) {
            map.put("kubernetes.io/ingress.class", ingressClass);
        }
        return map;
    }

    @Override
    public void configureIngress(String domainName, String clusterId, String nameSpace) {
        AutoCloseUtils.using(new DefaultKubernetesClient(), client -> {
            var ownerReference = getOwnerReference(nameSpace, clusterId, client);
            var ingress = new IngressBuilder()
                .withNewMetadata()
                .withName(clusterId)
                .addToAnnotations(buildIngressAnnotations(clusterId, nameSpace))
                .addToLabels(buildIngressLabels(clusterId))
                .addToOwnerReferences(ownerReference)
                .endMetadata()
                .withNewSpec()
                .addNewRule()
                .withHost(domainName)
                .withNewHttp()
                .addNewPath()
                .withPath("/" + nameSpace + "/" + clusterId + "/")
                .withNewBackend()
                .withServiceName(clusterId + "-rest")
                .withServicePort(new IntOrString("rest"))
                .endBackend()
                .endPath()
                .addNewPath()
                .withPath("/" + nameSpace + "/" + clusterId + "(/|$)(.*)")
                .withNewBackend()
                .withServiceName(clusterId + "-rest")
                .withServicePort(new IntOrString("rest"))
                .endBackend()
                .endPath()
                .endHttp()
                .endRule()
                .endSpec()
                .build();
            client.network().ingresses().inNamespace(nameSpace).create(ingress);
            return null;
        });
    }
}
