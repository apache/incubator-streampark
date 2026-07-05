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

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.conf.InternalConfigHolder;
import org.apache.streampark.common.conf.K8sFlinkConfig;
import org.apache.streampark.common.util.FileUtils;

import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.api.model.OwnerReference;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.DefaultKubernetesClient;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public abstract class IngressStrategy {

    protected static final String REST_SERVICE_IDENTIFICATION = "rest";

    protected final String ingressClass =
        InternalConfigHolder.get(K8sFlinkConfig.ingressClass);

    public abstract String getIngressUrl(String nameSpace, String clusterId, ClusterClient<?> clusterClient);

    public abstract void configureIngress(String domainName, String clusterId, String nameSpace);

    public String prepareIngressTemplateFiles(String buildWorkspace, String ingressTemplates)
            throws Exception {
        File workspaceDir = new File(buildWorkspace);
        if (!workspaceDir.exists()) {
            workspaceDir.mkdir();
        }
        if (ingressTemplates == null || ingressTemplates.isEmpty()) {
            return null;
        }
        String outputPath = buildWorkspace + "/ingress.yaml";
        FileUtils.writeFile(ingressTemplates, new File(outputPath));
        return outputPath;
    }

    protected Map<String, String> buildIngressAnnotations(String clusterId, String namespace) {
        Map<String, String> map = new HashMap<>();
        map.put("nginx.ingress.kubernetes.io/rewrite-target", "/$2");
        map.put("nginx.ingress.kubernetes.io/proxy-body-size", "1024m");
        map.put(
                "nginx.ingress.kubernetes.io/configuration-snippet",
                "rewrite ^(/"
                        + clusterId
                        + ")$ $1/ permanent; sub_filter '<base href=\"./\">' '<base href=\"/"
                        + namespace
                        + "/"
                        + clusterId
                        + "/\">'; sub_filter_once off;");
        return map;
    }

    protected Map<String, String> buildIngressLabels(String clusterId) {
        Map<String, String> map = new HashMap<>();
        map.put("app", clusterId);
        map.put("type", ConfigKeys.FLINK_NATIVE_KUBERNETES_LABEL());
        map.put("component", "ingress");
        return map;
    }

    protected OwnerReference getOwnerReference(String nameSpace, String clusterId, DefaultKubernetesClient client) {
        var deployment = client.apps().deployments().inNamespace(nameSpace).withName(clusterId).get();
        if (deployment == null) {
            throw new IllegalStateException("Deployment with name " + clusterId + " not found in namespace " + nameSpace);
        }
        return new OwnerReferenceBuilder()
            .withUid(deployment.getMetadata().getUid())
            .withApiVersion("apps/v1")
            .withKind("Deployment")
            .withName(clusterId)
            .withController(true)
            .withBlockOwnerDeletion(true)
            .build();
    }
}
