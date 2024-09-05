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

package org.apache.streampark.common.util

import org.apache.streampark.common.constants.Constants
import org.apache.streampark.common.util.Implicits._

import redis.clients.jedis._
import redis.clients.jedis.exceptions.JedisConnectionException

import java.util.concurrent.ConcurrentHashMap

import scala.annotation.meta.getter
import scala.annotation.tailrec
import scala.util.Random

object RedisClient extends Logger {

  @transient
  @getter
  private lazy val pools: ConcurrentHashMap[RedisEndpoint, JedisPool] =
    new ConcurrentHashMap[RedisEndpoint, JedisPool]()

  @transient
  @getter
  private lazy val clusters: ConcurrentHashMap[RedisEndpoint, JedisCluster] =
    new ConcurrentHashMap[RedisEndpoint, JedisCluster]()

  /**
   * Select a random RedisEndpoint to create or get a Redis connection pool
   *
   * @param res
   * @return
   */
  @tailrec
  def connect(endpoints: Array[RedisEndpoint]): Jedis = {
    require(endpoints.length > 0, "[StreamPark] The RedisEndpoint array is empty!!!")
    val index = Random.nextInt().abs % endpoints.length
    try {
      connect(endpoints(index))
    } catch {
      case e: Exception =>
        logger.error(e.getMessage)
        connect(endpoints.drop(index))
    }
  }

  /**
   * Create or get a Redis connection pool
   *
   * @param re
   * @return
   */
  def connect(re: RedisEndpoint): Jedis = {
    val pool = pools.getOrElseUpdate(re, createJedisPool(re))
    var sleepTime: Int = 4
    var conn: Jedis = null
    while (conn == null) {
      try {
        conn = pool.getResource
      } catch {
        case e: JedisConnectionException
            if e.getCause.toString.contains("ERR max number of clients reached") => {
          if (sleepTime < 500) sleepTime *= 2
          Thread.sleep(sleepTime)
        }
        case e: Exception => throw e
      }
    }
    conn
  }

  /**
   * Create a connection pool
   *
   * @param endpoint
   * @return
   */
  def createJedisPool(endpoint: RedisEndpoint): JedisPool = {
    val endpointEn: RedisEndpoint = endpoint.copy(auth = Constants.DEFAULT_DATAMASK_STRING)
    logInfo(s"[StreamPark] RedisClient: createJedisPool with $endpointEn ")
    new JedisPool(
      poolConfig,
      endpoint.host,
      endpoint.port,
      endpoint.timeout,
      endpoint.auth,
      endpoint.db)
  }

  private lazy val poolConfig = {
    val poolConfig: JedisPoolConfig = new JedisPoolConfig()
    // maximum number of connections
    poolConfig.setMaxTotal(1000)
    // maximum number of connections
    poolConfig.setMaxIdle(64)
    // check validity when getting connection, default false
    poolConfig.setTestOnBorrow(true)
    poolConfig.setTestOnReturn(false)
    // check validity at idle, default false
    poolConfig.setTestWhileIdle(false)
    // minimum idle time for eviction connections, default 1800000 milliseconds (30 minutes)
    poolConfig.setMinEvictableIdleTimeMillis(1800000)
    // evicting scan interval (ms), default -1, if negative, do not run the eviction thread
    poolConfig.setTimeBetweenEvictionRunsMillis(30000)
    poolConfig.setNumTestsPerEvictionRun(-1)
    poolConfig
  }

  def connectCluster(res: RedisEndpoint*): JedisCluster = {
    require(res.nonEmpty, "[StreamPark] The RedisEndpoint array is empty!!!")
    val head = res.head
    val cluster = clusters.getOrElseUpdate(
      head, {
        val hostPorts = res.map(r => new HostAndPort(r.host, r.port)).toSet
        new JedisCluster(hostPorts, head.timeout, 1000, 1, head.auth, poolConfig)
      })
    cluster
  }

  def close(): Unit = pools.foreach { case (_, v) => v.close() }
}
