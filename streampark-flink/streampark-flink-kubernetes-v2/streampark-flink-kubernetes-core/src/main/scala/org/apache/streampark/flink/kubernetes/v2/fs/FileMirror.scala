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

import zio.{IO, UIO, ZIO}

import java.io.File

object FileMirror {

  private val mirrorRoot = os.Path(new File(localMirrorDir).getAbsolutePath)

  /** Mirror the file to local mirror directory. Return tuple (namespace, file-name). */
  def mirror(srcFilePath: String, subspace: String): IO[Throwable, (String, String)] = ZIO.attemptBlocking {
    val srcPath  = os.Path(new File(srcFilePath).getAbsolutePath)
    val fileName = srcPath.last
    os.copy(
      from = srcPath,
      to = mirrorRoot / subspace / fileName,
      replaceExisting = true,
      createFolders = true,
      mergeFolders = true
    )
    subspace -> fileName
  }

  /** Get the http access url of the mirrored file resource. */
  def getHttpUrl(subspace: String, name: String): UIO[String] = {
    for {
      httpHost <- FileServerPeerAddress.getEnsure
      url       = s"http://$httpHost:$fileServerPort/fs/$subspace/$name"
    } yield url
  }

  def mirrorAndGetHttpUrl(srcFilePath: String, ns: String): ZIO[Any, Throwable, String] =
    mirror(srcFilePath, ns)
      .flatMap { case (ns, name) => getHttpUrl(ns, name) }

  /** Get the local File of the mirrored file resource. */
  def getLocalFile(subspace: String, name: String): IO[Throwable, File] = {
    for {
      localFile <- ZIO.succeed((mirrorRoot / subspace / name).toIO)
      _         <- ZIO
                     .fail(FileNotFound(localFile.getAbsolutePath))
                     .whenZIO(ZIO.attempt(localFile.exists()).map(!_))
      _         <- ZIO
                     .fail(NotAFile(localFile.getAbsolutePath))
                     .whenZIO(ZIO.attempt(localFile.isFile).map(!_))
    } yield localFile
  }

  case class FileNotFound(path: String) extends Exception(s"File not found: $path")
  case class NotAFile(path: String)     extends Exception(s"Not a file: $path")

}
