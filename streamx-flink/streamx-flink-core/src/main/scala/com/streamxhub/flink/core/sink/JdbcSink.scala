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
import com.streamxhub.common.util.{ConfigUtils, JdbcUtils, Logger}
import com.streamxhub.flink.core.StreamingContext
import com.streamxhub.flink.core.enums.ApiType
import com.streamxhub.flink.core.enums.ApiType.ApiType
import com.streamxhub.flink.core.function.ToSQLFunction
import com.streamxhub.flink.core.sink.Dialect.Dialect
import org.apache.flink.api.common.io.RichOutputFormat
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream

import scala.annotation.meta.param
import scala.collection.Map

object JdbcSink {

  /**
   * @param ctx   : StreamingContext
   * @param alias :    实例别名(用于区分多个不同的数据库实例...)
   * @return
   */
  def apply(@(transient@param) ctx: StreamingContext,
            overrideParams: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit alias: String = ""): JdbcSink = new JdbcSink(ctx, overrideParams, parallelism, name, uid)

}

class JdbcSink(@(transient@param) ctx: StreamingContext,
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
  def sink[T](stream: DataStream[T], dialect: Dialect = Dialect.MYSQL, isolationLevel: Integer = null)(implicit toSQLFn: T => String): DataStreamSink[T] = {
    val prop = ConfigUtils.getJdbcConf(ctx.parameter.toMap, dialect.toString, alias)
    overrideParams.foreach(x => prop.put(x._1, x._2))
    val sinkFun = new JdbcSinkFunction[T](prop, toSQLFn, isolationLevel)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

}

class JdbcSinkFunction[T](apiType: ApiType = ApiType.Scala, jdbc: Properties, isolationLevel: Integer = null) extends RichSinkFunction[T] with Logger {
  private var connection: Connection = _
  private var statement: Statement = _
  private var scalaToSQLFn: T => String = _
  private var javaToSQLFunc: ToSQLFunction[T] = _
  private val batchSize = jdbc.remove(KEY_JDBC_INSERT_BATCH) match {
    case null => DEFAULT_JDBC_INSERT_BATCH
    case batch => batch.toString.toInt
  }

  def this(jdbc: Properties, toSQLFn: T => String, isolationLevel: Integer) {
    this(ApiType.Scala, jdbc, isolationLevel)
    this.scalaToSQLFn = toSQLFn
  }

  def this(jdbc: Properties, toSQLFn: ToSQLFunction[T], isolationLevel: Integer) {
    this(ApiType.JAVA, jdbc, isolationLevel)
    require(toSQLFn != null, "[StreamX] ToSQLFunction can not be null")
    this.javaToSQLFunc = toSQLFn
  }

  private val offset: AtomicLong = new AtomicLong(0L)
  private var timestamp: Long = 0L

  @throws[Exception]
  override def open(parameters: Configuration): Unit = {
    require(jdbc != null, "[StreamX] JdbcSink jdbc can not be null")
    logInfo("[StreamX] JdbcSink Open....")
    connection = JdbcUtils.getConnection(jdbc)
    connection.setAutoCommit(false)
    if (isolationLevel != null) {
      connection.setTransactionIsolation(isolationLevel)
    }
    if (batchSize > 1) {
      statement = connection.createStatement()
    }
  }

  override def invoke(value: T, context: SinkFunction.Context[_]): Unit = {
    require(connection != null)
    val sql = apiType match {
      case ApiType.Scala => scalaToSQLFn(value)
      case ApiType.JAVA => javaToSQLFunc.toSQL(value)
    }
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
          case _: Throwable =>
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
          case _: Throwable =>
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


class JdbcOutputFormat[T: TypeInformation](implicit prop: Properties, toSQlFun: T => String, isolationLevel: Integer = null) extends RichOutputFormat[T] with Logger {

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



