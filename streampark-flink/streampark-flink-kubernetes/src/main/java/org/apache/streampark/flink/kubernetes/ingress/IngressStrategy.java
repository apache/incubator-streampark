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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public interface IngressStrategy {

    String REST_SERVICE_IDENTIFICATION = "rest";

    default String ingressClass() {
        return InternalConfigHolder.get(K8sFlinkConfig.ingressClass);
    }

    String getIngressUrl(String nameSpace, String clusterId, ClusterClient<?> clusterClient);

    void configureIngress(String domainName, String clusterId, String nameSpace);

    default String prepareIngressTemplateFiles(String buildWorkspace, String ingressTemplates) throws IOException {
        File workspaceDir = new File(buildWorkspace);
        if (!workspaceDir.exists()) {
            workspaceDir.mkdir();
        }
        if (ingressTemplates.isEmpty()) {
            return null;
        }
        String outputPath = buildWorkspace + "/ingress.yaml";
        File outputFile = new File(outputPath);
        FileUtils.writeFile(ingressTemplates, outputFile);
        return outputPath;
    }

    default Map<String, String> buildIngressAnnotations(String clusterId, String namespace) {
        Map<String, String> annotations = new HashMap<>();
        annotations.put("nginx.ingress.kubernetes.io/rewrite-target", "/$2");
        annotations.put("nginx.ingress.kubernetes.io/proxy-body-size", "1024m");
        annotations.put(
            "nginx.ingress.kubernetes.io/configuration-snippet",
            "rewrite ^(/"
                + clusterId
                + ")$ $1/ permanent; sub_filter '<base href=\"./\">' '<base href=\"/"
                + namespace
                + "/"
                + clusterId
                + "/\">'; sub_filter_once off;");
        return annotations;
    }

    default Map<String, String> buildIngressLabels(String clusterId) {
        Map<String, String> labels = new HashMap<>();
        labels.put("app", clusterId);
        labels.put("type", ConfigKeys.FLINK_NATIVE_KUBERNETES_LABEL());
        labels.put("component", "ingress");
        return labels;
    }

    default OwnerReference getOwnerReference(
                                             String nameSpace, String clusterId, DefaultKubernetesClient client) {
        var deployment =
            client.apps().deployments().inNamespace(nameSpace).withName(clusterId).get();

        if (deployment == null) {
            throw new IllegalArgumentException(
                "Deployment with name " + clusterId + " not found in namespace " + nameSpace);
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
