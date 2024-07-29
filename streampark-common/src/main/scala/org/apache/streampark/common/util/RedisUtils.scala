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

import org.apache.streampark.common.util.Implicits._

import redis.clients.jedis.{Jedis, JedisCluster, Pipeline, ScanParams}

import scala.collection.immutable
import scala.util.{Failure, Success, Try}

object RedisUtils extends Logger {

  def exists(key: String)(implicit endpoint: RedisEndpoint): Boolean = doRedis(_.exists(key))

  def hexists(key: String, field: String, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Boolean = doRedis(_.hexists(key, field), func)

  def get(key: String)(implicit endpoint: RedisEndpoint): String = doRedis(_.get(key))

  def hget(key: String, field: String)(implicit endpoint: RedisEndpoint): String = doRedis(
    _.hget(key, field))

  /**
   * The Redis Setex command sets the value and expiration time. If the key already exists, the
   * Setex command will replace the old value.
   */
  def setex(key: String, value: String, ttl: Integer = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): String =
    doRedis(_.setex(key, ttl, value), func)

  /**
   * The Redis Setnx (SET if Not eXists) command sets the specified value if the specified key does
   * not exist
   */
  def setnx(key: String, value: String, ttl: Integer = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long =
    doRedis(
      r => {
        val x = r.setnx(key, value)
        if (x == 1 && ttl != null) {
          r.expire(key, ttl)
        }
        x
      },
      func)

  /**
   * The Redis Hsetnx command is used to assign values to fields in a hash table that do not exist.
   * If the hash table does not exist, a new hash table is created and the HSET operation is
   * performed. If the field already exists in the hash table, the operation is invalid
   */
  def hsetnx(
      key: String,
      field: String,
      value: String,
      ttl: Integer = null,
      func: () => Unit = null)(
      implicit endpoint: RedisEndpoint): Long =
    doRedis(
      r => {
        val x = r.hsetnx(key, field, value)
        if (x == 1 && ttl != null) {
          r.expire(key, ttl)
        }
        x
      },
      func)

  def mget(keys: Array[String])(implicit endpoint: RedisEndpoint): Array[String] = doRedis(
    _.mget(keys: _*).asScala.toArray)

  def del(key: String, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long =
    doRedis(_.del(key), func)

  def set(key: String, value: String, ttl: Integer = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): String =
    doRedis(
      r => {
        val s = r.set(key, value)
        if (ttl != null) {
          r.expire(key, ttl)
        }
        s
      },
      func)

  def hset(key: String, field: String, value: String, ttl: Integer = null, func: () => Unit = null)(
      implicit endpoint: RedisEndpoint): Long =
    doRedis(
      r => {
        val s = r.hset(key, field, value)
        if (ttl != null) {
          r.expire(key, ttl)
        }
        s
      },
      func)

  def hmset(key: String, hash: Map[String, String], ttl: Integer = null, func: () => Unit = null)(
      implicit endpoint: RedisEndpoint): String = doRedis(
    r => {
      val s = r.hmset(key, hash.asJava)
      if (ttl != null) {
        r.expire(key, ttl)
      }
      s
    },
    func)

  def hmget(key: String, fields: String*)(implicit endpoint: RedisEndpoint): List[String] = doRedis(
    _.hmget(key, fields: _*).toList)

  def hgetAll(key: String)(implicit endpoint: RedisEndpoint): Map[String, String] = doRedis(
    _.hgetAll(key).toMap)

  def hdel(key: String, fields: immutable.List[String], func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long = {
    if (key == null || fields == null || fields.isEmpty) 0L
    else doRedis(_.hdel(key, fields.toArray: _*), func)
  }

  def sadd(
      key: String,
      members: immutable.List[String],
      ttl: Integer = null,
      func: () => Unit = null)(
      implicit endpoint: RedisEndpoint): Long = {
    doRedis(
      r => {
        val res = r.sadd(key, members.toArray: _*)
        if (ttl != null) {
          r.expire(key, ttl)
        }
        res
      },
      func)
  }

  def smembers(key: String)(implicit endpoint: RedisEndpoint): java.util.Set[String] =
    doRedis(_.smembers(key))

  def srem(key: String, members: immutable.List[String], func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long =
    doRedis(_.srem(key, members.toArray: _*), func)

  def getOrElseHset(
      key: String,
      field: String,
      value: String,
      ttl: Integer = null,
      func: () => Unit = null)(implicit endpoint: RedisEndpoint): String =
    doRedis(
      x => {
        val v = x.hget(key, field)
        if (v == null) {
          x.hset(key, field, value)
          if (ttl != null) {
            x.expire(key, ttl)
          }
        }
        v
      },
      func)

  def getOrElseSet(
      key: String,
      value: String,
      ttl: Integer = null,
      func: () => Unit = null)(implicit endpoint: RedisEndpoint): String =
    doRedis(
      x => {
        val v = x.get(key)
        if (v == null) {
          x.set(key, value)
          if (ttl != null) {
            x.expire(key, ttl)
          }
        }
        v
      },
      func)

  def hincrBy(
      key: String,
      field: String,
      value: Long,
      ttl: Integer = null,
      func: () => Unit = null)(
      implicit endpoint: RedisEndpoint): Long =
    doRedis(
      x => {
        val reply = x.hincrBy(key, field, value)
        if (ttl != null) {
          x.expire(key, ttl)
        }
        reply
      },
      func)

  def hincrByFloat(
      key: String,
      field: String,
      value: Double,
      ttl: Integer = null,
      func: () => Unit = null)(implicit endpoint: RedisEndpoint): Double =
    doRedis(
      x => {
        val reply = x.hincrByFloat(key, field, value)
        if (ttl != null) {
          x.expire(key, ttl)
        }
        reply
      },
      func)

  def incrBy(key: String, value: Long, ttl: Integer = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long =
    doRedis(
      x => {
        val reply = x.incrBy(key, value)
        if (ttl != null) {
          x.expire(key, ttl)
        }
        reply
      },
      func)

  def incrByFloat(key: String, value: Double, ttl: Integer = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Double =
    doRedis(
      x => {
        val reply = x.incrByFloat(key, value)
        if (ttl != null) {
          x.expire(key, ttl)
        }
        reply
      },
      func)

  /** Batch writing */
  def mSets(kvs: Seq[(String, String)], ttl: Integer = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long =
    doRedis(
      x => {
        val start = System.currentTimeMillis()
        val pipe = x.pipelined()
        kvs.foreach {
          case (k, v) =>
            pipe.mset(k, v)
            if (ttl != null) {
              pipe.expire(k, ttl)
            }
        }
        pipe.sync()
        System.currentTimeMillis() - start
      },
      func)

  def mSetex(kvs: Seq[(String, String)], ttl: Integer = null, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Long =
    doRedis(
      x => {
        val start = System.currentTimeMillis()
        val pipe = x.pipelined()
        kvs.foreach {
          case (k, v) =>
            pipe.setnx(k, v)
            if (ttl != null) {
              pipe.expire(k, ttl)
            }
        }
        pipe.sync()
        System.currentTimeMillis() - start
      },
      func)

  def expire(key: String, s: Int)(implicit endpoint: RedisEndpoint): Long =
    doRedis(_.expire(key, s))

  def delByPattern(key: String, func: () => Unit = null)(implicit endpoint: RedisEndpoint): Any =
    doRedis(
      r => {
        // Use scan to delete 10,000 records at a time,
        // to prevent a load of too many records, caused oom
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
      },
      func)

  def hlen(key: String)(implicit endpoint: RedisEndpoint): Long = doRedis(_.hlen(key))

  def doRedis[R](f: Jedis => R, func: () => Unit = null)(implicit endpoint: RedisEndpoint): R = {
    val redis = RedisClient.connect(endpoint)
    val result = func match {
      case null => f(redis)
      case _ =>
        // Ensure that redis operations and user operations are in the same redis transaction
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
