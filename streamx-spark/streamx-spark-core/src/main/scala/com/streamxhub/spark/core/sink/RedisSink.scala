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

import com.streamxhub.spark.core.support.redis.{RedisClient, RedisEndpoint}
import com.streamxhub.spark.core.support.redis.RedisClient._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.Time
import redis.clients.jedis.Protocol

import scala.collection.JavaConversions._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

/**
  *
  *
  *
  */
class RedisSink[T <: scala.Product : ClassTag : TypeTag](@transient override val sc: SparkContext,
                                                         initParams: Map[String, String] = Map.empty[String, String])
  extends Sink[T] {

  override val prefix: String = "spark.sink.redis."

  private lazy val prop = {
    val p = new Properties()
    p.putAll(param ++ initParams)
    p
  }


  private lazy val host = prop.getProperty("host", Protocol.DEFAULT_HOST)
  private lazy val port = prop.getProperty("port", Protocol.DEFAULT_PORT.toString).toInt
  private lazy val auth = prop.getProperty("auth", null)
  private lazy val db = prop.getProperty("db", Protocol.DEFAULT_DATABASE.toString).toInt
  private lazy val timeout = prop.getProperty("timeout", Protocol.DEFAULT_TIMEOUT.toString).toInt


  private val redisEndpoint = RedisEndpoint(host, port, auth, db, timeout)

  def sink(rdd: RDD[T], time: Time = Time(System.currentTimeMillis())): Unit = {
    rdd.foreachPartition(r => {
      closePipe { pipe =>
        r.foreach(d => {
          val (k, v, t) = d match {
            case n: (String, Any) => (n._1, n._2.toString, 3600 * 24 * 7)
            case n: (String, Any, Int) => (n._1, n._2.toString, n._3)
            case _ =>
              logger.warn("data type error. key is unknown")
              ("UNKNOWN", d.toString, 60)
          }
          pipe.set(k, v)
          pipe.expire(k, t)
        })
      }(RedisClient.connect(redisEndpoint))
    })
  }

  def close(): Unit = RedisClient.close()

}

object RedisSink {
  def apply(sc: SparkContext) = new RedisSink[(String, String)](sc)

  def apply[T <: scala.Product : ClassTag : TypeTag](rdd: RDD[T]) = new RedisSink[T](rdd.sparkContext)
}

