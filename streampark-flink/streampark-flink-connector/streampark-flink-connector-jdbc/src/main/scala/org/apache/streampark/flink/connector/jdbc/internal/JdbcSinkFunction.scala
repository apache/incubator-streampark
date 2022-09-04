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

package org.apache.streampark.flink.connector.jdbc.internal

import org.apache.streampark.common.conf.ConfigConst.{DEFAULT_JDBC_INSERT_BATCH, KEY_JDBC_INSERT_BATCH}
import org.apache.streampark.common.enums.ApiType
import org.apache.streampark.common.enums.ApiType.ApiType
import org.apache.streampark.common.util.{JdbcUtils, Logger}
import org.apache.streampark.flink.connector.function.TransformFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}

import java.sql.{Connection, PreparedStatement, Statement}
import java.util.Properties
import java.util.concurrent.atomic.AtomicLong

class JdbcSinkFunction[T](apiType: ApiType = ApiType.scala, jdbc: Properties) extends RichSinkFunction[T] with Logger {
  private var connection: Connection = _
  private var statement: Statement = _
  private var scalaToSQLFn: T => String = _
  private var javaToSQLFunc: TransformFunction[T, String] = _
  private val offset: AtomicLong = new AtomicLong(0L)
  private var timestamp: Long = 0L

  private val batchSize = jdbc.remove(KEY_JDBC_INSERT_BATCH) match {
    case null => DEFAULT_JDBC_INSERT_BATCH
    case batch => batch.toString.toInt
  }

  def this(jdbc: Properties, toSQLFn: T => String) {
    this(ApiType.scala, jdbc)
    this.scalaToSQLFn = toSQLFn
  }

  def this(jdbc: Properties, toSQLFn: TransformFunction[T, String]) {
    this(ApiType.java, jdbc)
    require(toSQLFn != null, "[StreamPark] ToSQLFunction can not be null")
    this.javaToSQLFunc = toSQLFn
  }

  @throws[Exception]
  override def open(parameters: Configuration): Unit = {
    require(jdbc != null, "[StreamPark] JdbcSink jdbc can not be null")
    logInfo("JdbcSink Open....")
    connection = JdbcUtils.getConnection(jdbc)
    connection.setAutoCommit(false)
    if (batchSize > 1) {
      statement = connection.createStatement()
    }
  }

  override def invoke(value: T, context: SinkFunction.Context): Unit = {
    require(connection != null)
    val sql = apiType match {
      case ApiType.scala => scalaToSQLFn(value)
      case ApiType.java => javaToSQLFunc.transform(value)
    }
    batchSize match {
      case 1 =>
        try {
          statement = connection.prepareStatement(sql)
          statement.asInstanceOf[PreparedStatement].executeUpdate
          connection.commit()
        } catch {
          case e: Exception =>
            logError(s"JdbcSink invoke error:${sql}")
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
            logError(s"JdbcSink batch invoke error:${sql}")
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
      logInfo(s"JdbcSink batch $count use ${System.currentTimeMillis() - start} MS")
      timestamp = System.currentTimeMillis()
    }
  }

}
