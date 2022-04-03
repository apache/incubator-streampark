/*
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
package com.streamxhub.streamx.test.flink.scala.datastream

import com.streamxhub.streamx.flink.connector.influxdb.domian.InfluxEntity
import com.streamxhub.streamx.flink.connector.influxdb.sink.InfluxDBSink
import com.streamxhub.streamx.flink.core.scala.FlinkStreaming
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala._

import scala.util.Random

/**
 * 侧输出流
 */
object InfluxDBSinkApp extends FlinkStreaming {


  override def handle(): Unit = {
    val source = context.addSource(new WeatherSource())

    //weather,altitude=1000,area=北 temperature=11,humidity=-4

    InfluxDBSink().sink(source, "mydb")(InfluxEntity[Weather](
      "mydb",
      "test",
      "autogen",
      x => Map("altitude" -> x.altitude.toString, "area" -> x.area.toString),
      x => Map("temperature" -> x.temperature, "humidity" -> x.humidity)))
  }

}

/**
 *
 * 温度 temperature
 * 湿度 humidity
 * 地区 area
 * 海拔 altitude
 */
case class Weather(temperature: Long,
                   humidity: Long,
                   area: String,
                   altitude: Long)

class WeatherSource extends SourceFunction[Weather] {

  private[this] var isRunning = true

  override def cancel(): Unit = this.isRunning = false

  val random = new Random()

  override def run(ctx: SourceFunction.SourceContext[Weather]): Unit = {
    while (isRunning) {
      val temperature = random.nextInt(100)
      val humidity = random.nextInt(30)
      val area = List("北", "上", "广", "深")(random.nextInt(4))
      val altitude = random.nextInt(10000)
      val order = Weather(temperature, humidity, area, altitude)
      ctx.collect(order)
    }
  }

}

