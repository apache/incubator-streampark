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

import org.apache.streampark.common.conf.{SparkVersion, Workspace}
import org.apache.streampark.common.conf.ConfigKeys._
import org.apache.streampark.common.constants.Constants
import org.apache.streampark.common.enums._
import org.apache.streampark.common.util.{DeflaterUtils, HdfsUtils, PropertiesUtils}
import org.apache.streampark.common.util.Implicits._
import org.apache.streampark.flink.packer.pipeline.{BuildResult, ShadedBuildResponse}

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.collections.MapUtils

import javax.annotation.Nullable

import java.io.{File, IOException}
import java.nio.file.Files

case class SubmitRequest(
    sparkVersion: SparkVersion,
    deployMode: SparkDeployMode,
    sparkYaml: String,
    jobType: SparkJobType,
    id: Long,
    appName: String,
    mainClass: String,
    appConf: String,
    appProperties: JavaMap[String, String],
    appArgs: JavaList[String],
    applicationType: ApplicationType,
    @Nullable hadoopUser: String,
    @Nullable buildResult: BuildResult,
    @Nullable extraParameter: JavaMap[String, Any]) {

  val DEFAULT_SUBMIT_PARAM = Map[String, String](
    "spark.driver.cores" -> "1",
    "spark.driver.memory" -> "1g",
    "spark.executor.cores" -> "1",
    "spark.executor.memory" -> "1g",
    "spark.executor.instances" -> "2")

  lazy val sparkParameterMap: Map[String, String] = getParameterMap(
    KEY_SPARK_PROPERTY_PREFIX)

  lazy val appMain: String = this.jobType match {
    case SparkJobType.SPARK_SQL => Constants.STREAMPARK_SPARKSQL_CLIENT_CLASS
    case SparkJobType.SPARK_JAR | SparkJobType.PYSPARK => mainClass
    case SparkJobType.UNKNOWN => throw new IllegalArgumentException("Unknown deployment Mode")
  }

  lazy val userJarPath: String = {
    checkBuildResult()
    buildResult.asInstanceOf[ShadedBuildResponse].shadedJarPath
  }

  def hasExtra(key: String): Boolean = MapUtils.isNotEmpty(extraParameter) && extraParameter.containsKey(key)

  def getExtra(key: String): Any = extraParameter.get(key)

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
    }
  }

  @throws[IOException]
  private def isSymlink(file: File): Boolean = {
    if (file == null) throw new NullPointerException("File must not be null")
    Files.isSymbolicLink(file.toPath)
  }

  private[client] lazy val hdfsWorkspace = {

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
      appJars = workspace.APP_JARS)
  }

  @throws[Exception]
  private def checkBuildResult(): Unit = {
    if (this.buildResult == null) {
      throw new Exception(
        s"[spark-submit] current job: $appName was not yet built, buildResult is empty")
    }
    if (!this.buildResult.pass) {
      throw new Exception(
        s"[spark-submit] current job $appName build failed, please check")
    }
  }

}

case class HdfsWorkspace(
    sparkName: String,
    sparkHome: String,
    sparkLib: String,
    sparkPlugins: String,
    appJars: String)
