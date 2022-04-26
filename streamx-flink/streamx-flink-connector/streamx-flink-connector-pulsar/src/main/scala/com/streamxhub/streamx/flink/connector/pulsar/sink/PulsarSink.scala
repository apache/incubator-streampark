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

package com.streamxhub.streamx.flink.connector.pulsar.sink

import com.streamxhub.streamx.common.conf.ConfigConst.PULSAR_SINK_PREFIX
import com.streamxhub.streamx.common.util.{ConfigUtils, Utils}
import com.streamxhub.streamx.flink.connector.sink.Sink
import com.streamxhub.streamx.flink.core.scala.StreamingContext
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.connector.base.DeliveryGuarantee
import org.apache.flink.connector.pulsar.common.config.PulsarOptions
import org.apache.flink.connector.pulsar.sink.{PulsarSink, PulsarSinkBuilder, PulsarSinkOptions}
import org.apache.flink.connector.pulsar.sink.writer.serializer.PulsarSerializationSchema
import org.apache.flink.connector.pulsar.sink.writer.serializer.PulsarSerializationSchema.{flinkSchema, pulsarSchema}
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.scala._

import java.util.Properties
import scala.annotation.meta.param
import scala.util.Try

class FlinkPulsarSink(@(transient@param) val ctx: StreamingContext,
                 property: Properties = new Properties(),
                 parallelism: Int = 0,
                 name: String = null,
                 uid: String = null) extends Sink {

  def sink[T](stream: DataStream[T],
              alias: String = "",
              topic: String = "",
              serializer: PulsarSerializationSchema[T] = flinkSchema(new SimpleStringSchema)): DataStreamSink[T] = {

    val producer: PulsarSinkBuilder[T] = {
      val prop: Properties = ConfigUtils.getConf(ctx.parameter.toMap, PULSAR_SINK_PREFIX + alias)
      Utils.copyProperties(property, prop)

      //adminUrl serviceUrl ser parameter...
      val adminUrl = Try(Some(prop.remove(s"${PulsarOptions.PULSAR_ADMIN_URL.key()}").toString)).getOrElse(None)
      val serviceUrl = Try(Some(prop.remove(s"${PulsarOptions.PULSAR_SERVICE_URL.key()}").toString)).getOrElse(None)
      val pulsarSinkBuilder: PulsarSinkBuilder[T] = (adminUrl, serviceUrl) match {
        case (Some(adminUrl), Some(serviceUrl)) => PulsarSink.builder().setAdminUrl(adminUrl).setServiceUrl(serviceUrl).setSerializationSchema(serializer).setTopics(topic)
        case _ => throw new IllegalArgumentException("[StreamX] The service URL provider of pulsar service and the HTTP URL of pulsar service " +
          "of management endpoint must be defined")
      }

      val semantic = Try(Some(prop.remove(PulsarSinkOptions.PULSAR_WRITE_DELIVERY_GUARANTEE.key()).toString.toUpperCase)).getOrElse(None) match {
        case None => DeliveryGuarantee.NONE //默认采用NONE
        case Some("AT_LEAST_ONCE") => DeliveryGuarantee.AT_LEAST_ONCE
        case Some("EXACTLY_ONCE") => DeliveryGuarantee.EXACTLY_ONCE
        case Some("NONE") => DeliveryGuarantee.NONE
        case _ => throw new IllegalArgumentException("[StreamX] pulsar.sink semantic error,must be (AT_LEAST_ONCE|EXACTLY_ONCE|NONE) ")
      }
      pulsarSinkBuilder.setDeliveryGuarantee(semantic).setProperties(prop);
    }

    val sink = stream.sinkTo(producer.build())
    afterSink(sink, parallelism, name, uid)
  }
}


