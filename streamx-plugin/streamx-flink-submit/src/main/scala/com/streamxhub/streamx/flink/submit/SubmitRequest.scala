/*
 * Copyright (c) 2019 The StreamX Project
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
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.flink.submit

import com.fasterxml.jackson.databind.ObjectMapper
import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.enums.{DevelopmentMode, ExecutionMode, ResolveOrder}
import com.streamxhub.streamx.common.util.{DeflaterUtils, HdfsUtils, PropertiesUtils}
import com.streamxhub.streamx.flink.submit.`trait`.WorkspaceEnv
import org.apache.flink.client.cli.CliFrontend
import org.apache.flink.client.cli.CliFrontend.loadCustomCommandLines
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.configuration.{Configuration, GlobalConfiguration}

import java.io.File
import java.util.{Map => JavaMap}
import scala.collection.JavaConversions._

case class SubmitRequest(flinkHome: String,
                         flinkVersion: String,
                         flinkYaml: String,
                         flinkUserJar: String,
                         developmentMode: DevelopmentMode,
                         executionMode: ExecutionMode,
                         resolveOrder: ResolveOrder,
                         appName: String,
                         appConf: String,
                         applicationType: String,
                         savePoint: String,
                         flameGraph: JavaMap[String, java.io.Serializable],
                         option: String,
                         property: JavaMap[String, Any],
                         dynamicOption: Array[String],
                         args: String) {

  lazy val appProperties: Map[String, String] = getParameterMap(KEY_FLINK_DEPLOYMENT_PROPERTY_PREFIX)

  lazy val appOption: Map[String, String] = getParameterMap(KEY_FLINK_DEPLOYMENT_OPTION_PREFIX)

  lazy val appMain: String = appProperties(ApplicationConfiguration.APPLICATION_MAIN_CLASS.key())

  lazy val effectiveAppName: String = if (this.appName == null) appProperties(KEY_FLINK_APP_NAME) else this.appName

  lazy val flinkSQL: String = property.remove(KEY_FLINK_SQL()).toString

  lazy val jobID: String = property.remove(KEY_JOB_ID).toString

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

  private[submit] lazy val workspaceEnv = {
    /**
     * 必须保持本机flink和hdfs里的flink版本和配置都完全一致.
     */
    val flinkName = new File(flinkHome).getName
    val flinkHdfsHome = s"${HdfsUtils.getDefaultFS}$APP_FLINK/$flinkName"
    WorkspaceEnv(
      flinkName,
      flinkHome,
      flinkLib = s"$flinkHdfsHome/lib",
      flinkDistJar = new File(s"$flinkHome/lib").list().filter(_.matches("flink-dist_.*\\.jar")) match {
        case Array() => throw new IllegalArgumentException(s"[StreamX] can no found flink-dist jar in $flinkHome/lib")
        case array if array.length == 1 => s"$flinkHdfsHome/lib/${array.head}"
        case more => throw new IllegalArgumentException(s"[StreamX] found multiple flink-dist jar in $flinkHome/lib,[${more.mkString(",")}]")
      },
      appJars = s"${HdfsUtils.getDefaultFS}$APP_JARS",
      appPlugins = s"${HdfsUtils.getDefaultFS}$APP_PLUGINS"
    )
  }

  private[submit] lazy val flinkDefaultConfiguration: Configuration = {
    GlobalConfiguration.loadConfiguration(s"$flinkHome/conf")
  }

  private[submit] lazy val customCommandLines = {
    // 1. find the configuration directory
    val configurationDirectory = s"$flinkHome/conf"
    // 2. load the custom command lines
    val customCommandLines = loadCustomCommandLines(flinkDefaultConfiguration, configurationDirectory)
    new CliFrontend(flinkDefaultConfiguration, customCommandLines)
    customCommandLines
  }

}
