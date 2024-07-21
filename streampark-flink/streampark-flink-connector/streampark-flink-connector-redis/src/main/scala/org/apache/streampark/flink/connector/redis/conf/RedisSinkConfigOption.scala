/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.flink.connector.redis.conf

import org.apache.streampark.common.conf.ConfigOption
import org.apache.streampark.common.util.ConfigUtils

import java.util.Properties

import org.apache.streampark.common.util.Implicits._

object RedisSinkConfigOption {
  val REDIS_SINK_PREFIX = "redis.sink"

  /**
   * @param properties
   * @return
   */
  def apply(
      prefixStr: String = REDIS_SINK_PREFIX,
      properties: Properties = new Properties): RedisSinkConfigOption =
    new RedisSinkConfigOption(prefixStr, properties)

}

class RedisSinkConfigOption(prefixStr: String, properties: Properties) extends Serializable {

  implicit val (prefix, prop) = (prefixStr, properties)

  val DEFAULT_CONNECT_TYPE: String = "jedisPool"

  val SIGN_COMMA = ","

  val SIGN_COLON = ":"

  val host: ConfigOption[String] = ConfigOption(
    key = "host",
    required = true,
    classType = classOf[String],
    handle = key => {
      properties.remove(key).toString
    })

  val connectType: ConfigOption[String] = ConfigOption(
    key = "connectType",
    required = false,
    defaultValue = DEFAULT_CONNECT_TYPE,
    classType = classOf[String],
    handle = k => {
      val value: String = properties
        .remove(k)
        .toString
      if (value == null || value.isEmpty) DEFAULT_CONNECT_TYPE else value
    }
  )

  val port: ConfigOption[Int] = ConfigOption(
    key = "port",
    required = false,
    defaultValue = 6379,
    classType = classOf[Int],
    handle = k => {
      val value: String = properties
        .remove(k)
        .toString
      if (value == null || value.isEmpty) 6379 else value.toInt
    }
  )

  def getInternalConfig(): Properties = {
    ConfigUtils.getConf(prop, prefix)(alias = "")
  }

}
