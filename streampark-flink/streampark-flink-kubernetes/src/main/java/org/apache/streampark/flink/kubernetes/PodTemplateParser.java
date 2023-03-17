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

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PodTemplateParser {

  private static final Logger LOG = LoggerFactory.getLogger(PodTemplateParser.class);

  public static final String POD_TEMPLATE_INIT_CONTENT =
      "apiVersion: v1\n" + "kind: Pod\n" + "metadata:\n" + "  name: pod-template\n";

  /** Get init content of pod template */
  public static String getInitPodTemplateContent() {
    return POD_TEMPLATE_INIT_CONTENT.concat("spec:\n");
  }

  /**
   * Complementary initialization pod templates
   *
   * @param podTemplateContent original pod template
   * @return complemented pod template
   */
  @SuppressWarnings("unchecked")
  public static String completeInitPodTemplate(String podTemplateContent) {
    if (Objects.isNull(podTemplateContent) || StringUtils.isBlank(podTemplateContent)) {
      return POD_TEMPLATE_INIT_CONTENT;
    }
    Yaml yaml = new Yaml();
    Map<String, Object> root = yaml.load(podTemplateContent);
    Map<String, Object> res = new LinkedHashMap<>();
    res.put("apiVersion", root.getOrDefault("apiVersion", "v1"));
    res.put("kind", root.getOrDefault("kind", "Pod"));
    res.put("metadata", new LinkedHashMap<String, Object>().put("name", "pod-template"));
    Map<String, Object> spec =
        (Map<String, Object>) root.getOrDefault("spec", Collections.emptyMap());
    if (!spec.isEmpty()) {
      res.put("spec", spec);
    }
    return yaml.dumpAsMap(res);
  }

  /**
   * Add or Merge host alias spec into pod template. When parser pod template error, it would return
   * the origin content.
   *
   * @param hosts hosts info [hostname, ip]
   * @param podTemplateContent pod template content
   * @return pod template content
   */
  @SuppressWarnings("unchecked")
  public static String completeHostAliasSpec(Map<String, String> hosts, String podTemplateContent) {
    if (hosts.isEmpty()) {
      return podTemplateContent;
    }
    try {
      String content = completeInitPodTemplate(podTemplateContent);
      List<Map<String, Object>> hostAlias = covertHostsMapToHostAliasNode(hosts);
      Yaml yaml = new Yaml();
      Map<String, Object> root = yaml.load(content);
      if (!root.containsKey("spec")) {
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("hostAliases", hostAlias);
        root.put("spec", spec);
        return yaml.dumpAsMap(root);
      }
      Map<String, Object> spec = (Map<String, Object>) root.get("spec");
      spec.put("hostAliases", hostAlias);
      root.put("spec", spec);
      return yaml.dumpAsMap(root);
    } catch (Exception e) {

      return podTemplateContent;
    }
  }

  /**
   * Extract host-ip map from pod template. When parser pod template error, it would return empty
   * Map.
   *
   * @param podTemplateContent pod template content
   * @return hostname -> ipv4
   */
  @SuppressWarnings("unchecked")
  public static Map<String, String> extractHostAliasMap(String podTemplateContent) {
    Map<String, String> hosts = new LinkedHashMap<>();
    if (StringUtils.isEmpty(podTemplateContent)) {
      return hosts;
    }
    try {
      Yaml yaml = new Yaml();
      Map<String, Object> root = yaml.load(podTemplateContent);
      if (!root.containsKey("spec")) {
        return hosts;
      }
      List<Map<String, Object>> hostAliases = (List<Map<String, Object>>) root.get("spec");
      if (Objects.nonNull(hostAliases) && hostAliases.isEmpty()) {
        return hosts;
      }
      for (Map<String, Object> hostAlias : hostAliases) {
        String ip = (String) hostAlias.get("ip");
        if (StringUtils.isEmpty(ip)) {
          continue;
        }
        List<String> hostnames = (List<String>) hostAlias.get("hostnames");
        hostnames.stream()
            .filter(StringUtils::isNotBlank)
            .forEach(hostname -> hosts.put(hostname, ip));
      }
      return hosts;
    } catch (Exception e) {
      LOG.warn("Failed to parse hostAlias from pod template content, it will return empty map");
      return Collections.emptyMap();
    }
  }

  /**
   * Preview HostAlias pod template content
   *
   * @param hosts hostname -> ipv4
   * @return pod template content
   */
  public static String previewHostAliasSpec(Map<String, String> hosts) {
    List<Map<String, Object>> hostAlias = covertHostsMapToHostAliasNode(hosts);
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("hostAliases", hostAlias);
    Yaml yaml = new Yaml();
    return yaml.dumpAsMap(root);
  }

  /** convert hosts map to host alias */
  private static List<Map<String, Object>> covertHostsMapToHostAliasNode(
      Map<String, String> hosts) {
    Map<String, List<String>> map = new LinkedHashMap<>();
    hosts.forEach(
        (key, value) -> {
          key = key.trim();
          value = value.trim();
          if (map.containsKey(value)) {
            map.get(value).add(key);
          } else {
            map.put(value, new ArrayList<>());
          }
        });
    return map.entrySet().stream()
        .map(
            entry -> {
              Map<String, Object> node = new LinkedHashMap<>();
              node.put("ip", entry.getKey());
              node.put("hostnames", entry.getValue());
              return node;
            })
        .collect(Collectors.toList());
  }
}
