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
package com.streamxhub.flink.core.failover

import java.util.Properties

import com.streamxhub.common.conf.ConfigConst._
import com.streamxhub.common.conf.FailoverStorageType
import com.streamxhub.common.conf.FailoverStorageType.{FailoverStorageType, HBase, HDFS, Kafka, MySQL}
import com.streamxhub.common.util.ConfigUtils

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

case class FailoverConf(parameters: Properties) {
  val numWriters: Int = parameters(KEY_SINK_THRESHOLD_NUM_WRITERS).toInt
  val queueMaxCapacity: Int = parameters(KEY_SINK_THRESHOLD_QUEUE_CAPACITY).toInt
  val checkTimeout: Long = parameters(KEY_SINK_THRESHOLD_CHECK_TIME).toLong
  val maxRetries: Int = parameters(KEY_SINK_THRESHOLD_RETRIES).toInt
  val failoverStorage: FailoverStorageType = FailoverStorageType.get(parameters.getOrElse(KEY_SINK_FAILOVER_STORAGE, throw new IllegalArgumentException(s"[Streamx] usage error! failover.storage muse be not null! ")))

  require(queueMaxCapacity > 0)
  require(numWriters > 0)
  require(checkTimeout > 0)
  require(maxRetries > 0)

  def getFailoverConfig: Properties = {
    failoverStorage match {
      case Kafka => ConfigUtils.getConf(parameters.toMap.asJava, "failover.kafka.")
      case MySQL => ConfigUtils.getConf(parameters.toMap.asJava, "failover.mysql.")
      case HBase => ConfigUtils.getConf(parameters.toMap.asJava, "failover.hbase.", HBASE_PREFIX)
      case HDFS => ConfigUtils.getConf(parameters.toMap.asJava, "failover.hdfs.")
    }
  }

}