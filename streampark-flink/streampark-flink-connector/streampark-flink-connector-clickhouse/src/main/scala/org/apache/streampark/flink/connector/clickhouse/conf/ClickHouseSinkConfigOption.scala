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

package org.apache.streampark.flink.connector.clickhouse.conf

import org.apache.streampark.common.Constant
import org.apache.streampark.common.conf.ConfigOption
import org.apache.streampark.common.util.ConfigUtils

import org.asynchttpclient.config.AsyncHttpClientConfigDefaults

import java.util.Properties

import org.apache.streampark.common.util.Implicits._

object ClickHouseSinkConfigOption {

  val CLICKHOUSE_SINK_PREFIX = "clickhouse.sink"

  /**
   * @param properties
   * @return
   */
  def apply(
      prefixStr: String = CLICKHOUSE_SINK_PREFIX,
      properties: Properties = new Properties): ClickHouseSinkConfigOption =
    new ClickHouseSinkConfigOption(prefixStr, properties)

}

class ClickHouseSinkConfigOption(prefixStr: String, properties: Properties) extends Serializable {

  implicit val (prefix, prop) = (prefixStr, properties)

  val SIGN_COMMA = ","

  val hosts: ConfigOption[List[String]] = ConfigOption[List[String]](
    key = "hosts",
    required = false,
    defaultValue = List(),
    classType = classOf[List[String]],
    handle = k => {
      properties
        .getProperty(k)
        .split(SIGN_COMMA)
        .filter(_.nonEmpty)
        .map(_.replaceAll("\\s+", "").replaceFirst("^http://|^", Constant.HTTP_SCHEMA))
        .toList
    }
  )

  val user: ConfigOption[String] =
    ConfigOption(key = "user", required = true, classType = classOf[String])

  val password: ConfigOption[String] =
    ConfigOption(key = "password", required = false, defaultValue = "", classType = classOf[String])

  val database: ConfigOption[String] = ConfigOption(
    key = "database",
    required = true,
    defaultValue = Constant.DEFAULT,
    classType = classOf[String])

  val requestTimeout: ConfigOption[Int] = ConfigOption(
    key = "requestTimeout",
    required = false,
    defaultValue = AsyncHttpClientConfigDefaults.defaultRequestTimeout,
    classType = classOf[Int])

  val connectTimeout: ConfigOption[Int] = ConfigOption(
    key = "connectTimeout",
    required = false,
    defaultValue = AsyncHttpClientConfigDefaults.defaultConnectTimeout(),
    classType = classOf[Long])

  val maxRequestRetry: ConfigOption[Int] = ConfigOption(
    key = "maxRequestRetry",
    required = false,
    defaultValue = AsyncHttpClientConfigDefaults.defaultMaxRequestRetry(),
    classType = classOf[Long])

  val maxConnections: ConfigOption[Int] = ConfigOption(
    key = "maxConnections",
    required = false,
    defaultValue = AsyncHttpClientConfigDefaults.defaultMaxConnections(),
    classType = classOf[Int])

  val failoverTable: ConfigOption[String] =
    ConfigOption(key = "failover.table", required = false, classType = classOf[String])

  val jdbcUrl: ConfigOption[String] =
    ConfigOption(key = "jdbcUrl", required = false, classType = classOf[String])

  val driverClassName: ConfigOption[String] = ConfigOption(
    key = "driverClassName",
    required = false,
    defaultValue = null,
    classType = classOf[String])

  val batchSize: ConfigOption[Int] =
    ConfigOption(key = "batchSize", required = false, defaultValue = 1, classType = classOf[Int])

  val flushInterval: ConfigOption[Long] = ConfigOption(
    key = "flushInterval",
    required = false,
    defaultValue = 1000L,
    classType = classOf[Long],
    handle = k => {
      properties.remove(k).toString.toLong
    })

  def getInternalConfig(): Properties = {
    ConfigUtils.getConf(prop, prefix)("")
  }

}
