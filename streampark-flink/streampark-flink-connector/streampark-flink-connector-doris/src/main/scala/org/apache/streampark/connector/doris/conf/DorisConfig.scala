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

import org.apache.streampark.common.Constant
import org.apache.streampark.common.conf.ConfigKeys

import java.util.Properties
import org.apache.streampark.common.util.Implicits._

object DorisConfig {

  val CSV = "csv"

  val JSON = "json"

  def apply(properties: Properties = new Properties): DorisConfig = new DorisConfig(properties)

}

class DorisConfig(parameters: Properties) {

  val sinkOption: DorisSinkConfigOption = DorisSinkConfigOption(properties = parameters)

  val user: String = sinkOption.user.get()

  val password: String = sinkOption.password.get()

  val loadUrl: List[String] = sinkOption.loadUrl.get()

  val loadFormat: String = sinkOption.loadFormat.get()

  val rowDelimiter: String = sinkOption.rowDelimiter.get()

  val timeout: Int = sinkOption.connectTimeout.get()

  val sinkMaxRow: Int = sinkOption.maxRow.get()

  val sinkMaxBytes: Int = sinkOption.maxBytes.get()

  val sinkMaxRetries: Int = sinkOption.maxRetries.get()

  val flushInterval: Long = sinkOption.flushInterval.get()

  val sinkOfferTimeout: Long = sinkOption.sinkOfferTimeout.get()

  val labelPrefix: String = sinkOption.labelPrefix.get()

  val semantic: String = sinkOption.semantic.get()

  val database: String = sinkOption.database.get()

  val table: String = sinkOption.table.get()

  def loadProperties: Properties = {
    sinkOption.getInternalProperties()
  }

  def getLoadUrlSize(): Int = {
    loadUrl.size
  }

  var currentHostId: Long = 0

  def getHostUrl: String = {
    currentHostId += 1
    loadUrl.get((currentHostId % loadUrl.size).toInt)
  }

  override def toString: String = {
    s"""
       |{ doris user: $user, password: ${Constant.DEFAULT_DATAMASK_STRING}, hosts: ${loadUrl.mkString(",")} }
       |""".stripMargin
  }
}
