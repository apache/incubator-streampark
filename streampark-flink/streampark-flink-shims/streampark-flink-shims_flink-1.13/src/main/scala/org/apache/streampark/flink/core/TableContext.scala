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

import org.apache.streampark.common.conf.ConfigConst.printLogo

import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.table.api.{Table, TableEnvironment}
import org.apache.flink.table.descriptors.{ConnectorDescriptor, ConnectTableDescriptor}
import org.apache.flink.table.module.ModuleEntry
import org.apache.flink.table.sources.TableSource

class TableContext(override val parameter: ParameterTool, private val tableEnv: TableEnvironment)
  extends FlinkTableTrait(parameter, tableEnv) {

  /**
   * for scala
   *
   * @param args
   */
  def this(args: (ParameterTool, TableEnvironment)) = this(args._1, args._2)

  /**
   * for java
   *
   * @param args
   */
  def this(args: TableEnvConfig) = this(FlinkTableInitializer.initialize(args))

  override def useModules(strings: String*): Unit = tableEnv.useModules(strings: _*)

  override def listFullModules(): Array[ModuleEntry] = tableEnv.listFullModules()

  @deprecated override def connect(
      connectorDescriptor: ConnectorDescriptor): ConnectTableDescriptor =
    tableEnv.connect(connectorDescriptor)

  override def execute(jobName: String): JobExecutionResult = {
    printLogo(s"FlinkTable $jobName Starting...")
    tableEnv.execute(jobName)
  }

  @deprecated override def fromTableSource(source: TableSource[_]): Table =
    tableEnv.fromTableSource(source)

  @deprecated override def insertInto(
      table: Table,
      sinkPath: String,
      sinkPathContinued: String*): Unit =
    tableEnv.insertInto(table, sinkPath, sinkPathContinued: _*)

  @deprecated override def insertInto(targetPath: String, table: Table): Unit =
    tableEnv.insertInto(targetPath, table)

  @deprecated override def explain(table: Table): String = tableEnv.explain(table)

  @deprecated override def explain(table: Table, extended: Boolean): String =
    tableEnv.explain(table, extended)

  @deprecated override def explain(extended: Boolean): String = tableEnv.explain(extended)

  @deprecated override def sqlUpdate(stmt: String): Unit = tableEnv.sqlUpdate(stmt)
}
