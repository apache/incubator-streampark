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
package com.streamxhub.flink.core.sink

import com.streamxhub.common.conf.ConfigConst
import com.streamxhub.common.util.ConfigUtils
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer011
import com.streamxhub.flink.core.StreamingContext

import scala.collection.Map

object KafkaSink {
  def apply(@transient ctx: StreamingContext,
            overwriteParams: Map[String, String] = Map.empty[String, String],
            parallelism: Int = 0,
            name: String = null,
            uid: String = null): KafkaSink = new KafkaSink(ctx, overwriteParams, parallelism, name, uid)
}

class KafkaSink(@transient val ctx: StreamingContext,
                overwriteParams: Map[String, String] = Map.empty[String, String],
                parallelism: Int = 0,
                name: String = null,
                uid: String = null) extends Sink {

  def sink[T](stream: DataStream[String])(implicit topic: String = ""): DataStreamSink[String] = {
    val prop = ConfigUtils.getKafkaSinkConf(ctx.parameter.toMap, topic)
    overwriteParams.foreach(x=>prop.put(x._1,x._2))
    val topicName = prop.getProperty(ConfigConst.KEY_KAFKA_TOPIC)
    val producer = new FlinkKafkaProducer011[String](topicName, new SimpleStringSchema, prop)
    val sink = stream.addSink(producer)
    afterSink(sink, parallelism, name, uid)
  }

}
