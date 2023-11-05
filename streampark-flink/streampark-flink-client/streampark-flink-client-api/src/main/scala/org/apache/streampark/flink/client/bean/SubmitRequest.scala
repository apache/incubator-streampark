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

package org.apache.streampark.flink.client.bean

import org.apache.streampark.common.Constant
import org.apache.streampark.common.conf.{FlinkVersion, Workspace}
import org.apache.streampark.common.conf.ConfigKeys._
import org.apache.streampark.common.enums._
import org.apache.streampark.common.util.{DeflaterUtils, HdfsUtils, PropertiesUtils}
import org.apache.streampark.flink.packer.pipeline.{BuildResult, ShadedBuildResponse}
import org.apache.streampark.flink.util.FlinkUtils
import org.apache.streampark.shaded.com.fasterxml.jackson.databind.ObjectMapper

import org.apache.commons.io.FileUtils
import org.apache.flink.runtime.jobgraph.{SavepointConfigOptions, SavepointRestoreSettings}

import javax.annotation.Nullable

import java.io.File
import java.net.URL
import java.util.{Map => JavaMap}

import scala.collection.convert.ImplicitConversions._
import scala.util.Try

case class SubmitRequest(
    flinkVersion: FlinkVersion,
    executionMode: FlinkExecutionMode,
    properties: JavaMap[String, Any],
    flinkYaml: String,
    developmentMode: FlinkDevelopmentMode,
    id: Long,
    jobId: String,
    appName: String,
    appConf: String,
    applicationType: ApplicationType,
    savePoint: String,
    restoreMode: FlinkRestoreMode,
    args: String,
    @Nullable buildResult: BuildResult,
    @Nullable k8sSubmitParam: KubernetesSubmitParam,
    @Nullable extraParameter: JavaMap[String, Any]) {

  private[this] val appProperties: Map[String, String] = getParameterMap(KEY_FLINK_PROPERTY_PREFIX)

  val appOption: Map[String, String] = getParameterMap(KEY_FLINK_OPTION_PREFIX)

  val appMain: String = this.developmentMode match {
    case FlinkDevelopmentMode.FLINK_SQL => Constant.STREAMPARK_FLINKSQL_CLIENT_CLASS
    case FlinkDevelopmentMode.PYFLINK => Constant.PYTHON_FLINK_DRIVER_CLASS_NAME
    case _ => appProperties(KEY_FLINK_APPLICATION_MAIN_CLASS)
  }

  val effectiveAppName: String =
    if (this.appName == null) appProperties(KEY_FLINK_APP_NAME) else this.appName

  val classPaths: List[URL] = {
    val path = s"${Workspace.local.APP_WORKSPACE}/$id/lib"
    val lib = Try(new File(path).listFiles().map(_.toURI.toURL).toList).getOrElse(List.empty[URL])
    flinkVersion.flinkLibs ++ lib
  }

  val flinkSQL: String = extraParameter.get(KEY_FLINK_SQL()).toString

  val allowNonRestoredState: Boolean = Try(
    properties.get(SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE.key).toString.toBoolean)
    .getOrElse(false)

  val savepointRestoreSettings: SavepointRestoreSettings = {
    savePoint match {
      case sp if Try(sp.isEmpty).getOrElse(true) => SavepointRestoreSettings.none
      case sp => SavepointRestoreSettings.forPath(sp, allowNonRestoredState)
    }
  }

  val userJarFile: File = {
    executionMode match {
      case FlinkExecutionMode.KUBERNETES_NATIVE_APPLICATION => null
      case _ =>
        checkBuildResult()
        new File(buildResult.asInstanceOf[ShadedBuildResponse].shadedJarPath)
    }
  }

  val safePackageProgram: Boolean = {
    // ref FLINK-21164 FLINK-9844 packageProgram.close()
    // must be flink 1.12.2 and above
    flinkVersion.version.split("\\.").map(_.trim.toInt) match {
      case Array(a, b, c) if a >= 1 => b > 12 || (b == 12 && c >= 2)
      case _ => false
    }
  }

  private[this] def getParameterMap(prefix: String = ""): Map[String, String] = {
    if (this.appConf == null) {
      return Map.empty[String, String]
    }
    val format = this.appConf.substring(0, 7)
    if (format == "json://") {
      val json = this.appConf.drop(7)
      new ObjectMapper()
        .readValue[JavaMap[String, String]](json, classOf[JavaMap[String, String]])
        .toMap
        .filter(_._2 != null)
    } else {
      lazy val content = DeflaterUtils.unzipString(this.appConf.trim.drop(7))
      val map = format match {
        case "yaml://" => PropertiesUtils.fromYamlText(content)
        case "conf://" => PropertiesUtils.fromHoconText(content)
        case "prop://" => PropertiesUtils.fromPropertiesText(content)
        case "hdfs://" =>
          /**
           * If the configuration file is HDFS mode, you need to copy the HDFS related configuration
           * file to resources.
           */
          val text = HdfsUtils.read(this.appConf)
          val extension = this.appConf.split("\\.").last.toLowerCase
          extension match {
            case "yml" | "yaml" => PropertiesUtils.fromYamlText(text)
            case "conf" => PropertiesUtils.fromHoconText(text)
            case "properties" => PropertiesUtils.fromPropertiesText(text)
            case _ =>
              throw new IllegalArgumentException(
                "[StreamPark] Usage: application config format error,must be [yaml|conf|properties]")
          }
        case _ =>
          throw new IllegalArgumentException("[StreamPark] application config format error.")
      }
      map
        .filter(_._1.startsWith(prefix))
        .filter(_._2.nonEmpty)
        .map(x => x._1.drop(prefix.length) -> x._2)
    }
  }

  private[client] lazy val hdfsWorkspace = {

    /**
     * The flink version and configuration in the native flink and hdfs must be kept exactly the
     * same.
     */
    val workspace = Workspace.remote
    val flinkHome = flinkVersion.flinkHome
    val flinkHomeDir = new File(flinkHome)
    val flinkName = if (FileUtils.isSymlink(flinkHomeDir)) {
      flinkHomeDir.getCanonicalFile.getName
    } else {
      flinkHomeDir.getName
    }
    val flinkHdfsHome = s"${workspace.APP_FLINK}/$flinkName"
    HdfsWorkspace(
      flinkName,
      flinkHome,
      flinkLib = s"$flinkHdfsHome/lib",
      flinkPlugins = s"$flinkHdfsHome/plugins",
      flinkDistJar = FlinkUtils.getFlinkDistJar(flinkHome),
      appJars = workspace.APP_JARS,
      appPlugins = workspace.APP_PLUGINS
    )
  }

  @throws[Exception]
  def checkBuildResult(): Unit = {
    executionMode match {
      case FlinkExecutionMode.KUBERNETES_NATIVE_SESSION =>
        if (buildResult == null) {
          throw new Exception(
            s"[flink-submit] current job: ${this.effectiveAppName} was not yet built, buildResult is empty" +
              s",clusterId=${k8sSubmitParam.clusterId}," +
              s",namespace=${k8sSubmitParam.kubernetesNamespace}")
        }
        if (!buildResult.pass) {
          throw new Exception(
            s"[flink-submit] current job ${this.effectiveAppName} build failed, clusterId" +
              s",clusterId=${k8sSubmitParam.clusterId}," +
              s",namespace=${k8sSubmitParam.kubernetesNamespace}")
        }
      case _ =>
        if (this.buildResult == null) {
          throw new Exception(
            s"[flink-submit] current job: ${this.effectiveAppName} was not yet built, buildResult is empty")
        }
        if (!this.buildResult.pass) {
          throw new Exception(
            s"[flink-submit] current job ${this.effectiveAppName} build failed, please check")
        }
    }
  }

}

/**
 * @param flinkName
 * @param flinkHome
 * @param flinkDistJar
 * @param flinkLib
 * @param flinkPlugins
 * @param appJars
 * @param appPlugins
 */
case class HdfsWorkspace(
    flinkName: String,
    flinkHome: String,
    flinkDistJar: String,
    flinkLib: String,
    flinkPlugins: String,
    appJars: String,
    appPlugins: String)
