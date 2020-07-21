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
package com.streamxhub.common.conf


object ConfigConst {

  /**
   *
   * about parameter...
   */

  val KEY_APP_HOME = "app.home"

  val KEY_HOST = "host"

  val KEY_PORT = "port"

  val KEY_DB = "db"

  val KEY_USER = "user"

  val KEY_PASSWORD = "password"

  val KEY_TIMEOUT = "timeout"

  /**
   * sign....
   */
  val SIGN_COLON = ":"

  val SIGN_SEMICOLON = ";"

  val SIGN_COMMA = ","

  val SIGN_EMPTY = ""

  //
  val KEY_FLINK_APP_CONF = "flink.conf"

  val KEY_FLINK_PARALLELISM = "flink.parallelism"

  val KEY_FLINK_APP_NAME = "flink.deployment.option.yarnname"

  // --checkpoints--
  val KEY_FLINK_CHECKPOINTS_ENABLE = "flink.checkpoints.enable"

  val KEY_FLINK_CHECKPOINTS_INTERVAL = "flink.checkpoints.interval"

  val KEY_FLINK_CHECKPOINTS_MODE = "flink.checkpoints.mode"

  val KEY_FLINK_CHECKPOINTS_CLEANUP = "flink.checkpoints.cleanup"

  val KEY_FLINK_CHECKPOINTS_TIMEOUT = "flink.checkpoints.timeout"

  val KEY_FLINK_CHECKPOINTS_MAX_CONCURRENT = "flink.checkpoints.maxConcurrent"

  val KEY_FLINK_CHECKPOINTS_MIN_PAUSEBETWEEN = "flink.checkpoints.minPauseBetween"

  //---state---

  val KEY_FLINK_STATE_CHECKPOINTS_DIR = "state.checkpoints.dir"

  val KEY_FLINK_STATE_BACKEND = "state.backend"

  val KEY_FLINK_STATE_BACKEND_ASYNC = "state.backend.async"

  val KEY_FLINK_STATE_BACKEND_INCREMENTAL = "state.backend.incremental"

  val KEY_FLINK_STATE_BACKEND_MEMORY = "state.backend.memory"

  val KEY_FLINK_STATE_ROCKSDB = "state.backend.rocksdb"


  //---restart-strategy---

  val KEY_FLINK_RESTART_STRATEGY = "restart-strategy"

  val KEY_FLINK_RESTART_FAILURE_PER_INTERVAL = "restart-strategy.failure-rate.max-failures-per-interval"

  val KEY_FLINK_RESTART_FAILURE_RATE_INTERVAL = "restart-strategy.failure-rate.failure-rate-interval"

  val KEY_FLINK_RESTART_FAILURE_RATE_DELAY = "restart-strategy.failure-rate.delay"

  val KEY_FLINK_RESTART_ATTEMPTS = "restart-strategy.fixed-delay.attempts"

  val KEY_FLINK_RESTART_DELAY = "restart-strategy.fixed-delay.delay"

  // ---watermark---
  val KEY_FLINK_WATERMARK_TIME_CHARACTERISTIC = "flink.watermark.time.characteristic"

  val KEY_FLINK_WATERMARK_INTERVAL = "flink.watermark.interval"


  /**
   * about config Kafka
   */

  val KAFKA_SINK_PREFIX = "kafka.sink."

  val KAFKA_SOURCE_PREFIX = "kafka.source."

  val KEY_KAFKA_TOPIC = "topic"

  val KEY_KAFKA_PATTERN = "pattern"

  val KEY_KAFKA_START_FROM = "start.from"

  val KEY_KAFKA_START_FROM_OFFSET = "offset"

  val KEY_KAFKA_START_FROM_TIMESTAMP = "timestamp"

  val REDIS_PREFIX = "redis."

  val KEY_INSTANCE = "instance"

  val KEY_ALIAS = "alias"

  /**
   * about config jdbc...
   */
  val MYSQL_PREFIX = "mysql."
  val KEY_JDBC_DRIVER = "driverClassName"
  val KEY_JDBC_DATABASE = "database"
  val KEY_JDBC_URL = "jdbcUrl"
  val KEY_JDBC_USER = "username"
  val KEY_JDBC_PASSWORD = "password"
  val KEY_JDBC_INSERT_BATCH = "batch.size"
  val DEFAULT_JDBC_INSERT_BATCH = 1

  val MONGO_PREFIX = "mongodb."
  /**
   * about config HBase
   */
  val HBASE_PREFIX = "hbase."

  val KEY_HBASE_COMMIT_BATCH = "hbase.commit.batch"

  val KEY_HBASE_WRITE_SIZE = "hbase.client.write.size"

  val DEFAULT_HBASE_COMMIT_BATCH = 1000

  val KEY_HBASE_AUTH_USER = "hbase.auth.user"

  val DEFAULT_HBASE_WRITE_SIZE = 1024 * 1024 * 10

  /**
   * about influx
   */
  val INFLUX_PREFIX = "influx."
  val KEY_INFLUX_ACTIONS = "actions"
  val KEY_INFLUX_FLUSH_DURATION = "flush.duration"
  /**
   * about clickhouse
   */
  val CLICKHOUSE_SINK_PREFIX = "clickhouse.sink"

  val HTTP_SINK_PREFIX = "http.sink"

