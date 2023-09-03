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

package org.apache.streampark.flink.kubernetes

import org.apache.streampark.flink.kubernetes.model.K8sPodTemplates

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.flink.configuration.Configuration

import java.io.File

import scala.collection.mutable

object PodTemplateTool {

  val KUBERNETES_POD_TEMPLATE: PodTemplateType =
    PodTemplateType("kubernetes.pod-template-file", "pod-template.yaml")

  val KUBERNETES_JM_POD_TEMPLATE: PodTemplateType =
    PodTemplateType("kubernetes.pod-template-file.jobmanager", "jm-pod-template.yaml")

  val KUBERNETES_TM_POD_TEMPLATE: PodTemplateType =
    PodTemplateType("kubernetes.pod-template-file.taskmanager", "tm-pod-template.yaml")

  /**
   * Prepare kubernetes pod template file to buildWorkspace direactory.
   *
   * @param buildWorkspace
   *   project workspace dir of flink job
   * @param podTemplates
   *   flink kubernetes pod templates
   * @return
   *   Map[k8s pod template option, template file output path]
   */
  def preparePodTemplateFiles(
      buildWorkspace: String,
      podTemplates: K8sPodTemplates): K8sPodTemplateFiles = {
    val workspaceDir = new File(buildWorkspace)
    if (!workspaceDir.exists()) {
      workspaceDir.mkdir()
    }

    val podTempleMap = mutable.Map[String, String]()
    val outputTmplContent = (tmplContent: String, podTmpl: PodTemplateType) => {
      if (StringUtils.isNotBlank(tmplContent)) {
        val outputPath = s"$buildWorkspace/${podTmpl.fileName}"
        val outputFile = new File(outputPath)
        FileUtils.write(outputFile, tmplContent, "UTF-8")
        podTempleMap += (podTmpl.key -> outputPath)
      }
    }

    outputTmplContent(podTemplates.podTemplate, KUBERNETES_POD_TEMPLATE)
    outputTmplContent(podTemplates.jmPodTemplate, KUBERNETES_JM_POD_TEMPLATE)
    outputTmplContent(podTemplates.tmPodTemplate, KUBERNETES_TM_POD_TEMPLATE)
    K8sPodTemplateFiles(podTempleMap.toMap)
  }

}

/**
 * @param tmplFiles
 *   key of flink pod template configuration -> absolute file path of pod template
 */
case class K8sPodTemplateFiles(tmplFiles: Map[String, String]) {

  /** merge k8s pod template configuration to Flink Configuration */
  def mergeToFlinkConf(flinkConf: Configuration): Unit =
    tmplFiles
      .filter(_._2.nonEmpty)
      .foreach(e => flinkConf.setString(e._1, e._2))

}

case class PodTemplateType(key: String, fileName: String)
