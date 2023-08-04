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
import org.apache.streampark.flink.kubernetes.v2.model._

import org.apache.flink.v1beta1.FlinkDeployment
import zio.UIO
import zio.concurrent.ConcurrentMap

/**
 * Observer for FlinkDeployment K8s CRs.
 *
 * See: [[org.apache.flink.v1beta1.FlinkDeployment]]
 */
case class DeployCRObserver(deployCRSnaps: ConcurrentMap[(Namespace, Name), (DeployCRStatus, Option[JobStatus])]) {

  // store all hooks of listeners fibers
  private val watchers = ConcurrentMap.empty[(Namespace, Name), K8sResourceWatcher[FlinkDeployment]].runUIO

  /** Monitor the status of K8s FlinkDeployment CR for a specified namespace and name. */
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
        watchers.remove((namespace, name)).unit
      }

//  private def existCr(namespace: String, name: String): IO[Throwable, Boolean] =
//    usingK8sClient { client =>
//      client
//        .resources(classOf[FlinkDeployment])
//        .inNamespace(namespace)
//        .withName(name)
//        .get != null
//    }

  private def launchProc(namespace: String, name: String): K8sResourceWatcher[FlinkDeployment] =
    watchK8sResourceForever(client =>
      client
        .resources(classOf[FlinkDeployment])
        .inNamespace(namespace)
        .withName(name)) { stream =>
      stream
        // Eval FlinkDeployment status
        .map { case (action, deployment) =>
          DeployCRStatus.eval(action, deployment) ->
          Option(deployment.getStatus.getJobStatus).map(JobStatus.fromDeployCR)
        }
        // Update FlinkDeployment status cache
        .tap(status => deployCRSnaps.put((namespace, name), status))
    }

}
