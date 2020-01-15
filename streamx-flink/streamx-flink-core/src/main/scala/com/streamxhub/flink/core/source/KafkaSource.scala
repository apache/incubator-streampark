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
package com.streamxhub.flink.core.source

import com.streamxhub.common.conf.ConfigConst
import com.streamxhub.common.util.ConfigUtils
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.scala.{DataStream, _}
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011
import com.streamxhub.flink.core.StreamingContext

import scala.collection.Map
import scala.language.postfixOps

class KafkaSource(@transient val ctx: StreamingContext, specialKafkaParams: Map[String, String] = Map.empty[String, String]) {

  /**
   * 获取DStream 流
   *
   * @return
   */
  def getDataStream(topic: String = "", instance: String = ""): DataStream[String] = {
    val prop = ConfigUtils.getKafkaSourceConf(ctx.parameter.toMap, topic, instance)
    specialKafkaParams.foreach(x => prop.put(x._1, x._2))
    val consumer = new FlinkKafkaConsumer011[String](prop.remove(ConfigConst.KEY_KAFKA_TOPIC).toString, new SimpleStringSchema(), prop)
    ctx.addSource(consumer)
  }
}