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

import ClickHouseConfigConst.{CLICKHOUSE_HOSTS, CLICKHOUSE_PASSWORD, CLICKHOUSE_USER, SIGN_COMMA}
import com.streamxhub.streamx.flink.connector.failover.ThresholdConf

import java.util.concurrent.ThreadLocalRandom
import java.util.{Base64, Properties}
import scala.collection.JavaConversions._


/**
 *
 * Flink sink for Clickhouse database. Powered by Async Http Client.
 *
 * High-performance library for loading data to Clickhouse.
 *
 * It has two triggers for loading data: by timeout and by buffer size.
 *
 */
//---------------------------------------------------------------------------------------

class ClickHouseConfig(parameters: Properties) extends ThresholdConf(parameters) {
  var currentHostId: Int = 0
  val credentials: String = (parameters.getProperty(CLICKHOUSE_USER), parameters.getProperty(CLICKHOUSE_PASSWORD)) match {
    case (null, null) => null
    case (u, p) => new String(Base64.getEncoder.encode(s"$u:$p".getBytes))
  }
  val hosts: java.util.List[String] = parameters.getOrElse(CLICKHOUSE_HOSTS, "")
    .split(SIGN_COMMA)
    .filter(_.nonEmpty)
    .map(_.replaceAll("\\s+", "").replaceFirst("^http://|^", "http://"))
    .toList

  require(hosts.nonEmpty)

  def getRandomHostUrl: String = {
    currentHostId = ThreadLocalRandom.current.nextInt(hosts.size)
    hosts.get(currentHostId)
  }

  def nextHost: String = {
    if (currentHostId >= hosts.size - 1) {
      currentHostId = 0
    } else {
      currentHostId += 1
    }
    hosts.get(currentHostId)
  }

}
