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

import org.apache.streampark.flink.kubernetes.model.K8sPodTemplates;
import org.apache.streampark.spark.kubernetes.model.SparkK8sPodTemplates;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PodTemplateTool {

    public static final PodTemplateType KUBERNETES_POD_TEMPLATE =
        new PodTemplateType("kubernetes.pod-template-file", "pod-template.yaml");

    public static final PodTemplateType KUBERNETES_JM_POD_TEMPLATE =
        new PodTemplateType("kubernetes.pod-template-file.jobmanager", "jm-pod-template.yaml");

    public static final PodTemplateType KUBERNETES_TM_POD_TEMPLATE =
        new PodTemplateType("kubernetes.pod-template-file.taskmanager", "tm-pod-template.yaml");

    public static final PodTemplateType KUBERNETES_DRIVER_POD_TEMPLATE =
        new PodTemplateType("spark.kubernetes.driver.podTemplateFile", "driver-pod-template.yaml");

    public static final PodTemplateType KUBERNETES_EXECUTOR_POD_TEMPLATE =
        new PodTemplateType("spark.kubernetes.executor.podTemplateFile", "executor-pod-template.yaml");

    private PodTemplateTool() {
    }

    /**
     * Prepare kubernetes pod template file to buildWorkspace direactory.
     *
     * @param buildWorkspace project workspace dir of flink job
     * @param podTemplates flink kubernetes pod templates
     * @return Map[k8s pod template option, template file output path]
     */
    public static K8sPodTemplateFiles preparePodTemplateFiles(
                                                              String buildWorkspace,
                                                              K8sPodTemplates podTemplates) throws IOException {
        File workspaceDir = new File(buildWorkspace);
        if (!workspaceDir.exists()) {
            workspaceDir.mkdir();
        }

        Map<String, String> podTempleMap = new HashMap<>();
        writeTemplate(buildWorkspace, podTemplates.podTemplate(), KUBERNETES_POD_TEMPLATE, podTempleMap);
        writeTemplate(buildWorkspace, podTemplates.jmPodTemplate(), KUBERNETES_JM_POD_TEMPLATE, podTempleMap);
        writeTemplate(buildWorkspace, podTemplates.tmPodTemplate(), KUBERNETES_TM_POD_TEMPLATE, podTempleMap);
        return new K8sPodTemplateFiles(Collections.unmodifiableMap(podTempleMap));
    }

    /**
     * Prepare kubernetes pod template file to buildWorkspace direactory.
     *
     * @param buildWorkspace project workspace dir of spark job
     * @param podTemplates spark kubernetes pod templates
     * @return Map[k8s pod template option, template file output path]
     */
    public static K8sPodTemplateFiles preparePodTemplateFiles(
                                                              String buildWorkspace,
                                                              SparkK8sPodTemplates podTemplates) throws IOException {
        File workspaceDir = new File(buildWorkspace);
        if (!workspaceDir.exists()) {
            workspaceDir.mkdir();
        }

        Map<String, String> podTempleMap = new HashMap<>();
        writeTemplate(buildWorkspace, podTemplates.driverPodTemplate(), KUBERNETES_DRIVER_POD_TEMPLATE, podTempleMap);
        writeTemplate(
            buildWorkspace, podTemplates.executorPodTemplate(), KUBERNETES_EXECUTOR_POD_TEMPLATE, podTempleMap);
        return new K8sPodTemplateFiles(Collections.unmodifiableMap(podTempleMap));
    }

    private static void writeTemplate(
                                      String buildWorkspace,
                                      String tmplContent,
                                      PodTemplateType podTmpl,
                                      Map<String, String> podTempleMap) throws IOException {
        if (StringUtils.isNotBlank(tmplContent)) {
            String outputPath = new File(buildWorkspace, podTmpl.fileName()).getPath();
            File outputFile = new File(outputPath);
            FileUtils.write(outputFile, tmplContent, "UTF-8");
            podTempleMap.put(podTmpl.key(), outputPath);
        }
    }
}
