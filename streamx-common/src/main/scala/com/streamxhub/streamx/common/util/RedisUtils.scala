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
package com.streamxhub.streamx.common.util

import redis.clients.jedis.{Jedis, JedisCluster, Pipeline, ScanParams}

import java.lang.{Integer => JInt}
import java.util.Set
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.util.{Failure, Success, Try}

/**
 *
 * <pre>
 *
 * <<出征>>
 *
 * 让我扭过头决绝地走
 * 擦干泪水松开了母亲的手
 * 敌忾同仇神州在紧要关头
 * 一声大吼 同志们全体都有
 *
 * 请接受我的请命
 * 就为了咱的百姓
 * 就为我姊妹兄弟
 * 请让我按下手印
 * 就为这天下太平
 * 就为这多少生命
 * 为这医者仁心
 * 就为我中华大地
 *
 * 他敬礼风中飘的旗
 * 那冲锋号也吹的急
 * 我连夜向疫区行进
 * 你誓要灭掉这场病
 * 多少的英雄在集结
 * 就让我为你冲在前
 * 所有的心都紧相连
 * 就会让我们更团结
 *
 * 怒发冲冠凭阑处
 * 潇潇雨歇出征夜
 * 抬望眼仰天长啸
 * 壮怀激烈我魂不灭
 * 三十功名尘与土
 * 八千里路云和月
 * 长江水不可断绝
 * 卫我同胞万万千
 * 岂曰无衣 与子同袍
 * 与子同泽 与子同裳
 *
 * 苟利国家生死以
 * 岂因祸福避趋之
 * 身袭白衣问自己
 * 是否能悬壶济世
 * 我念着先辈的意志
 * 救死扶伤的义士
 * 留取丹心照汗青
 * 那是我民族的气质
 *
 * 一定要阻断 这苦难 就顶住 武汉
 * 让我为请战 的同伴 再擦干 血汗
 * 拯救那病患
 * 内心充满胜算
 * 就算惊涛拍岸
 * 且看我如何应战
 *
 * 看这一次
 * 中华儿女决不会低下头
 * 活这一世
 * 我与你共患难风雨同舟
 * 看这一次
 * 在出发之前唱起满江红
 * 看这一次
 * 无数的双手撑起黄鹤楼
 * </pre>
 *
 * @author benjobs
 */
object RedisUtils extends Logger {

  /**
   * exists
   *
   * @param key
   * @return
   */
  def exists(key: String)(implicit endpoint: RedisEndpoint): Boolean = doRedis(_.exists(key))

