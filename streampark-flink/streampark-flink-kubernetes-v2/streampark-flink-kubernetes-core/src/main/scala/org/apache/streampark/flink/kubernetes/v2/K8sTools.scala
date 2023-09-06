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

package org.apache.streampark.flink.kubernetes.v2

import org.apache.streampark.common.util.Logger
import org.apache.streampark.common.zio.ZIOExt.{unsafeRun, IOOps, OptionZIOOps, UIOOps}

import io.fabric8.kubernetes.client._
import io.fabric8.kubernetes.client.dsl.WatchAndWaitable
import zio.{durationInt, Fiber, IO, Queue, Ref, Schedule, UIO, ZIO}
import zio.stream.{UStream, ZStream}

object K8sTools extends Logger {

  /** Create new fabric8 k8s client */
  def newK8sClient: KubernetesClient = new KubernetesClientBuilder().build

  @inline def usingK8sClient[A](f: KubernetesClient => A): IO[Throwable, A] = ZIO.scoped {
    ZIO
      .acquireRelease(ZIO.attempt(newK8sClient))(client => ZIO.attempt(client.close()).ignore)
      .flatMap(client => ZIO.attemptBlocking(f(client)))
  }

  /**
   * Converts fabric8 callback-style Watch to ZStream style.
   * Usage:
   * {{{
   *   watchK8sResource(client => client.services().withName("my-service"))
   *      .flatMap(watch => watch.stream.debug.runCollect)
   * }}}
   */
  def watchK8sResource[R](genWatch: KubernetesClient => WatchAndWaitable[R]): IO[Throwable, K8sWatcher[R]] = {
    for {
      queue  <- Queue.unbounded[(Watcher.Action, R)]
      client <- ZIO.attempt(newK8sClient)

      watcherF = new Watcher[R]() {
                   override def reconnecting(): Boolean                                  = true
                   override def eventReceived(action: Watcher.Action, resource: R): Unit = {
                     queue.offer((action, resource)).runIO
                   }
                   override def onClose(cause: WatcherException): Unit                   = {
                     logError("K8s Watcher was accidentally closed.", cause)
                   }
                   override def onClose(): Unit                                          = {
                     super.onClose()
                     queue.shutdown.runIO
                     client.close()
                   }
                 }
      watch   <- ZIO.attemptBlocking(genWatch(client).watch(watcherF))
      stream   = ZStream.fromQueue(queue)
    } yield K8sWatcher(watch, stream)
  }

  /** Rich Kubernetes watcher wrapper. */
  case class K8sWatcher[R](watch: Watch, stream: UStream[(Watcher.Action, R)])

  /**
   * Safely and automatically retry subscriptions to k8s resources。
   *
   * @param genWatch The shape of building Watch resources monad from KubeClient.
   * @param pipe The shape of consume watching stream.
   *
   * Usage:
   * {{{
   *   watchK8sResourceForever(client =>
   *      client
   *        .services()
   *        .inNamespace("test")
   *         .withName("my-svc")) { stream =>
   *      stream
   *        .debug
   *     .map(_._2)}
   * }}}
   */
  def watchK8sResourceForever[R](genWatch: KubernetesClient => WatchAndWaitable[R])(
      pipe: UStream[(Watcher.Action, R)] => UStream[_]): K8sResourceWatcher[R] = {
    K8sResourceWatcher(genWatch, pipe)
  }

  /** Rich Kubernetes watcher wrapper. */
  case class K8sResourceWatcher[R](
      genWatch: KubernetesClient => WatchAndWaitable[R],
      pipe: UStream[(Watcher.Action, R)] => UStream[_]) {

    private val queue: Queue[(Watcher.Action, R)]                 = Queue.unbounded[(Watcher.Action, R)].runUIO
    private val clientRef: Ref[Option[KubernetesClient]]          = unsafeRun(Ref.make(None))
    private val watchRef: Ref[Option[Watch]]                      = unsafeRun(Ref.make(None))
    private val consumeFiberRef: Ref[Option[Fiber.Runtime[_, _]]] = unsafeRun(Ref.make(None))
    private val mainFiberRef: Ref[Option[Fiber.Runtime[_, _]]]    = unsafeRun(Ref.make(None))

    def launch: UIO[Unit] =
      for {
        fiber <- (innerStop *> innerRun).retry(Schedule.spaced(1.seconds)).forkDaemon
        _     <- mainFiberRef.set(Some(fiber))
      } yield ()

    def stop: UIO[Unit] =
      for {
        _ <- innerStop
        _ <- mainFiberRef.get.someOrUnitZIO(_.interrupt)
        _ <- queue.shutdown
      } yield ()

    private def innerRun: ZIO[Any, Throwable, Unit] =
      for {
        client <- ZIO.attempt(newK8sClient)
        _      <- clientRef.set(Some(client))

        watcherShape = new Watcher[R]() {
                         override def reconnecting(): Boolean                                  = true
                         override def eventReceived(action: Watcher.Action, resource: R): Unit = {
                           queue.offer((action, resource)).runIO
                         }
                         override def onClose(cause: WatcherException): Unit                   = {
                           logError("K8s Watcher was accidentally closed.", cause)
                           launch.runIO
                         }
                       }
        watch       <- ZIO.attemptBlocking(genWatch(client).watch(watcherShape))
        _           <- watchRef.set(Some(watch))

        fiber <- pipe(ZStream.fromQueue(queue)).runDrain.forkDaemon
        _     <- consumeFiberRef.set(Some(fiber))
      } yield ()

    private def innerStop: UIO[Unit] =
      for {
        _ <- consumeFiberRef.get.someOrUnitZIO(_.interrupt)
        _ <- watchRef.get.someOrUnitZIO(watch => ZIO.attemptBlocking(watch.close()).ignore)
        _ <- clientRef.get.someOrUnitZIO(client => ZIO.attemptBlocking(client.close()).ignore)
      } yield ()

  }

}
