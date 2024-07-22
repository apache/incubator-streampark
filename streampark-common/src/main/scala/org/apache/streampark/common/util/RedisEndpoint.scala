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

import org.apache.streampark.common.conf.ConfigKeys._
import org.apache.streampark.common.util.Implicits._

import redis.clients.jedis.{Jedis, Protocol}
import redis.clients.jedis.util.{JedisClusterCRC16, JedisURIHelper, SafeEncoder}

import java.net.URI
import java.util.Properties

/**
 * RedisEndpoint represents a redis connection endpoint info: host, port, auth password db number,
 * and timeout
 *
 * @param host
 *   the redis host or ip
 * @param port
 *   the redis port
 * @param auth
 *   the authentication password
 * @param db
 *   database number (should be avoided in general)
 */
case class RedisEndpoint(
    host: String = Protocol.DEFAULT_HOST,
    port: Int = Protocol.DEFAULT_PORT,
    auth: String = null,
    db: Int = Protocol.DEFAULT_DATABASE,
    timeout: Int = Protocol.DEFAULT_TIMEOUT)
  extends Serializable {

  /**
   * Constructor from Properties. set params with redis.host, redis.port, redis.password and
   * redis.db
   *
   * @param conf
   *   Properties
   */
  def this(conf: Properties) {
    this(
      conf.getOrElse(KEY_HOST, Protocol.DEFAULT_HOST),
      conf.getOrElse(KEY_PORT, Protocol.DEFAULT_PORT).toString.toInt,
      conf.getOrElse(KEY_PASSWORD, null),
      conf.getOrElse(KEY_DB, Protocol.DEFAULT_DATABASE).toString.toInt,
      conf.getOrElse(KEY_TIMEOUT, Protocol.DEFAULT_TIMEOUT).toString.toInt)
  }

  /**
   * Constructor with Jedis URI
   *
   * @param uri
   *   connection URI in the form of redis://:$password@$host:$port/[dbnum]
   */
  def this(uri: URI) =
    this(uri.getHost, uri.getPort, JedisURIHelper.getPassword(uri), JedisURIHelper.getDBIndex(uri))

  /**
   * Constructor with Jedis URI from String
   *
   * @param uri
   *   connection URI in the form of redis://:$password@$host:$port/[dbnum]
   */
  def this(uri: String) = this(URI.create(uri))

  /**
   * Connect tries to open a connection to the redis endpoint, optionally authenticating and
   * selecting a db
   *
   * @return
   *   a new Jedis instance
   */
  def connect(): Jedis = RedisClient.connect(this)

}

case class RedisNode(endpoint: RedisEndpoint, startSlot: Int, endSlot: Int, idx: Int, total: Int) {
  def connect(): Jedis = {
    endpoint.connect()
  }
}

/**
 * RedisConfig holds the state of the cluster nodes, and uses consistent hashing to map keys to
 * nodes
 */
class RedisConfig(val initialHost: RedisEndpoint) extends Serializable {

  val initialAddr = initialHost.host

  val hosts = getHosts(initialHost)
  val nodes = getNodes(initialHost)

  /** @return initialHost's auth */
  def getAuth: String = initialHost.auth

  /** @return selected db number */
  def getDB: Int = initialHost.db

  def getRandomNode: RedisNode = {
    val rnd = scala.util.Random.nextInt().abs % hosts.length
    hosts(rnd)
  }

  /**
   * @param sPos
   *   start slot number
   * @param ePos
   *   end slot number
   * @return
   *   a list of RedisNode whose slots union [sPos, ePos] is not null
   */
  def getNodesBySlots(sPos: Int, ePos: Int): Array[RedisNode] = {
    /* This function judges if [sPos1, ePos1] union [sPos2, ePos2] is not null */
    def inter(sPos1: Int, ePos1: Int, sPos2: Int, ePos2: Int) =
      if (sPos1 <= sPos2) ePos1 >= sPos2 else ePos2 >= sPos1

    nodes
      .filter(node => inter(sPos, ePos, node.startSlot, node.endSlot))
      .filter(_.idx == 0) // master only now
  }

  /**
   * @param key
   *   *IMPORTANT* Please remember to close after using
   * @return
   *   jedis who is a connection for a given key
   */
  def connectionForKey(key: String): Jedis = {
    getHost(key).connect()
  }

