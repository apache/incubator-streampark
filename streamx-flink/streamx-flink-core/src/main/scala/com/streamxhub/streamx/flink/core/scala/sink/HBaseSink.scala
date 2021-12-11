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

package com.streamxhub.streamx.flink.core.scala.sink

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util.{ConfigUtils, HBaseClient, Logger, Utils}
import com.streamxhub.streamx.flink.core.scala.StreamingContext
import org.apache.flink.api.common.io.RichOutputFormat
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client._

import java.lang.{Iterable => JIter}
import java.util.Properties
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import scala.annotation.meta.param
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

object HBaseSink {

  def apply(@(transient@param)
            property: Properties = new Properties(),
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit ctx: StreamingContext): HBaseSink = new HBaseSink(ctx, property, parallelism, name, uid)

}

class HBaseSink(@(transient@param) ctx: StreamingContext,
                property: Properties = new Properties(),
                parallelism: Int = 0,
                name: String = null,
                uid: String = null)(implicit alias: String = "") extends Sink with Logger {

  /**
   * @param stream
   * @param tableName
   * @param fun
   * @tparam T
   * @return
   */
  def sink[T](stream: DataStream[T], tableName: String)(implicit fun: T => JIter[Mutation]): DataStreamSink[T] = {
    implicit val prop: Properties = ConfigUtils.getConf(ctx.parameter.toMap, HBASE_PREFIX, HBASE_PREFIX)(alias)
    Utils.copyProperties(property, prop)
    val sinkFun = new HBaseSinkFunction[T](tableName, fun)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

}

class HBaseSinkFunction[T](tabName: String, fun: T => JIter[Mutation])(implicit prop: Properties) extends RichSinkFunction[T] with Logger {

  private var connection: Connection = _
  private var table: Table = _
  private var mutator: BufferedMutator = _
  private val offset: AtomicLong = new AtomicLong(0L)
  private val scheduled: AtomicBoolean = new AtomicBoolean(false)
  private var timestamp = 0L

  private val commitBatch = prop.getOrElse(KEY_HBASE_COMMIT_BATCH, s"$DEFAULT_HBASE_COMMIT_BATCH").toInt
  private val writeBufferSize = prop.getOrElse(KEY_HBASE_WRITE_SIZE, s"$DEFAULT_HBASE_WRITE_SIZE").toLong

  private val mutations = new ArrayBuffer[Mutation]()
  private val putArray = new ArrayBuffer[Put]()

  @transient private var service: ScheduledExecutorService = _

  override def open(parameters: Configuration): Unit = {
    service = Executors.newSingleThreadScheduledExecutor()
    connection = HBaseClient(prop).connection
    val tableName = TableName.valueOf(tabName)
    val mutatorParam = new BufferedMutatorParams(tableName)
      .writeBufferSize(writeBufferSize)
      .listener(new BufferedMutator.ExceptionListener {
        override def onException(exception: RetriesExhaustedWithDetailsException, mutator: BufferedMutator): Unit = {
          for (i <- 0.until(exception.getNumExceptions)) {
            logger.error(s"[StreamX] HBaseSink Failed to sent put ${exception.getRow(i)},error:${exception.getLocalizedMessage}")
          }
        }
      })
    mutator = connection.getBufferedMutator(mutatorParam)
    table = connection.getTable(tableName)
  }

  override def invoke(value: T, context: SinkFunction.Context): Unit = {
    fun(value).foreach {
      case put: Put => putArray += put
      case other => mutations += other
    }

    offset.incrementAndGet() % commitBatch match {
      case 0 => execBatch()
      case _ => if (!scheduled.get()) {
        scheduled.set(true)
        service.schedule(new Runnable {
          override def run(): Unit = {
            scheduled.set(false)
            execBatch()
          }
        }, 10, TimeUnit.SECONDS)
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
      //put ...
      mutator.mutate(putArray)
      mutator.flush()
      putArray.clear()
      //mutation...
      if (mutations.nonEmpty) {
        table.batch(mutations, new Array[AnyRef](mutations.length))
        logInfo(s"HBaseSink batchSize:${mutations.length} use ${System.currentTimeMillis() - start} MS")
        mutations.clear()
      }
      offset.set(0L)
      timestamp = System.currentTimeMillis()
    }
  }

}

class HBaseOutputFormat[T: TypeInformation](tabName: String, fun: T => JIter[Mutation])(implicit prop: Properties) extends RichOutputFormat[T] with Logger {

  val sinkFunction = new HBaseSinkFunction[T](tabName, fun)

  var configuration: Configuration = _

  override def configure(configuration: Configuration): Unit = this.configuration = configuration

  override def open(taskNumber: Int, numTasks: Int): Unit = sinkFunction.open(this.configuration)

  override def writeRecord(record: T): Unit = sinkFunction.invoke(record, null)

  override def close(): Unit = sinkFunction.close()
}
