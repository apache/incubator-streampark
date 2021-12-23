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
import com.streamxhub.streamx.common.util.{ConfigUtils, Logger, Utils}
import com.streamxhub.streamx.flink.core.scala.StreamingContext
import org.apache.flink.api.common.io.RichOutputFormat
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction
import org.apache.flink.streaming.api.scala.DataStream
import org.influxdb.dto.Point
import org.influxdb.{InfluxDB, InfluxDBFactory}

import java.util.Properties
import java.util.concurrent.TimeUnit
import scala.annotation.meta.param
import scala.collection.JavaConversions._
import scala.collection.Map

object InfluxDBSink {

  def apply(@(transient@param)
            property: Properties = new Properties(),
            parallelism: Int = 0,
            name: String = null,
            uid: String = null)(implicit ctx: StreamingContext): InfluxDBSink = new InfluxDBSink(ctx, property, parallelism, name, uid)

}

class InfluxDBSink(@(transient@param) ctx: StreamingContext,
                   property: Properties = new Properties(),
                   parallelism: Int = 0,
                   name: String = null,
                   uid: String = null) extends Sink {

  def sink[T](stream: DataStream[T], alias: String = "")(implicit entity: InfluxEntity[T]): DataStreamSink[T] = {
    val prop = ConfigUtils.getInfluxConfig(ctx.parameter.toMap)(alias)
    Utils.copyProperties(property, prop)
    val sinkFun = new InfluxDBFunction[T](prop)
    val sink = stream.addSink(sinkFun)
    afterSink(sink, parallelism, name, uid)
  }

}

class InfluxDBFunction[T](config: Properties)(implicit endpoint: InfluxEntity[T]) extends RichSinkFunction[T] with Logger {

  var influxDB: InfluxDB = _

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    val url = config.getOrElse(KEY_JDBC_URL, null)
    require(url != null)
    val username = config.getOrElse(KEY_JDBC_USER, null)
    val password = config.getOrElse(KEY_JDBC_PASSWORD, null)
    influxDB = (username, password, url) match {
      case (null, _, u) => InfluxDBFactory.connect(u)
      case _ => InfluxDBFactory.connect(url, username, password)
    }
    influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS)
  }

  override def invoke(value: T): Unit = {
    val point = Point.measurement(endpoint.measurement)
      .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
      .tag(endpoint.tagFun(value))
      .fields(endpoint.fieldFun(value).asInstanceOf[Map[String, Object]])
      .build()
    influxDB.write(endpoint.database, endpoint.retentionPolicy, point)
  }

  override def close(): Unit = if (influxDB != null) {
    influxDB.flush()
    influxDB.close()
  }

}

class InfluxDBOutputFormat[T: TypeInformation](implicit prop: Properties, endpoint: InfluxEntity[T]) extends RichOutputFormat[T] with Logger {

  private val sinkFunction = new InfluxDBFunction[T](prop)

  private var configuration: Configuration = _

  override def configure(configuration: Configuration): Unit = this.configuration = configuration

  override def open(taskNumber: Int, numTasks: Int): Unit = sinkFunction.open(this.configuration)

  override def writeRecord(record: T): Unit = sinkFunction.invoke(record, null)

  override def close(): Unit = sinkFunction.close()
}


/**
 *
 * @param database
 * @param measurement
 * @param retentionPolicy
 * @param tagFun
 * @param fieldFun
 * @tparam T
 */
case class InfluxEntity[T](database: String, //指定database
                           measurement: String, //指定measurement
                           retentionPolicy: String, //失效策略
                           tagFun: T => Map[String, String], //tags 函数
                           fieldFun: T => Map[String, Any] //field 函数
                          ) {
}