  def hexists(key: String, field: String, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Boolean = doRedis(_.hexists(key, field), func)

  /**
   * get
   *
   * @param key
   * @return
   */
  def get(key: String)(implicit endpoint: RedisEndpoint): String = doRedis(_.get(key))

  def hget(key: String, field: String)(implicit endpoint: RedisEndpoint): String = doRedis(_.hget(key, field))

  /**
   * Redis Setex 命令为指定的 key 设置值及其过期时间。如果 key 已经存在， SETEX 命令将会替换旧的值。
   *
   * @param key
   * @return
   */
  def setex(key: String, value: String, ttl: JInt = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): String = doRedis(_.setex(key, ttl, value), func)

  /**
   * Redis Setnx（SET if Not eXists） 命令在指定的 key 不存在时，为 key 设置指定的值。
   *
   * @param key
   * @param value
   * @param ttl
   * @param endpoint
   * @return
   */
  def setnx(key: String, value: String, ttl: JInt = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long = doRedis(r => {
    val x = r.setnx(key, value)
    if (x == 1 && ttl != null) {
      r.expire(key, ttl)
    }
    x
  }, func)

  /**
   * Redis Hsetnx 命令用于为哈希表中不存在的的字段赋值 。
   * 如果哈希表不存在，一个新的哈希表被创建并进行 HSET 操作。
   * 如果字段已经存在于哈希表中，操作无效
   *
   * @param key
   * @param field
   * @param value
   * @param ttl
   * @param endpoint
   * @return
   */

  def hsetnx(key: String, field: String, value: String, ttl: JInt = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long = doRedis(r => {
    val x = r.hsetnx(key, field, value)
    if (x == 1 && ttl != null) {
      r.expire(key, ttl)
    }
    x
  }, func)

  /**
   * mget
   *
   * @param keys
   * @param endpoint
   * @return
   */
  def mget(keys: Array[String])(implicit endpoint: RedisEndpoint): Array[String] = doRedis(_.mget(keys: _*).asScala.toArray)

  /**
   * del
   *
   * @param key
   * @param endpoint
   * @return
   */
  def del(key: String, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long = doRedis(_.del(key), func)

  def set(key: String, value: String, ttl: JInt = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): String = doRedis(r => {
    val s = r.set(key, value)
    if (ttl != null) {
      r.expire(key, ttl)
    }
    s
  }, func)

  /**
   * hset
   *
   * @param key
   * @param field
   * @param value
   * @return
   */
  def hset(key: String, field: String, value: String, ttl: JInt = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long = doRedis(r => {
    val s = r.hset(key, field, value)
    if (ttl != null) {
      r.expire(key, ttl)
    }
    s
  }, func)

  /**
   *
   * @param key
   * @param hash
   * @param ttl
   * @param endpoint
   * @return
   */
  def hmset(key: String, hash: Map[String, String], ttl: JInt = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): String = doRedis(r => {
    val s = r.hmset(key, hash.asJava)
    if (ttl != null) {
      r.expire(key, ttl)
    }
    s
  }, func)

  /**
   *
   * @param key
   * @param fields
   * @param endpoint
   * @return
   */
  def hmget(key: String, fields: String*)(implicit endpoint: RedisEndpoint): List[String] = doRedis(_.hmget(key, fields: _*).toList)

  /**
   *
   * @param key
   * @param endpoint
   * @return
   */
  def hgetAll(key: String)(implicit endpoint: RedisEndpoint): Map[String, String] = doRedis(_.hgetAll(key).toMap)

  /**
   *
   * @param key
   * @param fields
   * @param endpoint
   * @return
   */
  def hdel(key: String, fields: immutable.List[String], func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long = {
    if (key == null || fields == null || fields.isEmpty) 0L
    else doRedis(_.hdel(key, fields.toArray: _*), func)
  }

  /**
   *
   * @param key
   * @param members
   * @param endpoint
   * @return
   */
  def sadd(key: String, members: immutable.List[String], ttl: JInt = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long = {
    doRedis(r => {
      val res = r.sadd(key, members.toArray: _*)
      if (ttl != null) {
        r.expire(key, ttl)
      }
      res
    }, func)
  }

  def smembers(key: String)(implicit endpoint: RedisEndpoint): Set[String] = doRedis(_.smembers(key))

  def srem(key: String, members: immutable.List[String], func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long = doRedis(_.srem(key, members.toArray: _*), func)

  def getOrElseHset(key: String, field: String, value: String, ttl: JInt = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): String = doRedis(x => {
    val v = x.hget(key, field)
    if (v == null) {
      x.hset(key, field, value)
      if (ttl != null) {
        x.expire(key, ttl)
      }
    }
    v
  }, func)

  def getOrElseSet(key: String, value: String, ttl: JInt = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): String = doRedis(x => {
    val v = x.get(key)
    if (v == null) {
      x.set(key, value)
      if (ttl != null) {
        x.expire(key, ttl)
      }
    }
    v
  }, func)

  def hincrBy(key: String, field: String, value: Long, ttl: JInt = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long = doRedis(x => {
    val reply = x.hincrBy(key, field, value)
    if (ttl != null) {
      x.expire(key, ttl)
    }
    reply
  }, func)

  def hincrByFloat(key: String, field: String, value: Double, ttl: JInt = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Double = doRedis(x => {
    val reply = x.hincrByFloat(key, field, value)
    if (ttl != null) {
      x.expire(key, ttl)
    }
    reply
  }, func)

  def incrBy(key: String, value: Long, ttl: JInt = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long = doRedis(x => {
    val reply = x.incrBy(key, value)
    if (ttl != null) {
      x.expire(key, ttl)
    }
    reply
  }, func)

  def incrByFloat(key: String, value: Double, ttl: JInt = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Double = doRedis(x => {
    val reply = x.incrByFloat(key, value)
    if (ttl != null) {
      x.expire(key, ttl)
    }
    reply
  }, func)

  /**
   * 批量写入
   *
   * @param kvs
   * @param ttl
   */
  def mSets(kvs: Seq[(String, String)], ttl: JInt = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long = doRedis(x => {
    val start = System.currentTimeMillis()
    val pipe = x.pipelined()
    kvs.foreach { case (k, v) =>
      pipe.mset(k, v)
      if (ttl != null) {
        pipe.expire(k, ttl)
      }
    }
    pipe.sync()
    System.currentTimeMillis() - start
  }, func)

  def mSetex(kvs: Seq[(String, String)], ttl: JInt = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long = doRedis(x => {
    val start = System.currentTimeMillis()
    val pipe = x.pipelined()
    kvs.foreach { case (k, v) =>
      pipe.setnx(k, v)
      if (ttl != null) {
        pipe.expire(k, ttl)
      }
    }
    pipe.sync()
    System.currentTimeMillis() - start
  }, func)

  def expire(key: String, s: Int)(implicit endpoint: RedisEndpoint): Long = doRedis(_.expire(key, s))

  def delByPattern(key: String, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Any = doRedis(r => {
    /**
     * 采用 scan 的方式,一次删除10000条记录,循环删除,防止一次加载的记录太多,内存撑爆
     */
    val scanParams = new ScanParams
    scanParams.`match`(key)
    scanParams.count(10000)
    var cursor = ScanParams.SCAN_POINTER_START
    do {
      val scanResult = r.scan(cursor, scanParams)
      cursor = scanResult.getCursor
      val keys = scanResult.getResult.toList
      if (keys.nonEmpty) {
        r.del(keys: _*)
      }
    } while (cursor != "0")
  }, func)

  def hlen(key: String)(implicit endpoint: RedisEndpoint): Long = doRedis(_.hlen(key))

  def doRedis[R](f: Jedis => R, func: () => Unit = null)(implicit endpoint: RedisEndpoint): R = {
    val redis = RedisClient.connect(endpoint)
    val result = func match {
      case null => f(redis)
      case _ =>
        // 确保redis的操作和用户的操作在用一个redis事务里...
        val transaction = redis.multi()
        val r = f(redis)
        func()
        transaction.exec()
        transaction.close()
        r
    }
    Try(redis.close()) match {
      case Success(_) => logger.debug("jedis.close successful.")
      case Failure(_) => logger.error("jedis.close failed.")
    }
    result
  }

  def doCluster[R](f: JedisCluster => R)(implicit endpoint: RedisEndpoint*): R = {
    val cluster = RedisClient.connectCluster(endpoint: _*)
    val result = f(cluster)
    Try(cluster.close()) match {
      case Success(_) => logger.debug("cluster.close successful.")
      case Failure(_) => logger.error("cluster.close failed.")
    }
    result
  }

  def doPipeline[R](f: Pipeline => R)(implicit endpoint: RedisEndpoint): R = {
    val redis = RedisClient.connect(endpoint)
    val pipe = redis.pipelined()
    val result = f(pipe)
    Try {
      pipe.sync()
      pipe.close()
      redis.close()
    } match {
      case Success(_) => logger.debug("pipe.close successful.")
      case Failure(_) => logger.error("pipe.close failed.")
    }
    result
  }

}
