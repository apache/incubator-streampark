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

package com.streamxhub.streamx.flink.kubernetes.watcher

import scala.language.implicitConversions

/**
 * auth: Al-assad
 */
trait FlinkWatcher extends AutoCloseable {

  /**
   * Start watcher process.
   * This method should be a thread-safe implementation of
   * light locking and can be called idempotently.
   */
  def start(): Unit

  /**
   * Stop watcher process.
   * This method should be a thread-safe implementation of
   * light locking and can be called idempotently.
   */
  def stop(): Unit

  /**
   * This method should be a thread-safe implementation of
   * light locking and can be called idempotently.
   */
  def restart(): Unit = {
    stop()
    start()
  }

  /**
   * Runnable streamline syntax
   */
  protected implicit def funcToRunnable(fun: () => Unit): Runnable = new Runnable() {
    def run(): Unit = fun()
  }

}
