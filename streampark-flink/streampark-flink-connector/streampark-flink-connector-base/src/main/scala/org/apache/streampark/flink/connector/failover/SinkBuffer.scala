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

package org.apache.streampark.flink.connector.failover

import org.apache.streampark.common.util.Logger

import java.util
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

import org.apache.streampark.common.util.Implicits._

case class SinkBuffer(writer: SinkWriter, flushInterval: Long, bufferSize: Int)
  extends AutoCloseable
  with Logger {

  private var timestamp = 0L

  var localValues = new CopyOnWriteArrayList[String]()

  def put(value: String): Unit = {
    tryAddToQueue()
    localValues.add(value)
    timestamp = System.currentTimeMillis
  }

  def tryAddToQueue(): Unit = {
    this.synchronized {
      if (flush) {
        addToQueue()
      }
    }
  }

  private[this] def addToQueue(): Unit = {
    val deepCopy = buildDeepCopy(localValues)
    val params = SinkRequest(deepCopy)
    logDebug(s"Build blank with params: buffer size = ${params.size}")
    writer.write(params)
    localValues.clear()
  }

  private[this] def flush: Boolean = {
    if (localValues.nonEmpty) {
      localValues.size >= bufferSize || {
        if (timestamp == 0) false
        else {
          val current = System.currentTimeMillis
          current - timestamp > flushInterval
        }
      }
    } else false
  }

  private[this] def buildDeepCopy(original: util.List[String]): util.List[String] =
    Collections.unmodifiableList(new util.ArrayList[String](original))

  override def close(): Unit = if (localValues.nonEmpty) addToQueue()

}
