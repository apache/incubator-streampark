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

package com.streamxhub.streamx.flink.connector.clickhouse.internal

import com.streamxhub.streamx.common.enums.ApiType
import com.streamxhub.streamx.common.enums.ApiType.ApiType
import com.streamxhub.streamx.common.util.{JdbcUtils, Logger}
import com.streamxhub.streamx.flink.connector.clickhouse.conf.ClickHouseJdbcConfig
import com.streamxhub.streamx.flink.connector.clickhouse.util.ClickhouseConvertUtils.convert
import com.streamxhub.streamx.flink.connector.function.TransformFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import ru.yandex.clickhouse.ClickHouseDataSource
import ru.yandex.clickhouse.settings.ClickHouseProperties

import java.sql.{Connection, Statement}
import java.util
import java.util.Properties
import java.util.concurrent.atomic.AtomicLong
import scala.collection.JavaConversions._
import scala.util.Try

class ClickHouseSinkFunction[T](apiType: ApiType = ApiType.scala, config: Properties) extends RichSinkFunction[T] with Logger {
  private var connection: Connection = _
  private var statement: Statement = _
  var clickHouseConf: ClickHouseJdbcConfig = new ClickHouseJdbcConfig(config)

  private val batchSize = clickHouseConf.batchSize
  private val offset: AtomicLong = new AtomicLong(0L)
  private var timestamp = 0L
  private val delayTime = clickHouseConf.batchDelayTime
  private val sqlValues = new util.ArrayList[String](batchSize)
  private var insertSqlPrefixes: String = _

  private[this] var scalaSqlFunc: T => String = _
  private[this] var javaSqlFunc: TransformFunction[T, String] = _


  //for Scala
  def this(properties: Properties,
           scalaSqlFunc: T => String) = {

    this(ApiType.scala, properties)
    this.scalaSqlFunc = scalaSqlFunc
  }

  //for JAVA
  def this(properties: Properties,
           javaSqlFunc: TransformFunction[T, String]) = {

    this(ApiType.java, properties)
    this.javaSqlFunc = javaSqlFunc
  }


  override def open(parameters: Configuration): Unit = {
    val user: String = clickHouseConf.user
    val driver: String = clickHouseConf.driverClassName
    val targetTable = clickHouseConf.table
    require(targetTable != null && targetTable.nonEmpty, () => s"ClickHouseSinkFunction insert targetTable must not null")
    insertSqlPrefixes = s"insert into  $targetTable  values "
    val properties = new ClickHouseProperties()
    (user, driver) match {
      case (u, d) if (u != null && d != null) =>
        Class.forName(d)
        properties.setUser(u)
      case (null, null) =>
      case (_, d) if d != null => Class.forName(d)
      case _ => properties.setUser(user)
    }
    //reflect set all properties...
    clickHouseConf.sinkOption.getInternalConfig().foreach(x => {
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
    val dataSource = new ClickHouseDataSource(clickHouseConf.jdbcUrl, properties)
    connection = dataSource.getConnection
  }

  override def invoke(value: T, context: SinkFunction.Context): Unit = {
    require(connection != null)

    val valueStr = (scalaSqlFunc, javaSqlFunc) match {
      case (null, null) => convert(value)
      case _ => apiType match {
        case ApiType.java => javaSqlFunc.transform(value)
        case ApiType.scala => scalaSqlFunc(value)
      }
    }
    batchSize match {
      case 1 =>
        try {
          val sql = s"$insertSqlPrefixes $valueStr"
          connection.prepareStatement(sql).executeUpdate
        } catch {
          case e: Exception =>
            logError(s"""ClickHouseSink invoke error:$valueStr""")
            throw e
          case _: Throwable =>
        }
      case batch =>
        try {
          sqlValues.add(valueStr)
          (offset.incrementAndGet() % batch, System.currentTimeMillis()) match {
            case (0, _) => execBatch()
            case (_, current) if current - timestamp > delayTime => execBatch()
            case _ =>
          }
        } catch {
          case e: Exception =>
            logError(s"""ClickHouseSink batch invoke error:$sqlValues""")
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
      try {
        logInfo(s"ClickHouseSink batch ${offset.get()} insert begin..")
        offset.set(0)
        val valuesStr: String = sqlValues.mkString(",")
        val sql = s"$insertSqlPrefixes $valuesStr"
        //clickhouse batch insert  return num always 1
        val insertNum: Int = connection.prepareStatement(sql).executeUpdate()
        logInfo(s"ClickHouseSink batch  successful..")
        timestamp = System.currentTimeMillis()
      } finally {
        sqlValues.clear()
      }
    }
  }

}
