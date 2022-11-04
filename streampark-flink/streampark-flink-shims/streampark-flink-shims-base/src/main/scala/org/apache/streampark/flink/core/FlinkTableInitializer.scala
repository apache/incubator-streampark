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

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.{Configuration, PipelineOptions}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.table.api.{EnvironmentSettings, TableConfig, TableEnvironment}
import org.apache.streampark.common.conf.ConfigConst._
import org.apache.streampark.common.enums.ApiType.ApiType
import org.apache.streampark.common.enums.TableMode.TableMode
import org.apache.streampark.common.enums.{ApiType, PlannerType, TableMode}
import org.apache.streampark.common.util.{DeflaterUtils, PropertiesUtils}
import org.apache.streampark.flink.core.conf.FlinkConfiguration
import org.apache.streampark.flink.core.EnhancerImplicit._

import java.io.File
import scala.collection.JavaConversions._
import scala.collection.{Map, mutable}
import scala.util.{Failure, Success, Try}

private[flink] object FlinkTableInitializer {

  private[this] var flinkInitializer: FlinkTableInitializer = _

  def initialize(args: Array[String],
                 config: (TableConfig, ParameterTool) => Unit):
  (ParameterTool, TableEnvironment) = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkTableInitializer(args, ApiType.scala)
          flinkInitializer.tableConfFunc = config
          flinkInitializer.initEnvironment(TableMode.batch)
        }
      }
    }
    (flinkInitializer.configuration.parameter, flinkInitializer.tableEnvironment)
  }

  def initialize(args: TableEnvConfig): (ParameterTool, TableEnvironment) = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkTableInitializer(args.args, ApiType.java)
          flinkInitializer.javaTableEnvConfFunc = args.conf
          flinkInitializer.initEnvironment(TableMode.batch)
        }
      }
    }
    (flinkInitializer.configuration.parameter, flinkInitializer.tableEnvironment)
  }

  def initialize(args: Array[String],
                 configStream: (StreamExecutionEnvironment, ParameterTool) => Unit,
                 configTable: (TableConfig, ParameterTool) => Unit):
  (ParameterTool, StreamExecutionEnvironment, StreamTableEnvironment) = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkTableInitializer(args, ApiType.scala)
          flinkInitializer.streamEnvConfFunc = configStream
          flinkInitializer.tableConfFunc = configTable
          flinkInitializer.initEnvironment(TableMode.streaming)
        }
      }
    }
    (flinkInitializer.configuration.parameter, flinkInitializer.streamEnvironment, flinkInitializer.streamTableEnvironment)
  }

  def initialize(args: StreamTableEnvConfig):
  (ParameterTool, StreamExecutionEnvironment, StreamTableEnvironment) = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkTableInitializer(args.args, ApiType.java)
          flinkInitializer.javaStreamEnvConfFunc = args.streamConfig
          flinkInitializer.javaTableEnvConfFunc = args.tableConfig
          flinkInitializer.initEnvironment(TableMode.streaming)
        }
      }
    }
    (flinkInitializer.configuration.parameter, flinkInitializer.streamEnvironment, flinkInitializer.streamTableEnvironment)
  }

}


