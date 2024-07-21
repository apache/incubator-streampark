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

import org.apache.streampark.flink.connector.conf.ThresholdConf

import org.apache.streampark.common.Constant
import java.util.{Base64, Properties}
import java.util.concurrent.ThreadLocalRandom

import org.apache.streampark.common.util.Implicits._

/**
 * Flink sink for Clickhouse database. Powered by Async Http Client.
 *
 * High-performance library for loading data to Clickhouse.
 *
 * It has two triggers for loading data: by timeout and by buffer size.
 */
//---------------------------------------------------------------------------------------

class ClickHouseHttpConfig(parameters: Properties)
  extends ThresholdConf(ClickHouseSinkConfigOption.CLICKHOUSE_SINK_PREFIX, parameters) {

  @transient val sinkOption: ClickHouseSinkConfigOption =
    ClickHouseSinkConfigOption(properties = parameters)

  val user: String = sinkOption.user.get()

  val password: String = sinkOption.password.get()

  val hosts: List[String] = sinkOption.hosts.get()

  var currentHostId: Int = 0

  val credentials: String = (user, password) match {
    case (null, null) => null
    case (u, p) => new String(Base64.getEncoder.encode(s"$u:$p".getBytes))
  }

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

  override def toString: String = {
    s"""
       |{ user: $user, password: ${Constant.DEFAULT_DATAMASK_STRING}, hosts: ${hosts.mkString(",")} }
       |""".stripMargin
  }
}
