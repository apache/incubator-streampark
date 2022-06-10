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

package com.streamxhub.streamx.flink.submit.bean

import com.fasterxml.jackson.databind.ObjectMapper
import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.conf.{ConfigConst, Workspace}
import com.streamxhub.streamx.common.domain.FlinkVersion
import com.streamxhub.streamx.common.enums._
import com.streamxhub.streamx.common.util.{DeflaterUtils, FlinkUtils, HdfsUtils, PropertiesUtils}
import com.streamxhub.streamx.flink.packer.pipeline.{BuildResult, ShadedBuildResponse}
import org.apache.commons.io.FileUtils
import org.apache.flink.runtime.jobgraph.{SavepointConfigOptions, SavepointRestoreSettings}

import java.io.File
import java.util.{Map => JavaMap}
import javax.annotation.Nullable
import scala.collection.JavaConversions._
import scala.util.Try

/**
 * @param clusterId            flink cluster id in k8s cluster.
 * @param kubernetesNamespace  k8s namespace.
 * @param flinkRestExposedType flink rest-service exposed type on k8s cluster.
 */
case class KubernetesSubmitParam(clusterId: String,
                                 kubernetesNamespace: String,
                                 @Nullable flinkRestExposedType: FlinkK8sRestExposedType)

case class SubmitRequest(flinkVersion: FlinkVersion,
                         flinkYaml: String,
                         developmentMode: DevelopmentMode,
                         executionMode: ExecutionMode,
                         resolveOrder: ResolveOrder,
                         appName: String,
                         appConf: String,
                         applicationType: ApplicationType,
                         savePoint: String,
                         flameGraph: JavaMap[String, java.io.Serializable],
                         option: JavaMap[String, Any],
                         dynamicOption: Array[String],
                         args: String,
                         @Nullable buildResult: BuildResult,
                         @Nullable k8sSubmitParam: KubernetesSubmitParam,
                         @Nullable extraParameter: JavaMap[String, Any]) {

  lazy val appProperties: Map[String, String] = getParameterMap(KEY_FLINK_DEPLOYMENT_PROPERTY_PREFIX)

  lazy val appOption: Map[String, String] = getParameterMap(KEY_FLINK_DEPLOYMENT_OPTION_PREFIX)

  lazy val appMain: String = this.developmentMode match {
    case DevelopmentMode.FLINKSQL => ConfigConst.STREAMX_FLINKSQL_CLIENT_CLASS
    case _ => appProperties(KEY_FLINK_APPLICATION_MAIN_CLASS)
  }

  lazy val effectiveAppName: String = if (this.appName == null) appProperties(KEY_FLINK_APP_NAME) else this.appName

  lazy val flinkSQL: String = extraParameter.get(KEY_FLINK_SQL()).toString

  lazy val jobID: String = extraParameter.get(KEY_JOB_ID).toString

  lazy val savepointRestoreSettings: SavepointRestoreSettings = {
    lazy val allowNonRestoredState = Try(extraParameter.get(SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE.key).toString.toBoolean).getOrElse(false)
    savePoint match {
      case sp if Try(sp.isEmpty).getOrElse(true) => SavepointRestoreSettings.none
      case sp => SavepointRestoreSettings.forPath(sp, allowNonRestoredState)
    }
  }

  lazy val userJarFile: File = {
    executionMode match {
      case ExecutionMode.KUBERNETES_NATIVE_APPLICATION => null
      case _ =>
        checkBuildResult()
        new File(buildResult.asInstanceOf[ShadedBuildResponse].shadedJarPath)
    }
  }

  lazy val safePackageProgram: Boolean = {
    // ref FLINK-21164 FLINK-9844 packageProgram.close()
    // must be flink 1.12.2 and above
    flinkVersion.version.split("\\.").map(_.trim.toInt) match {
      case Array(a, b, c) if a >= 1 => b > 12 || (b == 12 && c >= 2)
      case _ => false
    }
  }


  private[this] def getParameterMap(prefix: String = ""): Map[String, String] = {
    if (this.appConf == null) Map.empty[String, String] else {
      val map = this.appConf match {
        case x if x.trim.startsWith("yaml://") =>
          PropertiesUtils.fromYamlText(DeflaterUtils.unzipString(x.trim.drop(7)))
        case x if x.trim.startsWith("prop://") =>
          PropertiesUtils.fromPropertiesText(DeflaterUtils.unzipString(x.trim.drop(7)))
        case x if x.trim.startsWith("hdfs://") =>
          /*
           * 如果配置文件为hdfs方式,则需要用户将hdfs相关配置文件copy到resources下...
           */
          val text = HdfsUtils.read(this.appConf)
          val extension = this.appConf.split("\\.").last.toLowerCase
          extension match {
            case "properties" => PropertiesUtils.fromPropertiesText(text)
            case "yml" | "yaml" => PropertiesUtils.fromYamlText(text)
            case _ => throw new IllegalArgumentException("[StreamX] Usage:flink.conf file error,must be properties or yml")
          }
        case x if x.trim.startsWith("json://") =>
          val json = x.trim.drop(7)
          new ObjectMapper().readValue[JavaMap[String, String]](json, classOf[JavaMap[String, String]]).toMap.filter(_._2 != null)
        case _ => throw new IllegalArgumentException("[StreamX] appConf format error.")
      }
      if (this.appConf.trim.startsWith("json://")) map else {
        prefix match {
          case "" | null => map
          case other => map
            .filter(_._1.startsWith(other)).filter(_._2.nonEmpty)
            .map(x => x._1.drop(other.length) -> x._2)
        }
      }
    }
  }

  private[submit] lazy val hdfsWorkspace = {
    /**
     * 必须保持本机flink和hdfs里的flink版本和配置都完全一致.
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
      flinkDistJar = FlinkUtils.getFlinkDistJar(flinkHome),
      appJars = workspace.APP_JARS,
      appPlugins = workspace.APP_PLUGINS
    )
  }

  @throws[Exception] def checkBuildResult(): Unit = {
    executionMode match {
      case ExecutionMode.KUBERNETES_NATIVE_SESSION | ExecutionMode.KUBERNETES_NATIVE_APPLICATION =>
        if (buildResult == null) {
          throw new Exception(s"[flink-submit] current job: ${this.effectiveAppName} was not yet built, buildResult is empty" +
            s",clusterId=${k8sSubmitParam.clusterId}," +
            s",namespace=${k8sSubmitParam.kubernetesNamespace}")
        }
        if (!buildResult.pass) {
          throw new Exception(s"[flink-submit] current job ${this.effectiveAppName} build failed, clusterId" +
            s",clusterId=${k8sSubmitParam.clusterId}," +
            s",namespace=${k8sSubmitParam.kubernetesNamespace}")
        }
      case _ =>
        if (this.buildResult == null) {
          throw new Exception(s"[flink-submit] current job: ${this.effectiveAppName} was not yet built, buildResult is empty")
        }
        if (!this.buildResult.pass) {
          throw new Exception(s"[flink-submit] current job ${this.effectiveAppName} build failed, please check")
        }
    }
  }

}

/**
 *
 * @param flinkName
 * @param flinkHome
 * @param flinkDistJar
 * @param flinkLib
 * @param appJars
 * @param appPlugins
 * #TODO: className provisional
 */
case class HdfsWorkspace(flinkName: String,
                         flinkHome: String,
                         flinkDistJar: String,
                         flinkLib: String,
                         appJars: String,
                         appPlugins: String)
