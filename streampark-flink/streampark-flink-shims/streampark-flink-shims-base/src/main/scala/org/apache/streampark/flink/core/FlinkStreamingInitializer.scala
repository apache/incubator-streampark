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
package org.apache.streampark.flink.core

import org.apache.streampark.common.conf.ConfigConst._
import org.apache.streampark.common.enums.ApiType
import org.apache.streampark.common.enums.ApiType.ApiType
import org.apache.streampark.common.util._

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.environment.{StreamExecutionEnvironment => JavaStreamEnv}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.TableConfig

import java.io.File

import scala.collection.{mutable, Map}
import scala.collection.JavaConversions._
import scala.util.Try

private[flink] object FlinkStreamingInitializer {

  def initialize(args: Array[String], config: (StreamExecutionEnvironment, ParameterTool) => Unit)
      : (ParameterTool, StreamExecutionEnvironment) = {
    val flinkInitializer = new FlinkStreamingInitializer(args, ApiType.scala)
    flinkInitializer.streamEnvConfFunc = config
    (flinkInitializer.configuration.parameter, flinkInitializer.streamEnv)
  }

  def initialize(args: StreamEnvConfig): (ParameterTool, StreamExecutionEnvironment) = {
    val flinkInitializer = new FlinkStreamingInitializer(args.args, ApiType.java)
    flinkInitializer.javaStreamEnvConfFunc = args.conf
    (flinkInitializer.configuration.parameter, flinkInitializer.streamEnv)
  }
}

private[flink] class FlinkStreamingInitializer(args: Array[String], apiType: ApiType)
  extends Logger {

  var streamEnvConfFunc: (StreamExecutionEnvironment, ParameterTool) => Unit = _

  var tableConfFunc: (TableConfig, ParameterTool) => Unit = _

  var javaStreamEnvConfFunc: StreamEnvConfigFunction = _

  var javaTableEnvConfFunc: TableEnvConfigFunction = _

  implicit private[flink] val parameter: ParameterTool = configuration.parameter

  lazy val streamEnv: StreamExecutionEnvironment = {
    val env = new StreamExecutionEnvironment(
      JavaStreamEnv.getExecutionEnvironment(configuration.envConfig))

    apiType match {
      case ApiType.java if javaStreamEnvConfFunc != null =>
        javaStreamEnvConfFunc.configuration(env.getJavaEnv, configuration.parameter)
      case ApiType.scala if streamEnvConfFunc != null =>
        streamEnvConfFunc(env, configuration.parameter)
      case _ =>
    }
    env.getConfig.setGlobalJobParameters(configuration.parameter)
    env
  }

  lazy val configuration: FlinkConfiguration = initParameter()

  def initParameter(): FlinkConfiguration = {
    val argsMap = ParameterTool.fromArgs(args)
    val configMap = parseConfig(argsMap)
    if (!argsMap.has(KEY_APP_CONF()) && configMap.isEmpty) {
      throw new IllegalArgumentException(
        "[StreamPark] Usage:can't fond config, please set \"--conf $path \" in main arguments")
    }
    val appFlinkConf = extractConfigByPrefix(configMap, KEY_FLINK_PROPERTY_PREFIX)
    val appConf = extractConfigByPrefix(configMap, KEY_APP_PREFIX)
    // config priority: explicitly specified priority > project profiles > system profiles
    val parameter = ParameterTool
      .fromSystemProperties()
      .mergeWith(ParameterTool.fromMap(appFlinkConf))
      .mergeWith(ParameterTool.fromMap(appConf))
      .mergeWith(argsMap)

    val flinkConf: Map[String, String] = {
      parameter.get(KEY_FLINK_CONF(), null) match {
        case flinkConf if flinkConf != null =>
          PropertiesUtils
            .loadFlinkConfYaml(DeflaterUtils.unzipString(flinkConf))
            .filter(_._2.nonEmpty)
        case _ => Map.empty
      }
    }

    val envConfig = Configuration.fromMap(flinkConf + appFlinkConf)
    FlinkConfiguration(parameter, envConfig, null)
  }

  def parseConfig(parameterTool: ParameterTool): Map[String, String] = {
    parameterTool.get(KEY_APP_CONF(), null) match {
      case null | "" =>
        logWarn("Usage:can't fond config, now trying to find from jar")
        val configs =
          Set("application.yml", "application.yaml", "application.conf", "application.properties")
        configs
          .find(
            f => Try(this.getClass.getClassLoader.getResource(f).getPath != null).getOrElse(false))
          .map(
            f => {
              val input = this.getClass.getClassLoader.getResourceAsStream(f)
              val content = FileUtils.readString(input)
              val format = f.split("\\.").last.toLowerCase
              readConfig(format, content)
            })
          .getOrElse(return Map.empty[String, String])
      case config =>
        lazy val content = DeflaterUtils.unzipString(config.drop(7))
        lazy val format = config.split("\\.").last.toLowerCase
        val configAsMap = config match {
          case x if x.startsWith("yaml://") => PropertiesUtils.fromYamlText(content)
          case x if x.startsWith("conf://") => PropertiesUtils.fromHoconText(content)
          case x if x.startsWith("prop://") => PropertiesUtils.fromPropertiesText(content)
          case x if x.startsWith("hdfs://") =>
            // If the configuration file with the hdfs, user will need to copy the hdfs-related configuration files under the resources dir
            val text = HdfsUtils.read(x)
            readConfig(format, text)
          case _ =>
            val configFile = new File(config)
            require(
              configFile.exists(),
              s"[StreamPark] Usage: application config file: $configFile is not found!!!")
            val text = FileUtils.readString(configFile)
            readConfig(format, text)
        }
        configAsMap.filter(_._2.nonEmpty)
    }
  }

  private[this] def readConfig(format: String, text: String): Map[String, String] = {
    format match {
      case "yml" | "yaml" => PropertiesUtils.fromYamlText(text)
      case "conf" => PropertiesUtils.fromHoconText(text)
      case "properties" => PropertiesUtils.fromPropertiesText(text)
      case _ =>
        throw new IllegalArgumentException(
          "[StreamPark] Usage: application config file error,must be [yaml|conf|properties]")
    }
  }

  def extractConfigByPrefix(configMap: Map[String, String], prefix: String): Map[String, String] = {
    val map = mutable.Map[String, String]()
    configMap.foreach(
      x =>
        if (x._1.startsWith(prefix)) {
          map += x._1.drop(prefix.length) -> x._2
        })
    map
  }

}
