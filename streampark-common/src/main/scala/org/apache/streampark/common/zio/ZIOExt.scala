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

package org.apache.streampark.common.zio

import zio.{IO, Runtime, Unsafe, ZIO}
import zio.stream.ZStream

/** ZIO extension */
object ZIOExt {

  /* Unsafe run zio effect. */
  @throws[Exception]
  @inline def unsafeRun[E, A](zio: IO[E, A]): A = Unsafe.unsafe {
    implicit u =>
      Runtime.default.unsafe
        .run(zio.provideLayer(Runtime.removeDefaultLoggers >>> ZIOLogger.default))
        .getOrThrowFiberFailure()
  }

  implicit class IOOps[E, A](io: ZIO[Any, E, A]) {

    /** unsafe run IO */
    @throws[Throwable]
    def runIO: A = ZIOExt.unsafeRun(io)
  }

  implicit class UIOOps[A](uio: ZIO[Any, Nothing, A]) {

    /** unsafe run UIO */
    @inline def runUIO: A = ZIOExt.unsafeRun(uio)
  }

  implicit class ZIOOps[R, E, A](zio: ZIO[R, E, A]) {

    @inline def debugPretty: ZIO[R, E, A] =
      zio
        .tap(value => ZIO.succeed(println(toPrettyString(value))))
        .tapErrorCause(cause => ZIO.succeed(println(s"<FAIL> ${cause.prettyPrint}")))

    @inline def debugPretty(tag: String): ZIO[R, E, A] =
      zio
        .tap(value => ZIO.succeed(println(s"$tag: ${toPrettyString(value)}")))
        .tapErrorCause(cause => ZIO.succeed(println(s"<FAIL> $tag: ${cause.prettyPrint}")))
  }

  implicit class OptionZIOOps[R, E, A](zio: ZIO[R, E, Option[A]]) {
    @inline def someOrUnitZIO(effect: A => ZIO[R, E, _]): ZIO[R, E, Unit] =
      zio.flatMap {
        case Some(value) => effect(value).unit
        case None => ZIO.unit
      }

    @inline def noneOrUnitZIO(effect: ZIO[R, E, _]): ZIO[R, E, Unit] =
      zio.flatMap {
        case Some(_) => ZIO.unit
        case None => effect.unit
      }
  }

  implicit class ZStreamOps[R, E, A](zstream: ZStream[R, E, A]) {
    // noinspection DuplicatedCode
    @inline def debugPretty: ZStream[R, E, A] =
      zstream
        .tap(value => ZIO.succeed(println(toPrettyString(value))))
        .tapErrorCause(cause => ZIO.succeed(println(s"<FAIL> ${cause.prettyPrint}")))

    // noinspection DuplicatedCode
    @inline def debugPretty(tag: String): ZStream[R, E, A] =
      zstream
        .tap(value => ZIO.succeed(println(s"$tag: ${toPrettyString(value)}")))
        .tapErrorCause(cause => ZIO.succeed(println(s"<FAIL> $tag: ${cause.prettyPrint}")))

    /* Output a stream that does not repeat with the previous element. */
    @inline def diffPrev: ZStream[R, E, A] = zstream.zipWithPrevious
      .filter {
        case (None, cur) => true
        case (Some(prev), cur) => prev != cur
      }
      .map { case (_, cur) => cur }
  }

}
