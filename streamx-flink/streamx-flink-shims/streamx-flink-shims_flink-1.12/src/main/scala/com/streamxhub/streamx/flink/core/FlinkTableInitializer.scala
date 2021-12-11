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
package com.streamxhub.streamx.flink.core

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.enums.ApiType.ApiType
import com.streamxhub.streamx.common.enums.TableMode.TableMode
import com.streamxhub.streamx.common.enums.{ApiType, PlannerType, TableMode}
import com.streamxhub.streamx.common.util.{DeflaterUtils, PropertiesUtils}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.PipelineOptions
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.table.api.{EnvironmentSettings, TableConfig, TableEnvironment}

import java.io.File
import collection.JavaConversions._
import collection.Map
import util.{Failure, Success, Try}

private[flink] object FlinkTableInitializer {

  private[this] var flinkInitializer: FlinkTableInitializer = _

  def initTable(args: Array[String], config: (TableConfig, ParameterTool) => Unit = null): (ParameterTool, TableEnvironment) = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkTableInitializer(args, ApiType.scala)
          flinkInitializer.tableConfFunc = config
          flinkInitializer.initTableEnv(TableMode.batch)
        }
      }
    }
    (flinkInitializer.parameter, flinkInitializer.tableEnvironment)
  }

  def initJavaTable(args: TableEnvConfig): (ParameterTool, TableEnvironment) = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkTableInitializer(args.args, ApiType.java)
          flinkInitializer.javaTableEnvConfFunc = args.conf
          flinkInitializer.initTableEnv(TableMode.batch)
        }
      }
    }
    (flinkInitializer.parameter, flinkInitializer.tableEnvironment)
  }

  def initStreamTable(args: Array[String], configStream: (StreamExecutionEnvironment, ParameterTool) => Unit = null, configTable: (TableConfig, ParameterTool) => Unit = null): (ParameterTool, StreamExecutionEnvironment, StreamTableEnvironment) = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkTableInitializer(args, ApiType.scala)
          flinkInitializer.streamEnvConfFunc = configStream
          flinkInitializer.tableConfFunc = configTable
          flinkInitializer.initTableEnv(TableMode.streaming)
        }
      }
    }
    (flinkInitializer.parameter, flinkInitializer.streamEnvironment, flinkInitializer.streamTableEnvironment)
  }

  def initJavaStreamTable(args: StreamTableEnvConfig): (ParameterTool, StreamExecutionEnvironment, StreamTableEnvironment) = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkTableInitializer(args.args, ApiType.java)
          flinkInitializer.javaStreamEnvConfFunc = args.streamConfig
          flinkInitializer.javaTableEnvConfFunc = args.tableConfig
          flinkInitializer.initTableEnv(TableMode.streaming)
        }
      }
    }
    (flinkInitializer.parameter, flinkInitializer.streamEnvironment, flinkInitializer.streamTableEnvironment)
  }

}


private[flink] class FlinkTableInitializer(args: Array[String], apiType: ApiType) extends FlinkStreamingInitializer(args, apiType) {

  private[this] var localStreamTableEnv: StreamTableEnvironment = _

  private[this] var localTableEnv: TableEnvironment = _

  def streamTableEnvironment: StreamTableEnvironment = {
    if (localStreamTableEnv == null) {
      this.synchronized {
        if (localStreamTableEnv == null) {
          initTableEnv(TableMode.streaming)
        }
      }
    }
    localStreamTableEnv
  }

  def tableEnvironment: TableEnvironment = {
    if (localTableEnv == null) {
      this.synchronized {
        if (localTableEnv == null) {
          initTableEnv(TableMode.batch)
        }
      }
    }
    localTableEnv
  }


  /**
   * table SQL 下  --conf 非必须传入,取决于开发者.
   *
   * @return
   */
  override def initParameter(): ParameterTool = {
    val argsMap = ParameterTool.fromArgs(args)
    val parameter = argsMap.get(KEY_APP_CONF(), null) match {
      case null | "" =>
        logWarn("Usage:can't fond config,you can set \"--conf $path \" in main arguments")
        ParameterTool.fromSystemProperties().mergeWith(argsMap)
      case file =>
        val configArgs = super.readFlinkConf(file)
        //显示指定的优先级 > 项目配置文件 > 系统配置文件...
        ParameterTool.fromSystemProperties().mergeWith(ParameterTool.fromMap(configArgs)).mergeWith(argsMap)
    }
    parameter.get(KEY_FLINK_SQL()) match {
      case null => parameter
      case param =>
        //for streamx-console
        Try(DeflaterUtils.unzipString(param)) match {
          case Success(value) => parameter.mergeWith(ParameterTool.fromMap(Map(KEY_FLINK_SQL() -> value)))
          case Failure(_) =>
            val sqlFile = new File(param)
            Try(PropertiesUtils.fromYamlFile(sqlFile.getAbsolutePath)) match {
              case Success(value) => parameter.mergeWith(ParameterTool.fromMap(value))
              case Failure(e) =>
                new IllegalArgumentException(s"[StreamX] init sql error.$e")
                parameter
            }
        }
    }

  }

  def initTableEnv(tableMode: TableMode): Unit = {
    val builder = EnvironmentSettings.newInstance()
    val plannerType = Try(PlannerType.withName(parameter.get(KEY_FLINK_TABLE_PLANNER))).getOrElse {
      logWarn(s" $KEY_FLINK_TABLE_PLANNER undefined,use default by: blinkPlanner")
      PlannerType.blink
    }

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
        initStreamEnv()
        if (streamEnvConfFunc != null) {
          streamEnvConfFunc(streamEnvironment, parameter)
        }
        if (javaStreamEnvConfFunc != null) {
          javaStreamEnvConfFunc.configuration(streamEnvironment.getJavaEnv, parameter)
        }
        localStreamTableEnv = StreamTableEnvironment.create(streamEnvironment, setting)
    }
    val appName = (parameter.get(KEY_APP_NAME(), null), parameter.get(KEY_FLINK_APP_NAME, null)) match {
      case (appName: String, _) => appName
      case (null, appName: String) => appName
      case _ => null
    }
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

