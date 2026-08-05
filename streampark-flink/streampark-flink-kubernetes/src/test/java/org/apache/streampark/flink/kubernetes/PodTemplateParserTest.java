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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class PodTemplateParserTest {

    @Test
    void testCompleteInitPodTemplate() {
        Map<String, String> podTemplateExpect = new HashMap<>();
        podTemplateExpect.put(
            "",
            "apiVersion: v1\n"
                + "kind: Pod\n"
                + "metadata:\n"
                + "  name: pod-template\n");
        podTemplateExpect.put(
            "apiVersion: v1\n"
                + "kind: Pod\n"
                + "metadata:\n"
                + "  name: jm-pod-template\n",
            "apiVersion: v1\n"
                + "kind: Pod\n"
                + "metadata:\n"
                + "  name: jm-pod-template\n");
        podTemplateExpect.put(
            "apiVersion: v1\n"
                + "kind: Pod\n"
                + "metadata:\n"
                + "  name: pod-template\n"
                + "spec:\n",
            "apiVersion: v1\n"
                + "kind: Pod\n"
                + "metadata:\n"
                + "  name: pod-template\n");
        podTemplateExpect.put(
            "apiVersion: v1\n" + "spec:\n",
            "apiVersion: v1\n"
                + "kind: Pod\n"
                + "metadata:\n"
                + "  name: pod-template\n");
        podTemplateExpect.put(
            "apiVersion: v1\n"
                + "kind: Pod\n"
                + "metadata:\n"
                + "  name: pod-template\n"
                + "spec:\n"
                + "  containers:\n"
                + "    - name: flink-main-container\n"
                + "      volumeMounts:\n"
                + "        - name: checkpoint-pvc\n"
                + "          mountPath: /opt/flink/checkpoints\n"
                + "        - name: savepoint-pvc\n"
                + "          mountPath: /opt/flink/savepoints\n"
                + "  volumes:\n"
                + "    - name: checkpoint-pvc\n"
                + "      persistentVolumeClaim:\n"
                + "        claimName: flink-checkpoint\n"
                + "    - name: savepoint-pvc\n"
                + "      persistentVolumeClaim:\n"
                + "        claimName: flink-savepoint\n",
            "apiVersion: v1\n"
                + "kind: Pod\n"
                + "metadata:\n"
                + "  name: pod-template\n"
                + "spec:\n"
                + "  containers:\n"
                + "  - name: flink-main-container\n"
                + "    volumeMounts:\n"
                + "    - name: checkpoint-pvc\n"
                + "      mountPath: /opt/flink/checkpoints\n"
                + "    - name: savepoint-pvc\n"
                + "      mountPath: /opt/flink/savepoints\n"
                + "  volumes:\n"
                + "  - name: checkpoint-pvc\n"
                + "    persistentVolumeClaim:\n"
                + "      claimName: flink-checkpoint\n"
                + "  - name: savepoint-pvc\n"
                + "    persistentVolumeClaim:\n"
                + "      claimName: flink-savepoint\n");

        for (Map.Entry<String, String> expect : podTemplateExpect.entrySet()) {
            String res = PodTemplateParser.completeInitPodTemplate(expect.getKey());
            Assertions.assertEquals(expect.getValue().trim(), res.trim());
        }
    }

    @Test
    void testExtractHostAliasMapFromPodTemplate() {
        Map<String, Map<String, String>> expected = new HashMap<>();
        expected.put(
            "apiVersion: v1\n"
                + "kind: Pod\n"
                + "metadata:\n"
                + "  name: pod-template\n"
                + "spec:\n"
                + "  containers:\n"
                + "  - name: flink-main-container\n"
                + "    volumeMounts:\n"
                + "    - name: checkpoint-pvc\n"
                + "      mountPath: /opt/flink/checkpoints\n"
                + "    - name: savepoint-pvc\n"
                + "      mountPath: /opt/flink/savepoints\n"
                + "  volumes:\n"
                + "  - name: checkpoint-pvc\n"
                + "    persistentVolumeClaim:\n"
                + "      claimName: flink-checkpoint\n"
                + "  - name: savepoint-pvc\n"
                + "    persistentVolumeClaim:\n"
                + "      claimName: flink-savepoint\n"
                + "  hostAliases:\n"
                + "  - ip: 192.168.3.114\n"
                + "    hostnames:\n"
                + "    - hdp01.assad.site\n"
                + "    - hdp01\n"
                + "  - ip: 192.168.3.116\n"
                + "    hostnames:\n"
                + "    - hdp03.assad.site\n"
                + "  - ip: 192.168.3.115\n"
                + "    hostnames:\n"
                + "    - hdp02.assad.site\n"
                + "    - hdp02\n",
            new HashMap<String, String>() {

                {
                    put("hdp01.assad.site", "192.168.3.114");
                    put("hdp01", "192.168.3.114");
                    put("hdp03.assad.site", "192.168.3.116");
                    put("hdp02.assad.site", "192.168.3.115");
                    put("hdp02", "192.168.3.115");
                }
            });
        expected.put(
            "apiVersion: v1\n"
                + "kind: Pod\n"
                + "metadata:\n"
                + "  name: pod-template\n"
                + "spec:\n"
                + "  containers:\n"
                + "  - name: flink-main-container\n"
                + "    volumeMounts:\n"
                + "    - name: checkpoint-pvc\n"
                + "      mountPath: /opt/flink/checkpoints\n"
                + "    - name: savepoint-pvc\n"
                + "      mountPath: /opt/flink/savepoints\n"
                + "  volumes:\n"
                + "  - name: checkpoint-pvc\n"
                + "    persistentVolumeClaim:\n"
                + "      claimName: flink-checkpoint\n"
                + "  - name: savepoint-pvc\n"
                + "    persistentVolumeClaim:\n"
                + "      claimName: flink-savepoint\n"
                + "  hostAliases:\n",
            new HashMap<>());
        expected.put(
            "apiVersion: v1\n"
                + "kind: Pod\n"
                + "metadata:\n"
                + "  name: pod-template\n"
                + "spec:\n"
                + "  hostAliases:\n"
                + "  - ip:\n"
                + "    hostnames:\n"
                + "    - hdp01.assad.site\n"
                + "    - hdp01\n"
                + "  - ip: 192.168.3.116\n"
                + "    hostname:\n"
                + "    - hdp03.assad.site\n"
                + "  - ip: 192.168.3.115\n"
                + "    hostnames:\n"
                + "    - hdp02.assad.site\n"
                + "  - ip: 192.168.3.115\n"
                + "    hostname: hdp02.assad.site\n",
            new HashMap<String, String>() {

                {
                    put("hdp02.assad.site", "192.168.3.115");
                }
            });
        expected.put(
            "apiVersion: v1\n"
                + "kind: Pod\n"
                + "metadata:\n"
                + "  name: pod-template\n"
                + "spec: 2333\n",
            new HashMap<>());

        for (Map.Entry<String, Map<String, String>> expect : expected.entrySet()) {
            Map<String, String> hostsMap = PodTemplateParser.extractHostAliasMap(expect.getKey());
            Map<String, String> map = expect.getValue();
            Assertions.assertEquals(map.size(), hostsMap.size());
            for (Map.Entry<String, String> entry : hostsMap.entrySet()) {
                Assertions.assertEquals(map.get(entry.getKey()), entry.getValue());
            }
        }
    }
}
