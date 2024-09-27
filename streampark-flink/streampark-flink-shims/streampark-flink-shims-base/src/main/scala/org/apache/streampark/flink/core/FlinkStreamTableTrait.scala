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

import org.apache.streampark.common.util.Implicits.JavaList
import org.apache.streampark.common.util.Utils
import org.apache.streampark.flink.core.EnhancerImplicit._

import com.esotericsoftware.kryo.Serializer
import org.apache.flink.api.common.{JobExecutionResult, RuntimeExecutionMode}
import org.apache.flink.api.common.cache.DistributedCache
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.io.{FileInputFormat, FilePathFilter, InputFormat}
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.connector.source.{Source, SourceSplit}
import org.apache.flink.api.java.tuple
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.ReadableConfig
import org.apache.flink.core.execution.{JobClient, JobListener}
import org.apache.flink.runtime.state.StateBackend
import org.apache.flink.streaming.api._
import org.apache.flink.streaming.api.environment.{CheckpointConfig, StreamExecutionEnvironment => JavaStreamExecutionEnvironment}
import org.apache.flink.streaming.api.functions.source._
import org.apache.flink.streaming.api.graph.StreamGraph
import org.apache.flink.streaming.api.scala._
import org.apache.flink.table.api._
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.table.catalog.Catalog
import org.apache.flink.table.expressions.Expression
import org.apache.flink.table.functions._
import org.apache.flink.table.module.Module
import org.apache.flink.table.types.AbstractDataType
import org.apache.flink.util.SplittableIterator

import java.util.Optional

/**
 * Integration api of stream and table
 *
 * @param parameter
 *   parameter
 * @param streamEnv
 *   streamEnv
 * @param tableEnv
 *   tableEnv
 */
