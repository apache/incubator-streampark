/*
 * Copyright (c) 2021 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>œ
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.flink.kubernetes

import com.streamxhub.streamx.flink.kubernetes.model.K8sPodTemplates
import org.apache.commons.io.FileUtils
import org.apache.flink.configuration.ConfigOptions.key
import org.apache.flink.configuration.{ConfigOption, Configuration}
import scala.collection.JavaConverters

import java.io.File
import scala.collection.mutable

/**
 * @author Al-assad
 */
object PodTemplateTool {

  val KUBERNETES_POD_TEMPLATE_FILE: ConfigOption[String] = key("kubernetes.pod-template-file")
    .stringType()
    .noDefaultValue()

  val KUBERNETES_POD_TEMPLATE_FILE_JOBMANAGER: ConfigOption[String] = key("kubernetes.pod-template-file.jobmanager")
    .stringType()
    .noDefaultValue()

  val KUBERNETES_POD_TEMPLATE_FILE_TASKMANAGER: ConfigOption[String] = key("kubernetes.pod-template-file.taskmanager")
    .stringType()
    .noDefaultValue()

  val K8S_POD_TEMPLATE_DEFAULT_FILE_NAME: Map[ConfigOption[String], String] = Map(
    KUBERNETES_POD_TEMPLATE_FILE -> "pod-template.yaml",
    KUBERNETES_POD_TEMPLATE_FILE_JOBMANAGER -> "jm-pod-template.yaml",
    KUBERNETES_POD_TEMPLATE_FILE_TASKMANAGER -> "tm-pod-template.yaml")

  /**
   * Prepare kubernetes pod template file to buildWorkspace direactory.
   *
   * @param buildWorkspace project workspace dir of flink job
   * @param podTemplates   flink kubernetes pod templates
   * @return Map[k8s pod template option, template file output path]
   */
  def preparePodTemplateFiles(buildWorkspace: String, podTemplates: K8sPodTemplates): K8sPodTemplateFiles = {
    val workspaceDir = new File(buildWorkspace)
    if (!workspaceDir.exists())
      workspaceDir.mkdir()

    val podTempleMap = mutable.Map[ConfigOption[String], String]()
    val outputTmplContent = (tmplContent: String, tmplOption: ConfigOption[String]) => {
      if (tmplContent.nonEmpty) {
        val outputPath = s"${buildWorkspace}/${K8S_POD_TEMPLATE_DEFAULT_FILE_NAME(tmplOption)}"
        val outputFile = new File(outputPath)
        FileUtils.write(outputFile, tmplContent, "UTF-8")
        podTempleMap += (tmplOption -> outputPath)
      }
    }

    outputTmplContent(podTemplates.podTemplate, KUBERNETES_POD_TEMPLATE_FILE)
    outputTmplContent(podTemplates.jmPodTemplate, KUBERNETES_POD_TEMPLATE_FILE_JOBMANAGER)
    outputTmplContent(podTemplates.tmPodTemplate, KUBERNETES_POD_TEMPLATE_FILE_TASKMANAGER)
    K8sPodTemplateFiles(podTempleMap.toMap)
  }


}


case class K8sPodTemplateFiles(tmplFiles: Map[ConfigOption[String], String]) {

  /**
   * merge k8s pod template configuration to Flink Cnfiguartion
   */
  def mergeToFlinkConf(flinkConf: Configuration): Unit =
    tmplFiles.filter(_._2.nonEmpty).foreach(e => flinkConf.set(e._1, e._2))

}
