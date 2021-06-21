/*
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
package com.streamxhub.streamx.flink.core.scala.failover

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util.ConfigUtils
import com.streamxhub.streamx.flink.core.scala.failover.FailoverStorageType.{FailoverStorageType, HBase, HDFS, Kafka, MySQL}

import java.util.Properties
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.util.Try

case class ThresholdConf(parameters: Properties) {

  val bufferSize: Int = Try(parameters(KEY_SINK_THRESHOLD_BUFFER_SIZE).toInt).getOrElse(DEFAULT_SINK_THRESHOLD_BUFFER_SIZE)
  val queueCapacity: Int = Try(parameters(KEY_SINK_THRESHOLD_QUEUE_CAPACITY).toInt).getOrElse(DEFAULT_SINK_THRESHOLD_QUEUE_CAPACITY)
  val delayTime: Long = Try(parameters(KEY_SINK_THRESHOLD_DELAY_TIME).toLong).getOrElse(DEFAULT_SINK_THRESHOLD_DELAY_TIME)
  val timeout: Int = Try(parameters(KEY_SINK_THRESHOLD_REQ_TIMEOUT).toInt).getOrElse(DEFAULT_SINK_REQUEST_TIMEOUT)
  val successCode: List[Int] = Try(parameters(KEY_SINK_THRESHOLD_SUCCESS_CODE).split(",").map(_.toInt).toList).getOrElse(List(DEFAULT_HTTP_SUCCESS_CODE))
  val numWriters: Int = Try(parameters(KEY_SINK_THRESHOLD_NUM_WRITERS).toInt).getOrElse(DEFAULT_SINK_THRESHOLD_NUM_WRITERS)
  val maxRetries: Int = Try(parameters(KEY_SINK_THRESHOLD_RETRIES).toInt).getOrElse(DEFAULT_SINK_THRESHOLD_RETRIES)
  val storageType: FailoverStorageType = FailoverStorageType.get(parameters.getOrElse(KEY_SINK_FAILOVER_STORAGE, throw new IllegalArgumentException(s"[StreamX] usage error! failover.storage must not be null! ")))

  def getFailoverConfig: Properties = {
    storageType match {
      case Kafka => ConfigUtils.getConf(parameters.toMap.asJava, "failover.kafka.")
      case MySQL => ConfigUtils.getConf(parameters.toMap.asJava, "failover.mysql.")
      case HBase => ConfigUtils.getConf(parameters.toMap.asJava, "failover.hbase.", HBASE_PREFIX)
      case HDFS => ConfigUtils.getConf(parameters.toMap.asJava, "failover.hdfs.")
    }
  }
}

object FailoverStorageType extends Enumeration {
  type FailoverStorageType = Value
  val MySQL, HBase, HDFS, Kafka = Value

  def get(key: String): Value = values.find(_.toString.equalsIgnoreCase(key)).get
}
