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

package org.apache.streampark.console.core.Impl

import org.apache.streampark.common.util.Logger
import org.apache.streampark.common.zio.ZIOExt.IOOps
import org.apache.streampark.console.core.entity.FlinkCluster
import org.apache.streampark.flink.client.bean.{DeployRequest, ShutDownRequest}
import org.apache.streampark.flink.kubernetes.v2.model.{FlinkDeploymentDef, JobManagerDef, TaskManagerDef}
import org.apache.streampark.flink.kubernetes.v2.operator.FlinkK8sOperator

import org.apache.flink.configuration.JobManagerOptions
import org.apache.flink.v1beta1.FlinkDeploymentSpec.FlinkVersion

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object FlinkClusterServiceImplV2 extends Logger {
  protected type FailureMessage = String

  protected val KUBERNETES_JM_CPU_KEY        = "kubernetes.jobmanager.cpu"
  protected val KUBERNETES_JM_CPU_AMOUNT_KEY = "kubernetes.jobmanager.cpu.amount"
  protected val KUBERNETES_JM_CPU_DEFAULT    = 1.0
  protected val KUBERNETES_JM_MEMORY_DEFAULT = "1600m"

  protected val KUBERNETES_TM_CPU_KEY        = "kubernetes.taskmanager.cpu"
  protected val KUBERNETES_TM_CPU_AMOUNT_KEY = "kubernetes.taskmanager.cpu.amount"
  protected val KUBERNETES_TM_CPU_DEFAULT    = -1.0
  protected val KUBERNETES_TM_MEMORY_DEFAULT = "1728m"

  @throws[Throwable]
  def doSubmit(deployRequest: DeployRequest, flinkCluster: FlinkCluster): Unit = {

    val richMsg: String => String = s"[flink-submit][appId=${deployRequest.id}] " + _

    // Convert to FlinkDeployment CR definition
    val flinkDeployDef = genFlinkDeployDef(deployRequest, flinkCluster) match {
      case Right(result) => result
      case Left(errMsg)  =>
        throw new IllegalArgumentException(richMsg(s"Error occurred while parsing parameters: $errMsg"))
    }

    // Submit FlinkDeployment CR to Kubernetes
    FlinkK8sOperator.deployApplicationJob(deployRequest.id, flinkDeployDef).runIOAsTry match {
      case Success(_)   =>
        logInfo(richMsg("Flink job has been submitted successfully."))
      case Failure(err) =>
        logError(richMsg(s"Submit Flink job fail in ${deployRequest.executionMode.getName}_V2 mode!"), err)
        throw err
    }
  }
  @throws[Throwable]
  def shutdown(shutDownRequest: ShutDownRequest): Unit = {
    val name      = shutDownRequest.clusterId
    val namespace = shutDownRequest.kubernetesDeployParam.kubernetesNamespace

    def richMsg: String => String = s"[flink-shutdown][clusterId=$name][namespace=$namespace] " + _

    FlinkK8sOperator.k8sCrOpr.deleteSessionJob(namespace, name).runIOAsTry match {
      case Success(_)   =>
        logInfo(richMsg("Shutdown Flink cluster successfully."))
      case Failure(err) =>
        logError(richMsg(s"Fail to shutdown Flink cluster"), err)
        throw err
    }
  }

  private def genFlinkDeployDef(
      deployRequest: DeployRequest,
      flinkCluster: FlinkCluster): Either[FailureMessage, FlinkDeploymentDef] = {

    val namespace = deployRequest.k8sDeployParam.kubernetesNamespace

    val name = deployRequest.k8sDeployParam.clusterId

    val image          = deployRequest.k8sDeployParam.flinkImage
    val serviceAccount = deployRequest.k8sDeployParam.serviceAccount

    val flinkVersion = Option(deployRequest.flinkVersion.majorVersion)
      .map(majorVer => "V" + majorVer.replace(".", "_"))
      .flatMap(v => FlinkVersion.values().find(_.name() == v))
      .getOrElse(return Left(s"Unsupported Flink version: ${deployRequest.flinkVersion.majorVersion}"))

    val jobManager = {
      val cpu = KUBERNETES_JM_CPU_DEFAULT

      val mem = KUBERNETES_JM_MEMORY_DEFAULT

      JobManagerDef(cpu = cpu, memory = mem, ephemeralStorage = None, podTemplate = None)
    }

    val taskManager = {
      val cpu = KUBERNETES_TM_CPU_DEFAULT

      val mem = KUBERNETES_TM_MEMORY_DEFAULT
      TaskManagerDef(cpu = cpu, memory = mem, ephemeralStorage = None, podTemplate = None)
    }

    val extraFlinkConfiguration = flinkCluster.getFlinkConfig.asScala.toMap

    Right(
      FlinkDeploymentDef(
        namespace = namespace,
        name = name,
        image = image,
        serviceAccount = serviceAccount,
        flinkVersion = flinkVersion,
        jobManager = jobManager,
        taskManager = taskManager,
        flinkConfiguration = extraFlinkConfiguration,
        extJarPaths = Array.empty
      ))
  }

}
