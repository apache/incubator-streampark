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

import com.streamxhub.common.util.{ConfigUtils, Logger, MySQLUtils}
import com.streamxhub.flink.core.StreamingContext
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala.DataStream
import ru.yandex.clickhouse.ClickHouseDataSource
import ru.yandex.clickhouse.settings.ClickHouseProperties
import com.streamxhub.common.conf.ConfigConst._
import org.apache.flink.api.common.io.RichOutputFormat
import org.apache.flink.api.common.typeinfo.TypeInformation

import scala.collection.JavaConversions._
import scala.collection.Map
import scala.util.Try

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
  var preparedStatement: Statement = _
  val batchSize: Int = config.getOrElse(KEY_JDBC_INSERT_BATCH, s"${DEFAULT_JDBC_INSERT_BATCH}").toInt
  var index = 0

  override def open(parameters: Configuration): Unit = {
    val url: String = Try(config.remove(KEY_JDBC_URL).toString).getOrElse(null)
    val user: String = Try(config.remove(KEY_JDBC_USER).toString).getOrElse(null)
    val driver: String = Try(config.remove(KEY_JDBC_DRIVER).toString).getOrElse(null)

    val properties = new ClickHouseProperties()
    (user, driver) match {
      case (u,d) if(u!=null && d !=null) =>
        Class.forName(d)
        properties.setUser(u)
      case (null,null) =>
      case (_, d) if d != null => Class.forName(d)
      case _ => properties.setUser(user)
    }
    //reflect set all properties...
    config.foreach(x => {
      val field = Try(Option(properties.getClass.getDeclaredField(x._1))).getOrElse(None) match {
        case None =>
          val boolField = s"is${x._1.substring(0, 1).toUpperCase}${x._1.substring(1)}"
          Try(Option(properties.getClass.getDeclaredField(boolField))).getOrElse(None) match {
            case Some(x) => x
            case None => throw new IllegalArgumentException(s"ClickHouseProperties config error,property:${x._1} invalid,please see ru.yandex.clickhouse.settings.ClickHouseProperties")
          }
        case Some(x) => x
      }
      field.setAccessible(true)
      field.getType.getSimpleName match {
        case "String" => field.set(properties, x._2)
        case "int" | "Integer" => field.set(properties, x._2.toInt)
        case "long" | "Long" => field.set(properties, x._2.toLong)
        case "boolean" | "Boolean" => field.set(properties, x._2.toBoolean)
        case _ =>
      }
    })
    val dataSource = new ClickHouseDataSource(url, properties)
    connection = dataSource.getConnection
  }

  override def invoke(value: T, context: SinkFunction.Context[_]): Unit = {
    require(connection != null)
    val sql = toSQLFn(value)
    batchSize match {
      case 1 =>
        try {
          preparedStatement = connection.prepareStatement(sql)
          preparedStatement.asInstanceOf[PreparedStatement].executeUpdate
        } catch {
          case e: Exception =>
            logError(s"[StreamX] ClickHouseSink invoke error:${sql}")
            throw e
          case _ =>
        }
      case batch =>
        try {
          preparedStatement = connection.createStatement()
          preparedStatement.addBatch(sql)
          if (index > 0 && index % batch == 0) {
            preparedStatement.executeBatch().sum
            preparedStatement.clearBatch()
            index = 0
          }
        } catch {
          case e: Exception =>
            logError(s"[StreamX] ClickHouseSink batch invoke error:${sql}")
            throw e
          case _ =>
        }

    }
  }

  override def close(): Unit = MySQLUtils.close(preparedStatement, connection)

}


class ClickHouseOutputFormat[T: TypeInformation](implicit prop: Properties, toSQlFun: T => String) extends RichOutputFormat[T] with Logger {

  val sinkFunction = new ClickHouseSinkFunction[T](prop, toSQlFun)

  var configuration: Configuration = _

  override def configure(configuration: Configuration): Unit = this.configuration = configuration

  override def open(taskNumber: Int, numTasks: Int): Unit = sinkFunction.open(this.configuration)

  override def writeRecord(record: T): Unit = sinkFunction.invoke(record, null)

  override def close(): Unit = sinkFunction.close()
}
