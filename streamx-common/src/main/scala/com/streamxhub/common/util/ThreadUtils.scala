/*
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
package com.streamxhub.common.util

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit


object ThreadUtils {

  def threadFactory(threadName: String, isDaemon: Boolean): ThreadFactory = new ThreadFactoryBuilder().setNameFormat(threadName + "-%d").setDaemon(isDaemon).build

  def threadFactory(threadName: String): ThreadFactory = threadFactory(threadName, isDaemon = true)

  @throws[InterruptedException]
  def shutdownExecutorService(executorService: ExecutorService): Unit = {
    shutdownExecutorService(executorService, 5)
  }

  @throws[InterruptedException]
  def shutdownExecutorService(executorService: ExecutorService, timeoutS: Int): Unit = {
    if (executorService != null && !executorService.isShutdown) {
      executorService.shutdown()
      if (!executorService.awaitTermination(timeoutS, TimeUnit.SECONDS)) {
        executorService.shutdownNow
        executorService.awaitTermination(timeoutS, TimeUnit.SECONDS)
      }
    }
  }
}
