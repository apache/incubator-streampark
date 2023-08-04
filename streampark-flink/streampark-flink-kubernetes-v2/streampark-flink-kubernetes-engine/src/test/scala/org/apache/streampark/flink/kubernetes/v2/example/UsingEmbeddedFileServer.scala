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

package org.apache.streampark.flink.kubernetes.v2.example

import org.apache.streampark.common.zio.ZIOExt.unsafeRun
import org.apache.streampark.flink.kubernetes.v2.fs.{EmbeddedFileServer, FileMirror}

import org.scalatest.{BeforeAndAfterAll, Ignore}
import org.scalatest.wordspec.AnyWordSpecLike
import zio.ZIO

/**
 * Example of using an embedded file server.
 * Tips: Please uncomment the @Ignore tag to execute the example code.
 */
@Ignore
class UsingEmbeddedFileServer extends AnyWordSpecLike with BeforeAndAfterAll {

  "Launch embedded http file server and mirror files" in unsafeRun {
    for {
      _ <- EmbeddedFileServer.launch

      // mirror local file to http filer server which can be any local path.
      _ <- FileMirror.mirror(s"$assetPath/flink-faker-0.5.3.jar", "test")
      _ <- FileMirror.mirror(s"$assetPath/quick-sql-1.0.jar", "test")

      // print the http url that corresponds to the accessible file.
      _ <- FileMirror.getHttpUrl("test", "flink-faker-0.5.3.jar").debug
      _ <- FileMirror.getHttpUrl("test", "quick-sql-1.0.jar").debug
      /* OUTPUT:
        http://{LAN_IP}:10030/fs/test/flink-faker-0.5.3.jar
        http://{LAN_IP}:10030/fs/test/quick-sql-1.0.jar
       */
      _ <- ZIO.never
    } yield ()
  }

  "A more simplified example" in unsafeRun {
    for {
      _ <- EmbeddedFileServer.launch

      _ <- FileMirror.mirrorAndGetHttpUrl(s"$assetPath/flink-faker-0.5.3.jar", "test2").debug
      _ <- FileMirror.mirrorAndGetHttpUrl(s"$assetPath/quick-sql-1.0.jar", "test2").debug
      /* OUTPUT:
        http://{LAN_IP}:10030/fs/test2/flink-faker-0.5.3.jar
        http://{LAN_IP}:10030/fs/test2/quick-sql-1.0.jar
       */
      _ <- ZIO.never
    } yield ()
  }

  override protected def beforeAll(): Unit = prepareTestAssets()
}
