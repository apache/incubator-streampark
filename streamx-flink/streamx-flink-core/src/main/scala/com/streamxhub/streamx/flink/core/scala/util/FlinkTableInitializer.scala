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
package com.streamxhub.streamx.flink.core.scala.util

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.flink.core.scala.enums.ApiType.ApiType
import com.streamxhub.streamx.flink.core.scala.enums.TableMode.TableMode
import com.streamxhub.streamx.flink.core.scala.enums.{ApiType, PlannerType, TableMode}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.PipelineOptions
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.table.api.{EnvironmentSettings, TableEnvironment}

import scala.collection.JavaConversions._
import scala.util.Try

private[scala] object FlinkTableInitializer {

  private[this] var flinkInitializer: FlinkTableInitializer = _

  def initTable(args: Array[String]): (ParameterTool, TableEnvironment) = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkTableInitializer(args, ApiType.scala)
          flinkInitializer.initTableEnv(TableMode.batch)
        }
      }
    }
    (flinkInitializer.parameter, flinkInitializer.tableEnvironment)
  }

  def initStreamTable(args: Array[String], config: (StreamExecutionEnvironment, ParameterTool) => Unit = null): (ParameterTool, StreamExecutionEnvironment, StreamTableEnvironment) = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkTableInitializer(args, ApiType.scala)
          flinkInitializer.streamEnvConfFunc = config
          flinkInitializer.initTableEnv(TableMode.streaming)
        }
      }
    }
    (flinkInitializer.parameter, flinkInitializer.streamEnvironment, flinkInitializer.streamTableEnvironment)
  }

  def initJavaStreamTable(args: StreamEnvConfig): (ParameterTool, StreamExecutionEnvironment, StreamTableEnvironment) = {
    if (flinkInitializer == null) {
      this.synchronized {
        if (flinkInitializer == null) {
          flinkInitializer = new FlinkTableInitializer(args.args, ApiType.java)
          flinkInitializer.javaStreamEnvConfFunc = args.conf
          flinkInitializer.initTableEnv(TableMode.streaming)
        }
      }
    }
    (flinkInitializer.parameter, flinkInitializer.streamEnvironment, flinkInitializer.streamTableEnvironment)
  }

}


private[this] class FlinkTableInitializer(args: Array[String], apiType: ApiType) extends FlinkStreamingInitializer(args, apiType) {

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
   * table SQL 下--flink.conf 非必须传入,取决于开发者.
   *
   * @return
   */
  override def initParameter(): ParameterTool = {
    val argsMap = ParameterTool.fromArgs(args)
    argsMap.get(KEY_FLINK_CONF(), null) match {
      case null | "" =>
        logWarn("Usage:can't fond config,you can set \"--flink.conf $path \" in main arguments")
        ParameterTool.fromSystemProperties().mergeWith(argsMap)
      case file =>
        val configArgs = super.readFlinkConf(file)
        //显示指定的优先级 > 项目配置文件 > 系统配置文件...
        ParameterTool.fromSystemProperties().mergeWith(ParameterTool.fromMap(configArgs)).mergeWith(argsMap)
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
        if (tableMode == TableMode.streaming) {
          throw new ExceptionInInitializerError("[StreamX] can not use batch mode in StreamTableEnvironment")
        }
        logInfo("components should work in batch mode")
        builder.inBatchMode()
      case TableMode.streaming =>
        if (tableMode == TableMode.batch) {
          throw new ExceptionInInitializerError("[StreamX] can not use streaming mode in TableEnvironment")
        }
        logInfo("components should work in streaming mode")
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
  }

}

