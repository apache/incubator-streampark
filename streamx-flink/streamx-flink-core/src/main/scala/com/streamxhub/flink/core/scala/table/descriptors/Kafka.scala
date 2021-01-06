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
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.table.descriptors.{Kafka => KFK}

import scala.collection.JavaConversions._

object Kafka {

  def apply(version: String, topic: String, alias: String = "")(implicit parameter: ParameterTool): KFK = {
    val prop = ConfigUtils.getConf(parameter.toMap, KAFKA_SOURCE_PREFIX)
    require(prop != null && prop.nonEmpty && prop.exists(x => x._1 == KEY_KAFKA_TOPIC))
    val kafka = new KFK()
    kafka.version(version)
    kafka.topic(topic)
    prop.foreach(p => kafka.property(p._1, p._2))
    kafka
  }

}
