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

import com.streamxhub.streamx.common.conf.Workspace
import com.streamxhub.streamx.common.domain.FlinkVersion
import com.streamxhub.streamx.common.enums.{DevelopmentMode, ExecutionMode}
import com.streamxhub.streamx.flink.kubernetes.model.K8sPodTemplates
import com.streamxhub.streamx.flink.packer.docker.DockerAuthConf
import com.streamxhub.streamx.flink.packer.maven.DependencyInfo

import scala.collection.mutable.ArrayBuffer

/**
 * Params of a BuildPipeline instance.
 *
 * @author Al-assad
 */
sealed trait BuildParam {
  def appName: String

  def mainClass: String
}

sealed trait FlinkBuildParam extends BuildParam {

  private[this] val localWorkspace = Workspace.local

  def workspace: String

  def executionMode: ExecutionMode

  def developmentMode: DevelopmentMode

  def flinkVersion: FlinkVersion

  def dependencyInfo: DependencyInfo

  def customFlinkUserJar: String

  lazy val providedLibs: DependencyInfo = {
    val providedLibs = ArrayBuffer(
      localWorkspace.APP_JARS,
      localWorkspace.APP_PLUGINS,
      customFlinkUserJar
    )
    if (developmentMode == DevelopmentMode.FLINKSQL) {
      providedLibs += {
        val version = flinkVersion.version.split("\\.").map(_.trim.toInt)
        version match {
          case Array(1, 12, _) => s"${localWorkspace.APP_SHIMS}/flink-1.12"
          case Array(1, 13, _) => s"${localWorkspace.APP_SHIMS}/flink-1.13"
          case Array(1, 14, _) => s"${localWorkspace.APP_SHIMS}/flink-1.14"
          case Array(1, 15, _) => s"${localWorkspace.APP_SHIMS}/flink-1.15"
          case _ => throw new UnsupportedOperationException(s"Unsupported flink version: $flinkVersion")
        }
      }
    }
    dependencyInfo.merge(providedLibs.toSet)
  }

  def getShadedJarPath(rootWorkspace: String): String = {
    val safeAppName: String = appName.replaceAll("\\s+", "_")
    s"$rootWorkspace/streamx-flinkjob_$safeAppName.jar"
  }

}

sealed trait FlinkK8sBuildParam extends FlinkBuildParam {

  def clusterId: String

  def k8sNamespace: String
}

case class FlinkK8sSessionBuildRequest(appName: String,
                                       workspace: String,
                                       mainClass: String,
                                       customFlinkUserJar: String,
                                       executionMode: ExecutionMode,
                                       developmentMode: DevelopmentMode,
                                       flinkVersion: FlinkVersion,
                                       dependencyInfo: DependencyInfo,
                                       clusterId: String,
                                       k8sNamespace: String
                                      ) extends FlinkK8sBuildParam

case class FlinkK8sApplicationBuildRequest(appName: String,
                                           workspace: String,
                                           mainClass: String,
                                           customFlinkUserJar: String,
                                           executionMode: ExecutionMode,
                                           developmentMode: DevelopmentMode,
                                           flinkVersion: FlinkVersion,
                                           dependencyInfo: DependencyInfo,
                                           clusterId: String,
                                           k8sNamespace: String,
                                           flinkBaseImage: String,
                                           flinkPodTemplate: K8sPodTemplates,
                                           integrateWithHadoop: Boolean = false,
                                           dockerAuthConfig: DockerAuthConf
                                          ) extends FlinkK8sBuildParam {

}

case class FlinkRemotePerJobBuildRequest(appName: String,
                                         workspace: String,
                                         mainClass: String,
                                         customFlinkUserJar: String,
                                         skipBuild: Boolean,
                                         executionMode: ExecutionMode,
                                         developmentMode: DevelopmentMode,
                                         flinkVersion: FlinkVersion,
                                         dependencyInfo: DependencyInfo
                                  ) extends FlinkBuildParam {


}


case class FlinkYarnApplicationBuildRequest(appName: String,
                                            mainClass: String,
                                            localWorkspace: String,
                                            yarnProvidedPath: String,
                                            developmentMode: DevelopmentMode,
                                            dependencyInfo: DependencyInfo) extends BuildParam
