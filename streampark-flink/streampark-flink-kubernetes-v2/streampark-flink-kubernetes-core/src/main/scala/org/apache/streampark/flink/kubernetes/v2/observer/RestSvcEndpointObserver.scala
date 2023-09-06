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

package org.apache.streampark.flink.kubernetes.v2.observer

import org.apache.streampark.common.zio.ZIOExt.{OptionZIOOps, UIOOps}
import org.apache.streampark.flink.kubernetes.v2.K8sTools.{watchK8sResourceForever, K8sResourceWatcher}
import org.apache.streampark.flink.kubernetes.v2.model.RestSvcEndpoint

import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.client.Watcher
import zio.UIO
import zio.concurrent.ConcurrentMap

import scala.jdk.CollectionConverters._

/**
 * Observer for Flink cluster REST svc endpoint on K8s,
 * monitor the status of FlinkSessionJob CRs for a specified namespace and name.
 */
case class RestSvcEndpointObserver(restSvcEndpointSnaps: ConcurrentMap[(Namespace, Name), RestSvcEndpoint]) {

  // store all hooks of listeners fibers
  private val watchers = ConcurrentMap.empty[(Namespace, Name), K8sResourceWatcher[Service]].runUIO

  def watch(namespace: String, name: String): UIO[Unit] =
    watchers
      .get((namespace, name))
      .noneOrUnitZIO {
        val watch = launchProc(namespace, name)
        watchers.put((namespace, name), watch) *> watch.launch
      }

  def unWatch(namespace: String, name: String): UIO[Unit] =
    watchers
      .get((namespace, name))
      .someOrUnitZIO { watcher =>
        for {
          _ <- watcher.stop
          _ <- watchers.remove((namespace, name))
          _ <- restSvcEndpointSnaps.remove((namespace, name))
        } yield ()
      }

  private def launchProc(namespace: String, name: String): K8sResourceWatcher[Service] =
    watchK8sResourceForever(client =>
      client
        .services()
        .inNamespace(namespace)
        .withName(s"$name-rest")) { stream =>
      stream
        .map {
          case (Watcher.Action.DELETED, _) => None
          case (_, svc)                    =>
            val namespace = svc.getMetadata.getNamespace
            val name      = svc.getMetadata.getName
            val clusterIP = svc.getSpec.getClusterIP
            val port      = svc.getSpec.getPorts.asScala
              .find(_.getPort == 8081)
              .map(_.getTargetPort.getIntVal.toInt)
              .getOrElse(8081)

            Some(RestSvcEndpoint(namespace, name, port, clusterIP))
        }
        .mapZIO {
          case None           => restSvcEndpointSnaps.remove((namespace, name))
          case Some(endpoint) => restSvcEndpointSnaps.put((namespace, name), endpoint)
        }
    }

}
