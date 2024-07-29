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

import java.util.concurrent.{Callable, CompletableFuture, ScheduledThreadPoolExecutor, ThreadFactory, TimeoutException, TimeUnit}
import java.util.function.{BiConsumer, Consumer, Function => JavaFunc}

object CompletableFutureUtils {

  private[this] class DaemonThreadFactory extends ThreadFactory {
    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r)
      t.setDaemon(true)
      t.setName("CompletableFutureDelayScheduler")
      t
    }
  }

  private[this] lazy val completableDelayer: ScheduledThreadPoolExecutor = {
    val delayer = new ScheduledThreadPoolExecutor(1, new DaemonThreadFactory())
    delayer.setRemoveOnCancelPolicy(true)
    delayer
  }

  private[this] def setTimeout[T](timeout: Long, unit: TimeUnit): CompletableFuture[T] = {
    val result = new CompletableFuture[T]
    completableDelayer.schedule(
      new Callable[Boolean] {
        override def call(): Boolean =
          result.completeExceptionally(new TimeoutException)
      },
      timeout,
      unit)
    result
  }

  def supplyTimeout[T](
      future: CompletableFuture[T],
      timeout: Long,
      unit: TimeUnit,
      handle: JavaFunc[T, T],
      exceptionally: JavaFunc[Throwable, T]): CompletableFuture[T] = {
    future
      .applyToEither(setTimeout(timeout, unit), handle)
      .exceptionally(exceptionally)
      .whenComplete(new BiConsumer[T, Throwable]() {
        override def accept(t: T, u: Throwable): Unit = {
          if (!future.isDone) {
            future.cancel(true)
          }
        }
      })
  }

  /**
   * Callback handler when the future is done before timeout. Callback exceptionally when the future
   * completed exceptionally or future isn't done after timeout.
   *
   * @param future
   *   The future that needs to be watched.
   * @param timeout
   *   timeout
   * @param unit
   *   timeout unit
   * @param handle
   *   The handler will be called when future complete normally before timeout.
   * @param exceptionally
   *   The exceptionally will be called when future completed exceptionally or future isn't done
   *   after timeout.
   */
  def runTimeout[T](
      future: CompletableFuture[T],
      timeout: Long,
      unit: TimeUnit,
      handle: Consumer[T],
      exceptionally: Consumer[Throwable]): CompletableFuture[Unit] = {

    future
      .applyToEither(
        setTimeout(timeout, unit),
        new JavaFunc[T, Unit]() {
          override def apply(t: T): Unit = {
            if (handle != null) {
              handle.accept(t)
            }
          }
        })
      .exceptionally(new JavaFunc[Throwable, Unit]() {
        override def apply(t: Throwable): Unit = {
          if (exceptionally != null) {
            exceptionally.accept(t)
          }
        }
      })
      .whenComplete(new BiConsumer[Unit, Throwable]() {
        override def accept(t: Unit, u: Throwable): Unit = {
          if (!future.isDone) {
            future.cancel(true)
          }
        }
      })
  }

  def runTimeout[T](
      future: CompletableFuture[T],
      timeout: Long,
      unit: TimeUnit): CompletableFuture[Unit] = {
    runTimeout(future, timeout, unit, null, null)
  }

}
