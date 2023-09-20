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
import org.apache.streampark.common.enums.ApiTypeEnum
import org.apache.streampark.common.enums.ApiTypeEnum.ApiType
import org.apache.streampark.common.util._
import org.apache.streampark.flink.core.conf.FlinkConfiguration

import collection.{mutable, Map}
import collection.JavaConversions._
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.environment.{StreamExecutionEnvironment => JavaStreamEnv}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.TableConfig

import java.io.File

private[flink] object FlinkStreamingInitializer {

  def initialize(args: Array[String], config: (StreamExecutionEnvironment, ParameterTool) => Unit)
      : (ParameterTool, StreamExecutionEnvironment) = {
    val flinkInitializer = new FlinkStreamingInitializer(args, ApiTypeEnum.scala)
    flinkInitializer.streamEnvConfFunc = config
    (flinkInitializer.configuration.parameter, flinkInitializer.streamEnv)
  }

  def initialize(args: StreamEnvConfig): (ParameterTool, StreamExecutionEnvironment) = {
    val flinkInitializer = new FlinkStreamingInitializer(args.args, ApiTypeEnum.java)
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
      case ApiTypeEnum.java if javaStreamEnvConfFunc != null =>
        javaStreamEnvConfFunc.configuration(env.getJavaEnv, configuration.parameter)
      case ApiTypeEnum.scala if streamEnvConfFunc != null =>
        streamEnvConfFunc(env, configuration.parameter)
      case _ =>
    }
    env.getConfig.setGlobalJobParameters(configuration.parameter)
    env
  }

  lazy val configuration: FlinkConfiguration = initParameter()

  def initParameter(): FlinkConfiguration = {
    val argsMap = ParameterTool.fromArgs(args)
    val config = argsMap.get(KEY_APP_CONF(), null) match {
      case null | "" =>
        throw new ExceptionInInitializerError(
          "[StreamPark] Usage:can't fond config,please set \"--conf $path \" in main arguments")
      case file => file
    }
    val configMap = parseConfig(config)
    val properConf = extractConfigByPrefix(configMap, KEY_FLINK_PROPERTY_PREFIX)
    val appConf = extractConfigByPrefix(configMap, KEY_APP_PREFIX)

    // config priority: explicitly specified priority > project profiles > system profiles
    val parameter = ParameterTool
      .fromSystemProperties()
      .mergeWith(ParameterTool.fromMap(properConf))
      .mergeWith(ParameterTool.fromMap(appConf))
      .mergeWith(argsMap)

    val envConfig = Configuration.fromMap(properConf)
    FlinkConfiguration(parameter, envConfig, null)
  }

  def parseConfig(config: String): Map[String, String] = {

    lazy val content = DeflaterUtils.unzipString(config.drop(7))

    def readConfig(text: String): Map[String, String] = {
      val format = config.split("\\.").last.toLowerCase
      format match {
        case "yml" | "yaml" => PropertiesUtils.fromYamlText(text)
        case "conf" => PropertiesUtils.fromHoconText(text)
        case "properties" => PropertiesUtils.fromPropertiesText(text)
        case _ =>
          throw new IllegalArgumentException(
            "[StreamPark] Usage: application config file error,must be [yaml|conf|properties]")
      }
    }

    val map = config match {
      case x if x.startsWith("yaml://") => PropertiesUtils.fromYamlText(content)
      case x if x.startsWith("conf://") => PropertiesUtils.fromHoconText(content)
      case x if x.startsWith("prop://") => PropertiesUtils.fromPropertiesText(content)
      case x if x.startsWith("hdfs://") =>
        // If the configuration file with the hdfs, user will need to copy the hdfs-related configuration files under the resources dir
        val text = HdfsUtils.read(x)
        readConfig(text)
      case _ =>
        val configFile = new File(config)
        require(
          configFile.exists(),
          s"[StreamPark] Usage: application config file: $configFile is not found!!!")
        val text = FileUtils.readFile(configFile)
        readConfig(text)
    }
    map.filter(_._2.nonEmpty)
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
