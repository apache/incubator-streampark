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

import org.apache.streampark.common.zio.ZIOExt.{unsafeRun, UIOOps}
import org.apache.streampark.flink.kubernetes.v2.K8sTools.{newK8sClient, usingK8sClient}

import zio.{durationInt, Ref, UIO, ZIO}

import java.net.{InetAddress, InetSocketAddress, Socket}

import scala.util.Using

object FileServerPeerAddress {

  private val address: Ref[Option[String]] = unsafeRun(Ref.make(None))

  private val STREAMPARK_K8S_SVC_NAME = "streampark-service"

  // Auto calculate address when initialized.
  infer
    .map(Some(_))
    .tap(address.set)
    .tap(addr => ZIO.logInfo(s"Embedded HTTP file server K8s peer address: ${addr.getOrElse("unknown")}"))
    .forkDaemon
    .runUIO

  /** Get the peer communication address snapshot. */
  def get: UIO[Option[String]] = address.get

  /** Get the address, blocking the caller until the address is calculated. */
  def getEnsure: UIO[String] = address.get.flatMap {
    case None       => getEnsure.delay(100.millis)
    case Some(addr) => ZIO.succeed(addr)
  }

  /** Refresh the peer communication address. */
  def refresh: UIO[Unit] = infer.tap(r => address.set(Some(r))).unit

  /** Infer the relative file service peer address for k8s resources. */
  def infer: UIO[String] = {
    inferInsidePod.some
      .orElse(inferSocketReplyFromK8sApiServer.some)
      .orElse(directLocalHost.some)
      .orElse(ZIO.succeed("127.0.0.1"))
  }

  private def inferInsidePod: UIO[Option[String]] =
    usingK8sClient { client =>
      Option(client.getNamespace).flatMap { ns =>
        Option(
          client.services
            .inNamespace(ns)
            .withName(STREAMPARK_K8S_SVC_NAME)
            .get()
        ).map(_ => s"$STREAMPARK_K8S_SVC_NAME.$ns")
      }
    }.catchAll(_ => ZIO.succeed(None))

  private def inferSocketReplyFromK8sApiServer: UIO[Option[String]] =
    ZIO
      .attemptBlocking {
        val masterUrl = newK8sClient.getConfiguration.getMasterUrl

        extractHostPortFromUrl(masterUrl).flatMap { case (host, port) =>
          Using(new Socket()) { socket =>
            socket.connect(new InetSocketAddress(host, port))
            socket.getLocalSocketAddress.asInstanceOf[InetSocketAddress].getAddress.getHostAddress
          }.toOption
        }
      }
      .catchAll(_ => ZIO.succeed(None))

  private def directLocalHost: UIO[Option[String]] =
    ZIO
      .attemptBlocking(InetAddress.getLocalHost.getHostAddress)
      .map(Some(_))
      .catchAll(_ => ZIO.succeed(None))

  private def extractHostPortFromUrl(url: String): Option[(String, Int)] = {
    val p1 = url.split("://")
    if (p1.length != 2) None
    else {
      val protocol = p1(0)
      val p2       = p1(1).split("/").head.split(":")
      if (p2.length == 2) Some(p2(0) -> p2(1).toInt)
      else if (p2.length == 1) protocol match {
        case "http"  => Some(p2(0) -> 80)
        case "https" => Some(p2(0) -> 443)
        case _       => None
      }
      else None
    }
  }

}
