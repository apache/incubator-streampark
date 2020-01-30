/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.flink.core.failover

import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory, TimeUnit}

import com.streamxhub.common.util.{Logger, ThreadUtils}
import scala.collection.JavaConversions._

import scala.collection.mutable.ListBuffer

case class FailoverChecker(timeout: Long) extends AutoCloseable with Logger {

  val sinkBuffers: ListBuffer[SinkBuffer] = ListBuffer[SinkBuffer]()
  val factory: ThreadFactory = ThreadUtils.threadFactory("FailoverChecker")
  val scheduledExecutorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(factory)
  scheduledExecutorService.scheduleWithFixedDelay(getTask, timeout, timeout, TimeUnit.MILLISECONDS)
  logInfo(s"[StreamX] Build Sink scheduled checker, timeout (microSeconds) = $timeout")

  def addSinkBuffer(buffer: SinkBuffer): Unit = {
    this.synchronized(sinkBuffers.add(buffer))
    logger.debug(s"[StreamX] Add SinkBuffer, target table = ${buffer.table}")
  }

  def getTask: Runnable = new Runnable {
    override def run(): Unit = {
      this synchronized {
        logger.debug(s"[StreamX] Start checking buffers. Current count of buffers = ${sinkBuffers.size}")
        sinkBuffers.foreach(_.tryAddToQueue())
      }
    }
  }

  override def close(): Unit = ThreadUtils.shutdownExecutorService(scheduledExecutorService)

}