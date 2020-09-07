package com.streamxhub.repl.flink.interpreter

import com.streamxhub.repl.flink.shims.FlinkShims
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.flink.api.scala.{ExecutionEnvironment => ScalaEnv}
import org.apache.flink.streaming.api.environment.{StreamExecutionEnvironment => JavaStreamEnv}
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment => ScalaStreamEnv}
import org.apache.flink.table.api.{EnvironmentSettings, TableConfig, TableEnvironment, TableException}
import org.apache.flink.table.api.bridge.java.internal.{BatchTableEnvironmentImpl => JavaBatchTable, StreamTableEnvironmentImpl => JavaStreamTable}
import org.apache.flink.table.api.bridge.scala.internal.{BatchTableEnvironmentImpl => ScalaBatchTable, StreamTableEnvironmentImpl => ScalaStreamTable}
import org.apache.flink.table.catalog.{CatalogManager, FunctionCatalog}
import org.apache.flink.table.delegation.{Executor, ExecutorFactory, PlannerFactory}
import org.apache.flink.table.factories.ComponentFactoryService
import org.apache.flink.table.module.ModuleManager
import org.slf4j.LoggerFactory


/**
 * Factory class for creating flink table env for different purpose:
 * 1. java/scala
 * 2. stream table / batch table
 * 3. flink planner / blink planner
 *
 */

