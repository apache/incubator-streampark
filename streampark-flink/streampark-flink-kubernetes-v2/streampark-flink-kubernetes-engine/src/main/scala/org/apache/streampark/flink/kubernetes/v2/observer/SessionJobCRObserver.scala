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
import org.apache.streampark.flink.kubernetes.v2.model.{JobStatus, SessionJobCRStatus}

import org.apache.flink.v1beta1.FlinkSessionJob
import zio.UIO
import zio.concurrent.ConcurrentMap

/**
 * Observer for FlinkSessionJob K8s CRs,
 * monitor the status of FlinkSessionJob CRs for a specified namespace and name.
 *
 * See: [[org.apache.streampark.shaded.org.apache.flink.kubernetes.operator.api.FlinkSessionJob]]
 */
case class SessionJobCRObserver(
    sessionJobCRSnaps: ConcurrentMap[(Namespace, Name), (SessionJobCRStatus, Option[JobStatus])]) {

  // store all hooks of listeners fibers
  private val watchers = ConcurrentMap.empty[(Namespace, Name), K8sResourceWatcher[FlinkSessionJob]].runUIO

  def watch(namespace: String, name: String): UIO[Unit] =
    watchers
      .get((namespace, name))
      .noneOrUnitZIO {
        val watch = launchProc(namespace, name)
        watchers.put((namespace, name), watch) *>
        watch.launch
      }

  def unWatch(namespace: String, name: String): UIO[Unit] =
    watchers
      .get((namespace, name))
      .someOrUnitZIO { watcher =>
        watcher.stop *>
        watchers.remove((namespace, name))
      }

  private def launchProc(namespace: String, name: String): K8sResourceWatcher[FlinkSessionJob] =
    watchK8sResourceForever(client =>
      client
        .resources(classOf[FlinkSessionJob])
        .inNamespace(namespace)
        .withName(name)) { stream =>
      stream
        // eval SessionJobCR status
        .map { case (action, cr) =>
          SessionJobCRStatus.eval(action, cr) ->
          Option(cr.getStatus.getJobStatus).map(JobStatus.fromSessionJobCR)
        }
        // update SessionJobCR status cache
        .tap(status => sessionJobCRSnaps.put((namespace, name), status))
    }

}
