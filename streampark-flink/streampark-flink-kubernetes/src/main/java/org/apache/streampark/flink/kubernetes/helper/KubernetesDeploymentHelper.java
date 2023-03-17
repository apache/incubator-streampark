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

import org.apache.streampark.common.util.SystemPropertyUtils;
import org.apache.streampark.flink.kubernetes.KubernetesRetriever;

import org.apache.curator.shaded.com.google.common.base.Charsets;

import com.google.common.io.Files;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class KubernetesDeploymentHelper {
  private static final Logger LOG = LoggerFactory.getLogger(KubernetesDeploymentHelper.class);

  private static List<Pod> getPods(String namespace, String deploymentName) {
    try (KubernetesClient kubernetesClient = KubernetesRetriever.newK8sClient()) {
      return kubernetesClient
          .pods()
          .inNamespace(namespace)
          .withLabels(
              kubernetesClient
                  .apps()
                  .deployments()
                  .inNamespace(namespace)
                  .withName(deploymentName)
                  .get()
                  .getSpec()
                  .getSelector()
                  .getMatchLabels())
          .list()
          .getItems();
    }
  }

  public static boolean getDeploymentStatusChanges(String namespace, String deploymentName) {
    List<Pod> pods = getPods(namespace, deploymentName);
    return pods.get(0).getStatus().getContainerStatuses().get(0).getLastState().getTerminated()
        != null;
  }

  public static int getTheNumberOfTaskDeploymentRetries(String namespace, String deploymentName) {
    List<Pod> pods = getPods(namespace, deploymentName);
    return pods.get(0).getStatus().getContainerStatuses().get(0).getRestartCount();
  }

  public static boolean deleteTaskDeployment(String namespace, String deploymentName) {
    try (KubernetesClient kubernetesClient = KubernetesRetriever.newK8sClient()) {
      return kubernetesClient
          .apps()
          .deployments()
          .inNamespace(namespace)
          .withName(deploymentName)
          .delete();
    }
  }

  public static boolean isTheK8sConnectionNormal() {
    try (KubernetesClient kubernetesClient = KubernetesRetriever.newK8sClient()) {
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static String watchDeploymentLog(String namespace, String jobName, String jobId) {
    try (KubernetesClient kubernetesClient = KubernetesRetriever.newK8sClient()) {
      String jobLogPath = getJobLog(jobId);
      File jobLogFile = new File(jobLogPath);
      String log =
          kubernetesClient.apps().deployments().inNamespace(namespace).withName(jobName).getLog();
      Files.asCharSink(jobLogFile, Charsets.UTF_8).write(log);
      return jobLogPath;
    } catch (Exception e) {
      throw new RuntimeException("Write k8s deployment log failed, log file maybe empty", e);
    }
  }

  public static String watchPodTerminatedLog(String namespace, String jobName, String jobId) {
    try (KubernetesClient kubernetesClient = KubernetesRetriever.newK8sClient()) {
      String podName = getPods(namespace, jobName).get(0).getMetadata().getName();
      String jobErrorLogPath = getJobErrorLog(jobId);
      File jobErrorLogFile = new File(jobErrorLogPath);
      String log =
          kubernetesClient
              .pods()
              .inNamespace(namespace)
              .withName(podName)
              .terminated()
              .withPrettyOutput()
              .getLog();
      Files.asCharSink(jobErrorLogFile, Charsets.UTF_8).write(log);
      return jobErrorLogPath;
    } catch (Exception e) {
      throw new RuntimeException("Write k8s deployment error log failed, log file maybe empty", e);
    }
  }

  public static boolean deleteTaskConfigMap(String namespace, String deploymentName) {
    try (KubernetesClient kubernetesClient = KubernetesRetriever.newK8sClient()) {
      return kubernetesClient
          .configMaps()
          .inNamespace(namespace)
          .withLabel("app", deploymentName)
          .delete();
    } catch (Exception e) {
      return false;
    }
  }

  private static String getJobLog(String jobId) {
    String tmpdir = SystemPropertyUtils.getTmpdir();
    return String.format("%s/%s.log", tmpdir, jobId);
  }

  private static String getJobErrorLog(String jobId) {
    String tmpdir = SystemPropertyUtils.getTmpdir();
    return String.format("%s/%s_err.log", tmpdir, jobId);
  }
}
