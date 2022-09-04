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


import java.util.Properties

class RedisConfig(parameters: Properties) extends Serializable {

  val sinkOption: RedisSinkConfigOption = RedisSinkConfigOption(properties = parameters)

  val connectType: String = sinkOption.connectType.get()

  val host: String = sinkOption.host.get()

  val port: Int = sinkOption.port.get()

  val sentinels: Set[String] = if (connectType.equals(sinkOption.DEFAULT_CONNECT_TYPE)) Set() else {
    host.split(sinkOption.SIGN_COMMA).map(x => {
      if (x.contains(sinkOption.SIGN_COLON)) x; else {
        throw new IllegalArgumentException(s"redis sentinel host invalid {$x} must match host:port ")
      }
    }).toSet
  }

}
