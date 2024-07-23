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

package org.apache.streampark.flink.connector.elasticsearch6.conf

import org.apache.streampark.common.conf.ConfigOption
import org.apache.streampark.common.util.ConfigUtils

import org.apache.http.HttpHost

import java.net.InetSocketAddress
import java.util.Properties

import org.apache.streampark.common.util.Implicits._

object ESSinkConfigOption {

  val ES_SINK_PREFIX = "es.sink"

  /**
   * @param properties
   * @return
   */
  def apply(
      prefixStr: String = ES_SINK_PREFIX,
      properties: Properties = new Properties): ESSinkConfigOption =
    new ESSinkConfigOption(prefixStr, properties)

}

class ESSinkConfigOption(prefixStr: String, properties: Properties) extends Serializable {

  implicit val (prefix, prop) = (prefixStr, properties)

  val SIGN_COMMA = ","

  val SIGN_COLON = ":"

  val disableFlushOnCheckpoint: ConfigOption[Boolean] = ConfigOption(
    key = "es.disableFlushOnCheckpoint",
    required = false,
    classType = classOf[Boolean],
    defaultValue = false)

  val host: ConfigOption[Array[HttpHost]] = ConfigOption(
    key = "host",
    required = true,
    classType = classOf[Array[InetSocketAddress]],
    handle = key =>
      properties
        .getProperty(key)
        .split(SIGN_COMMA)
        .map(
          x => {
            x.split(SIGN_COLON) match {
              case Array(host, port) => new HttpHost(host, port.toInt)
            }
          })
  )

  val userName: ConfigOption[String] = ConfigOption(
    key = "es.auth.user",
    required = false,
    classType = classOf[String],
    defaultValue = null)

  val password: ConfigOption[String] = ConfigOption(
    key = "es.auth.password",
    required = false,
    classType = classOf[String],
    defaultValue = null)

  val connectRequestTimeout: ConfigOption[Int] = ConfigOption(
    key = "es.connect.request.timeout",
    required = false,
    classType = classOf[Int],
    defaultValue = -1)

  val connectTimeout: ConfigOption[Int] = ConfigOption(
    key = "es.connect.timeout",
    required = false,
    classType = classOf[Int],
    defaultValue = -1)

  val maxRetry: ConfigOption[Int] = ConfigOption(
    key = "es.rest.max.retry.timeout",
    required = false,
    classType = classOf[Int],
    defaultValue = 10000)

  val contentType: ConfigOption[String] = ConfigOption(
    key = "es.rest.content.type",
    required = false,
    classType = classOf[String],
    defaultValue = "application/json")

  val pathPrefix: ConfigOption[String] = ConfigOption(
    key = "es.rest.path.prefix",
    required = false,
    classType = classOf[String],
    defaultValue = null)

  val staleConnectionCheckEnabled: ConfigOption[Boolean] = ConfigOption(
    key = "es.connect.check.enable",
    required = false,
    classType = classOf[Boolean],
    defaultValue = false)

  val redirectsEnabled: ConfigOption[Boolean] = ConfigOption(
    key = "es.redirects.enable",
    required = false,
    classType = classOf[Boolean],
    defaultValue = false)

  val maxRedirects: ConfigOption[Int] = ConfigOption(
    key = "es.max.redirects",
    required = false,
    classType = classOf[Int],
    defaultValue = 50)

  val relativeRedirectsAllowed: ConfigOption[Boolean] = ConfigOption(
    key = "es.relative.redirects.allowed",
    required = false,
    classType = classOf[Boolean],
    defaultValue = true)

  val authenticationEnabled: ConfigOption[Boolean] = ConfigOption(
    key = "es.authentication.enable",
    required = false,
    classType = classOf[Boolean],
    defaultValue = true)

  val socketTimeout: ConfigOption[Int] = ConfigOption(
    key = "es.socket.timeout",
    required = false,
    classType = classOf[Int],
    defaultValue = -1)

  val contentCompressionEnabled: ConfigOption[Boolean] = ConfigOption(
    key = "es.content.compression.enable",
    required = false,
    classType = classOf[Boolean],
    defaultValue = true)

  val normalizeUri: ConfigOption[Boolean] = ConfigOption(
    key = "es.normalize.uri",
    required = false,
    classType = classOf[Boolean],
    defaultValue = true)

  def getInternalConfig(): JavaMap[String, String] = {
    ConfigUtils.getConf(prop, prefix)(alias = "")
  }

}
