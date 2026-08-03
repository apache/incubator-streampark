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
import org.apache.streampark.common.util.LoggerSupport;

import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.DefaultKubernetesClient;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IngressController extends LoggerSupport {

    private static final IngressController INSTANCE = new IngressController();
    private static final Pattern VERSION_REGEXP = Pattern.compile("(\\d+\\.\\d+)");

    private static volatile Double clusterVersion;
    private static volatile IngressStrategy ingressStrategy;

    private IngressController() {
    }

    private static double clusterVersion() {
        if (clusterVersion == null) {
            synchronized (IngressController.class) {
                if (clusterVersion == null) {
                    clusterVersion =
                        AutoCloseUtils.using(
                            new DefaultKubernetesClient(),
                            client -> {
                                Matcher matcher =
                                    VERSION_REGEXP.matcher(client.getVersion().getGitVersion());
                                if (matcher.find()) {
                                    return Double.parseDouble(matcher.group(1));
                                }
                                throw new IllegalStateException("Unable to parse kubernetes version");
                            });
                }
            }
        }
        return clusterVersion;
    }

    private static IngressStrategy ingressStrategy() {
        if (ingressStrategy == null) {
            synchronized (IngressController.class) {
                if (ingressStrategy == null) {
                    ingressStrategy =
                        clusterVersion() >= 1.19 ? new IngressStrategyV1() : new IngressStrategyV1beta1();
                }
            }
        }
        return ingressStrategy;
    }

    public static void configureIngress(String domainName, String clusterId, String nameSpace) {
        ingressStrategy().configureIngress(domainName, clusterId, nameSpace);
    }

    public static String getIngressUrlAddress(
                                              String nameSpace, String clusterId, ClusterClient<?> clusterClient) {
        return ingressStrategy().getIngressUrl(nameSpace, clusterId, clusterClient);
    }

    public static String prepareIngressTemplateFiles(String buildWorkspace,
                                                     String ingressTemplates) throws IOException {
        return ingressStrategy().prepareIngressTemplateFiles(buildWorkspace, ingressTemplates);
    }
}
