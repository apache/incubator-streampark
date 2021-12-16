/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.packer.pipeline

import com.streamxhub.streamx.common.domain.FlinkVersion
import com.streamxhub.streamx.common.enums.{DevelopmentMode, ExecutionMode}
import com.streamxhub.streamx.flink.kubernetes.model.K8sPodTemplates
import com.streamxhub.streamx.flink.packer.docker.DockerAuthConf
import com.streamxhub.streamx.flink.packer.maven.JarPackDeps

/**
 * Params of a BuildPipeline instance.
 *
 * @author Al-assad
 */
sealed trait BuildParam {
  def appName: String
}

sealed trait FlinkBuildParam extends BuildParam {

  def executionMode: ExecutionMode

  def developmentMode: DevelopmentMode

  def flinkVersion: FlinkVersion

  def jarPackDeps: JarPackDeps

  def customFlinkUsrJarPath: String
}

sealed trait FlinkK8sBuildParam extends FlinkBuildParam {

  def clusterId: String

  def k8sNamespace: String
}

case class FlinkK8sSessionBuildRequest(appName: String,
                                       executionMode: ExecutionMode,
                                       developmentMode: DevelopmentMode,
                                       flinkVersion: FlinkVersion,
                                       jarPackDeps: JarPackDeps,
                                       customFlinkUsrJarPath: String,
                                       clusterId: String,
                                       k8sNamespace: String
                                      ) extends FlinkK8sBuildParam

case class FlinkK8sApplicationBuildRequest(appName: String,
                                           executionMode: ExecutionMode,
                                           developmentMode: DevelopmentMode,
                                           flinkVersion: FlinkVersion,
                                           jarPackDeps: JarPackDeps,
                                           customFlinkUsrJarPath: String,
                                           clusterId: String,
                                           k8sNamespace: String,
                                           flinkBaseImage: String,
                                           flinkPodTemplate: K8sPodTemplates,
                                           integrateWithHadoop: Boolean = false,
                                           dockerAuthConfig: DockerAuthConf
                                          ) extends FlinkK8sBuildParam


// todo case class FlinkYarnApplicationBuildRequest() extends FlinkBuildParam

// todo case class FlinkYarnSessionBuildRequest() extends FlinkBuildParam

// todo case class FlinkStandaloneBuildRequest() extends FlinkBuildParam

