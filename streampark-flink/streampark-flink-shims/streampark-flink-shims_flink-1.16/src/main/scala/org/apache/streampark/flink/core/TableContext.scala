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
import org.apache.flink.table.api.{CompiledPlan, PlanReference, Table, TableDescriptor, TableEnvironment, TableResult}
import org.apache.flink.table.module.ModuleEntry

class TableContext(override val parameter: ParameterTool, private val tableEnv: TableEnvironment)
  extends FlinkTableTrait(parameter, tableEnv) {

  def this(args: (ParameterTool, TableEnvironment)) = this(args._1, args._2)

  def this(args: TableEnvConfig) = this(FlinkTableInitializer.initialize(args))

  override def useModules(strings: String*): Unit = tableEnv.useModules(strings: _*)

  override def createTemporaryTable(path: String, descriptor: TableDescriptor): Unit = {
    tableEnv.createTemporaryTable(path, descriptor)
  }

  override def createTable(path: String, descriptor: TableDescriptor): Unit = {
    tableEnv.createTable(path, descriptor)
  }

  override def from(tableDescriptor: TableDescriptor): Table = {
    tableEnv.from(tableDescriptor)
  }

  override def listFullModules(): Array[ModuleEntry] = tableEnv.listFullModules()

  /** @since 1.15 */
  override def listTables(catalogName: String, databaseName: String): Array[String] =
    tableEnv.listTables(catalogName, databaseName)

  /** @since 1.15 */
  override def loadPlan(planReference: PlanReference): CompiledPlan =
    tableEnv.loadPlan(planReference)

  /** @since 1.15 */
  override def compilePlanSql(stmt: String): CompiledPlan = tableEnv.compilePlanSql(stmt)
}
