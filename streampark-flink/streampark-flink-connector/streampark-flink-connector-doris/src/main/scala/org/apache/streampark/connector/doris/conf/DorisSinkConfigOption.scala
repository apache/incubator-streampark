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

package org.apache.streampark.connector.doris.conf

import org.apache.streampark.common.conf.ConfigOption
import org.apache.streampark.common.util.ConfigUtils

import java.util.Properties

import org.apache.streampark.common.util.Implicits._

object DorisSinkConfigOption {
  val DORIS_SINK_PREFIX = "doris.sink"

  def apply(
      prefixStr: String = DORIS_SINK_PREFIX,
      properties: Properties = new Properties): DorisSinkConfigOption =
    new DorisSinkConfigOption(prefixStr, properties)
}

class DorisSinkConfigOption(prefixStr: String, properties: Properties) extends Serializable {

  implicit val (prefix, prop) = (prefixStr, properties)

  val SIGN_COMMA = ","

  val loadUrl: ConfigOption[List[String]] = ConfigOption[List[String]](
    key = "load_url",
    required = true,
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

  val loadFormat: ConfigOption[String] = ConfigOption(
    key = "loadFormat",
    required = false,
    defaultValue = "csv",
    classType = classOf[String])

  val password: ConfigOption[String] =
    ConfigOption(key = "password", required = true, classType = classOf[String])

  val database: ConfigOption[String] =
    ConfigOption(key = "database", required = false, defaultValue = "", classType = classOf[String])

  val table: ConfigOption[String] =
    ConfigOption(key = "table", required = false, defaultValue = "", classType = classOf[String])

  val sinkOfferTimeout: ConfigOption[Long] = ConfigOption(
    key = "sinkOfferTimeout",
    required = false,
    defaultValue = 3000L,
    classType = classOf[Long],
    handle = k => {
      properties.remove(k).toString.toLong
    })

  val rowDelimiter: ConfigOption[String] = ConfigOption(
    key = "properties.row_delimiter",
    required = false,
    defaultValue = "\n",
    classType = classOf[String])

  val flushInterval: ConfigOption[Long] = ConfigOption(
    key = "flushInterval",
    required = false,
    defaultValue = 300000L,
    classType = classOf[Long],
    handle = k => {
      properties.remove(k).toString.toLong
    })

  val connectTimeout: ConfigOption[Int] = ConfigOption(
    key = "connectTimeout",
    required = false,
    defaultValue = 5000,
    classType = classOf[Long])

  val maxRequestRetry: ConfigOption[Int] = ConfigOption(
    key = "maxRequestRetry",
    required = false,
    defaultValue = 1,
    classType = classOf[Long])

  val maxConnections: ConfigOption[Int] = ConfigOption(
    key = "maxConnections",
    required = false,
    defaultValue = -1,
    classType = classOf[Int])

  val maxRow: ConfigOption[Int] =
    ConfigOption(key = "maxRow", required = false, defaultValue = 100000, classType = classOf[Int])

  val maxBytes: ConfigOption[Int] = ConfigOption(
    key = "maxRow",
    required = false,
    defaultValue = 94371840,
    classType = classOf[Int])

  val maxRetries: ConfigOption[Int] =
    ConfigOption(key = "maxRetries", required = false, defaultValue = 1, classType = classOf[Int])

  val labelPrefix: ConfigOption[String] = ConfigOption(
    key = "labelPrefix",
    required = false,
    defaultValue = "doris",
    classType = classOf[String])

  val semantic: ConfigOption[String] = ConfigOption(
    key = "semantic",
    required = false,
    defaultValue = "AT_LEAST_ONCE",
    classType = classOf[String])

  def getInternalConfig(): Properties = {
    ConfigUtils.getConf(prop, prefix)("")
  }
  def getInternalProperties(): Properties = {
    ConfigUtils.getConf(prop, prefix)(".properties")
  }

}
