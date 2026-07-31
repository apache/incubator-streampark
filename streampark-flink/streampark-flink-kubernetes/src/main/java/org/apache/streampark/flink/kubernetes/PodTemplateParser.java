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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PodTemplateParser {

    private static final String KEY_HOST_ALIASES = "hostAliases";
    private static final String KEY_HOSTNAMES = "hostnames";
    private static final String KEY_SPEC = "spec";
    private static final String KEY_IP = "ip";

    public static final String POD_TEMPLATE_INIT_CONTENT =
        "apiVersion: v1\n"
            + "kind: Pod\n"
            + "metadata:\n"
            + "  name: pod-template\n";

    private PodTemplateParser() {
    }

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
        if (podTemplateContent == null || podTemplateContent.trim().isEmpty()) {
            return POD_TEMPLATE_INIT_CONTENT;
        }
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(podTemplateContent);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("apiVersion", root.getOrDefault("apiVersion", "v1"));
        res.put("kind", root.getOrDefault("kind", "Pod"));
        Object metadata = root.getOrDefault("metadata", defaultMetadata());
        res.put("metadata", metadata);

        if (root.containsKey("spec")) {
            Object spec = root.get("spec");
            if (spec instanceof Map && !((Map<?, ?>) spec).isEmpty()) {
                res.put("spec", spec);
            }
        }
        return yaml.dumpAsMap(res);
    }

    private static Map<String, Object> defaultMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", "pod-template");
        return metadata;
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
                spec.put(KEY_HOST_ALIASES, hostAlias);
                root.put(KEY_SPEC, spec);
                return yaml.dumpAsMap(root);
            }
            Map<String, Object> spec = (Map<String, Object>) root.get(KEY_SPEC);
            spec.put(KEY_HOST_ALIASES, hostAlias);
            return yaml.dumpAsMap(root);
        } catch (Throwable e) {
            return podTemplateContent;
        }
    }

    /** convert hosts map to host alias */
    private static List<Map<String, Object>> covertHostsMapToHostAliasNode(Map<String, String> hosts) {
        Map<String, List<String>> ipToHostnames = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : hosts.entrySet()) {
            String hostname = entry.getKey().trim();
            String ip = entry.getValue().trim();
            ipToHostnames.computeIfAbsent(ip, k -> new ArrayList<>()).add(hostname);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : ipToHostnames.entrySet()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(KEY_IP, entry.getKey());
            map.put(KEY_HOSTNAMES, new ArrayList<>(entry.getValue()));
            result.add(map);
        }
        return result;
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
        Map<String, String> hosts = new LinkedHashMap<>(0);
        if (podTemplateContent == null || podTemplateContent.isEmpty()) {
            return hosts;
        }
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(podTemplateContent);
            if (!root.containsKey(KEY_SPEC)) {
                return hosts;
            }
            Map<String, Object> spec = (Map<String, Object>) root.get(KEY_SPEC);
            if (!spec.containsKey(KEY_HOST_ALIASES)) {
                return hosts;
            }
            List<Map<String, Object>> hostAliases = (List<Map<String, Object>>) spec.get(KEY_HOST_ALIASES);
            if (CollectionUtils.isEmpty(hostAliases)) {
                return hosts;
            }
            for (Map<String, Object> hostAlias : hostAliases) {
                collectHostAliasEntry(hosts, hostAlias);
            }
        } catch (Throwable e) {
            return new LinkedHashMap<>(0);
        }
        return hosts;
    }

    @SuppressWarnings("unchecked")
    private static void collectHostAliasEntry(Map<String, String> hosts, Map<String, Object> hostAlias) {
        if (!hostAlias.containsKey(KEY_IP) || !hostAlias.containsKey(KEY_HOSTNAMES)) {
            return;
        }
        String ip = (String) hostAlias.get(KEY_IP);
        if (StringUtils.isBlank(ip)) {
            return;
        }
        List<String> hostnames = (List<String>) hostAlias.get(KEY_HOSTNAMES);
        if (hostnames == null) {
            return;
        }
        for (String hostname : hostnames) {
            if (StringUtils.isNotBlank(hostname)) {
                hosts.put(hostname, ip);
            }
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
        root.put(KEY_HOST_ALIASES, hostAlias);
        return new Yaml().dumpAsMap(root);
    }
}
