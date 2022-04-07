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

package com.streamxhub.streamx.flink.connector.clickhouse.conf

import com.streamxhub.streamx.common.conf.ConfigOption

import java.util.Properties


case class ClickHouseSinkConfigOption(properties: Properties) {

  implicit val (prefix, prop) = ("clickhouse.sink", properties)

  val hosts = ConfigOption[List[String]](
    key = "hosts",
    required = true,
    classType = classOf[List[String]],
    handle = k => {
      properties
        .getProperty(k)
        .split(",")
        .filter(_.nonEmpty)
        .map(_.replaceAll("\\s+", "").replaceFirst("^http://|^", "http://"))
        .toList
    }
  )

  val user = ConfigOption(
    key = "user",
    required = true,
    classType = classOf[String]
  )

  val password = ConfigOption(
    key = "password",
    required = true,
    classType = classOf[String]
  )

  val targetTable = ConfigOption(
    key = "targetTable",
    required = true,
    classType = classOf[String]
  )

}
