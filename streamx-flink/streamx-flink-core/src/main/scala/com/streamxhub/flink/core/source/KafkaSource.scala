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

import com.streamxhub.common.util.ConfigUtils
import org.apache.flink.api.common.serialization.{DeserializationSchema,SimpleStringSchema}
import org.apache.flink.streaming.api.scala.{DataStream, _}
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011
import com.streamxhub.flink.core.StreamingContext
import org.apache.kafka.clients.consumer.ConsumerConfig

import scala.annotation.meta.param
import scala.collection.JavaConversions._
import scala.collection.Map

object KafkaSource {

  def apply(@(transient@param) ctx: StreamingContext, overrideParams: Map[String, String] = Map.empty[String, String]): KafkaSource = new KafkaSource(ctx, overrideParams)

}
/*
 * @param ctx
 * @param overrideParams
 */
class KafkaSource(@(transient@param) val ctx: StreamingContext, overrideParams: Map[String, String] = Map.empty[String, String]) {

  /**
   *
   * commit offset 方式:<br/>
   *    &nbsp;&nbsp;Flink kafka consumer commit offset 方式需要区分是否开启了 checkpoint。<br/>
   *  &nbsp;&nbsp; 1) checkpoint 关闭: commit offset 要依赖于 kafka 客户端的 auto commit。
   *  需设置 enable.auto.commit，auto.commit.interval.ms 参数到 consumer properties，
   *  就会按固定的时间间隔定期 auto commit offset 到 kafka。<br/>
   * &nbsp;&nbsp; 2) checkpoint 开启: 这个时候作业消费的 offset 是 Flink 在 state 中自己管理和容错。
   *    此时提交 offset 到 kafka，一般都是作为外部进度的监控，想实时知道作业消费的位置和 lag 情况。
   *    此时需要 setCommitOffsetsOnCheckpoints 为 true 来设置当 checkpoint 成功时提交 offset 到 kafka。
   *    此时 commit offset 的间隔就取决于 checkpoint 的间隔
   *
   * 获取DStream 流
   * @param topics topic集合
   * @param alias 别名,区分不同的kafka连接实例
   * @param deserializer DeserializationSchema
   * @tparam T
   */
  def getDataStream[T](topics: List[String], alias: String = "",deserializer: DeserializationSchema[T] = new SimpleStringSchema().asInstanceOf[DeserializationSchema[T]]): DataStream[T] = {
    require(topics.nonEmpty,"[Streamx-Flink] topics can not be null")
    val prop = ConfigUtils.getKafkaSourceConf(ctx.paramMap, topics.head, alias)
    overrideParams.foreach(x => prop.put(x._1, x._2))
    val consumer = new FlinkKafkaConsumer011(topics, deserializer, prop)
    val enableChk = ctx.getCheckpointConfig.isCheckpointingEnabled
    val autoCommit = prop.getOrElse(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true").toBoolean
    (enableChk, autoCommit) match {
      case (true, _) => consumer.setCommitOffsetsOnCheckpoints(true)
      case (_, false) => throw new IllegalArgumentException("[StreamX] error:flink checkpoint was disable,and kafka autoCommit was false.you can enable checkpoint or enable kafka autoCommit...")
      case _ =>
    }
    ctx.addSource(consumer)
  }

}