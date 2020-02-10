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

package com.streamxhub.common.util

import java.util.concurrent.ConcurrentHashMap

import redis.clients.jedis.Jedis

import redis.clients.jedis.exceptions.JedisConnectionException
import redis.clients.jedis._
import scala.annotation.meta.getter
import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

object RedisClient extends Logger {

  @transient
  @getter
  private lazy val pools: ConcurrentHashMap[RedisEndpoint, JedisPool] =
    new ConcurrentHashMap[RedisEndpoint, JedisPool]()

  @transient
  @getter
  private lazy val clusters: ConcurrentHashMap[RedisEndpoint, JedisCluster] = new ConcurrentHashMap[RedisEndpoint, JedisCluster]()

  /**
   * 随机选择一个 RedisEndpoint 创建 或者获取一个Redis 连接池
   *
   * @param res
   * @return
   */
  def connect(res: Array[RedisEndpoint]): Jedis = {
    assert(res.length > 0, "The RedisEndpoint array is empty!!!")
    val rnd = scala.util.Random.nextInt().abs % res.length
    try {
      connect(res(rnd))
    } catch {
      case e: Exception => logger.error(e.getMessage)
        connect(res.drop(rnd))
    }
  }

  /**
   * 创建或者获取一个Redis 连接池
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
        case e: JedisConnectionException if e.getCause.toString.
          contains("ERR max number of clients reached") => {
          if (sleepTime < 500) sleepTime *= 2
          Thread.sleep(sleepTime)
        }
        case e: Exception => throw e
      }
    }
    conn
  }

  /**
   * 创建一个连接池
   *
   * @param re
   * @return
   */
  def createJedisPool(re: RedisEndpoint): JedisPool = {
    println(s"createJedisPool with $re ")
    new JedisPool(poolConfig, re.host, re.port, re.timeout, re.auth, re.db)
  }

  private lazy val poolConfig = {

    val poolConfig: JedisPoolConfig = new JedisPoolConfig()
    /*最大连接数*/
    poolConfig.setMaxTotal(1000)
    /*最大空闲连接数*/
    poolConfig.setMaxIdle(64)
    /*在获取连接的时候检查有效性, 默认false*/
    poolConfig.setTestOnBorrow(true)
    poolConfig.setTestOnReturn(false)
    /*在空闲时检查有效性, 默认false*/
    poolConfig.setTestWhileIdle(false)
    /*逐出连接的最小空闲时间 默认1800000毫秒(30分钟)*/
    poolConfig.setMinEvictableIdleTimeMillis(1800000)
    /*逐出扫描的时间间隔(毫秒) 如果为负数,则不运行逐出线程, 默认-1*/
    poolConfig.setTimeBetweenEvictionRunsMillis(30000)
    poolConfig.setNumTestsPerEvictionRun(-1)
    poolConfig
  }

  def connectCluster(res: RedisEndpoint*): JedisCluster = {

    assert(res.nonEmpty, "The RedisEndpoint array is empty!!!")

    val head = res.head

    val cluster = clusters.getOrElseUpdate(head, {
      val haps = res.map(r => new HostAndPort(r.host, r.port)).toSet
      new JedisCluster(haps, head.timeout, 1000, 1, head.auth, poolConfig)
    })

    cluster
  }


  def doRedis[R](f: Jedis => R)(implicit redis: Jedis): R = {
    val result = f(redis)
    Try {
      redis.close()
    } match {
      case Success(_) => logger.debug("jedis.close successful.")
      case Failure(e) => logger.error(s"jedis.close failed.error:${e.getLocalizedMessage}")
    }
    result
  }

  def doCluster[R](f: JedisCluster => R)(implicit cluster: JedisCluster): R = {
    val result = f(cluster)
    Try {
      cluster.close()
    } match {
      case Success(o) => logger.debug("jedis.close successful.")
      case Failure(o) => logger.error("jedis.close failed.")
    }
    result
  }

  def doPipe[R](f: Pipeline => R)(implicit jedis: Jedis): R = {
    val pipe = jedis.pipelined()
    val result = f(pipe)
    Try {
      pipe.sync()
      pipe.close()
      jedis.close()
    } match {
      case Success(o) => logger.debug("pipe.close successful.")
      case Failure(o) => logger.error("pipe.close failed.")
    }
    result
  }

  def close(): Unit = pools.foreach { case (_, v) => v.close() }


}
