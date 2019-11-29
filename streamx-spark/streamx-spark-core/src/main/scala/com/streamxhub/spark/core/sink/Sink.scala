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

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.Time
import org.slf4j.LoggerFactory

import scala.annotation.meta.getter
import scala.util.Try

/**
  *
  */
trait Sink[T] extends Serializable {

  lazy val logger = LoggerFactory.getLogger(getClass)

  @(transient@getter)
  val sc: SparkContext
  @(transient@getter)
  lazy val sparkConf = sc.getConf

  val prefix: String

  lazy val param: Map[String, String] = sparkConf.getAll.flatMap {
    case (k, v) if k.startsWith(prefix) && Try(v.nonEmpty).getOrElse(false) => Some(k.substring(prefix.length) -> v)
    case _ => None
  } toMap

  /**
    * 输出
    *
    */
  def sink(dStream: DStream[T]): Unit = {
    dStream.foreachRDD((rdd, time) => sink(rdd, time))
  }

  /**
    * 输出
    *
    * @param rdd  spark.RDD
    * @param time spark.streaming.Time
    */
  def sink(rdd: RDD[T], time: Time)
}
