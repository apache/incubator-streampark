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

package com.streamxhub.streamx.spark.core.sink

import com.streamxhub.streamx.spark.core.support.kafka.writer.KafkaWriter._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.Time

import java.util.UUID
import scala.reflect.ClassTag

/**
 *
 *
 * 输出到kafka
 */
class KafkaSink[T: ClassTag](@transient override val sc: SparkContext,
                             initParams: Map[String, String] = Map.empty[String, String])
  extends Sink[T] {

  override val prefix: String = "spark.sink.kafka."

  private lazy val prop = filterProp(param, initParams, prefix)

  private val outputTopic = prop.getProperty("topic")

  /**
   * 以字符串的形式输出到kafka
   *
   */
  override def sink(rdd: RDD[T], time: Time = Time(System.currentTimeMillis())): Unit = {
    rdd.writeToKafka(prop, x => new ProducerRecord[String, String](outputTopic, UUID.randomUUID().toString, x.toString))
  }
}
