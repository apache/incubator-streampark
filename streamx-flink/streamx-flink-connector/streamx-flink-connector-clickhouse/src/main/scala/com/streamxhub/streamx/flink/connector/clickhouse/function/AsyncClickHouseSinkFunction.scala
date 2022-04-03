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

package com.streamxhub.streamx.flink.connector.clickhouse.function

import com.streamxhub.streamx.common.enums.ApiType
import com.streamxhub.streamx.common.enums.ApiType.ApiType
import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.connector.clickhouse.conf.ClickHouseConfig
import com.streamxhub.streamx.flink.connector.clickhouse.conf.ClickHouseConfigConst.CLICKHOUSE_TARGET_TABLE
import com.streamxhub.streamx.flink.connector.clickhouse.internal
import com.streamxhub.streamx.flink.connector.clickhouse.internal.ClickHouseSinkWriter
import com.streamxhub.streamx.flink.connector.clickhouse.util.PoJoConvertUtils.parsePojoToInsertValue
import com.streamxhub.streamx.flink.connector.failover.{FailoverChecker, SinkBuffer}
import com.streamxhub.streamx.flink.connector.function.SQLFromFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction

import java.util.Properties
import scala.collection.JavaConversions._

class AsyncClickHouseSinkFunction[T](apiType: ApiType = ApiType.scala, properties: Properties) extends RichSinkFunction[T] with Logger {

  private[this] object Lock {
    @volatile var initialized = false
    val lock = new Object()
  }

  private[this] var scalaSqlFunc: T => String = _
  private[this] var javaSqlFunc: SQLFromFunction[T] = _


  //for Scala
  def this(properties: Properties,
           scalaSqlFunc: T => String) = {

    this(ApiType.scala, properties)
    this.scalaSqlFunc = scalaSqlFunc
  }

  //for JAVA
  def this(properties: Properties,
           javaSqlFunc: SQLFromFunction[T]) = {

    this(ApiType.java, properties)
    this.javaSqlFunc = javaSqlFunc
  }

  @transient var clickHouseConf: ClickHouseConfig = _
  @transient var sinkBuffer: SinkBuffer = _
  @transient var clickHouseWriter: ClickHouseSinkWriter = _
  @transient var failoverChecker: FailoverChecker = _
  @volatile var isClosed: Boolean = false

  override def open(config: Configuration): Unit = {
    if (!Lock.initialized) {
      Lock.lock.synchronized {
        if (!Lock.initialized) {
          Lock.initialized = true
          clickHouseConf = new ClickHouseConfig(properties)
          val targetTable = properties(CLICKHOUSE_TARGET_TABLE)
          require(targetTable != null && !targetTable.isEmpty, () => s"ClickHouseSinkFunction insert targetTable must not null")
          clickHouseWriter = internal.ClickHouseSinkWriter(clickHouseConf)
          failoverChecker = FailoverChecker(clickHouseConf.delayTime)
          sinkBuffer = SinkBuffer(clickHouseWriter, clickHouseConf.delayTime, clickHouseConf.bufferSize, targetTable)
          failoverChecker.addSinkBuffer(sinkBuffer)
          logInfo("AsyncClickHouseSink initialize... ")
        }
      }
    }
  }

  override def invoke(value: T): Unit = {
    val csv = (javaSqlFunc, scalaSqlFunc) match {
      case (null, null) =>
        parsePojoToInsertValue[T](value)
      case _ => apiType match {
        case ApiType.java => javaSqlFunc.from(value)
        case ApiType.scala => scalaSqlFunc(value)
      }
    }
    try {
      sinkBuffer.put(csv)
    } catch {
      case e: Exception =>
        logError(s"""Error while sending data to Clickhouse, record = $csv,error:$e""")
        throw new RuntimeException(e)
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
