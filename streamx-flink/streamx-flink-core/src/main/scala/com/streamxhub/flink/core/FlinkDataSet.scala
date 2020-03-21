/**
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
package com.streamxhub.flink.core

import com.streamxhub.common.util.{Logger, PropertiesUtils, SystemPropertyUtils}
import com.streamxhub.common.conf.ConfigConst._
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala._

import scala.collection.JavaConversions._
import scala.annotation.meta.getter


class DataSetContext(val parameter: ParameterTool, val env: ExecutionEnvironment) extends ExecutionEnvironment(env.getJavaEnv)

trait FlinkDataSet extends Logger {

  @(transient@getter)
  private var env: ExecutionEnvironment = _

  private var parameter: ParameterTool = _

  private var context: DataSetContext = _

  def handler(context: DataSetContext): Unit

  private def initialize(args: Array[String]): Unit = {
    //read config and merge config......
    SystemPropertyUtils.setAppHome(KEY_APP_HOME, classOf[FlinkStreaming])
    val argsMap = ParameterTool.fromArgs(args)
    val config = argsMap.get(KEY_FLINK_APP_CONF, null) match {
      case null | "" => throw new ExceptionInInitializerError("[StreamX] Usage:can't fond config,please set \"--flink.conf $path \" in main arguments")
      case file => file
    }
    val configFile = new java.io.File(config)
    require(configFile.exists(), s"[StreamX] Usage:flink.conf file $configFile is not found!!!")
    val configArgs = config.split("\\.").last match {
      case "properties" => PropertiesUtils.fromPropertiesFile(configFile.getAbsolutePath)
      case "yml" | "yaml" => PropertiesUtils.fromYamlFile(configFile.getAbsolutePath)
      case _ => throw new IllegalArgumentException("[StreamX] Usage:flink.conf file error,muse be properties or yml")
    }

    this.parameter = ParameterTool.fromMap(configArgs).mergeWith(argsMap).mergeWith(ParameterTool.fromSystemProperties())

    env = ExecutionEnvironment.getExecutionEnvironment
    env.getConfig.setGlobalJobParameters(parameter)
  }

  /**
   * 用户可覆盖次方法...
   *
   * @param env
   */
  def beforeStart(env: ExecutionEnvironment): Unit = {}

  private def createContext(): Unit = {
    context = new DataSetContext(parameter, env)
  }

  def main(args: Array[String]): Unit = {
    initialize(args)
    beforeStart(env)
    createContext()
    doStart()
    handler(context)
  }

  private[this] def doStart(): Unit = {
    println(s"\033[95;1m${LOGO}\033[1m\n")
    val appName = parameter.get(KEY_FLINK_APP_NAME, "")
    println(s"[StreamX] FlinkDataSet $appName Starting...")
  }

}



