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

package org.apache.streampark.flink.kubernetes.v2.model

import io.fabric8.kubernetes.api.model.ObjectMeta
import org.apache.flink.v1beta1.{FlinkSessionJob, FlinkSessionJobSpec}

import scala.jdk.CollectionConverters.mapAsJavaMapConverter

/**
 * Flink Session job CR definition.
 * Typed-safe Mirror of [[org.apache.flink.v1beta1.FlinkSessionJob]]
 *
 * @param namespace          K8s CR namespace
 * @param name               K8s CR name
 * @param deploymentName     The name of the target session cluster deployment.
 * @param job                Job definition
 * @param flinkConfiguration Flink configuration overrides for the Flink deployment or Flink session job.
 * @param restartNonce       Nonce used to manually trigger restart for the cluster/session job. In order to
 *                           trigger restart, change the number to anything other than the current value.
 */
case class FlinkSessionJobDef(
    namespace: String,
    name: String,
    deploymentName: String,
    job: JobDef,
    flinkConfiguration: Map[String, String] = Map.empty,
    restartNonce: Option[Long] = None) {

  // noinspection DuplicatedCode
  def toFlinkSessionJob: FlinkSessionJob = {
    val spec = new FlinkSessionJobSpec()
    spec.setDeploymentName(deploymentName)
    spec.setJob(job.toFlinkSessionJobSpec)
    if (flinkConfiguration.nonEmpty) spec.setFlinkConfiguration(flinkConfiguration.asJava)
    restartNonce.foreach(spec.setRestartNonce(_))

    val sessionJob = new FlinkSessionJob()
    val metadata   = new ObjectMeta()
    metadata.setNamespace(namespace)
    metadata.setName(name)
    sessionJob.setMetadata(metadata)
    sessionJob.setSpec(spec)
    sessionJob.setStatus(null)
    sessionJob
  }
}
