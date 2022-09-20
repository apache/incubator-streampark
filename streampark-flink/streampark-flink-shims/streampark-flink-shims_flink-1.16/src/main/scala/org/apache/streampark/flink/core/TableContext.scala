package org.apache.streampark.flink.core

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.table.api.{CompiledPlan, PlanReference, Table, TableDescriptor, TableEnvironment, TableResult}
import org.apache.flink.table.module.ModuleEntry

class TableContext(override val parameter: ParameterTool,
                   private val tableEnv: TableEnvironment)
  extends FlinkTableTrait(parameter, tableEnv) {

  def this(args: (ParameterTool, TableEnvironment)) = this(args._1, args._2)

  def this(args: TableEnvConfig) = this(FlinkTableInitializer.initJavaTable(args))

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

  /**
   * @since 1.15
   */
  override def listTables(catalogName: String, databaseName: String): Array[String] = tableEnv.listTables(catalogName, databaseName)

  /**
   * @since 1.15
   */
  override def loadPlan(planReference: PlanReference): CompiledPlan = tableEnv.loadPlan(planReference)

  /**
   * @since 1.15
   */
  override def compilePlanSql(stmt: String): CompiledPlan = tableEnv.compilePlanSql(stmt)
}
