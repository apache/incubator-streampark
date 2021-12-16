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

package com.streamxhub.streamx.spark.core.support.kafka.offset

import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf

/**
  *
  *
  * 默认Offset管理，不做任何操作
  */
private[kafka] class DefaultOffset(val sparkConf: SparkConf) extends Offset {

  /**
    * 获取存储的Offset
    *
    * @param groupId
    * @param topics
    * @return
    */
  override def get(groupId: String, topics: Set[String]): Map[TopicPartition, Long] = {
    Map.empty[TopicPartition, Long]
  }

  /**
    * 更新 Offsets
    *
    * @param groupId
    * @param offsetInfos
    */
  override def update(groupId: String, offsetInfos: Map[TopicPartition, Long]): Unit = {

  }

  /**
    * 删除 Offsets
    *
    * @param groupId
    * @param topics
    */
  override def delete(groupId: String, topics: Set[String]): Unit = {

  }
}
