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

package org.apache.streampark.flink.core.scala

import org.apache.streampark.common.conf.ConfigKeys._
import org.apache.streampark.common.util.{Logger, SystemPropertyUtils}
import org.apache.streampark.flink.core.{FlinkTableInitializer, TableContext}

import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.table.api.TableConfig

trait FlinkTable extends Logger {

  var jobExecutionResult: JobExecutionResult = _

  implicit final lazy val parameter: ParameterTool = context.parameter

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
    context = new TableContext(FlinkTableInitializer.initialize(args, config))
  }

  def ready(): Unit = {}

  def config(tableConfig: TableConfig, parameter: ParameterTool): Unit = {}

  def handle(): Unit

  def destroy(): Unit = {}

}
