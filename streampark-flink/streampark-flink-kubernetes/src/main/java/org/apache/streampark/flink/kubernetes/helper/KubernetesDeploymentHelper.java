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
import org.apache.streampark.common.util.LoggerSupport;
import org.apache.streampark.common.util.SystemPropertyUtils;
import org.apache.streampark.flink.kubernetes.KubernetesRetriever;

import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.api.model.Pod;
import org.apache.flink.kubernetes.shaded.io.fabric8.kubernetes.client.DefaultKubernetesClient;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class KubernetesDeploymentHelper extends LoggerSupport {

    private static final KubernetesDeploymentHelper INSTANCE = new KubernetesDeploymentHelper();

    private KubernetesDeploymentHelper() {
    }

    private static List<Pod> getPods(String nameSpace, String deploymentName) {
        return AutoCloseUtils.using(
            KubernetesRetriever.newK8sClient(),
            client -> {
                try {
                    Map<String, String> matchLabels =
                        client.apps()
                            .deployments()
                            .inNamespace(nameSpace)
                            .withName(deploymentName)
                            .get()
                            .getSpec()
                            .getSelector()
                            .getMatchLabels();
                    return client.pods()
                        .inNamespace(nameSpace)
                        .withLabels(matchLabels)
                        .list()
                        .getItems();
                } catch (Exception e) {
                    return Collections.<Pod>emptyList();
                }
            });
    }

    public static boolean isDeploymentError(String nameSpace, String deploymentName) {
        try {
            List<Pod> pods = getPods(nameSpace, deploymentName);
            if (pods.isEmpty()) {
                return true;
            }
            String phase = pods.get(0).getStatus().getPhase();
            switch (phase) {
                case "Unknown":
                case "Failed":
                    return true;
                case "Pending":
                    return false;
                default:
                    return pods.get(0).getStatus().getContainerStatuses().get(0).getLastState().getTerminated() != null;
            }
        } catch (Exception e) {
            return true;
        }
    }

    private static void deleteDeployment(String nameSpace, String deploymentName) {
        AutoCloseUtils.using(
            KubernetesRetriever.newK8sClient(),
            client -> {
                client.apps().deployments().inNamespace(nameSpace).withLabel("app", deploymentName).delete();
                client.apps().deployments().inNamespace(nameSpace).withName(deploymentName).delete();
                return null;
            });
    }

    private static void deleteConfigMap(String nameSpace, String deploymentName) {
        AutoCloseUtils.using(
            KubernetesRetriever.newK8sClient(),
            client -> {
                client.configMaps().inNamespace(nameSpace).withLabel("app", deploymentName).delete();
                client.configMaps().inNamespace(nameSpace).withName(deploymentName).delete();
                return null;
            });
    }

    public static void delete(String nameSpace, String deploymentName) {
        deleteDeployment(nameSpace, deploymentName);
        deleteConfigMap(nameSpace, deploymentName);
    }

    public static boolean checkConnection() {
        try {
            DefaultKubernetesClient client = new DefaultKubernetesClient();
            client.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String watchDeploymentLog(String nameSpace, String jobName, String jobId) throws IOException {
        return AutoCloseUtils.using(
            KubernetesRetriever.newK8sClient(),
            client -> {
                try {
                    String path = getJobLog(jobId);
                    File file = new File(path);
                    String log =
                        client.apps().deployments().inNamespace(nameSpace).withName(jobName).getLog();
                    Files.asCharSink(file, Charsets.UTF_8).write(log);
                    return path;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    public static String watchPodTerminatedLog(String nameSpace, String jobName, String jobId) {
        return AutoCloseUtils.using(
            KubernetesRetriever.newK8sClient(),
            client -> {
                try {
                    String podName = getPods(nameSpace, jobName).get(0).getMetadata().getName();
                    String path = getJobErrorLog(jobId);
                    File file = new File(path);
                    String log =
                        client.pods()
                            .inNamespace(nameSpace)
                            .withName(podName)
                            .terminated()
                            .withPrettyOutput()
                            .getLog();
                    Files.asCharSink(file, Charsets.UTF_8).write(log);
                    return path;
                } catch (Exception e) {
                    return null;
                }
            },
            error -> {
                throw new RuntimeException(error);
            });
    }

    public static String getJobLog(String jobId) {
        String tmpPath = SystemPropertyUtils.getTmpdir();
        return tmpPath + "/" + jobId + ".log";
    }

    public static String getJobErrorLog(String jobId) {
        String tmpPath = SystemPropertyUtils.getTmpdir();
        return tmpPath + "/" + jobId + "_err.log";
    }
}