private[flink] class FlinkTableInitializer(args: Array[String], apiType: ApiType) extends FlinkStreamingInitializer(args, apiType) {

  private[this] var localStreamTableEnv: StreamTableEnvironment = _

  private[this] var localTableEnv: TableEnvironment = _

  def streamTableEnvironment: StreamTableEnvironment = {
    if (localStreamTableEnv == null) {
      this.synchronized {
        if (localStreamTableEnv == null) {
          initEnvironment(TableMode.streaming)
        }
      }
    }
    localStreamTableEnv
  }

  def tableEnvironment: TableEnvironment = {
    if (localTableEnv == null) {
      this.synchronized {
        if (localTableEnv == null) {
          initEnvironment(TableMode.batch)
        }
      }
    }
    localTableEnv
  }

  /**
   * In case of table SQL, the parameter conf is not required, it depends on the developer.
   */

  override def initParameter(): FlinkConfiguration = {
    val configuration = {
      val argsMap = ParameterTool.fromArgs(args)
      argsMap.get(KEY_APP_CONF(), null) match {
        case null | "" =>
          logWarn("Usage:can't fond config,you can set \"--conf $path \" in main arguments")
          val parameter = ParameterTool.fromSystemProperties().mergeWith(argsMap)
          FlinkConfiguration(parameter, new Configuration(), new Configuration())
        case file =>
          val configMap = parseConfig(file)
          // set sql..
          val sqlConf = mutable.Map[String, String]()
          configMap.foreach(x => {
            if (x._1.startsWith(KEY_SQL_PREFIX)) {
              sqlConf += x._1.drop(KEY_SQL_PREFIX.length) -> x._2
            }
          })

          // config priority: explicitly specified priority > project profiles > system profiles
          val properConf = extractConfigByPrefix(configMap, KEY_FLINK_PROPERTY_PREFIX)
          val appConf = extractConfigByPrefix(configMap, KEY_APP_PREFIX)
          val tableConf = extractConfigByPrefix(configMap, KEY_FLINK_TABLE_PREFIX)

          val tableConfig = Configuration.fromMap(tableConf)
          val envConfig = Configuration.fromMap(properConf)

          val parameter = ParameterTool.fromSystemProperties()
            .mergeWith(ParameterTool.fromMap(properConf))
            .mergeWith(ParameterTool.fromMap(tableConf))
            .mergeWith(ParameterTool.fromMap(appConf))
            .mergeWith(ParameterTool.fromMap(sqlConf))
            .mergeWith(argsMap)

          FlinkConfiguration(parameter, envConfig, tableConfig)
      }
    }

    configuration.parameter.get(KEY_FLINK_SQL()) match {
      case null => configuration
      case param =>
        // for streampark-console
        Try(DeflaterUtils.unzipString(param)) match {
          case Success(value) =>
            configuration.copy(parameter = configuration.parameter.mergeWith(ParameterTool.fromMap(Map(KEY_FLINK_SQL() -> value))))
          case Failure(_) =>
            val sqlFile = new File(param)
            Try(PropertiesUtils.fromYamlFile(sqlFile.getAbsolutePath)) match {
              case Success(value) =>
                configuration.copy(parameter = configuration.parameter.mergeWith(ParameterTool.fromMap(value)))
              case Failure(e) =>
                new IllegalArgumentException(s"[StreamPark] init sql error.$e")
                configuration
            }
        }
    }
  }

  def initEnvironment(tableMode: TableMode): Unit = {
    val builder = EnvironmentSettings.newInstance()
    val parameter = configuration.parameter
    val plannerType = Try(PlannerType.withName(parameter.get(KEY_FLINK_TABLE_PLANNER))).getOrElse(PlannerType.blink)

    try {
      plannerType match {
        case PlannerType.blink =>
          logInfo("blinkPlanner will be use.")
          builder.useBlinkPlanner()
        case PlannerType.old =>
          logInfo("oldPlanner will be use.")
          builder.useOldPlanner()
        case PlannerType.any =>
          logInfo("anyPlanner will be use.")
          builder.useAnyPlanner()
      }
    } catch {
      case e: IncompatibleClassChangeError =>
    }

    val mode = Try(TableMode.withName(parameter.get(KEY_FLINK_TABLE_MODE))).getOrElse(tableMode)
    mode match {
      case TableMode.batch =>
        logInfo(s"components should work in $tableMode mode")
        builder.inBatchMode()
      case TableMode.streaming =>
        logInfo(s"components should work in $tableMode mode")
        builder.inStreamingMode()
    }

    val buildWith = (parameter.get(KEY_FLINK_TABLE_CATALOG), parameter.get(KEY_FLINK_TABLE_DATABASE))
    buildWith match {
      case (x: String, y: String) if x != null && y != null =>
        logInfo(s"with built in catalog: $x")
        logInfo(s"with built in database: $y")
        builder.withBuiltInCatalogName(x)
        builder.withBuiltInDatabaseName(y)
      case (x: String, _) if x != null =>
        logInfo(s"with built in catalog: $x")
        builder.withBuiltInCatalogName(x)
      case (_, y: String) if y != null =>
        logInfo(s"with built in database: $y")
        builder.withBuiltInDatabaseName(y)
      case _ =>
    }
    val setting = builder.build()
    tableMode match {
      case TableMode.batch => localTableEnv = TableEnvironment.create(setting)
      case TableMode.streaming =>
        initEnvironment()
        if (streamEnvConfFunc != null) {
          streamEnvConfFunc(streamEnvironment, parameter)
        }
        if (javaStreamEnvConfFunc != null) {
          javaStreamEnvConfFunc.configuration(streamEnvironment.getJavaEnv, parameter)
        }
        localStreamTableEnv = StreamTableEnvironment.create(streamEnvironment, setting)
    }
    val appName = parameter.getAppName()
    if (appName != null) {
      tableMode match {
        case TableMode.batch => localTableEnv.getConfig.getConfiguration.setString(PipelineOptions.NAME, appName)
        case TableMode.streaming => localStreamTableEnv.getConfig.getConfiguration.setString(PipelineOptions.NAME, appName)
      }
    }

    apiType match {
      case ApiType.java =>
        if (javaTableEnvConfFunc != null) {
          tableMode match {
            case TableMode.batch => javaTableEnvConfFunc.configuration(localTableEnv.getConfig, parameter)
            case TableMode.streaming => javaTableEnvConfFunc.configuration(localStreamTableEnv.getConfig, parameter)
          }
        }
      case ApiType.scala =>
        if (tableConfFunc != null) {
          tableMode match {
            case TableMode.batch => tableConfFunc(localTableEnv.getConfig, parameter)
            case TableMode.streaming => tableConfFunc(localStreamTableEnv.getConfig, parameter)
          }
        }
    }
  }

}
