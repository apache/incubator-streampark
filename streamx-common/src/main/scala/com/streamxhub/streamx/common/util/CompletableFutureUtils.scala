/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.common.util

import java.util.concurrent.{Callable, CompletableFuture, ScheduledThreadPoolExecutor, ThreadFactory, TimeUnit, TimeoutException}
import java.util.function.{Consumer, Function => JavaFunc}

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
    val delayer = new ScheduledThreadPoolExecutor(
      1,
      new DaemonThreadFactory())
    delayer.setRemoveOnCancelPolicy(true)
    delayer
  }

  def setTimeout[T](timeout: Long, unit: TimeUnit): CompletableFuture[T] = {
    val result = new CompletableFuture[T]
    completableDelayer.schedule(new Callable[Boolean] {
      override def call(): Boolean = result.completeExceptionally(new TimeoutException)
    }, timeout, unit)
    result
  }

  def supplyTimeout[T](future: CompletableFuture[T],
                       timeout: Long,
                       unit: TimeUnit,
                       handle: JavaFunc[T, T],
                       exceptionally: JavaFunc[Throwable, T]): CompletableFuture[T] = {
    future.applyToEither(setTimeout(timeout, unit), handle).exceptionally(exceptionally)
  }

  def runTimeout[T](future: CompletableFuture[T],
                    timeout: Long,
                    unit: TimeUnit,
                    handle: Consumer[T],
                    exceptionally: Consumer[Throwable]): CompletableFuture[Unit] = {
    future.applyToEither(setTimeout(timeout, unit), new JavaFunc[T, Unit] {
      override def apply(t: T): Unit = {
        handle.accept(t)
      }
    }).exceptionally(new JavaFunc[Throwable, Unit] {
      override def apply(t: Throwable): Unit = {
        exceptionally.accept(t)
      }
    })
  }

}
