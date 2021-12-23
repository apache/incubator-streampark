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

package com.streamxhub.streamx.flink.core.scala

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util.{Logger, SystemPropertyUtils}
import com.streamxhub.streamx.flink.core.{FlinkTableInitializer, TableContext}
import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.table.api.TableConfig

import scala.language.implicitConversions

trait FlinkTable extends Logger {

  var jobExecutionResult: JobExecutionResult = _

  final implicit lazy val parameter: ParameterTool = context.parameter

  implicit var context: TableContext = _

  def main(args: Array[String]): Unit = {
    init(args)
    ready()
    handle()
    jobExecutionResult = context.start()
    destroy()
  }

  private[this] def init(args: Array[String]): Unit = {
    SystemPropertyUtils.setAppHome(KEY_APP_HOME, classOf[FlinkTable])
    context = new TableContext(FlinkTableInitializer.initTable(args, config))
  }

  /**
   * 用户可覆盖次方法...
   *
   */
  def ready(): Unit = {}

  def config(tableConfig: TableConfig, parameter: ParameterTool): Unit = {}

  def handle(): Unit

  def destroy(): Unit = {}

}
