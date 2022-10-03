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

package org.apache.streampark.flink.connector.http.function

import org.apache.streampark.common.util.Logger
import org.apache.streampark.flink.connector.conf.ThresholdConf
import org.apache.streampark.flink.connector.failover.{FailoverChecker, SinkBuffer}
import org.apache.streampark.flink.connector.http.conf.HttpConfigOption
import org.apache.streampark.flink.connector.http.internal.HttpSinkWriter
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction

import java.util.Properties
import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable


class HttpSinkFunction(properties: mutable.Map[String, String],
                       header: Map[String, String],
                       method: String) extends RichSinkFunction[String] with Logger {

  private[this] object Lock {
    @volatile var initialized = false
    val lock = new ReentrantLock()
  }

  @transient var sinkBuffer: SinkBuffer = _
  @transient var thresholdConf: ThresholdConf = _
  @transient var httpSinkWriter: HttpSinkWriter = _
  @transient var failoverChecker: FailoverChecker = _
  @volatile var isClosed: Boolean = false

  override def open(config: Configuration): Unit = {
    if (!Lock.initialized) {
      Lock.lock.lock()
      if (!Lock.initialized) {
        Lock.initialized = true

        val prop: Properties = new Properties()
        properties.foreach { case (k, v) => prop.put(k, v) }
        thresholdConf = ThresholdConf(HttpConfigOption.HTTP_SINK_PREFIX, prop)
        val bufferSize = 1
        val table = thresholdConf.failoverTable
        require(table != null && table.nonEmpty, () => s"http async  insert failoverTable must not null")

        httpSinkWriter = HttpSinkWriter(thresholdConf, header)
        failoverChecker = FailoverChecker(thresholdConf.delayTime)
        sinkBuffer = SinkBuffer(httpSinkWriter, thresholdConf.delayTime, bufferSize)
        failoverChecker.addSinkBuffer(sinkBuffer)
        logInfo("HttpSink initialize... ")
      }
      Lock.lock.unlock()
    }
  }

  override def invoke(url: String): Unit = {
    sinkBuffer.put(s"$method///$url")
  }

  override def close(): Unit = {
    if (!isClosed) {
      Lock.lock.synchronized {
        if (!isClosed) {
          if (sinkBuffer != null) sinkBuffer.close()
          if (httpSinkWriter != null) httpSinkWriter.close()
          if (failoverChecker != null) failoverChecker.close()
          isClosed = true
          super.close()
        }
      }
    }
  }
}
