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

package org.apache.streampark.flink.connector.hbase.internal

import org.apache.streampark.common.conf.ConfigConst.{DEFAULT_HBASE_COMMIT_BATCH, DEFAULT_HBASE_WRITE_SIZE, KEY_HBASE_COMMIT_BATCH, KEY_HBASE_WRITE_SIZE}
import org.apache.streampark.common.enums.ApiType
import org.apache.streampark.common.enums.ApiType.ApiType
import org.apache.streampark.common.util.{HBaseClient, Logger}
import org.apache.streampark.flink.connector.function.TransformFunction

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client._

import java.lang.{Iterable => JIter}
import java.util.Properties
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

import org.apache.streampark.common.util.Implicits._
import scala.collection.mutable.ArrayBuffer

class HBaseSinkFunction[T](apiType: ApiType = ApiType.scala, tabName: String, prop: Properties)
  extends RichSinkFunction[T]
  with Logger {

  private var connection: Connection = _
  private var table: Table = _
  private var mutator: BufferedMutator = _
  private val offset: AtomicLong = new AtomicLong(0L)
  private val scheduled: AtomicBoolean = new AtomicBoolean(false)
  private var timestamp = 0L

  private val commitBatch =
    prop.getOrElse(KEY_HBASE_COMMIT_BATCH, s"$DEFAULT_HBASE_COMMIT_BATCH").toInt
  private val writeBufferSize =
    prop.getOrElse(KEY_HBASE_WRITE_SIZE, s"$DEFAULT_HBASE_WRITE_SIZE").toLong

  private val mutations = new ArrayBuffer[Mutation]()
  private val putArray = new ArrayBuffer[Put]()

  private[this] var scalaTransformFunc: T => JIter[Mutation] = _
  private[this] var javaTransformFunc: TransformFunction[T, JIter[Mutation]] = _

  // for Scala
  def this(tabName: String, properties: Properties, scalaTransformFunc: T => JIter[Mutation]) = {

    this(ApiType.scala, tabName, properties)
    this.scalaTransformFunc = scalaTransformFunc
  }

  // for JAVA
  def this(
      tabName: String,
      properties: Properties,
      javaTransformFunc: TransformFunction[T, JIter[Mutation]]) = {

    this(ApiType.java, tabName, properties)
    this.javaTransformFunc = javaTransformFunc
  }

  @transient private var service: ScheduledExecutorService = _

  override def open(parameters: Configuration): Unit = {
    service = Executors.newSingleThreadScheduledExecutor()
    connection = HBaseClient(prop).connection
    val tableName = TableName.valueOf(tabName)
    val mutatorParam = new BufferedMutatorParams(tableName)
      .writeBufferSize(writeBufferSize)
      .listener(new BufferedMutator.ExceptionListener {
        override def onException(
            exception: RetriesExhaustedWithDetailsException,
            mutator: BufferedMutator): Unit = {
          for (i <- 0.until(exception.getNumExceptions)) {
            logger.error(
              s"[StreamPark] HBaseSink Failed to sent put ${exception.getRow(i)},error:${exception.getLocalizedMessage}")
          }
        }
      })
    mutator = connection.getBufferedMutator(mutatorParam)
    table = connection.getTable(tableName)
  }

  override def invoke(value: T, context: SinkFunction.Context): Unit = {
    val list = apiType match {
      case ApiType.java => javaTransformFunc.transform(value)
      case ApiType.scala => scalaTransformFunc(value)
    }

    list.foreach {
      case put: Put => putArray += put
      case other => mutations += other
    }

    offset.incrementAndGet() % commitBatch match {
      case 0 => execBatch()
      case _ =>
        if (!scheduled.get()) {
          scheduled.set(true)
          service.schedule(
            new Runnable {
              override def run(): Unit = {
                scheduled.set(false)
                execBatch()
              }
            },
            10,
            TimeUnit.SECONDS)
        }
    }

  }

  override def close(): Unit = {
    execBatch()
    if (mutator != null) {
      mutator.flush()
      mutator.close()
    }
    if (table != null) {
      table.close()
    }
  }

  private[this] def execBatch(): Unit = {
    if (offset.get() > 0) {
      val start = System.currentTimeMillis()
      // put ...
      mutator.mutate(putArray)
      mutator.flush()
      putArray.clear()
      // mutation...
      if (mutations.nonEmpty) {
        table.batch(mutations, new Array[AnyRef](mutations.length))
        logInfo(
          s"HBaseSink batchSize:${mutations.length} use ${System.currentTimeMillis() - start} MS")
        mutations.clear()
      }
      offset.set(0L)
      timestamp = System.currentTimeMillis()
    }
  }

}