abstract class FlinkStreamTableTrait(
    val parameter: ParameterTool,
    private val streamEnv: StreamExecutionEnvironment,
    private val tableEnv: StreamTableEnvironment)
  extends StreamTableEnvironment {

  /**
   * Once a Table has been converted to a DataStream, the DataStream job must be executed using the
   * execute method of the StreamExecutionEnvironment.
   */
  var isConvertedToDataStream: Boolean = false

  /** Recommended to use this Api to start tasks */
  def start(name: String = null): JobExecutionResult = {
    val appName = parameter.getAppName(name, true)
    execute(appName)
  }

  @deprecated def execute(jobName: String): JobExecutionResult = {
    Utils.printLogo(s"FlinkStreamTable $jobName Starting...")
    if (isConvertedToDataStream) {
      streamEnv.execute(jobName)
    } else null
  }

  def sql(sql: String = null)(implicit callback: String => Unit = null): Unit =
    FlinkSqlExecutor.executeSql(sql, parameter, this)
  // ...streamEnv api start...

  def getJavaEnv: JavaStreamExecutionEnvironment = this.streamEnv.getJavaEnv

  def $getCachedFiles: JavaList[tuple.Tuple2[String, DistributedCache.DistributedCacheEntry]] =
    this.streamEnv.getCachedFiles

  def $getJobListeners: JavaList[JobListener] = this.streamEnv.getJobListeners

  def $setParallelism(parallelism: Int): Unit =
    this.streamEnv.setParallelism(parallelism)

  def $setRuntimeMode(deployMode: RuntimeExecutionMode): StreamExecutionEnvironment =
    this.streamEnv.setRuntimeMode(deployMode)

  def $setMaxParallelism(maxParallelism: Int): Unit =
    this.streamEnv.setMaxParallelism(maxParallelism)

  def $getParallelism: Int = this.streamEnv.getParallelism

  def $getMaxParallelism: Int = this.streamEnv.getMaxParallelism

  def $setBufferTimeout(timeoutMillis: Long): StreamExecutionEnvironment =
    this.streamEnv.setBufferTimeout(timeoutMillis)

  def $getBufferTimeout: Long = this.streamEnv.getBufferTimeout

  def $disableOperatorChaining(): StreamExecutionEnvironment =
    this.streamEnv.disableOperatorChaining()

  def $getCheckpointConfig: CheckpointConfig =
    this.streamEnv.getCheckpointConfig

  def $enableCheckpointing(interval: Long, mode: CheckpointingMode): StreamExecutionEnvironment =
    this.streamEnv.enableCheckpointing(interval, mode)

  def $enableCheckpointing(interval: Long): StreamExecutionEnvironment =
    this.streamEnv.enableCheckpointing(interval)

  def $getCheckpointingMode: CheckpointingMode =
    this.streamEnv.getCheckpointingMode

  def $setStateBackend(backend: StateBackend): StreamExecutionEnvironment =
    this.streamEnv.setStateBackend(backend)

  def $getStateBackend: StateBackend = this.streamEnv.getStateBackend

  def $setRestartStrategy(
      restartStrategyConfiguration: RestartStrategies.RestartStrategyConfiguration): Unit =
    this.streamEnv.setRestartStrategy(restartStrategyConfiguration)

  def $getRestartStrategy: RestartStrategies.RestartStrategyConfiguration =
    this.streamEnv.getRestartStrategy

  def $setNumberOfExecutionRetries(numRetries: Int): Unit =
    this.streamEnv.setNumberOfExecutionRetries(numRetries)

  def $getNumberOfExecutionRetries: Int =
    this.streamEnv.getNumberOfExecutionRetries

  def $addDefaultKryoSerializer[T <: Serializer[_] with Serializable](
      `type`: Class[_],
      serializer: T): Unit =
    this.streamEnv.addDefaultKryoSerializer(`type`, serializer)

  def $addDefaultKryoSerializer(
      `type`: Class[_],
      serializerClass: Class[_ <: Serializer[_]]): Unit =
    this.streamEnv.addDefaultKryoSerializer(`type`, serializerClass)

  def $registerTypeWithKryoSerializer[T <: Serializer[_] with Serializable](
      clazz: Class[_],
      serializer: T): Unit =
    this.streamEnv.registerTypeWithKryoSerializer(clazz, serializer)

  def $registerTypeWithKryoSerializer(
      clazz: Class[_],
      serializer: Class[_ <: Serializer[_]]): Unit =
    this.streamEnv.registerTypeWithKryoSerializer(clazz, serializer)

  def $registerType(typeClass: Class[_]): Unit =
    this.streamEnv.registerType(typeClass)

  def $getStreamTimeCharacteristic: TimeCharacteristic =
    this.streamEnv.getStreamTimeCharacteristic

  def $configure(configuration: ReadableConfig, classLoader: ClassLoader): Unit =
    this.streamEnv.configure(configuration, classLoader)

  def $fromSequence(from: Long, to: Long): DataStream[Long] =
    this.streamEnv.fromSequence(from, to)

  def $fromElements[T](data: T*)(implicit info: TypeInformation[T]): DataStream[T] =
    this.streamEnv.fromElements(data: _*)

  def $fromCollection[T](data: Seq[T])(implicit info: TypeInformation[T]): DataStream[T] =
    this.streamEnv.fromCollection(data)

  def $fromCollection[T](data: Iterator[T])(implicit info: TypeInformation[T]): DataStream[T] =
    this.streamEnv.fromCollection(data)

  def $fromParallelCollection[T](data: SplittableIterator[T])(implicit info: TypeInformation[T]): DataStream[T] =
    this.streamEnv.fromParallelCollection(data)

  def $readTextFile(filePath: String): DataStream[String] =
    this.streamEnv.readTextFile(filePath)

  def $readTextFile(filePath: String, charsetName: String): DataStream[String] =
    this.streamEnv.readTextFile(filePath, charsetName)

  def $readFile[T](inputFormat: FileInputFormat[T], filePath: String)(implicit info: TypeInformation[T]): DataStream[T] =
    this.streamEnv.readFile(inputFormat, filePath)

  def $readFile[T](
      inputFormat: FileInputFormat[T],
      filePath: String,
      watchType: FileProcessingMode,
      interval: Long)(implicit info: TypeInformation[T]): DataStream[T] =
    this.streamEnv.readFile(inputFormat, filePath, watchType, interval)

  def $socketTextStream(
      hostname: String,
      port: Int,
      delimiter: Char,
      maxRetry: Long): DataStream[String] =
    this.streamEnv.socketTextStream(hostname, port, delimiter, maxRetry)

  def $createInput[T](inputFormat: InputFormat[T, _])(implicit info: TypeInformation[T]): DataStream[T] =
    this.streamEnv.createInput(inputFormat)

  def $addSource[T](function: SourceFunction[T])(implicit info: TypeInformation[T]): DataStream[T] =
    this.streamEnv.addSource(function)

  def $addSource[T](function: SourceFunction.SourceContext[T] => Unit)(implicit info: TypeInformation[T]): DataStream[T] =
    this.streamEnv.addSource(function)

  def $fromSource[T](
      source: Source[T, _ <: SourceSplit, _],
      watermarkStrategy: WatermarkStrategy[T],
      sourceName: String)(implicit info: TypeInformation[T]): DataStream[T] =
    this.streamEnv.fromSource(source, watermarkStrategy, sourceName)

  def $registerJobListener(jobListener: JobListener): Unit =
    this.streamEnv.registerJobListener(jobListener)

  def $clearJobListeners(): Unit = this.streamEnv.clearJobListeners()

  def $executeAsync(): JobClient = this.streamEnv.executeAsync()

  def $executeAsync(jobName: String): JobClient =
    this.streamEnv.executeAsync(jobName)

  def $getExecutionPlan: String = this.streamEnv.getExecutionPlan

  def $getStreamGraph: StreamGraph = this.streamEnv.getStreamGraph

  def $getWrappedStreamExecutionEnvironment: JavaStreamExecutionEnvironment =
    this.streamEnv.getWrappedStreamExecutionEnvironment

  def $registerCachedFile(filePath: String, name: String): Unit =
    this.streamEnv.registerCachedFile(filePath, name)

  def $registerCachedFile(filePath: String, name: String, executable: Boolean): Unit =
    this.streamEnv.registerCachedFile(filePath, name, executable)

  def $isUnalignedCheckpointsEnabled: Boolean =
    this.streamEnv.isUnalignedCheckpointsEnabled

  def $isForceUnalignedCheckpoints: Boolean =
    this.streamEnv.isForceUnalignedCheckpoints

  @deprecated def $enableCheckpointing(
      interval: Long,
      mode: CheckpointingMode,
      force: Boolean): StreamExecutionEnvironment =
    this.streamEnv.enableCheckpointing(interval, mode, force)

  @deprecated def $enableCheckpointing(): StreamExecutionEnvironment =
    this.streamEnv.enableCheckpointing()

  @deprecated def $generateSequence(from: Long, to: Long): DataStream[Long] =
    this.streamEnv.generateSequence(from, to)

  @deprecated def $readFileStream(
      StreamPath: String,
      intervalMillis: Long,
      watchType: FileMonitoringFunction.WatchType): DataStream[String] =
    this.streamEnv.readFileStream(StreamPath, intervalMillis, watchType)

  @deprecated def $readFile[T](
      inputFormat: FileInputFormat[T],
      filePath: String,
      watchType: FileProcessingMode,
      interval: Long,
      filter: FilePathFilter)(implicit info: TypeInformation[T]): DataStream[T] =
    this.streamEnv.readFile(inputFormat, filePath, watchType, interval, filter)

  // ...streamEnv api end...

  override def fromDataStream[T](dataStream: DataStream[T]): Table =
    tableEnv.fromDataStream(dataStream)

  override def fromDataStream[T](dataStream: DataStream[T], fields: Expression*): Table =
    tableEnv.fromDataStream(dataStream, fields: _*)

  override def createTemporaryView[T](path: String, dataStream: DataStream[T]): Unit =
    tableEnv.createTemporaryView(path, dataStream)

  override def createTemporaryView[T](
      path: String,
      dataStream: DataStream[T],
      fields: Expression*): Unit =
    tableEnv.createTemporaryView(path, dataStream, fields: _*)

  override def toAppendStream[T](table: Table)(implicit info: TypeInformation[T]): DataStream[T] = {
    isConvertedToDataStream = true
    tableEnv.toAppendStream(table)
  }

  override def toRetractStream[T](table: Table)(implicit info: TypeInformation[T]): DataStream[(Boolean, T)] = {
    isConvertedToDataStream = true
    tableEnv.toRetractStream(table)
  }

  override def fromValues(values: Expression*): Table =
    tableEnv.fromValues(values)

  override def fromValues(rowType: AbstractDataType[_], values: Expression*): Table =
    tableEnv.fromValues(rowType, values: _*)

  override def fromValues(values: java.lang.Iterable[_]): Table =
    tableEnv.fromValues(values)

  override def fromValues(rowType: AbstractDataType[_], values: java.lang.Iterable[_]): Table =
    tableEnv.fromValues(rowType, values)

  override def registerCatalog(catalogName: String, catalog: Catalog): Unit =
    tableEnv.registerCatalog(catalogName, catalog)

  override def getCatalog(catalogName: String): Optional[Catalog] =
    tableEnv.getCatalog(catalogName)

  override def loadModule(moduleName: String, module: Module): Unit =
    tableEnv.loadModule(moduleName, module)

  override def unloadModule(moduleName: String): Unit =
    tableEnv.unloadModule(moduleName)

  override def createTemporarySystemFunction(
      name: String,
      functionClass: Class[_ <: UserDefinedFunction]): Unit =
    tableEnv.createTemporarySystemFunction(name, functionClass)

  override def createTemporarySystemFunction(
      name: String,
      functionInstance: UserDefinedFunction): Unit =
    tableEnv.createTemporarySystemFunction(name, functionInstance)

  override def dropTemporarySystemFunction(name: String): Boolean =
    tableEnv.dropTemporarySystemFunction(name)

  override def createFunction(path: String, functionClass: Class[_ <: UserDefinedFunction]): Unit =
    tableEnv.createFunction(path, functionClass)

  override def createFunction(
      path: String,
      functionClass: Class[_ <: UserDefinedFunction],
      ignoreIfExists: Boolean): Unit =
    tableEnv.createFunction(path, functionClass)

  override def dropFunction(path: String): Boolean = tableEnv.dropFunction(path)

  override def createTemporaryFunction(
      path: String,
      functionClass: Class[_ <: UserDefinedFunction]): Unit =
    tableEnv.createTemporaryFunction(path, functionClass)

  override def createTemporaryFunction(path: String, functionInstance: UserDefinedFunction): Unit =
    tableEnv.createTemporaryFunction(path, functionInstance)

  override def dropTemporaryFunction(path: String): Boolean =
    tableEnv.dropTemporaryFunction(path)

  override def createTemporaryView(path: String, view: Table): Unit =
    tableEnv.createTemporaryView(path, view)

  override def from(path: String): Table = tableEnv.from(path)

  override def listCatalogs(): Array[String] = tableEnv.listCatalogs()

  override def listModules(): Array[String] = tableEnv.listModules()

  override def listDatabases(): Array[String] = tableEnv.listDatabases()

  override def listTables(): Array[String] = tableEnv.listTables()

  override def listViews(): Array[String] = tableEnv.listViews()

  override def listTemporaryTables(): Array[String] =
    tableEnv.listTemporaryTables

  override def listTemporaryViews(): Array[String] =
    tableEnv.listTemporaryViews()

  override def listUserDefinedFunctions(): Array[String] =
    tableEnv.listUserDefinedFunctions()

  override def listFunctions(): Array[String] = tableEnv.listFunctions()

  override def dropTemporaryTable(path: String): Boolean =
    tableEnv.dropTemporaryTable(path)

  override def dropTemporaryView(path: String): Boolean =
    tableEnv.dropTemporaryView(path)

  override def explainSql(statement: String, extraDetails: ExplainDetail*): String =
    tableEnv.explainSql(statement, extraDetails: _*)

  override def sqlQuery(query: String): Table = tableEnv.sqlQuery(query)

  override def executeSql(statement: String): TableResult =
    tableEnv.executeSql(statement)

  override def getCurrentCatalog: String = tableEnv.getCurrentCatalog

  override def useCatalog(catalogName: String): Unit =
    tableEnv.useCatalog(catalogName)

  override def getCurrentDatabase: String = tableEnv.getCurrentDatabase

  override def useDatabase(databaseName: String): Unit =
    tableEnv.useDatabase(databaseName)

  override def getConfig: TableConfig = tableEnv.getConfig

  /**
   * @param name
   * @param dataStream
   * @tparam T
   */
  @deprecated override def registerFunction[T](name: String, tf: TableFunction[T])(implicit info: TypeInformation[T]): Unit =
    tableEnv.registerFunction(name, tf)

  @deprecated override def registerFunction[T, ACC](name: String, f: AggregateFunction[T, ACC])(
      implicit
      info1: TypeInformation[T],
      info2: TypeInformation[ACC]): Unit = tableEnv.registerFunction(name, f)

  @deprecated override def registerFunction[T, ACC](
      name: String,
      f: TableAggregateFunction[T, ACC])(implicit info1: TypeInformation[T], info2: TypeInformation[ACC]): Unit = tableEnv.registerFunction(name, f)

  @deprecated override def registerDataStream[T](name: String, dataStream: DataStream[T]): Unit =
    tableEnv.registerDataStream(name, dataStream)

  @deprecated override def registerDataStream[T](
      name: String,
      dataStream: DataStream[T],
      fields: Expression*): Unit =
    tableEnv.registerDataStream(name, dataStream, fields: _*)

  @deprecated override def registerFunction(name: String, function: ScalarFunction): Unit =
    tableEnv.registerFunction(name, function)

  @deprecated override def registerTable(name: String, table: Table): Unit =
    tableEnv.registerTable(name, table)

  @deprecated override def scan(tablePath: String*): Table =
    tableEnv.scan(tablePath: _*)

  @deprecated override def getCompletionHints(statement: String, position: Int): Array[String] =
    tableEnv.getCompletionHints(statement, position)
}
