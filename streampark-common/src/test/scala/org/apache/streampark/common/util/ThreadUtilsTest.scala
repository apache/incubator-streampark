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

package org.apache.streampark.common.util

import org.scalatest.funsuite.AnyFunSuite

import java.util.concurrent.Executors

class ThreadUtilsTest extends AnyFunSuite {

  test("threadFactory should create a daemon thread factory if isDaemon is true") {
    val factory = ThreadUtils.threadFactory("testThread", isDaemon = true)
    val thread = factory.newThread(() => {})
    assert(thread.isDaemon)
    assert(thread.getName.startsWith("testThread"))
  }

  test("threadFactory should create a non-daemon thread factory if isDaemon is false") {
    val factory = ThreadUtils.threadFactory("testThread", isDaemon = false)
    val thread = factory.newThread(() => {})
    assert(!thread.isDaemon)
    assert(thread.getName.startsWith("testThread"))
  }

  test("shutdownExecutorService should gracefully shut down ExecutorService") {
    val executorService = Executors.newSingleThreadExecutor()
    val task = new Runnable {
      def run(): Unit = Thread.sleep(1000)
    }
    executorService.submit(task)
    Thread.sleep(500)
    ThreadUtils.shutdownExecutorService(executorService)
    assert(executorService.isShutdown)
    assert(executorService.isTerminated)
  }

  test(
    "shutdownExecutorService with timeout should forcefully shut down ExecutorService if not terminated within timeout") {
    val executorService = Executors.newSingleThreadExecutor()
    val task = new Runnable {
      def run(): Unit = Thread.sleep(5000)
    }
    executorService.submit(task)
    Thread.sleep(1000)
    ThreadUtils.shutdownExecutorService(executorService, 1)
    assert(executorService.isShutdown)
    assert(executorService.isTerminated)
  }
}
