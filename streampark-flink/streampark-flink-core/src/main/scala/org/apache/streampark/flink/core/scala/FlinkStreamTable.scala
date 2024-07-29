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
import org.apache.streampark.flink.core.{FlinkTableInitializer, StreamTableContext}
import org.apache.streampark.flink.core.TableExt

import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.{Table, TableConfig}

import scala.language.implicitConversions

trait FlinkStreamTable extends Logger {

  implicit final def tableExt(table: Table): TableExt.Table =
    new TableExt.Table(table)

  implicit final def tableConversions(table: Table): TableExt.TableConversions =
    new TableExt.TableConversions(table)

  implicit final lazy val parameter: ParameterTool = context.parameter

  implicit var context: StreamTableContext = _

  var jobExecutionResult: JobExecutionResult = _

  def main(args: Array[String]): Unit = {
    init(args)
    ready()
    handle()
    jobExecutionResult = context.start()
    destroy()
  }

  private[this] def init(args: Array[String]): Unit = {
    SystemPropertyUtils.setAppHome(KEY_APP_HOME, classOf[FlinkStreamTable])
    context = new StreamTableContext(
      FlinkTableInitializer.initialize(args, configStream, configTable))
  }

  def configStream(env: StreamExecutionEnvironment, parameter: ParameterTool): Unit = {}

  def configTable(tableConfig: TableConfig, parameter: ParameterTool): Unit = {}

  def ready(): Unit = {}

  def handle(): Unit

  def destroy(): Unit = {}

}
