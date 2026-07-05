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
package org.apache.streampark.flink.kubernetes.helper;

import org.apache.streampark.common.util.AutoCloseUtils;
import org.apache.streampark.common.util.SystemPropertyUtils;
import org.apache.streampark.flink.kubernetes.KubernetesRetriever;

import org.apache.commons.io.FileUtils;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.api.model.Pod;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.DefaultKubernetesClient;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Slf4j
public final class KubernetesDeploymentHelper {

    private KubernetesDeploymentHelper() {}

    private static List<Pod> getPods(String nameSpace, String deploymentName) {
        try {
            return AutoCloseUtils.using(
                KubernetesRetriever.newK8sClient(),
                client -> {
                    try {
                        return client.pods()
                            .inNamespace(nameSpace)
                            .withLabels(client.apps().deployments().inNamespace(nameSpace)
                                .withName(deploymentName).get().getSpec().getSelector().getMatchLabels())
                            .list().getItems();
                    } catch (Exception e) {
                        return Collections.<Pod>emptyList();
                    }
                });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static boolean isDeploymentError(String nameSpace, String deploymentName) {
        try {
            List<Pod> pods = getPods(nameSpace, deploymentName);
            if (pods.isEmpty()) return true;
            var podStatus = pods.get(0).getStatus();
            switch (podStatus.getPhase()) {
                case "Unknown":
                case "Failed":
                    return true;
                case "Pending":
                    return false;
                default:
                    return podStatus.getContainerStatuses().get(0).getLastState().getTerminated() != null;
            }
        } catch (Exception e) {
            return true;
        }
    }

    public static void delete(String nameSpace, String deploymentName) {
        AutoCloseUtils.using(KubernetesRetriever.newK8sClient(), client -> {
            var map = client.apps().deployments().inNamespace(nameSpace);
            map.withLabel("app", deploymentName).delete();
            map.withName(deploymentName).delete();
            var cm = client.configMaps().inNamespace(nameSpace);
            cm.withLabel("app", deploymentName).delete();
            cm.withName(deploymentName).delete();
            return null;
        });
    }

    public static boolean checkConnection() {
        try (DefaultKubernetesClient client = new DefaultKubernetesClient()) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String watchDeploymentLog(String nameSpace, String jobName, String jobId) {
        return watchPodTerminatedLog(nameSpace, jobName, jobId);
    }

    public static String watchPodTerminatedLog(String nameSpace, String jobName, String jobId) {
        return AutoCloseUtils.using(
            KubernetesRetriever.newK8sClient(),
            client -> {
                try {
                    String podName = getPods(nameSpace, jobName).get(0).getMetadata().getName();
                    String path = getJobErrorLog(jobId);
                    String logContent = client.pods().inNamespace(nameSpace).withName(podName)
                        .terminated().withPrettyOutput().getLog();
                    FileUtils.writeStringToFile(new File(path), logContent, StandardCharsets.UTF_8);
                    return path;
                } catch (Exception e) {
                    return null;
                }
            },
            error -> { throw new RuntimeException(error); });
    }

    public static String getJobLog(String jobId) {
        return SystemPropertyUtils.getTmpdir() + "/" + jobId + ".log";
    }

    public static String getJobErrorLog(String jobId) {
        return SystemPropertyUtils.getTmpdir() + "/" + jobId + "_err.log";
    }
}
