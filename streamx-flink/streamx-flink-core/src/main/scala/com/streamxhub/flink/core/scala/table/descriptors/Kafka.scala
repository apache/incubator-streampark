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
package com.streamxhub.flink.core.scala.table.descriptors

import com.streamxhub.common.conf.ConfigConst.{KAFKA_SOURCE_PREFIX, KEY_KAFKA_TOPIC}
import com.streamxhub.common.util.ConfigUtils
import com.streamxhub.flink.core.scala.table.descriptors.KafkaVer.KafkaVer
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.table.descriptors.{Kafka => KFK}

import scala.collection.JavaConversions._

object Kafka {

  def apply(topic: String, version: KafkaVer = KafkaVer.UNIVERSAL, alias: String = "")(implicit parameter: ParameterTool): KFK = {
    val prop = ConfigUtils.getConf(parameter.toMap, KAFKA_SOURCE_PREFIX)
    require(version != null)
    require(prop != null && prop.nonEmpty && prop.exists(x => x._1 == KEY_KAFKA_TOPIC))
    val kafka = new KFK()
    version match {
      case KafkaVer.`010` => kafka.version("0.10")
      case KafkaVer.`011` => kafka.version("0.11")
      case _ => kafka.version("universal")
    }
    kafka.topic(topic)
    prop.foreach(p => kafka.property(p._1, p._2))
    kafka
  }

}

object KafkaVer extends Enumeration {
  type KafkaVer = Value
  val `010`, `011`, UNIVERSAL = Value
}
