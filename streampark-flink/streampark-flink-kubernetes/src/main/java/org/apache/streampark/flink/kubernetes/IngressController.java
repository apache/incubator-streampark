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

package org.apache.streampark.flink.kubernetes;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.client.program.ClusterClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IngressController {

  private static final Logger LOG = LoggerFactory.getLogger(IngressController.class);

  public static void configureIngress(String domainName, String clusterId, String namespace) {
    try (DefaultKubernetesClient kubernetesClient =
        (DefaultKubernetesClient) KubernetesRetriever.newK8sClient()) {
      Map<String, String> annotMap = new LinkedHashMap<>();
      annotMap.put("nginx.ingress.kubernetes.io/rewrite-target", "/$2");
      annotMap.put("nginx.ingress.kubernetes.io/proxy-body-size", "1024m");
      annotMap.put(
          "nginx.ingress.kubernetes.io/configuration-snippet",
          "rewrite ^(/" + clusterId + ")$ $1/ permanent;");
      Map<String, String> labelsMap = new LinkedHashMap<>();
      labelsMap.put("app", clusterId);
      labelsMap.put("type", "flink-native-kubernetes");
      labelsMap.put("component", "ingress");
      Ingress ingress =
          new IngressBuilder()
              .withNewMetadata()
              .withName(clusterId)
              .addToAnnotations(annotMap)
              .addToLabels(labelsMap)
              .endMetadata()
              .withNewSpec()
              .addNewRule()
              .withHost(domainName)
              .withNewHttp()
              .addNewPath()
              .withPath(String.format("/%s/%s/", namespace, clusterId))
              .withNewBackend()
              .withServiceName(String.format("%s-rest", clusterId))
              .withServicePort(new IntOrString("rest"))
              .endBackend()
              .endPath()
              .addNewPath()
              .withPath(String.format("/%s/%s/", namespace, clusterId) + "(/|$)(.*)")
              .withNewBackend()
              .withServiceName(String.format("%s-rest", clusterId))
              .withServicePort(new IntOrString("rest"))
              .endBackend()
              .endPath()
              .endHttp()
              .endRule()
              .endSpec()
              .build();
      kubernetesClient.network().ingress().inNamespace(namespace).create(ingress);
    } catch (Exception e) {
      LOG.error("Error creating ingress", e);
    }
  }

  public static void configureIngress(String ingressOutput) {
    try (DefaultKubernetesClient kubernetesClient =
        (DefaultKubernetesClient) KubernetesRetriever.newK8sClient()) {
      kubernetesClient
          .network()
          .ingress()
          .load(Files.newInputStream(Paths.get(ingressOutput)))
          .get();
    } catch (Exception e) {
      LOG.error("Configure ingress use [{}] failed", ingressOutput);
    }
  }

  public static void deleteIngress(String ingressName, String namespace) {
    if (determineThePodSurvivalStatus(ingressName, namespace)) {
      try (KubernetesClient kubernetesClient = KubernetesRetriever.newK8sClient()) {
        kubernetesClient.network().ingress().inNamespace(namespace).withName(ingressName).delete();
      }
    }
  }

  public static String ingressUrlAddress(
      String namespace, String clusterId, ClusterClient<?> clusterClient) {
    if (determineIfIngressExists(namespace, clusterId)) {
      try (KubernetesClient kubernetesClient = KubernetesRetriever.newK8sClient()) {
        Ingress ingress =
            kubernetesClient.network().ingress().inNamespace(namespace).withName(clusterId).get();
        String publicEndpoints =
            ingress.getMetadata().getAnnotations().get("field.cattle.io/publicEndpoints");
        List<IngressMeta> ingressMetas = IngressMeta.as(publicEndpoints);
        if (ingressMetas.isEmpty()) {
          throw new RuntimeException("[StreamPark] get ingress url address failed");
        }
        IngressMeta ingressMeta = ingressMetas.get(0);
        String hostname = ingressMeta.getHostname();
        String path = ingressMeta.getPath();
        LOG.info(
            "Retrieve flink cluster {} successfully, JobManager Web Interface: https://{}{}",
            clusterId,
            hostname,
            path);
        return String.format("https://%s%s", hostname, path);
      }
    } else {
      return clusterClient.getWebInterfaceURL();
    }
  }

  public static String prepareIngressTemplateFiles(String buildWorkspace, String ingressTemplates)
      throws IOException {
    File workspaceDir = new File(buildWorkspace);
    if (!workspaceDir.exists()) {
      if (workspaceDir.mkdir()) {
        throw new IOException("Could not create workspace directory " + buildWorkspace);
      }
    }
    if (StringUtils.isEmpty(ingressTemplates)) {
      return null;
    }
    String outputPath = String.format("%s/ingress.yaml", buildWorkspace);
    File outputFile = new File(outputPath);
    FileUtils.write(outputFile, ingressTemplates, "UTF-8");
    LOG.info("Writing ingress config yaml file [{}] successfully", outputPath);
    return outputPath;
  }

  public static class IngressMeta {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final List<String> addresses;

    private final int port;

    private final String protocol;

    private final String serviceName;

    private final String ingressName;

    private final String hostname;

    private final String path;

    private final boolean allNodes;

    public static List<IngressMeta> as(String json) {
      try {
        JsonNode jsonNode = OBJECT_MAPPER.readTree(json);
        List<IngressMeta> ingressMetas = new ArrayList<>();
        jsonNode.forEach(
            node -> {
              List<String> addresses = Collections.emptyList();
              if (node.has("addresses")) {
                List<String> list = new ArrayList<>();
                node.get("addresses").forEach(n -> list.add(node.asText()));
                addresses = list;
              }
              int port = 0;
              if (node.has("port")) {
                port = node.get("port").asInt();
              }
              String protocol = null;
              if (node.has("protocol")) {
                protocol = node.get("protocol").asText();
              }
              String serviceName = null;
              if (node.has("serviceName")) {
                serviceName = node.get("serviceName").asText();
              }
              String ingressName = null;
              if (node.has("ingressName")) {
                ingressName = node.get("ingressName").asText();
              }
              String hostname = null;
              if (node.has("hostname")) {
                hostname = node.get("hostname").asText();
              }
              String path = null;
              if (node.has("path")) {
                path = node.get("path").asText();
              }
              boolean allNodes = false;
              if (node.has("allNodes")) {
                allNodes = node.get("allNodes").asBoolean();
              }
              IngressMeta ingressMeta =
                  new IngressMeta(
                      addresses,
                      port,
                      protocol,
                      serviceName,
                      ingressName,
                      hostname,
                      path,
                      allNodes);
              ingressMetas.add(ingressMeta);
            });
        return ingressMetas;
      } catch (JsonProcessingException e) {
        return Collections.emptyList();
      }
    }

    public IngressMeta(
        List<String> addresses,
        int port,
        String protocol,
        String serviceName,
        String ingressName,
        String hostname,
        String path,
        boolean allNodes) {
      this.addresses = addresses;
      this.port = port;
      this.protocol = protocol;
      this.serviceName = serviceName;
      this.ingressName = ingressName;
      this.hostname = hostname;
      this.path = path;
      this.allNodes = allNodes;
    }

    public List<String> getAddresses() {
      return addresses;
    }

    public int getPort() {
      return port;
    }

    public String getProtocol() {
      return protocol;
    }

    public String getServiceName() {
      return serviceName;
    }

    public String getIngressName() {
      return ingressName;
    }

    public String getHostname() {
      return hostname;
    }

    public String getPath() {
      return path;
    }

    public boolean isAllNodes() {
      return allNodes;
    }
  }

  private static boolean determineIfIngressExists(String namespace, String clusterId) {
    try (KubernetesClient kubernetesClient = KubernetesRetriever.newK8sClient()) {
      kubernetesClient
          .extensions()
          .ingresses()
          .inNamespace(namespace)
          .withName(clusterId)
          .get()
          .getMetadata()
          .getName();
      return true;
    } catch (Exception e) {
      LOG.warn("Ingress is not existed, cluster [{}], namespace [{}]", clusterId, namespace);
      return false;
    }
  }

  private static boolean determineThePodSurvivalStatus(String name, String namespace) {
    try (KubernetesClient kubernetesClient = KubernetesRetriever.newK8sClient()) {
      kubernetesClient
          .apps()
          .deployments()
          .inNamespace(namespace)
          .withName(name)
          .get()
          .getSpec()
          .getSelector()
          .getMatchLabels();
      return false;
    } catch (Exception e) {
      LOG.warn("Pod status is down", e);
      return true;
    }
  }
}
