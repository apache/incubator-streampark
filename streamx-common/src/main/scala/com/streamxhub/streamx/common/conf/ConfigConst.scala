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
package com.streamxhub.streamx.common.conf

import java.time.LocalDateTime

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

  val KEY_JOB_ID = "jobId"

  val KEY_SEMANTIC = "semantic"

  val KEY_STREAMX_CONSOLE_URL = "streamx.console.url"

  /**
   * sign....
   */
  val SIGN_COLON = ":"

  val SIGN_SEMICOLON = ";"

  val SIGN_COMMA = ","

  val SIGN_EMPTY = ""

  /**
   * kerberos
   */
  val KEY_KERBEROS = "kerberos"

  val KEY_HADOOP_USER_NAME = "HADOOP_USER_NAME"

  /**
   * hadoop.security.authentication
   */
  val KEY_HADOOP_SECURITY_AUTHENTICATION = "hadoop.security.authentication"

  val KEY_SECURITY_KERBEROS_ENABLE = "security.kerberos.login.enable"

  val KEY_SECURITY_KERBEROS_DEBUG = "security.kerberos.login.debug"

  val KEY_SECURITY_KERBEROS_KEYTAB = "security.kerberos.login.keytab"

  val KEY_SECURITY_KERBEROS_PRINCIPAL = "security.kerberos.login.principal"

  val KEY_SECURITY_KERBEROS_KRB5_CONF = "security.kerberos.login.krb5"

  val KEY_JAVA_SECURITY_KRB5_CONF = "java.security.krb5.conf"

  //spark

  val KEY_SPARK_USER_ARGS = "spark.user.args"

  val KEY_SPARK_CONF = "spark.conf"

  val KEY_SPARK_DEBUG_CONF = "spark.debug.conf"

  val KEY_SPARK_MAIN_CLASS = "spark.main.class"

  val KEY_SPARK_APP_NAME = "spark.app.name"

  val KEY_SPARK_BATCH_DURATION = "spark.batch.duration"

  // flink
  def KEY_APP_CONF(prefix: String = null): String = if (prefix == null) "conf" else s"${prefix}conf"

  def KEY_FLINK_CONF(prefix: String = null): String = if (prefix == null) "flink.conf" else s"${prefix}flink.conf"

  def KEY_APP_NAME(prefix: String = null): String = if (prefix == null) "app.name" else s"${prefix}app.name"

  def KEY_FLINK_SQL(prefix: String = null): String = if (prefix == null) "sql" else s"${prefix}sql"

  def KEY_FLINK_PARALLELISM(prefix: String = null): String = if (prefix == null) "parallelism.default" else s"${prefix}parallelism.default"

  val KEY_FLINK_DEPLOYMENT_PROPERTY_PREFIX = "flink.deployment.property."

  val KEY_FLINK_DEPLOYMENT_OPTION_PREFIX = "flink.deployment.option."

  val KEY_FLINK_APP_NAME = "pipeline.name"

  val KEY_YARN_APP_ID = "yarn.application.id"

  val KEY_YARN_APP_NAME = "yarn.application.name"

  val KEY_YARN_APP_QUEUE = "yarn.application.queue"

  // --checkpoints--
  val KEY_FLINK_CHECKPOINTS_ENABLE = "flink.checkpoints.enable"

  val KEY_FLINK_CHECKPOINTS_UNALIGNED = "flink.checkpoints.unaligned"

  val KEY_FLINK_CHECKPOINTS_INTERVAL = "flink.checkpoints.interval"

  val KEY_FLINK_CHECKPOINTS_MODE = "flink.checkpoints.mode"

  val KEY_FLINK_CHECKPOINTS_CLEANUP = "flink.checkpoints.cleanup"

  val KEY_FLINK_CHECKPOINTS_TIMEOUT = "flink.checkpoints.timeout"

  val KEY_FLINK_CHECKPOINTS_MAX_CONCURRENT = "flink.checkpoints.maxConcurrent"

  val KEY_FLINK_CHECKPOINTS_MIN_PAUSEBETWEEN = "flink.checkpoints.minPauseBetween"

  //---state---

  val KEY_FLINK_STATE_SAVEPOINTS_DIR = "flink.state.savepoints.dir"

  val KEY_FLINK_STATE_CHECKPOINTS_DIR = "flink.state.checkpoints.dir"

  val KEY_FLINK_STATE_CHECKPOINT_STORAGE = "flink.state.checkpoint-storage"

  val KEY_FLINK_STATE_BACKEND = "flink.state.backend.value"

  val KEY_FLINK_STATE_BACKEND_ASYNC = "flink.state.backend.async"

  val KEY_FLINK_STATE_BACKEND_INCREMENTAL = "flink.state.backend.incremental"

  val KEY_FLINK_STATE_BACKEND_MEMORY = "flink.state.backend.memory"

  val KEY_FLINK_STATE_ROCKSDB = "flink.state.backend.rocksdb"

  //---restart-strategy---

  val KEY_FLINK_RESTART_STRATEGY = "flink.restart-strategy.value"

  val KEY_FLINK_RESTART_STRATEGY_FAILURE_RATE_PER_INTERVAL = "flink.restart-strategy.failure-rate.max-failures-per-interval"

  val KEY_FLINK_RESTART_STRATEGY_FAILURE_RATE_RATE_INTERVAL = "flink.restart-strategy.failure-rate.failure-rate-interval"

  val KEY_FLINK_RESTART_STRATEGY_FAILURE_RATE_DELAY = "flink.restart-strategy.failure-rate.delay"

  val KEY_FLINK_RESTART_STRATEGY_FIXED_DELAY_ATTEMPTS = "flink.restart-strategy.fixed-delay.attempts"

  val KEY_FLINK_RESTART_STRATEGY_FIXED_DELAY_DELAY = "flink.restart-strategy.fixed-delay.delay"

  val KEY_EXECUTION_RUNTIME_MODE = "flink.execution.runtime-mode"

  val KEY_FLINK_WATERMARK_INTERVAL = "flink.watermark.interval"

  // ---watermark---
  val KEY_FLINK_WATERMARK_TIME_CHARACTERISTIC = "flink.watermark.time.characteristic"

  // ---table---
  val KEY_FLINK_TABLE_PLANNER = "flink.table.planner"

  val KEY_FLINK_TABLE_MODE = "flink.table.mode"

  val KEY_FLINK_TABLE_CATALOG = "flink.table.catalog"

  val KEY_FLINK_TABLE_DATABASE = "flink.table.database"

  /**
   * about config Kafka
   */

  val KAFKA_SINK_PREFIX = "kafka.sink."

  val KAFKA_SOURCE_PREFIX = "kafka.source."

  val KEY_KAFKA_TOPIC = "topic"

  val KEY_KAFKA_SEMANTIC = "semantic"

  val KEY_KAFKA_PATTERN = "pattern"

  val KEY_KAFKA_START_FROM = "start.from"

  val KEY_KAFKA_START_FROM_OFFSET = "offset"

  val KEY_KAFKA_START_FROM_TIMESTAMP = "timestamp"


  val KEY_ALIAS = "alias"

  /**
   * about config jdbc...
   */
  val KEY_JDBC_PREFIX = "jdbc."

  val KEY_JDBC_DRIVER = "driverClassName"

  val KEY_JDBC_DATABASE = "database"

  val KEY_JDBC_URL = "jdbcUrl"

  val KEY_JDBC_USER = "username"

  val KEY_JDBC_PASSWORD = "password"

  val KEY_JDBC_INSERT_BATCH = "batch.size"

  val KEY_JDBC_INSERT_BATCH_DELAYTIME = "batch.delayTime"

  val DEFAULT_JDBC_INSERT_BATCH = 1

  val DEFAULT_JDBC_INSERT_BATCH_DELAYTIME = 1000L

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
   * flink config key
   */
  val KEY_FLINK_APPLICATION_ARGS = "$internal.application.program-args"

  val KEY_FLINK_APPLICATION_MAIN_CLASS = "$internal.application.main"

  val KEY_FLINK_JM_PROCESS_MEMORY = "jobmanager.memory.process.size"

  val KEY_FLINK_TM_PROCESS_MEMORY = "taskmanager.memory.process.size"

  val KEY_FLINK_TOTAL_MEMORY = "jobmanager.memory.flink.size"

  val KEY_FLINK_JVM_HEAP_MEMORY = "jobmanager.memory.heap.size"

  val KEY_FLINK_JVM_OFF_HEAP_MEMORY = "jobmanager.memory.off-heap.size"

  val STREAMX_FLINKSQL_CLIENT_CLASS = "com.streamxhub.streamx.flink.cli.SqlClient"

  def printLogo(info: String): Unit = {
    // scalastyle:off println
    println("\n\n                 .+.                                ")
    println("           _____/ /_________  ____ _____ ___  _  __     ")
    println("          / ___/ __/ ___/ _ \\/ __ `/ __ `__ \\| |/_/   ")
    println("         (__  ) /_/ /  /  __/ /_/ / / / / / />  <       ")
    println("        /____/\\__/_/   \\___/\\__,_/_/ /_/ /_/_/|_|    ")
    println("                                              |/        ")
    println("                                              .         ")
    println("\n       WebSite:  http://www.streamxhub.com            ")
    println("       GitHub :  https://github.com/streamxhub/streamx  ")
    println("       Gitee  :  https://gitee.com/streamxhub/streamx   ")
    println("       Ver    :  1.2.4                                  ")
    println(s"       Info   :  $info                                 ")
    println(s"       Time   :  ${LocalDateTime.now}              \n\n")
    // scalastyle:on println
  }

}



