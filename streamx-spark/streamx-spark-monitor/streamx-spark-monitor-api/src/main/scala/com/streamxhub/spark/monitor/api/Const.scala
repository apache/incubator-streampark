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

package com.streamxhub.spark.monitor.api

object Const {

  val SPARK_CONF_PATH_PREFIX = "/StreamX/spark/conf"

  val SPARK_MONITOR_PATH_PREFIX = "/StreamX/spark/monitor"

  val SPARK_CONF_REGEXP = "(^\\s+|^)spark.app.*"

  val SPARK_APP_CONF_DEFAULT_VERSION = "1"

  val SPARK_PARAM_CONF = "spark.conf"

  val SPARK_PARAM_USER_ARGS = "spark.user.args"

  val SPARK_PARAM_APP_NAME = "spark.app.name"

  val SPARK_PARAM_APP_ID = "spark.app.id"

  val SPARK_PARAM_APP_MYID = "spark.app.myid"

  val SPARK_PARAM_APP_DEBUG = "spark.app.debug"

  val SPARK_PARAM_MONITOR_ZOOKEEPER = "spark.monitor.zookeeper"

  val SPARK_PARAM_APP_CONF_SOURCE = "spark.app.conf.source"

  val SPARK_PARAM_APP_CONF_LOCAL_VERSION = "spark.app.conf.local.version"

  val SPARK_PARAM_APP_CONF_CLOUD_VERSION = "spark.app.conf.cloud.version"


}
