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

package org.apache.streampark.flink.kubernetes.v2.fs

import org.apache.streampark.common.zio.ZIOExt.UIOOps

import zio.{Ref, UIO, ZIO}
import zio.http._

object EmbeddedFileServer {

  private val routes = Http.collectHttp[Request] {
    case Method.GET -> Root / "health"               => Handler.ok.toHttp
    case Method.GET -> Root / "fs" / subspace / name =>
      Http.fromFileZIO(FileMirror.getLocalFile(subspace, name))
  }

  private val isLaunch: Ref[Boolean] = Ref.make(false).runUIO

  /** Launch the netty-based internal http file server at port specified by fileServerPort param. */
  def launch: UIO[Unit] = {
    val serve = for {
      _ <- ZIO.log(s"Launch internal http file server at port: $fileServerPort")
      _ <- Server
             .serve(routes.withDefaultErrorResponse)
             .provide(Server.defaultWithPort(fileServerPort))
             .forkDaemon
    } yield ()
    (serve *> isLaunch.set(true)).unlessZIO(isLaunch.get).unit
  }

}
