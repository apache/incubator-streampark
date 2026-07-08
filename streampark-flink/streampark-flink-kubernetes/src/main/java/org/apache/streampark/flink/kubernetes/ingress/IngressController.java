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
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.DefaultKubernetesClient;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public final class IngressController {

    private static final Pattern VERSION_REGEXP = Pattern.compile("(\\d{1,3}\\.\\d{1,3})");
    private static final double clusterVersion;
    private static final IngressStrategy ingressStrategy;

    static {
        double version = 1.19;
        try {
            version = AutoCloseUtils.using(new DefaultKubernetesClient(), client -> {
                Matcher matcher = VERSION_REGEXP.matcher(client.getVersion().getGitVersion());
                if (matcher.find()) {
                    return Double.parseDouble(matcher.group(1));
                }
                return 1.19;
            });
        } catch (Exception ignored) {
        }
        clusterVersion = version;
        ingressStrategy = clusterVersion >= 1.19 ? new IngressStrategyV1() : new IngressStrategyV1beta1();
    }

    private IngressController() {
    }

    public static void configureIngress(String domainName, String clusterId, String nameSpace) {
        ingressStrategy.configureIngress(domainName, clusterId, nameSpace);
    }

    public static String getIngressUrlAddress(String nameSpace, String clusterId, ClusterClient<?> clusterClient) {
        return ingressStrategy.getIngressUrl(nameSpace, clusterId, clusterClient);
    }

    public static String prepareIngressTemplateFiles(String buildWorkspace, String ingressTemplates) throws Exception {
        return ingressStrategy.prepareIngressTemplateFiles(buildWorkspace, ingressTemplates);
    }
}
