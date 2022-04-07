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

package com.streamxhub.streamx.flink.connector.redis.bean

import com.streamxhub.streamx.common.enums.ApiType
import com.streamxhub.streamx.common.enums.ApiType.ApiType
import com.streamxhub.streamx.flink.connector.function.TransformFunction
import org.apache.flink.streaming.connectors.redis.common.mapper.{RedisCommand, RedisCommandDescription, RedisMapper => BahirRedisMapper}

object RedisMapper {

  def map[T](cmd: RedisCommand,
             additionalKey: String,
             scalaKeyFun: T => String,
             scalaValueFun: T => String): RedisMapper[T] = {
    require(cmd != null, () => s"redis cmd  insert failoverTable must not null")
    require(additionalKey != null, () => s"redis additionalKey  insert failoverTable must not null")
    require(scalaKeyFun != null, () => s"redis scalaKeyFun  insert failoverTable must not null")
    require(scalaValueFun != null, () => s"redis scalaValueFun  insert failoverTable must not null")
    new RedisMapper[T](cmd, additionalKey, scalaKeyFun, scalaValueFun)
  }

  def map[T](cmd: RedisCommand,
             additionalKey: String,
             javaKeyFun: TransformFunction[T, String],
             javaValueFun: TransformFunction[T, String]): RedisMapper[T] = {
    require(cmd != null, () => s"redis cmd  insert failoverTable must not null")
    require(additionalKey != null, () => s"redis additionalKey  insert failoverTable must not null")
    require(javaKeyFun != null, () => s"redis javaKeyFun  insert failoverTable must not null")
    require(javaValueFun != null, () => s"redis javaValueFun  insert failoverTable must not null")
    new RedisMapper[T](cmd, additionalKey, javaKeyFun, javaValueFun)
  }
}

class RedisMapper[T](apiType: ApiType = ApiType.scala, cmd: RedisCommand, additionalKey: String) extends BahirRedisMapper[T] {

  private[this] var scalaKeyFun: T => String = _
  private[this] var scalaValueFun: T => String = _
  private[this] var javaKeyFun: TransformFunction[T, String] = _
  private[this] var javaValueFun: TransformFunction[T, String] = _

  //for scala
  def this() = {
    this(ApiType.scala, null, "")
  }

  //for scala
  def this(cmd: RedisCommand,
           additionalKey: String,
           scalaKeyFun: T => String,
           scalaValueFun: T => String) = {
    this(ApiType.scala, cmd, additionalKey)
    this.scalaKeyFun = scalaKeyFun
    this.scalaValueFun = scalaValueFun
  }

  //for scala
  def this(cmd: RedisCommand,
           additionalKey: String,
           javaKeyFun: TransformFunction[T, String],
           javaValueFun: TransformFunction[T, String]) = {
    this(ApiType.java, cmd, additionalKey)
    this.javaKeyFun = javaKeyFun
    this.javaValueFun = javaValueFun
  }


  override def getCommandDescription: RedisCommandDescription = new RedisCommandDescription(cmd, additionalKey)

  override def getKeyFromData(r: T): String = apiType match {
    case ApiType.java => javaKeyFun.transform(r)
    case ApiType.scala => scalaKeyFun(r)
  }

  override def getValueFromData(r: T): String = apiType match {
    case ApiType.java => javaValueFun.transform(r)
    case ApiType.scala => scalaValueFun(r)
  }

}
