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

import com.mongodb._

import java.util.Properties

object MongoConfig {

  val client_uri = "client-uri"
  val address = "address"
  val replica_set = "replica-set"
  val database = "database"
  val username = "username"
  val password = "password"
  val min_connections_per_host = "min-connections-per-host"
  val max_connections_per_host = "max-connections-per-host"
  val threads_allowed_to_block_for_connection_multiplier =
    "threads-allowed-to-block-for-connection-multiplier"
  val server_selection_timeout = "server-selection-timeout"
  val max_wait_time = "max-wait-time"
  val max_connection_idel_time = "max-connection-idel-time"
  val max_connection_life_time = "max-connection-life-time"
  val connect_timeout = "connect-timeout"
  val socket_timeout = "socket-timeout"
  val socket_keep_alive = "socket-keep-alive"
  val ssl_enabled = "ssl-enabled"
  val ssl_invalid_host_name_allowed = "ssl-invalid-host-name-allowed"
  val always_use_m_beans = "always-use-m-beans"
  val heartbeat_socket_timeout = "heartbeat-socket-timeout"
  val heartbeat_connect_timeout = "heartbeat-connect-timeout"
  val min_heartbeat_frequency = "min-heartbeat-frequency"
  val heartbeat_frequency = "heartbeat-frequency"
  val local_threshold = "local-threshold"
  val authentication_database = "authentication-database"

  def getProperty(properties: Properties, k: String)(implicit alias: String = ""): String = {
    val prop = getProperties(properties)
    prop.getProperty(k)
  }

  def getProperties(properties: Properties)(implicit alias: String = ""): Properties = {
    val prop = new Properties()
    properties
      .filter(_._1.startsWith(MONGO_PREFIX))
      .filter(_._2.nonEmpty)
      .map(x => {
        val k =
          x._1.replaceAll(s"$MONGO_PREFIX$alias", "").replaceFirst("^\\.", "")
        prop.put(k, x._2.trim)
      })
    prop
  }

  def getClient(properties: Properties)(implicit alias: String = ""): MongoClient = {
    val mongoParam = getProperties(properties)
    if (mongoParam.containsKey(client_uri)) {
      val clientURI = new MongoClientURI(mongoParam(client_uri))
      new MongoClient(clientURI)
    } else {
      // 客户端配置（连接数、副本集群验证）
      val builder = new MongoClientOptions.Builder
      if (mongoParam.containsKey(max_connections_per_host)) {
        builder.connectionsPerHost(mongoParam(max_connections_per_host).toInt)
      }
      if (mongoParam.containsKey(min_connections_per_host)) {
        builder.minConnectionsPerHost(mongoParam(min_connections_per_host).toInt)
      }
      if (mongoParam.containsKey(replica_set)) {
        builder.requiredReplicaSetName(mongoParam(replica_set))
      }
      if (mongoParam.containsKey(threads_allowed_to_block_for_connection_multiplier)) {
        builder.threadsAllowedToBlockForConnectionMultiplier(
          mongoParam(threads_allowed_to_block_for_connection_multiplier).toInt)
      }
      if (mongoParam.containsKey(server_selection_timeout)) {
        builder.serverSelectionTimeout(mongoParam(server_selection_timeout).toInt)
      }
      if (mongoParam.containsKey(max_wait_time)) {
        builder.maxWaitTime(mongoParam(max_wait_time).toInt)
      }
      if (mongoParam.containsKey(max_connection_idel_time)) {
        builder.maxConnectionIdleTime(mongoParam(max_connection_idel_time).toInt)
      }
      if (mongoParam.containsKey(max_connection_life_time)) {
        builder.maxConnectionLifeTime(mongoParam(max_connection_life_time).toInt)
      }
      if (mongoParam.containsKey(connect_timeout)) {
        builder.connectTimeout(mongoParam(connect_timeout).toInt)
      }
      if (mongoParam.containsKey(socket_timeout)) {
        builder.socketTimeout(mongoParam(socket_timeout).toInt)
      }
      if (mongoParam.containsKey(ssl_enabled)) {
        builder.sslEnabled(mongoParam(ssl_enabled).toBoolean)
      }
      if (mongoParam.containsKey(ssl_invalid_host_name_allowed)) {
        builder.sslInvalidHostNameAllowed(mongoParam(ssl_invalid_host_name_allowed).toBoolean)
      }
      if (mongoParam.containsKey(always_use_m_beans)) {
        builder.alwaysUseMBeans(mongoParam(always_use_m_beans).toBoolean)
      }
      if (mongoParam.containsKey(heartbeat_frequency)) {
        builder.heartbeatFrequency(mongoParam(heartbeat_frequency).toInt)
      }
      if (mongoParam.containsKey(min_heartbeat_frequency)) {
        builder.minHeartbeatFrequency(mongoParam(min_heartbeat_frequency).toInt)
      }
      if (mongoParam.containsKey(heartbeat_connect_timeout)) {
        builder.heartbeatConnectTimeout(mongoParam(heartbeat_connect_timeout).toInt)
      }
      if (mongoParam.containsKey(heartbeat_socket_timeout)) {
        builder.heartbeatSocketTimeout(mongoParam(heartbeat_socket_timeout).toInt)
      }
      if (mongoParam.containsKey(local_threshold)) {
        builder.localThreshold(mongoParam(local_threshold).toInt)
      }
      val mongoClientOptions = builder.build
      val serverAddresses = mongoParam(address)
        .split(",")
        .map(x => {
          val hostAndPort = x.split(":")
          val host = hostAndPort.head
          val port = hostAndPort(1).toInt
          new ServerAddress(host, port)
        })
      if (mongoParam.containsKey(username)) {
        val db =
          if (mongoParam.containsKey(authentication_database)) {
            mongoParam(authentication_database)
          } else {
            mongoParam(database)
          }
        val mongoCredential = MongoCredential.createScramSha1Credential(
          mongoParam(username),
          db,
          mongoParam(password).toCharArray)
        new MongoClient(serverAddresses.toList, List(mongoCredential), mongoClientOptions)
      } else {
        new MongoClient(serverAddresses.toList, mongoClientOptions)
      }
    }
  }

}
