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

package org.apache.streampark.flink.packer.pipeline

import org.apache.streampark.common.conf.{FlinkVersion, SparkVersion, Workspace}
import org.apache.streampark.common.enums.{FlinkDeployMode, FlinkJobType, SparkDeployMode, SparkJobType}
import org.apache.streampark.flink.kubernetes.model.K8sPodTemplates
import org.apache.streampark.flink.packer.docker.DockerConf
import org.apache.streampark.flink.packer.maven.DependencyInfo

import scala.collection.mutable.ArrayBuffer

/** Params of a BuildPipeline instance. */
sealed trait BuildParam {
  def appName: String

  def mainClass: String
}

sealed trait FlinkBuildParam extends BuildParam {

  private[this] val localWorkspace = Workspace.local

  def workspace: String

  def deployMode: FlinkDeployMode

  def flinkJobType: FlinkJobType

  def flinkVersion: FlinkVersion

  def dependencyInfo: DependencyInfo

  def customFlinkUserJar: String

  lazy val providedLibs: DependencyInfo = {
    val providedLibs =
      ArrayBuffer(localWorkspace.APP_JARS, customFlinkUserJar)
    if (flinkJobType == FlinkJobType.FLINK_SQL) {
      providedLibs += s"${localWorkspace.APP_SHIMS}/flink-${flinkVersion.majorVersion}"
    }
    dependencyInfo.merge(providedLibs.toSet)
  }

  def getShadedJarPath(rootWorkspace: String): String = {
    val safeAppName: String = appName.replaceAll("\\s+", "_")
    s"$rootWorkspace/streampark-flinkjob_$safeAppName.jar"
  }

}

sealed trait SparkBuildParam extends BuildParam {

  private[this] val localWorkspace = Workspace.local

  def workspace: String

  def deployMode: SparkDeployMode

  def jobType: SparkJobType

  def sparkVersion: SparkVersion

  def dependencyInfo: DependencyInfo

  def customSparkUserJar: String

  lazy val providedLibs: DependencyInfo = {
    val providedLibs =
      ArrayBuffer(localWorkspace.APP_JARS, customSparkUserJar)
    if (jobType == SparkJobType.SPARK_SQL) {
      providedLibs += s"${localWorkspace.APP_SHIMS}/spark-${sparkVersion.majorVersion}"
    }
    dependencyInfo.merge(providedLibs.toSet)
  }

  def getShadedJarPath(rootWorkspace: String): String = {
    val safeAppName: String = appName.replaceAll("\\s+", "_")
    s"$rootWorkspace/streampark-sparkjob_$safeAppName.jar"
  }

}

sealed trait FlinkK8sBuildParam extends FlinkBuildParam {

  def clusterId: String

  def k8sNamespace: String
}

case class FlinkK8sSessionBuildRequest(
    appName: String,
    workspace: String,
    mainClass: String,
    customFlinkUserJar: String,
    deployMode: FlinkDeployMode,
    flinkJobType: FlinkJobType,
    flinkVersion: FlinkVersion,
    dependencyInfo: DependencyInfo,
    clusterId: String,
    k8sNamespace: String)
  extends FlinkK8sBuildParam

case class FlinkK8sApplicationBuildRequest(
    appName: String,
    workspace: String,
    mainClass: String,
    customFlinkUserJar: String,
    deployMode: FlinkDeployMode,
    flinkJobType: FlinkJobType,
    flinkVersion: FlinkVersion,
    dependencyInfo: DependencyInfo,
    clusterId: String,
    k8sNamespace: String,
    flinkBaseImage: String,
    flinkPodTemplate: K8sPodTemplates,
    integrateWithHadoop: Boolean = false,
    dockerConfig: DockerConf,
    ingressTemplate: String)
  extends FlinkK8sBuildParam

case class FlinkRemotePerJobBuildRequest(
    appName: String,
    workspace: String,
    mainClass: String,
    customFlinkUserJar: String,
    skipBuild: Boolean,
    deployMode: FlinkDeployMode,
    flinkJobType: FlinkJobType,
    flinkVersion: FlinkVersion,
    dependencyInfo: DependencyInfo)
  extends FlinkBuildParam

case class FlinkYarnApplicationBuildRequest(
    appName: String,
    mainClass: String,
    localWorkspace: String,
    yarnProvidedPath: String,
    flinkJobType: FlinkJobType,
    dependencyInfo: DependencyInfo)
  extends BuildParam

case class SparkYarnBuildRequest(
    appName: String,
    mainClass: String,
    localWorkspace: String,
    yarnProvidedPath: String,
    sparkJobType: SparkJobType,
    deployMode: SparkDeployMode,
    dependencyInfo: DependencyInfo)
  extends SparkBuildParam