  /**
   * @param initialHost
   *   any redis endpoint of a cluster or a single server
   * @return
   *   true if the target server is in cluster mode
   */
  private def clusterEnabled(initialHost: RedisEndpoint): Boolean = {
    val conn = initialHost.connect()
    val info = conn.info.split("\n")
    val version = info.filter(_.contains("redis_version:"))(0)
    val clusterEnable = info.filter(_.contains("cluster_enabled:"))
    val mainVersion = version.substring(14, version.indexOf(".")).toInt
    val res = mainVersion > 2 && clusterEnable.length > 0 && clusterEnable(0).contains("1")
    conn.close()
    res
  }

  /**
   * @param key
   * @return
   *   host whose slots should involve key
   */
  def getHost(key: String): RedisNode = {
    val slot = JedisClusterCRC16.getSlot(key)
    hosts.filter(host => {
      host.startSlot <= slot && host.endSlot >= slot
    })(0)
  }

  /**
   * @param initialHost
   *   any redis endpoint of a cluster or a single server
   * @return
   *   list of host nodes
   */
  private def getHosts(initialHost: RedisEndpoint): Array[RedisNode] = {
    getNodes(initialHost).filter(_.idx == 0)
  }

  /**
   * @param initialHost
   *   any redis endpoint of a single server
   * @return
   *   list of nodes
   */
  private def getNonClusterNodes(initialHost: RedisEndpoint): Array[RedisNode] = {
    val master = (initialHost.host, initialHost.port)
    val conn = initialHost.connect()

    val replinfo = conn.info("Replication").split("\n")
    conn.close()

    // If  this node is a slave, we need to extract the slaves from its master
    if (replinfo.exists(_.contains("role:slave"))) {
      val host =
        replinfo.filter(_.contains("master_host:"))(0).trim.substring(12)
      val port =
        replinfo.filter(_.contains("master_port:"))(0).trim.substring(12).toInt

      // simply re-enter this function witht he master host/port
      getNonClusterNodes(initialHost = RedisEndpoint(host, port, initialHost.auth, initialHost.db))

    } else {
      // this is a master - take its slaves

      val slaves = replinfo
        .filter(x => x.contains("slave") && x.contains("online"))
        .map(rl => {
          val content = rl.substring(rl.indexOf(':') + 1).split(",")
          val ip = content(0)
          val port = content(1)
          (ip.substring(ip.indexOf('=') + 1), port.substring(port.indexOf('=') + 1).toInt)
        })

      val nodes = master +: slaves
      val range = nodes.length
      (0 until range)
        .map(i =>
          RedisNode(
            RedisEndpoint(
              nodes(i)._1,
              nodes(i)._2,
              initialHost.auth,
              initialHost.db,
              initialHost.timeout),
            0,
            16383,
            i,
            range))
        .toArray
    }
  }

  /**
   * @param initialHost
   *   any redis endpoint of a cluster server
   * @return
   *   list of nodes
   */
  private def getClusterNodes(initialHost: RedisEndpoint): Array[RedisNode] = {
    val conn = initialHost.connect()
    val res = conn
      .clusterSlots()
      .flatMap {
        slotInfoObj =>
          {
            val slotInfo =
              slotInfoObj.asInstanceOf[java.util.List[java.lang.Object]]
            val sPos = slotInfo.get(0).toString.toInt
            val ePos = slotInfo.get(1).toString.toInt
            /*
             * We will get all the nodes with the slots range [sPos, ePos],
             * and create RedisNode for each nodes, the total field of all
             * RedisNode are the number of the nodes whose slots range is
             * as above, and the idx field is just an index for each node
             * which will be used for adding support for slaves and so on.
             * And the idx of a master is always 0, we rely on this fact to
             * filter master.
             */
            (0 until (slotInfo.size - 2)).map(i => {
              val node =
                slotInfo(i + 2).asInstanceOf[java.util.List[java.lang.Object]]
              val host = SafeEncoder.encode(node.get(0).asInstanceOf[Array[scala.Byte]])
              val port = node.get(1).toString.toInt
              RedisNode(
                RedisEndpoint(host, port, initialHost.auth, initialHost.db, initialHost.timeout),
                sPos,
                ePos,
                i,
                slotInfo.size - 2)
            })
          }
      }
      .toArray
    conn.close()
    res
  }

  /**
   * @param initialHost
   *   any redis endpoint of a cluster or a single server
   * @return
   *   list of nodes
   */
  def getNodes(initialHost: RedisEndpoint): Array[RedisNode] = {
    if (clusterEnabled(initialHost)) {
      getClusterNodes(initialHost)
    } else {
      getNonClusterNodes(initialHost)
    }
  }
}
