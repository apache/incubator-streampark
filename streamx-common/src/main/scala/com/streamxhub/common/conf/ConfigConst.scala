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


  /**
   * sign....
   */
  val SIGN_COLON = ":"

  val SIGN_SEMICOLON = ";"

  val SIGN_COMMA = ","

  val SIGN_EMPTY = ""

  //flink.....

  val KEY_FLINK_APP_CONF = "flink.conf"

  val KEY_FLINK_CHECKPOINT_INTERVAL = "flink.checkpoint.interval"

  val KEY_FLINK_PARALLELISM = "flink.parallelism"

  val KEY_FLINK_RESTART_ATTEMPTS = "flink.restart.attempts"

  val KEY_FLINK_DELAY_ATTEMPTS = "flink.delay.attempts"

  val KEY_FLINK_CHECKPOINT_MODE = "flink.checkpoint.mode"

  val KEY_FLINK_TIME_CHARACTERISTIC = "flink.time.characteristic"

  val KEY_FLINK_APP_NAME = "flink.deployment.resource.yarnname"

  /**
   * about config Kafka
   */

  val KAFKA_SINK_PREFIX = "kafka.sink."

  val KAFKA_SOURCE_PREFIX = "kafka.source."

  val KEY_KAFKA_TOPIC = "topic"

  val REDIS_PREFIX = "redis."

  /**
   * about config MySQL
   */
  val MYSQL_PREFIX = "mysql."
  val KEY_MYSQL_INSTANCE = "instance"
  val KEY_MYSQL_DRIVER = "driverClassName"
  val KEY_MYSQL_URL = "jdbcUrl"
  val KEY_MYSQL_USER = "username"
  val KEY_MYSQL_PASSWORD = "password"

  /**
   * about config HBase
   */
  val HBASE_PREFIX = "hbase."

  val KEY_HBASE_COMMIT_BATCH ="hbase.commit.batch"

  val KEY_HBASE_AUTH_USER = "hbase.auth.user"
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

}
