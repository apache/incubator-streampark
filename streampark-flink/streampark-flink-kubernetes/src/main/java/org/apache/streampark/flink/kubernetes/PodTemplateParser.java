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

/** Pod template YAML parser and host alias helper. */
public final class PodTemplateParser {

    public static final String POD_TEMPLATE_INIT_CONTENT =
        "apiVersion: v1\n"
            + "kind: Pod\n"
            + "metadata:\n"
            + "  name: pod-template\n";

    private PodTemplateParser() {
    }

    public static String getInitPodTemplateContent() {
        return POD_TEMPLATE_INIT_CONTENT.concat("spec:\n");
    }

    public static String completeInitPodTemplate(String podTemplateContent) {
        if (podTemplateContent == null || podTemplateContent.trim().isEmpty()) {
            return POD_TEMPLATE_INIT_CONTENT;
        }
        Yaml yaml = new Yaml();
        @SuppressWarnings("unchecked")
        Map<String, Object> root = yaml.load(podTemplateContent);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("apiVersion", root.getOrDefault("apiVersion", "v1"));
        res.put("kind", root.getOrDefault("kind", "Pod"));
        Object metadata = root.get("metadata");
        if (metadata == null) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("name", "pod-template");
            res.put("metadata", meta);
        } else {
            res.put("metadata", metadata);
        }
        if (root.containsKey("spec")) {
            Object spec = root.get("spec");
            if (spec instanceof Map && !((Map<?, ?>) spec).isEmpty()) {
                res.put("spec", spec);
            }
        }
        return yaml.dumpAsMap(res);
    }

    public static String completeHostAliasSpec(Map<String, String> hosts, String podTemplateContent) {
        if (hosts == null || hosts.isEmpty()) {
            return podTemplateContent;
        }
        try {
            String content = completeInitPodTemplate(podTemplateContent);
            List<Map<String, Object>> hostAlias = covertHostsMapToHostAliasNode(hosts);
            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> root = yaml.load(content);
            if (!root.containsKey("spec")) {
                Map<String, Object> spec = new LinkedHashMap<>();
                spec.put("hostAliases", hostAlias);
                root.put("spec", spec);
                return yaml.dumpAsMap(root);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> spec = (Map<String, Object>) root.get("spec");
            spec.put("hostAliases", hostAlias);
            return yaml.dumpAsMap(root);
        } catch (Throwable ignored) {
            return podTemplateContent;
        }
    }

    public static Map<String, String> extractHostAliasMap(String podTemplateContent) {
        Map<String, String> hosts = new LinkedHashMap<>();
        if (podTemplateContent == null || podTemplateContent.isEmpty()) {
            return hosts;
        }
        try {
            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> root = yaml.load(podTemplateContent);
            if (!root.containsKey("spec")) {
                return hosts;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> spec = (Map<String, Object>) root.get("spec");
            if (!spec.containsKey("hostAliases")) {
                return hosts;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hostAliases = (List<Map<String, Object>>) spec.get("hostAliases");
            if (CollectionUtils.isEmpty(hostAliases)) {
                return hosts;
            }
            for (Map<String, Object> hostAlias : hostAliases) {
                if (!hostAlias.containsKey("ip") && !hostAlias.containsKey("hostnames")) {
                    continue;
                }
                Object ipObj = hostAlias.get("ip");
                if (!(ipObj instanceof String) || StringUtils.isBlank((String) ipObj)) {
                    continue;
                }
                String ip = (String) ipObj;
                @SuppressWarnings("unchecked")
                List<String> hostnames = (List<String>) hostAlias.get("hostnames");
                if (hostnames == null) {
                    continue;
                }
                for (String hostname : hostnames) {
                    if (StringUtils.isNotBlank(hostname)) {
                        hosts.put(hostname, ip);
                    }
                }
            }
        } catch (Throwable ignored) {
            return new LinkedHashMap<>();
        }
        return hosts;
    }

    public static String previewHostAliasSpec(Map<String, String> hosts) {
        List<Map<String, Object>> hostAlias = covertHostsMapToHostAliasNode(hosts);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("hostAliases", hostAlias);
        return new Yaml().dumpAsMap(root);
    }

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
            map.put("ip", entry.getKey());
            map.put("hostnames", new ArrayList<>(entry.getValue()));
            result.add(map);
        }
        return result;
    }
}
