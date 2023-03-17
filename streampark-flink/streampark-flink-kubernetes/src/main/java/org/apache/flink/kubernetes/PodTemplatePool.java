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

package org.apache.flink.kubernetes;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.kubernetes.model.K8sPodTemplates;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class PodTemplatePool {
  public static final PodTemplateType KUBERNETES_POD_TEMPLATE =
      new PodTemplateType("kubernetes.pod-template-file", "pod-template.yaml");
  public static final PodTemplateType KUBERNETES_JM_POD_TEMPLATE =
      new PodTemplateType("kubernetes.pod-template-file.jobmanager", "jm-pod-template.yaml");
  public static final PodTemplateType KUBERNETES_TM_POD_TEMPLATE =
      new PodTemplateType("kubernetes.pod-template-file.taskmanager", "tm-pod-template.yaml");

  public static K8sPodTemplateFiles preparePodTemplateFiles(
      String buildWorkspace, K8sPodTemplates podTemplates) throws IOException {
    File workspace = new File(buildWorkspace);
    if (!workspace.exists()) {
      workspace.mkdir();
    }
    Map<String, String> podTempleMap = new LinkedHashMap<>();
    writePodTemplateFiles(
        podTemplates.getPodTemplate(), KUBERNETES_POD_TEMPLATE, buildWorkspace, podTempleMap);
    writePodTemplateFiles(
        podTemplates.getJmPodTemplate(), KUBERNETES_JM_POD_TEMPLATE, buildWorkspace, podTempleMap);
    writePodTemplateFiles(
        podTemplates.getTmPodTemplate(), KUBERNETES_TM_POD_TEMPLATE, buildWorkspace, podTempleMap);
    return new K8sPodTemplateFiles(podTempleMap);
  }

  private static void writePodTemplateFiles(
      String templateContent,
      PodTemplateType podTemplateType,
      String buildWorkspace,
      Map<String, String> podTempleMap)
      throws IOException {
    if (StringUtils.isNotEmpty(templateContent)) {
      String outputPath = String.format("%s/%s", buildWorkspace, podTemplateType.getFileName());
      File file = new File(outputPath);
      FileUtils.write(file, templateContent, "UTF-8");
      podTempleMap.put(podTemplateType.getKey(), outputPath);
    }
  }

  public static class PodTemplateType {
    private final String key;

    private final String fileName;

    public PodTemplateType(String key, String fileName) {
      this.key = key;
      this.fileName = fileName;
    }

    public String getKey() {
      return key;
    }

    public String getFileName() {
      return fileName;
    }
  }

  public static class K8sPodTemplateFiles {
    private final Map<String, String> templateFiles;

    public K8sPodTemplateFiles(Map<String, String> templateFiles) {
      this.templateFiles = templateFiles;
    }

    public Map<String, String> getTemplateFiles() {
      return templateFiles;
    }

    public void mergeToFlinkConf(Configuration flinkConf) {
      templateFiles.forEach(
          (key, value) -> {
            if (StringUtils.isNotEmpty(value)) {
              flinkConf.setString(key, value);
            }
          });
    }
  }
}
