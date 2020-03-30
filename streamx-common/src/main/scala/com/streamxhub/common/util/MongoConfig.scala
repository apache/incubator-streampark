package com.streamxhub.common.util

import com.mongodb.{MongoClient, MongoClientOptions, MongoCredential, ServerAddress}
import java.util
import com.streamxhub.common.conf.ConfigConst._

import scala.collection.JavaConversions._

object MongoConfig {

  val address = "address"
  val replica_set = "replica-set"
  val database = "database"
  val username = "username"
  val password = "password"
  val min_connections_per_host = "min-connections-per-host"
  val max_connections_per_host = "max-connections-per-host"
  val threads_allowed_to_block_for_connection_multiplier = "threads-allowed-to-block-for-connection-multiplier"
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

  def getClient(parameter: util.Map[String, String], alias: String): MongoClient = {
    val mongoParam = parameter.filter(_._1.startsWith(MONGO_PREFIX)).map(x => {
      x._1.replaceAll(s"$MONGO_PREFIX$alias", "").replaceFirst("^\\.", "") -> x._2
    })
    // 客户端配置（连接数、副本集群验证）
    val builder = new MongoClientOptions.Builder
    if (mongoParam.contains(max_connections_per_host)) {
      builder.connectionsPerHost(mongoParam(max_connections_per_host).toInt)
    }
    if (mongoParam.contains(min_connections_per_host)) {
      builder.minConnectionsPerHost(mongoParam(min_connections_per_host).toInt)
    }
    if (mongoParam.contains(replica_set)) {
      builder.requiredReplicaSetName(mongoParam(replica_set))
    }
    if (mongoParam.contains(threads_allowed_to_block_for_connection_multiplier)) {
      builder.threadsAllowedToBlockForConnectionMultiplier(mongoParam(threads_allowed_to_block_for_connection_multiplier).toInt)
    }
    if (mongoParam.contains(server_selection_timeout)) {
      builder.serverSelectionTimeout(mongoParam(server_selection_timeout).toInt)
    }
    if (mongoParam.contains(max_wait_time)) {
      builder.maxWaitTime(mongoParam(max_wait_time).toInt)
    }
    if (mongoParam.contains(max_connection_idel_time)) {
      builder.maxConnectionIdleTime(mongoParam(max_connection_idel_time).toInt)
    }
    if (mongoParam.contains(max_connection_life_time)) {
      builder.maxConnectionLifeTime(mongoParam(max_connection_life_time).toInt)
    }
    if (mongoParam.contains(connect_timeout)) {
      builder.connectTimeout(mongoParam(connect_timeout).toInt)
    }
    if (mongoParam.contains(socket_timeout)) {
      builder.socketTimeout(mongoParam(socket_timeout).toInt)
    }
    if (mongoParam.contains(ssl_enabled)) {
      builder.sslEnabled(mongoParam(ssl_enabled).toBoolean)
    }
    if (mongoParam.contains(ssl_invalid_host_name_allowed)) {
      builder.sslInvalidHostNameAllowed(mongoParam(ssl_invalid_host_name_allowed).toBoolean)
    }
    if (mongoParam.contains(always_use_m_beans)) {
      builder.alwaysUseMBeans(mongoParam(always_use_m_beans).toBoolean)
    }
    if (mongoParam.contains(heartbeat_frequency)) {
      builder.heartbeatFrequency(mongoParam(always_use_m_beans).toInt)
    }
    if (mongoParam.contains(min_heartbeat_frequency)) {
      builder.minHeartbeatFrequency(mongoParam(min_heartbeat_frequency).toInt)
    }
    if (mongoParam.contains(heartbeat_connect_timeout)) {
      builder.heartbeatConnectTimeout(mongoParam(heartbeat_connect_timeout).toInt)
    }
    if (mongoParam.contains(heartbeat_socket_timeout)) {
      builder.heartbeatSocketTimeout(mongoParam(heartbeat_socket_timeout).toInt)
    }
    if (mongoParam.contains(local_threshold)) {
      builder.localThreshold(mongoParam(local_threshold).toInt)
    }

    val mongoClientOptions = builder.build

    val serverAddresses = mongoParam(address).split(",").map(x => {
      val hostAndPort = x.split(":")
      val host = hostAndPort.head
      val port = hostAndPort(1).toInt
      new ServerAddress(host, port)
    })

    if (mongoParam(username) != null) {
      val mongoCredential = MongoCredential.createScramSha1Credential(
        mongoParam(username),
        if (mongoParam(authentication_database) != null) mongoParam(authentication_database) else mongoParam(database),
        mongoParam(database).toCharArray)
      new MongoClient(serverAddresses.toList, mongoCredential, mongoClientOptions)
    } else {
      new MongoClient(serverAddresses.toList, mongoClientOptions)
    }
  }

}
