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

package org.apache.streampark.spark.connector.sink

import org.apache.streampark.common.util.Logger

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.Time
import org.apache.spark.streaming.dstream.DStream

import java.util.Properties

import scala.annotation.meta.getter
import scala.collection.Map
import scala.util.Try

/** Base output trait */
trait Sink[T] extends Serializable with Logger {

  @(transient @getter)
  val sc: SparkContext
  @(transient @getter)
  lazy val sparkConf = sc.getConf

  val prefix: String

  lazy val param: Map[String, String] = sparkConf.getAll.flatMap {
    case (k, v) if k.startsWith(prefix) && Try(v.nonEmpty).getOrElse(false) =>
      Some(k.substring(prefix.length) -> v)
    case _ => None
  }.toMap

  def filterProp(
      param: Map[String, String],
      overrides: Map[String, String],
      prefix: String = "",
      replacement: String = ""): Properties = {
    val p = new Properties()
    val map = param ++ overrides
    val filtered =
      if (prefix.isEmpty) map else map.filter(_._1.startsWith(prefix))
    filtered.foreach(x => p.put(x._1.replace(prefix, replacement), x._2))
    p
  }

  /**
   * sink
   *
   * @param dStream
   *   dStream
   */
  def sink(dStream: DStream[T]): Unit = {
    dStream.foreachRDD((rdd, time) => sink(rdd, time))
  }

  /**
   * sink
   *
   * @param rdd
   *   spark.RDD
   * @param time
   *   spark.streaming.Time
   */
  def sink(rdd: RDD[T], time: Time): Unit
}
