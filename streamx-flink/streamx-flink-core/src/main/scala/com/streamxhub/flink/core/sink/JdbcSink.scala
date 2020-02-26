/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.flink.core.sink


import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import java.sql._
import java.util.concurrent.atomic.AtomicLong
import java.util.Properties

import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.util.{ConfigUtils, Logger, JdbcUtils}
import com.streamxhub.flink.core.StreamingContext
import com.streamxhub.flink.core.sink.Dialect.Dialect
import org.apache.flink.api.common.io.RichOutputFormat
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.environment.CheckpointConfig
import org.apache.flink.streaming.api.scala.DataStream

import scala.collection.Map

object JdbcSink {

  /**
   * @param ctx   : StreamingContext
   * @param alias :    实例别名(用于区分多个不同的数据库实例...)
   * @return
   */
  def apply(@transient ctx: StreamingContext,
            overrideParams: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit alias: String = ""): JdbcSink = new JdbcSink(ctx, overrideParams, parallelism, name, uid)

}

class JdbcSink(@transient ctx: StreamingContext,
               overrideParams: Map[String, String] = Map.empty[String, String],
               parallelism: Int = 0,
               name: String = null,
               uid: String = null)(implicit alias: String = "") extends Sink with Logger {

  /**
   *
   * @param stream  : DataStream
   * @param dialect : 数据库方言
   * @param toSQLFn : 转换成SQL的函数,有用户提供.
   * @tparam T : DataStream里的流的数据类型
   * @return
   */
  def sink[T](stream: DataStream[T], dialect: Dialect = Dialect.MYSQL, isolationLevel: Int = -1)(implicit toSQLFn: T => String): DataStreamSink[T] = {
    val prop = ConfigUtils.getJdbcConf(ctx.paramMap, dialect.toString.toLowerCase, alias)
    overrideParams.foreach(x => prop.put(x._1, x._2))
    val sinkFun = new JdbcSinkFunction[T](prop, toSQLFn, isolationLevel)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

}

class JdbcSinkFunction[T](config: Properties, toSQLFn: T => String, isolationLevel: Int) extends RichSinkFunction[T] with Logger {
  private var connection: Connection = _
  private var statement: Statement = _
  private val batchSize = config.remove(KEY_JDBC_INSERT_BATCH) match {
    case null => DEFAULT_JDBC_INSERT_BATCH
    case batch => batch.toString.toInt
  }

  private val offset: AtomicLong = new AtomicLong(0L)
  private var timestamp: Long = 0L

  @throws[Exception]
  override def open(parameters: Configuration): Unit = {
    logInfo("[StreamX] JdbcSink Open....")
    connection = JdbcUtils.getConnection(config)
    connection.setAutoCommit(false)
    if (isolationLevel > -1) {
      connection.setTransactionIsolation(isolationLevel)
    }
    if (batchSize > 1) {
      statement = connection.createStatement()
    }
  }

  override def invoke(value: T, context: SinkFunction.Context[_]): Unit = {
    require(connection != null)
    val sql = toSQLFn(value)
    batchSize match {
      case 1 =>
        try {
          statement = connection.prepareStatement(sql)
          statement.asInstanceOf[PreparedStatement].executeUpdate
          connection.commit()
        } catch {
          case e: Exception =>
            logError(s"[StreamX] JdbcSink invoke error:${sql}")
            throw e
          case _ =>
        }
      case batch =>
        try {
          statement.addBatch(sql)
          (offset.incrementAndGet() % batch, System.currentTimeMillis()) match {
            case (0, _) => execBatch()
            case (_, current) if current - timestamp > 1000 => execBatch()
            case _ =>
          }
        } catch {
          case e: Exception =>
            logError(s"[StreamX] JdbcSink batch invoke error:${sql}")
            throw e
          case _ =>
        }
    }
  }

  override def close(): Unit = {
    execBatch()
    JdbcUtils.close(statement, connection)
  }

  private[this] def execBatch(): Unit = {
    if (offset.get() > 0) {
      offset.set(0L)
      val start = System.currentTimeMillis()
      val count = statement.executeBatch().sum
      statement.clearBatch()
      connection.commit()
      logInfo(s"[StreamX] JdbcSink batch $count use ${System.currentTimeMillis() - start} MS")
      timestamp = System.currentTimeMillis()
    }
  }

}


class JdbcOutputFormat[T: TypeInformation](implicit prop: Properties, toSQlFun: T => String, isolationLevel: Int = -1) extends RichOutputFormat[T] with Logger {

  val sinkFunction = new JdbcSinkFunction[T](prop, toSQlFun, isolationLevel)

  var configuration: Configuration = _

  override def configure(configuration: Configuration): Unit = this.configuration = configuration

  override def open(taskNumber: Int, numTasks: Int): Unit = sinkFunction.open(this.configuration)

  override def writeRecord(record: T): Unit = sinkFunction.invoke(record, null)

  override def close(): Unit = sinkFunction.close()
}

object Dialect extends Enumeration {
  type Dialect = Value
  val MYSQL, ORACLE, MSSQL, H2 = Value
}



