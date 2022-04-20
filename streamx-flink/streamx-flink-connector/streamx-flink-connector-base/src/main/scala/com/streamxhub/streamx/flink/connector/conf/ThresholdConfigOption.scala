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

package com.streamxhub.streamx.flink.connector.conf

import com.streamxhub.streamx.common.conf.ConfigOption
import com.streamxhub.streamx.flink.connector.conf
import com.streamxhub.streamx.flink.connector.conf.FailoverStorageType.FailoverStorageType

import java.util.Properties

object ThresholdConfigOption {
  def apply(prefixStr: String, properties: Properties = new Properties): ThresholdConfigOption = new ThresholdConfigOption(prefixStr, properties)
}

class ThresholdConfigOption(prefixStr: String, properties: Properties) {

  implicit val (prefix, prop) = (prefixStr, properties)

  val SIGN_COMMA = ","

  val bufferSize: ConfigOption[Int] = ConfigOption(
    key = "threshold.bufferSize",
    required = false,
    defaultValue = 1000,
    classType = classOf[Int]
  )

  val queueCapacity: ConfigOption[Int] = ConfigOption(
    key = "threshold.queueCapacity",
    required = false,
    defaultValue = 10000,
    classType = classOf[Int]
  )

  val delayTime: ConfigOption[Long] = ConfigOption(
    key = "threshold.delayTime",
    required = false,
    defaultValue = 1000L,
    classType = classOf[Long]
  )

  val timeout: ConfigOption[Int] = ConfigOption(
    key = "threshold.requestTimeout",
    required = false,
    defaultValue = 2000,
    classType = classOf[Int]
  )

  val numWriters: ConfigOption[Int] = ConfigOption(
    key = "threshold.numWriters",
    required = false,
    defaultValue = Runtime.getRuntime.availableProcessors(),
    classType = classOf[Int]
  )

  val maxRetries: ConfigOption[Int] = ConfigOption(
    key = "threshold.retries",
    required = false,
    defaultValue = 3,
    classType = classOf[Int]
  )

  val storageType: ConfigOption[conf.FailoverStorageType.Value] = ConfigOption(
    key = "failover.storage",
    required = false,
    classType = classOf[FailoverStorageType],
    defaultValue = FailoverStorageType.NONE,
    handle = k => {
      FailoverStorageType.get(properties.getProperty(k))
    }
  )


  val failoverTable: ConfigOption[String] = ConfigOption(
    key = "failover.table",
    required = false,
    defaultValue = "",
    classType = classOf[String]
  )


}

