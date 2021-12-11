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

package com.streamxhub.streamx.spark.core.sink

import com.streamxhub.streamx.common.util.{Logger, RedisClient, RedisEndpoint, RedisUtils}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.Time
import redis.clients.jedis.Protocol

import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

/**
 *
 *
 *
 */
class RedisSink[T <: scala.Product : ClassTag : TypeTag](@transient override val sc: SparkContext,
                                                         initParams: Map[String, String] = Map.empty[String, String])
  extends Sink[T] with Logger {

  override val prefix: String = "spark.sink.redis."

  private[this] lazy implicit val redisEndpoint = {
    val prop = filterProp(param, initParams, prefix)
    val host = prop.getProperty("host", Protocol.DEFAULT_HOST)
    val port = prop.getProperty("port", Protocol.DEFAULT_PORT.toString).toInt
    val auth = prop.getProperty("auth", null)
    val db = prop.getProperty("db", Protocol.DEFAULT_DATABASE.toString).toInt
    val timeout = prop.getProperty("timeout", Protocol.DEFAULT_TIMEOUT.toString).toInt
    RedisEndpoint(host, port, auth, db, timeout)
  }

  def sink(rdd: RDD[T], time: Time = Time(System.currentTimeMillis())): Unit = {
    rdd.foreachPartition(r => {
      RedisUtils.doPipeline(pipe => {
        r.foreach(d => {
          val (k, v, t) = d match {
            case n: (String, Any) => (n._1, n._2.toString, 3600 * 24 * 7)
            case n: (String, Any, Int) => (n._1, n._2.toString, n._3)
            case _ =>
              logWarn("data type error. key is unknown")
              ("UNKNOWN", d.toString, 60)
          }
          pipe.set(k, v)
          pipe.expire(k, t)
        })
      })
    })
  }

  def close(): Unit = RedisClient.close()

}

object RedisSink {
  def apply(sc: SparkContext): RedisSink[(String, String)] = new RedisSink[(String, String)](sc)

  def apply[T <: scala.Product : ClassTag : TypeTag](rdd: RDD[T]): RedisSink[T] = new RedisSink[T](rdd.sparkContext)
}

