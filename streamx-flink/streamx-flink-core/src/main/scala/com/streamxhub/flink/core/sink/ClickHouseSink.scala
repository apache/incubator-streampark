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

import java.sql._
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

import com.streamxhub.common.util.{ConfigUtils, Logger, MySQLUtils}
import com.streamxhub.flink.core.StreamingContext
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala.DataStream
import ru.yandex.clickhouse.ClickHouseDataSource
import ru.yandex.clickhouse.settings.ClickHouseProperties
import com.streamxhub.common.conf.ConfigConst._

import scala.collection.JavaConversions._
import scala.collection.Map

object ClickHouseSink {

  /**
   * @param ctx      : StreamingContext
   * @param instance : ClickHouse实例名称(用于区分多个不同的ClickHouse实例...)
   * @return
   */
  def apply(@transient ctx: StreamingContext,
            overwriteParams: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit instance: String = ""): ClickHouseSink = new ClickHouseSink(ctx, overwriteParams, parallelism, name, uid)

}

class ClickHouseSink(@transient ctx: StreamingContext,
                     overwriteParams: Map[String, String] = Map.empty[String, String],
                     parallelism: Int = 0,
                     name: String = null,
                     uid: String = null)(implicit instance: String = "") extends Sink with Logger {


  def sink[T](stream: DataStream[T])(implicit toSQLFn: T => String): DataStreamSink[T] = {
    val prop = ConfigUtils.getConf(ctx.paramMap, CLICKHOUSE_PREFIX)(instance)
    overwriteParams.foreach(x => prop.put(x._1, x._2))
    val sinkFun = new ClickHouseSinkFunction[T](prop, toSQLFn)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

}

class ClickHouseSinkFunction[T](config: Properties, toSQLFn: T => String) extends RichSinkFunction[T] with Logger {

  var connection: Connection = _
  var preparedStatement: PreparedStatement = _
  val dataSourcePool = new ConcurrentHashMap[String, ClickHouseDataSource]()

  // 继承open方法
  override def open(parameters: Configuration): Unit = {
    val instance = config(KEY_INSTANCE)
    val dataSource = dataSourcePool.getOrElseUpdate(instance, {
      val database: String = config(KEY_JDBC_DATABASE)
      val user: String = config.getOrElse(KEY_JDBC_USER, null)
      val password: String = config.getOrElse(KEY_JDBC_PASSWORD, null)
      val url: String = config(KEY_JDBC_URL)
      val properties = new ClickHouseProperties()
      (Option(user), Option(password)) match {
        case (Some(u), Some(p)) =>
          properties.setUser(u)
          properties.setPassword(p)
        case (None, None) =>
        case _ => throw new IllegalArgumentException("[StreamX] ClickHouse user|password muse be all not null or all null")
      }
      properties.setDatabase(database)
      properties.setSocketTimeout(50000)
      new ClickHouseDataSource(url, properties)
    })
    connection = dataSource.getConnection
  }

  override def invoke(value: T, context: SinkFunction.Context[_]): Unit = {
    require(connection != null)
    val sql = toSQLFn(value)
    preparedStatement = connection.prepareStatement(sql)
    try {
      preparedStatement.executeUpdate
      connection.commit()
    } catch {
      case e: Exception =>
        logError(s"[StreamX] JdbcSink invoke error:${sql}")
        throw e
    }
  }

  override def close(): Unit = MySQLUtils.close(preparedStatement, connection)

}
