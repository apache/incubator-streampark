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
package com.streamxhub.flink.core.scala

import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.util.Logger
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala._

import scala.annotation.meta.getter


class DataSetContext(val parameter: ParameterTool, private val env: ExecutionEnvironment) extends ExecutionEnvironment(env.getJavaEnv)

trait FlinkDataSet extends Logger {

  @(transient@getter)
  private var env: ExecutionEnvironment = _

  private var parameter: ParameterTool = _

  private var context: DataSetContext = _

  def handle(context: DataSetContext): Unit

  private def initialize(args: Array[String]): Unit = {
    this.parameter = FlinkInitializer.get(args, null).parameter
    env = ExecutionEnvironment.getExecutionEnvironment
    env.getConfig.setGlobalJobParameters(parameter)
  }

  /**
   * 用户可覆盖次方法...
   *
   * @param env
   */
  def beforeStart(env: ExecutionEnvironment): Unit = {}

  def config(env: ExecutionEnvironment, parameter: ParameterTool): Unit = {}

  private def createContext(): Unit = {
    context = new DataSetContext(parameter, env)
  }

  def main(args: Array[String]): Unit = {
    initialize(args)
    beforeStart(env)
    createContext()
    doStart()
    handle(context)
  }

  private[this] def doStart(): Unit = {
    println(s"\033[95;1m${LOGO}\033[1m\n")
    val appName = parameter.get(KEY_FLINK_APP_NAME, "")
    println(s"[StreamX] FlinkDataSet $appName Starting...")
  }

}



