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

package org.apache.streampark.spark.client.bean

import org.apache.streampark.common.Constant
import org.apache.streampark.common.conf.{SparkVersion, Workspace}
import org.apache.streampark.common.conf.ConfigKeys._
import org.apache.streampark.common.enums._
import org.apache.streampark.common.util.{DeflaterUtils, HdfsUtils, PropertiesUtils}
import org.apache.streampark.flink.packer.pipeline.{BuildResult, ShadedBuildResponse}

import com.fasterxml.jackson.databind.ObjectMapper

import javax.annotation.Nullable

import java.io.{File, IOException}
import java.net.URL
import java.nio.file.Files
import java.util.{Map => JavaMap}

import scala.collection.convert.ImplicitConversions._
import scala.util.Try

case class SubmitRequest(
    sparkVersion: SparkVersion,
    executionMode: SparkExecutionMode,
    properties: JavaMap[String, Any],
    sparkYaml: String,
    developmentMode: FlinkDevelopmentMode,
    id: Long,
    jobId: String,
    appName: String,
    appConf: String,
    applicationType: ApplicationType,
    args: String,
    @Nullable hadoopUser: String,
    @Nullable buildResult: BuildResult,
    @Nullable extraParameter: JavaMap[String, Any]) {

  private[this] lazy val appProperties: Map[String, String] = getParameterMap(
    KEY_SPARK_PROPERTY_PREFIX)

  lazy val appMain: String = this.developmentMode match {
    case FlinkDevelopmentMode.FLINK_SQL =>
      Constant.STREAMPARK_SPARKSQL_CLIENT_CLASS
    case _ => appProperties(KEY_FLINK_APPLICATION_MAIN_CLASS)
  }

  lazy val effectiveAppName: String =
    if (this.appName == null) appProperties(KEY_FLINK_APP_NAME)
    else this.appName

  lazy val libs: List[URL] = {
    val path = s"${Workspace.local.APP_WORKSPACE}/$id/lib"
    Try(new File(path).listFiles().map(_.toURI.toURL).toList)
      .getOrElse(List.empty[URL])
  }

  lazy val classPaths: List[URL] = sparkVersion.sparkLibs ++ libs

  lazy val flinkSQL: String = extraParameter.get(KEY_FLINK_SQL()).toString

  lazy val userJarFile: File = {
    executionMode match {
      case _ =>
        checkBuildResult()
        new File(buildResult.asInstanceOf[ShadedBuildResponse].shadedJarPath)
    }
  }

  lazy val safePackageProgram: Boolean = {
    sparkVersion.version.split("\\.").map(_.trim.toInt) match {
      case Array(a, b, c) if a >= 3 => b > 1
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

  @throws[IOException]
  def isSymlink(file: File): Boolean = {
    if (file == null) throw new NullPointerException("File must not be null")
    Files.isSymbolicLink(file.toPath)
  }

  private[client] val hdfsWorkspace = {

    /**
     * The spark version and configuration in the native spark and hdfs must be kept exactly the
     * same.
     */
    val workspace = Workspace.remote
    val sparkHome = sparkVersion.sparkHome
    val sparkHomeDir = new File(sparkHome)
    val sparkName = if (isSymlink(sparkHomeDir)) {
      sparkHomeDir.getCanonicalFile.getName
    } else {
      sparkHomeDir.getName
    }
    val sparkHdfsHome = s"${workspace.APP_SPARK}/$sparkName"
    HdfsWorkspace(
      sparkName,
      sparkHome,
      sparkLib = s"$sparkHdfsHome/jars",
      sparkPlugins = s"$sparkHdfsHome/plugins",
      appJars = workspace.APP_JARS,
      appPlugins = workspace.APP_PLUGINS)
  }

  @throws[Exception]
  def checkBuildResult(): Unit = {
    executionMode match {
      case _ =>
        if (this.buildResult == null) {
          throw new Exception(
            s"[spark-submit] current job: ${this.effectiveAppName} was not yet built, buildResult is empty")
        }
        if (!this.buildResult.pass) {
          throw new Exception(
            s"[spark-submit] current job ${this.effectiveAppName} build failed, please check")
        }
    }
  }

}

case class HdfsWorkspace(
    sparkName: String,
    sparkHome: String,
    sparkLib: String,
    sparkPlugins: String,
    appJars: String,
    appPlugins: String)