  /**
   * sink threshold and failover...
   */
  val KEY_SINK_THRESHOLD_BUFFER_SIZE: String = "threshold.bufferSize"
  val KEY_SINK_THRESHOLD_NUM_WRITERS: String = "threshold.numWriters"
  val KEY_SINK_THRESHOLD_QUEUE_CAPACITY: String = "threshold.queueCapacity"
  val KEY_SINK_THRESHOLD_DELAY_TIME: String = "threshold.delayTime"
  val KEY_SINK_THRESHOLD_REQ_TIMEOUT: String = "threshold.requestTimeout"
  val KEY_SINK_THRESHOLD_RETRIES: String = "threshold.retries"
  val KEY_SINK_THRESHOLD_SUCCESS_CODE: String = "threshold.successCode"

  val KEY_SINK_FAILOVER_TABLE: String = "failover.table"
  val KEY_SINK_FAILOVER_STORAGE: String = "failover.storage"

  val DEFAULT_SINK_REQUEST_TIMEOUT = 2000
  val DEFAULT_HTTP_SUCCESS_CODE = 200
  val DEFAULT_SINK_THRESHOLD_QUEUE_CAPACITY = 10000
  val DEFAULT_SINK_THRESHOLD_DELAY_TIME = 1000L
  val DEFAULT_SINK_THRESHOLD_BUFFER_SIZE = 1000
  val DEFAULT_SINK_THRESHOLD_RETRIES = 3
  val DEFAULT_SINK_THRESHOLD_NUM_WRITERS: Int = Runtime.getRuntime.availableProcessors()

  /**
   * about config es
   */
  val ES_PREFIX = "es.sink."

  val KEY_ES_AUTH_USER = "es.auth.user"

  val KEY_ES_AUTH_PASSWORD = "es.auth.password"

  val KEY_ES_REST_MAX_RETRY = "es.rest.max.retry.timeout"

  val KEY_ES_REST_CONTENT_TYPE = "es.rest.content.type"

  val KEY_ES_CONN_REQ_TIME_OUT = "es.connect.request.timeout"

  val KEY_ES_CONN_TIME_OUT = "es.connect.timeout"

  val KEY_ES_CLUSTER_NAME = "es.cluster.name"

  val KEY_ES_BULK_PREFIX = "bulk.flush."

  val KEY_ES_CLIENT_TRANSPORT_SNIFF = "client.transport.sniff"

  val LOGO =
    s"""
       |
       |                         ▒▓██▓██▒
       |                     ▓████▒▒█▓▒▓███▓▒
       |                  ▓███▓░░        ▒▒▒▓██▒  ▒
       |                ░██▒   ▒▒▓▓█▓▓▒░      ▒████
       |                ██▒         ░▒▓███▒    ▒█▒█▒
       |                  ░▓█            ███   ▓░▒██
       |                    ▓█       ▒▒▒▒▒▓██▓░▒░▓▓█
       |                  █░ █   ▒▒░       ███▓▓█ ▒█▒▒▒
       |                  ████░   ▒▓█▓      ██▒▒▒ ▓███▒
       |               ░▒█▓▓██       ▓█▒    ▓█▒▓██▓ ░█░
       |         ▓░▒▓████▒ ██         ▒█    █▓░▒█▒░▒█▒
       |        ███▓░██▓  ▓█           █   █▓ ▒▓█▓▓█▒
       |      ░██▓  ░█░            █  █▒ ▒█████▓▒ ██▓░▒
       |     ███░ ░ █░          ▓ ░█ █████▒░░    ░█░▓  ▓░
       |    ██▓█ ▒▒▓▒          ▓███████▓░       ▒█▒ ▒▓ ▓██▓
       | ▒██▓ ▓█ █▓█       ░▒█████▓▓▒░         ██▒▒  █ ▒  ▓█▒
       | ▓█▓  ▓█ ██▓ ░▓▓▓▓▓▓▓▒              ▒██▓           ░█▒
       | ▓█    █ ▓███▓▒░              ░▓▓▓███▓          ░▒░ ▓█
       | ██▓    ██▒    ░▒▓▓███▓▓▓▓▓██████▓▒            ▓███  █
       |▓███▒ ███   ░▓▓▒░░   ░▓████▓░                  ░▒▓▒  █▓
       |█▓▒▒▓▓██  ░▒▒░░░▒▒▒▒▓██▓░                            █▓
       |██ ▓░▒█   ▓▓▓▓▒░░  ▒█▓       ▒▓▓██▓    ▓▒          ▒▒▓
       |▓█▓ ▓▒█  █▓░  ░▒▓▓██▒            ░▓█▒   ▒▒▒░▒▒▓█████▒
       | ██░ ▓█▒█▒  ▒▓▓▒  ▓█                █░      ░░░░   ░█▒
       | ▓█   ▒█▓   ░     █░                ▒█              █▓
       |  █▓   ██         █░                 ▓▓        ▒█▓▓▓▒█░
       |   █▓ ░▓██░       ▓▒                  ▓█▓▒░░░▒▓█░    ▒█
       |    ██   ▓█▓░      ▒                    ░▒█▒██▒      ▓▓
       |     ▓█▒   ▒█▓▒░                         ▒▒ █▒█▓▒▒░░▒██
       |      ░██▒    ▒▓▓▒                     ▓██▓▒█▒ ░▓▓▓▓▒█▓
       |        ░▓██▒                          ▓░  ▒█▓█  ░░▒▒▒
       |            ▒▓▓▓▓▓▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒░░▓▓  ▓░▒█░
       |
       |                 +----------------------+
       |                 +  十步杀一人，千里不留行  +
       |                 +  事了拂衣去，深藏功与名  +
       |                 +----------------------+
       |
       |              [StreamX] let's flink|spark easy ô‿ô!
       |
       |""".stripMargin

}



