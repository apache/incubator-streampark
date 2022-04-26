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

package com.streamxhub.streamx.flink.connector.pulsar.source

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.util.{ConfigUtils, Utils}
import com.streamxhub.streamx.flink.core.scala.StreamingContext
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.connector.pulsar.common.config.PulsarOptions
import org.apache.flink.connector.pulsar.source.enumerator.cursor.{StartCursor, StopCursor}
import org.apache.flink.connector.pulsar.source.{PulsarSource, PulsarSourceBuilder, PulsarSourceOptions}
import org.apache.flink.connector.pulsar.source.reader.deserializer.PulsarDeserializationSchema
import org.apache.flink.streaming.api.datastream.DataStreamSource
import org.apache.pulsar.client.api.SubscriptionType

import java.io
import java.util.Properties
import java.util.regex.Pattern
import scala.annotation.meta.param
import scala.collection.JavaConversions._
import scala.util.Try

object FlinkPulsarSource {
  def getSource[T: TypeInformation](ctx: StreamingContext,
                                    alias: String,
                                    startCursor: StartCursor = StartCursor.defaultStartCursor(),
                                    stopCursor: StopCursor = StopCursor.defaultStopCursor(),
                                    topic: io.Serializable,
                                    deserializer: PulsarDeserializationSchema[T],
                                    property: Properties = new Properties(),
                                   ): PulsarSource[T] = {

    val prop = ConfigUtils.getConf(ctx.parameter.toMap, PULSAR_SOURCE_PREFIX + alias)
    Utils.copyProperties(property, prop)
    require(prop != null && prop.nonEmpty
      && prop.exists(x => x._1 == KEY_PULSAR_TOPIC || x._1 == KEY_PULSAR_PATTERN))

    //adminUrl serviceUrl ser parameter...
    val adminUrl = Try(Some(prop.remove(s"${PulsarOptions.PULSAR_ADMIN_URL.key()}").toString)).getOrElse(None)
    val serviceUrl = Try(Some(prop.remove(s"${PulsarOptions.PULSAR_SERVICE_URL.key()}").toString)).getOrElse(None)

    val pulsarSourceBuilder: PulsarSourceBuilder[T] = (adminUrl, serviceUrl) match {
      case (Some(adminUrl), Some(serviceUrl)) => PulsarSource.builder[T].setAdminUrl(adminUrl)
        .setServiceUrl(serviceUrl).setDeserializationSchema(deserializer)
      case _ => throw new IllegalArgumentException("[StreamX] The service URL provider of pulsar service and the HTTP URL of pulsar service " +
        "of management endpoint must be defined")
    }

    //startCursor and stopCursor parameter...
    pulsarSourceBuilder.setStartCursor(startCursor).setUnboundedStopCursor(stopCursor)

    //topic parameter
    val topicOpt = Try(Some(prop.remove(KEY_PULSAR_TOPIC).toString)).getOrElse(None)
    val regexOpt = Try(Some(prop.remove(KEY_PULSAR_PATTERN).toString)).getOrElse(None)

    (topicOpt, regexOpt) match {
      case (Some(_), Some(_)) =>
        throw new IllegalArgumentException("[StreamX] topic and regex cannot be defined at the same time")
      case (Some(top), _) =>
        val topics = top.split(",|\\s+")
        val topicList = topic match {
          case null => topics.toList
          case x: String => List(x)
          case x: Array[String] => x.toList
          case x: List[String] => x
          case _ => throw new IllegalArgumentException("[StreamX] topic type must be String(one topic) or List[String](more topic)")
        }
        pulsarSourceBuilder.setTopics(topicList)
      case (_, Some(reg)) =>
        val pattern: Pattern = topic match {
          case null => reg.r.pattern
          case x: String => x.r.pattern
          case _ => throw new IllegalArgumentException("[StreamX] subscriptionPattern type must be String(regex)")
        }
        pulsarSourceBuilder.setTopicPattern(pattern)
    }

    //subscription parameter
    val subscriptionName = Try(Some(prop.remove(s"${PulsarSourceOptions.PULSAR_SUBSCRIPTION_NAME.key()}").toString)).getOrElse(None)
    val subscriptionType = Try(Some(prop.remove(s"${PulsarSourceOptions.PULSAR_SUBSCRIPTION_TYPE.key()}").toString)).getOrElse(None)
    (subscriptionName, subscriptionType) match {
      case (Some(name), _) => pulsarSourceBuilder.setSubscriptionName(name).setSubscriptionType(SubscriptionType.Shared)
      case (Some(name), Some(sType)) => pulsarSourceBuilder.setSubscriptionName(name).setSubscriptionType(sType.toUpperCase match {
        case "EXCLUSIVE" => SubscriptionType.Exclusive
        case "SHARED" => SubscriptionType.Shared
        case "FAILOVER" => SubscriptionType.Failover
        case "KEY_SHARED" => SubscriptionType.Key_Shared
        case _ => SubscriptionType.Shared
      })
      case _ =>
    }
    pulsarSourceBuilder.setProperties(property).build()
  }
}
/*
 * @param ctx
 * @param property
 */
class FlinkPulsarSource(@(transient@param) private[this] val ctx: StreamingContext, property: Properties = new Properties()) {
  /**
   * 获取DStream 流
   *
   * @param topic         一组topic或者单个topic
   * @param alias         别名,区分不同的pulsar连接实例
   * @param startCursor   指定起始消费位置
   * @param stopCursor    指定边界
   * @param deserializer  DeserializationSchema
   * @param strategy      Watermarks 策略
   * @param sourceName    数据源名称
   * @tparam T
   */
  def getDataStream[T: TypeInformation](topic: java.io.Serializable = null,
                                        alias: String = "",
                                        startCursor: StartCursor = StartCursor.defaultStartCursor(),
                                        stopCursor: StopCursor = StopCursor.defaultStopCursor(),
                                        deserializer: PulsarDeserializationSchema[T] = PulsarDeserializationSchema
                                          .flinkSchema(new SimpleStringSchema),
                                        strategy: WatermarkStrategy[T] = WatermarkStrategy.noWatermarks(),
                                        sourceName: String = "Pulsar Source"
                                       ): DataStreamSource[T] = {

    val pulsarSource = FlinkPulsarSource.getSource[T](this.ctx, alias, startCursor, stopCursor, topic, deserializer, property)
    ctx.getJavaEnv.fromSource(pulsarSource, strategy, sourceName);
  }
}
