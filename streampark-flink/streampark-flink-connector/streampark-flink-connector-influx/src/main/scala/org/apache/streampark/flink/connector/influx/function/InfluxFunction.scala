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

package org.apache.streampark.flink.connector.influx.function

import org.apache.streampark.common.conf.ConfigKeys.{KEY_JDBC_PASSWORD, KEY_JDBC_URL, KEY_JDBC_USER}
import org.apache.streampark.common.enums.ApiType
import org.apache.streampark.common.util.Logger
import org.apache.streampark.flink.connector.influx.bean.InfluxEntity

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction
import org.apache.flink.streaming.api.functions.sink.SinkFunction.Context
import org.influxdb.{InfluxDB, InfluxDBFactory}
import org.influxdb.dto.Point

import java.util.Properties
import java.util.concurrent.TimeUnit

import org.apache.streampark.common.util.Implicits._;

class InfluxFunction[T](config: Properties)(implicit endpoint: InfluxEntity[T])
  extends RichSinkFunction[T]
  with Logger {

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

  override def invoke(value: T, context: Context): Unit = {
    val tag: JavaMap[String, String] = endpoint.apiType match {
      case ApiType.java => endpoint.javaTagFun.transform(value)
      case ApiType.scala => endpoint.scalaTagFun(value)
    }
    val fields: JavaMap[String, Object] = endpoint.apiType match {
      case ApiType.java => endpoint.javaFieldFun.transform(value)
      case ApiType.scala => endpoint.scalaFieldFun(value)
    }
    val point = Point
      .measurement(endpoint.measurement)
      .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
      .tag(tag)
      .fields(fields)
      .build()
    influxDB.write(endpoint.database, endpoint.retentionPolicy, point)
  }

  override def close(): Unit = if (influxDB != null) {
    influxDB.flush()
    influxDB.close()
  }

}
