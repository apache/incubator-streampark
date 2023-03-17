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
import org.yaml.snakeyaml.Yaml;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PodTemplateParser {
    public static final String POD_TEMPLATE_INIT_CONTENT =
        "apiVersion: v1\n" +
            "kind: Pod\n" +
            "metadata:\n" +
            "  name: pod-template\n";

    public static String getInitPodTemplateContent() {
        return POD_TEMPLATE_INIT_CONTENT.concat("spec:\n");
    }

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
        Map<String, Object> spec = (Map<String, Object>) root.getOrDefault("spec", Collections.emptyMap());
        if (!spec.isEmpty()) {
            res.put("spec", spec);
        }
        return yaml.dumpAsMap(res);
    }

    public static String completeHostAliasSpec(Map<String, String> hosts, String podTemplateContent) {
        if (hosts.isEmpty()) {
            return podTemplateContent;
        }
        try {
            String content = completeInitPodTemplate(podTemplateContent);

        }
    }

    private static List<Map<String, Object>> covertHostsMapToHostAliasNode(Map<String, String> hosts) {

    }

}
