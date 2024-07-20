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

package org.apache.streampark.flink.connector.conf

import org.apache.streampark.common.util.ConfigUtils
import org.apache.streampark.flink.connector.conf.FailoverStorageType.{Console, FailoverStorageType, Kafka, MySQL, NONE}

import java.util.Properties

import org.apache.streampark.common.util.Implicits._

case class ThresholdConf(prefixStr: String, parameters: Properties) {

  private val option: ThresholdConfigOption = ThresholdConfigOption(prefixStr, parameters)

  val bufferSize: Int = option.bufferSize.get()
  val queueCapacity: Int = option.queueCapacity.get()
  val delayTime: Long = option.delayTime.get()
  val timeout: Int = option.timeout.get()
  val numWriters: Int = option.numWriters.get()
  val maxRetries: Int = option.maxRetries.get()
  val storageType: FailoverStorageType = option.storageType.get()
  val failoverTable: String = option.failoverTable.get()

  def getFailoverConfig: Properties = {
    storageType match {
      case Console | NONE => null
      case Kafka => ConfigUtils.getConf(parameters.toMap, "failover.kafka.")
      case MySQL => ConfigUtils.getConf(parameters.toMap, "failover.mysql.")
      case _ =>
        throw new IllegalArgumentException(
          s"[StreamPark] usage error! failover.storage must not be null! ")
    }
  }
}

object FailoverStorageType extends Enumeration {
  type FailoverStorageType = Value
  val Console, MySQL, Kafka, NONE = Value

  def get(key: String): Value = values.find(_.toString.equalsIgnoreCase(key)).get

}