class TableEnvFactory(var flinkShims: FlinkShims,
                      var benv: ScalaEnv,
                      var senv: ScalaStreamEnv,
                      var tblConfig: TableConfig,
                      var catalogManager: CatalogManager,
                      var moduleManager: ModuleManager,
                      var flinkFunctionCatalog: FunctionCatalog,
                      var blinkFunctionCatalog: FunctionCatalog) {

  private val LOGGER = LoggerFactory.getLogger(classOf[TableEnvFactory])

  private var executor: Executor = _

  def createScalaFlinkBatchTableEnvironment(): TableEnvironment = try {
    new ScalaBatchTable(
      benv,
      tblConfig,
      catalogManager,
      moduleManager
    )
  } catch {
    case e: Exception =>  throw new TableException("Fail to createScalaFlinkBatchTableEnvironment", e)
  }

  def createScalaFlinkStreamTableEnvironment(settings: EnvironmentSettings, classLoader: ClassLoader): TableEnvironment = try {
    val executor = lookupExecutor(settings.toExecutorProperties, senv.getJavaEnv)
    val plannerProperties = settings.toPlannerProperties
    val planner = ComponentFactoryService
      .find(classOf[PlannerFactory], plannerProperties)
      .create(plannerProperties, executor, tblConfig, flinkFunctionCatalog, catalogManager)
    new ScalaStreamTable(
      catalogManager, moduleManager, flinkFunctionCatalog, tblConfig, senv, planner, executor, settings.isStreamingMode, classLoader
    )
  } catch {
    case e: Exception =>
      throw new TableException("Fail to createScalaFlinkStreamTableEnvironment", e)
  }

  def createJavaFlinkBatchTableEnvironment(): TableEnvironment = try {
    new JavaBatchTable(
      benv.getJavaEnv, tblConfig, catalogManager, moduleManager
    )
  } catch {
    case t: Throwable => throw new TableException("Create BatchTableEnvironment failed.", t)
  }

  def createJavaFlinkStreamTableEnvironment(settings: EnvironmentSettings, classLoader: ClassLoader): TableEnvironment = try {
    val executor = lookupExecutor(settings.toExecutorProperties, senv.getJavaEnv)
    val plannerProperties = settings.toPlannerProperties
    val planner = ComponentFactoryService.find(classOf[PlannerFactory], plannerProperties).create(plannerProperties, executor, tblConfig, flinkFunctionCatalog, catalogManager)
    new JavaStreamTable(
      catalogManager,
      moduleManager,
      flinkFunctionCatalog,
      tblConfig,
      senv.getJavaEnv,
      planner,
      executor,
      settings.isStreamingMode,
      classLoader
    )
  } catch {
    case e: Exception =>  throw new TableException("Fail to createJavaFlinkStreamTableEnvironment", e)
  }

  def createScalaBlinkStreamTableEnvironment(settings: EnvironmentSettings, classLoader: ClassLoader): TableEnvironment = try {
    val executor = lookupExecutor(settings.toExecutorProperties, senv.getJavaEnv)
    val plannerProperties = settings.toPlannerProperties
    val planner = ComponentFactoryService
      .find(classOf[PlannerFactory], plannerProperties)
      .create(
        plannerProperties,
        executor,
        tblConfig,
        blinkFunctionCatalog,
        catalogManager
      )

    new ScalaStreamTable(
      catalogManager,
      moduleManager,
      blinkFunctionCatalog,
      tblConfig,
      senv,
      planner,
      executor,
      settings.isStreamingMode,
      classLoader
    )
  } catch {
    case e: Exception =>
      throw new TableException("Fail to createScalaBlinkStreamTableEnvironment", e)
  }

  def createJavaBlinkStreamTableEnvironment(settings: EnvironmentSettings, classLoader: ClassLoader): TableEnvironment = try {

    val executor = lookupExecutor(settings.toExecutorProperties, senv.getJavaEnv)

    val plannerProperties = settings.toPlannerProperties

    val planner = ComponentFactoryService
      .find(classOf[PlannerFactory], plannerProperties)
      .create(
        plannerProperties,
        executor,
        tblConfig,
        blinkFunctionCatalog,
        catalogManager
      )

    new JavaStreamTable(
      catalogManager,
      moduleManager,
      blinkFunctionCatalog,
      tblConfig,
      senv.getJavaEnv,
      planner,
      executor,
      settings.isStreamingMode,
      classLoader
    )
  } catch {
    case e: Exception =>
      throw new TableException("Fail to createJavaBlinkStreamTableEnvironment", e)
  }

  def createJavaBlinkBatchTableEnvironment(settings: EnvironmentSettings, classLoader: ClassLoader): TableEnvironment = try {
    executor = lookupExecutor(settings.toExecutorProperties, senv.getJavaEnv)
    val plannerProperties = settings.toPlannerProperties
    val planner = ComponentFactoryService
      .find(classOf[PlannerFactory], plannerProperties)
      .create(plannerProperties, executor, tblConfig, blinkFunctionCatalog, catalogManager)
    new JavaStreamTable(
      catalogManager,
      moduleManager,
      blinkFunctionCatalog,
      tblConfig,
      senv.getJavaEnv,
      planner,
      executor,
      settings.isStreamingMode,
      classLoader
    )
  } catch {
    case e: Exception =>
      LOGGER.info(ExceptionUtils.getStackTrace(e))
      throw new TableException("Fail to createJavaBlinkBatchTableEnvironment", e)
  }

  def createPlanner(settings: EnvironmentSettings): Unit = {
    val executor = lookupExecutor(settings.toExecutorProperties, senv.getJavaEnv)
    val plannerProperties = settings.toPlannerProperties
    val planner = ComponentFactoryService.find(classOf[PlannerFactory], plannerProperties).create(plannerProperties, executor, tblConfig, blinkFunctionCatalog, catalogManager)
    this.flinkShims.setCatalogManagerSchemaResolver(catalogManager, planner.getParser, settings)
  }

  private def lookupExecutor(executorProperties: java.util.Map[String, String], executionEnvironment: JavaStreamEnv): Executor = try {
    val executorFactory = ComponentFactoryService.find(classOf[ExecutorFactory], executorProperties)
    val createMethod = executorFactory.getClass.getMethod("create", classOf[java.util.Map[_, _]], classOf[JavaStreamEnv])
    createMethod.invoke(executorFactory, executorProperties, executionEnvironment).asInstanceOf[Executor]
  } catch {
    case e: Exception =>
      throw new TableException("Could not instantiate the executor. Make sure a planner module is on the classpath", e)
  }
}
