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

import org.apache.streampark.common.util.{Logger, ThreadUtils}

import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory, TimeUnit}

import org.apache.streampark.common.util.Implicits._
import scala.collection.mutable.ListBuffer

case class FailoverChecker(delayTime: Long) extends AutoCloseable with Logger {

  val sinkBuffers: ListBuffer[SinkBuffer] = ListBuffer[SinkBuffer]()
  val factory: ThreadFactory = ThreadUtils.threadFactory("FailoverChecker")
  val scheduledExecutorService: ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor(factory)
  scheduledExecutorService.scheduleWithFixedDelay(
    getTask,
    delayTime,
    delayTime,
    TimeUnit.MILLISECONDS)
  logInfo(s"Build Sink scheduled checker, timeout (microSeconds) = $delayTime")

  def addSinkBuffer(buffer: SinkBuffer): Unit = {
    this.synchronized(sinkBuffers.add(buffer))
    logDebug(s"Add SinkBuffer, size: ${buffer.bufferSize}")
  }

  def getTask(): Runnable = new Runnable {
    override def run(): Unit = {
      this synchronized {
        logDebug(s"Start checking buffers. Current count of buffers = ${sinkBuffers.size}")
        sinkBuffers.foreach(_.tryAddToQueue())
      }
    }
  }

  override def close(): Unit = ThreadUtils.shutdownExecutorService(scheduledExecutorService)

}
