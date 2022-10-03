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

package org.apache.streampark.flink.connector.clickhouse.internal

import org.apache.streampark.common.enums.ApiType
import org.apache.streampark.common.enums.ApiType.ApiType
import org.apache.streampark.common.util.Logger
import org.apache.streampark.flink.connector.clickhouse.conf.ClickHouseHttpConfig
import org.apache.streampark.flink.connector.clickhouse.internal
import org.apache.streampark.flink.connector.clickhouse.util.ClickhouseConvertUtils.convert
import org.apache.streampark.flink.connector.failover.{FailoverChecker, SinkBuffer}
import org.apache.streampark.flink.connector.function.TransformFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction

import java.util.Properties
import scala.util.{Failure, Try}

class AsyncClickHouseSinkFunction[T](apiType: ApiType = ApiType.scala, properties: Properties) extends RichSinkFunction[T] with Logger {

  private[this] object Lock {
    @volatile var initialized = false
    val lock = new Object()
  }

  private[this] var scalaSqlFunc: T => String = _
  private[this] var javaSqlFunc: TransformFunction[T, String] = _


  //for Scala
  def this(properties: Properties, scalaSqlFunc: T => String) = {
    this(ApiType.scala, properties)
    this.scalaSqlFunc = scalaSqlFunc
  }

  //for JAVA
  def this(properties: Properties, javaSqlFunc: TransformFunction[T, String]) = {
    this(ApiType.java, properties)
    this.javaSqlFunc = javaSqlFunc
  }

  @transient var clickHouseConf: ClickHouseHttpConfig = _
  @transient var sinkBuffer: SinkBuffer = _
  @transient var clickHouseWriter: ClickHouseSinkWriter = _
  @transient var failoverChecker: FailoverChecker = _
  @volatile var isClosed: Boolean = false

  override def open(config: Configuration): Unit = {
    if (!Lock.initialized) {
      Lock.lock.synchronized {
        if (!Lock.initialized) {
          Lock.initialized = true
          clickHouseConf = new ClickHouseHttpConfig(properties)
          clickHouseWriter = internal.ClickHouseSinkWriter(clickHouseConf)
          failoverChecker = FailoverChecker(clickHouseConf.delayTime)
          sinkBuffer = SinkBuffer(clickHouseWriter, clickHouseConf.delayTime, clickHouseConf.bufferSize)
          failoverChecker.addSinkBuffer(sinkBuffer)
          logInfo("AsyncClickHouseSink initialize... ")
        }
      }
    }
  }

  override def invoke(value: T): Unit = {
    val sql = (javaSqlFunc, scalaSqlFunc) match {
      case (null, null) => convert[T](value)
      case _ => apiType match {
        case ApiType.java => javaSqlFunc.transform(value)
        case ApiType.scala => scalaSqlFunc(value)
      }
    }

    Try(sinkBuffer.put(sql)) match {
      case Failure(e) =>
        logError(s"""Error while sending data to Clickhouse, record = $sql,error:$e""")
        throw e
      case _ =>
    }
  }


  override def close(): Unit = {
    if (!isClosed) {
      Lock.lock.synchronized {
        if (!isClosed) {
          if (sinkBuffer != null) sinkBuffer.close()
          if (clickHouseWriter != null) clickHouseWriter.close()
          if (failoverChecker != null) failoverChecker.close()
          isClosed = true
          super.close()
        }
      }
    }
  }
}
