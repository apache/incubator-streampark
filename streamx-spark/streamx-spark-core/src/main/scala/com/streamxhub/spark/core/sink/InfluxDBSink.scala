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

package com.streamxhub.spark.core.sink

import java.util.Properties

import com.streamxhub.spark.core.util.HttpUtil
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.Time

import scala.collection.JavaConversions._
import scala.collection.Map
import scala.reflect.ClassTag

/**
  *
  *
  * 暂不支持checkpoint模式
  */
class InfluxDBSink[T: ClassTag](@transient override val sc: SparkContext,
                                initParams: Map[String, String] = Map.empty[String, String])
  extends Sink[T] {

  override val prefix: String = "spark.sink.influxDB."

  private lazy val prop = {
    val p = new Properties()
    p.putAll(param ++ initParams)
    p
  }

  private val host = prop.getProperty("host")
  private val port = prop.getProperty("port", "8086")
  private val db = prop.getProperty("db", "influx")

  def sink(rdd: RDD[T], time: Time = Time(System.currentTimeMillis())): Unit = {
    rdd.foreach(d => {
      val (postData, ip, pt, dbName) = d match {
        case t: String => (t, host, port, db)
        case (k: String, v: String) =>
          val info = k.split(",")
          info.size match {
            case 1 => (v, info(0), port, db)
            case 2 => (v, info(0), info(1), db)
            case 3 => (v, info(0), info(1), info(2))
            case _ => (v, host, port, db)
          }
        case (h: String, p: String, d: String, v: String) => (v, h, p, d)
        case _ => (d.toString, host, port, db)
      }
      val (code, res) = HttpUtil.httpPost(s"http://$ip:$pt/write?db=$dbName", postData)
      code match {
        case d if d >= 200 && d < 300 =>
          logger.info(s"Write influxDB successful. $code")
        case _ =>
          logger.warn(s"Write influxDB failed. code: $code, res: $res")
      }
    })
  }

}

object InfluxDBSink {
  def apply(sc: SparkContext) = new InfluxDBSink[String](sc)

  def mkInsertData(table: String, dimension: Map[String, String], values: Map[String, Any]) = {
    val ds = dimension.map(d => s"${d._1}=${d._2}").mkString(",")
    val vs = values.map(d => s"${d._1}=${d._2}").mkString(",")
    s"$table,$ds $vs"
  }
}
