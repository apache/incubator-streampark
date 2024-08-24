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

package org.apache.streampark.flink.client.impl

import org.apache.streampark.flink.client.`trait`.YarnClientTrait
import org.apache.streampark.flink.client.bean._
import org.apache.streampark.flink.util.FlinkUtils

import org.apache.flink.client.program.PackagedProgram
import org.apache.flink.configuration.{Configuration, DeploymentOptions}
import org.apache.flink.yarn.YarnClusterDescriptor
import org.apache.flink.yarn.configuration.YarnDeploymentTarget
import org.apache.flink.yarn.entrypoint.YarnJobClusterEntrypoint
import org.apache.hadoop.fs.{Path => HadoopPath}
import org.apache.hadoop.yarn.api.records.ApplicationId

import java.io.File
import java.lang.{Boolean => JavaBool}

import scala.collection.JavaConversions._

/** yarn PerJob mode submit */
object YarnPerJobClient extends YarnClientTrait {

  override def setConfig(submitRequest: SubmitRequest, flinkConfig: Configuration): Unit = {
    super.setConfig(submitRequest, flinkConfig)
    // execution.target
    flinkConfig
      .safeSet(DeploymentOptions.TARGET, YarnDeploymentTarget.PER_JOB.getName)
      .safeSet(DeploymentOptions.ATTACHED, JavaBool.TRUE)
      .safeSet(DeploymentOptions.SHUTDOWN_IF_ATTACHED, JavaBool.TRUE)

    logInfo(s"""
               |------------------------------------------------------------------
               |Effective submit configuration: $flinkConfig
               |------------------------------------------------------------------
               |""".stripMargin)
  }

  override def doSubmit(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration): SubmitResponse = {

    val flinkHome = submitRequest.flinkVersion.flinkHome

    val (clusterSpecification, clusterDescriptor: YarnClusterDescriptor) =
      getYarnClusterDeployDescriptor(submitRequest.ugi, flinkConfig)
    val flinkDistJar = FlinkUtils.getFlinkDistJar(flinkHome)
    clusterDescriptor.setLocalJarPath(new HadoopPath(flinkDistJar))
    clusterDescriptor.addShipFiles(List(new File(s"$flinkHome/lib")))

    var packagedProgram: PackagedProgram = null
    val clusterClient = {
      logInfo(s"""
                 |------------------------<<specification>>-------------------------
                 |$clusterSpecification
                 |------------------------------------------------------------------
                 |""".stripMargin)

      val programJobGraph = super.getJobGraph(flinkConfig, submitRequest, submitRequest.userJarFile)
      packagedProgram = programJobGraph._1
      val jobGraph = programJobGraph._2

      logInfo(s"""
                 |-------------------------<<applicationId>>------------------------
                 |jobGraph getJobID: ${jobGraph.getJobID.toString}
                 |__________________________________________________________________
                 |""".stripMargin)
      deployInternal(
        clusterDescriptor,
        clusterSpecification,
        submitRequest.effectiveAppName,
        classOf[YarnJobClusterEntrypoint].getName,
        jobGraph,
        true).getClusterClient
    }
    val applicationId = clusterClient.getClusterId
    val jobManagerUrl = clusterClient.getWebInterfaceURL
    logInfo(s"""
               |-------------------------<<applicationId>>------------------------
               |Flink Job Started: applicationId: $applicationId
               |__________________________________________________________________
               |""".stripMargin)

    val resp =
      SubmitResponse(applicationId.toString, flinkConfig.toMap, jobManagerUrl = jobManagerUrl)
    closeSubmit(submitRequest, packagedProgram, clusterClient, clusterDescriptor)
    resp
  }

  override def doCancel(cancelRequest: CancelRequest, flinkConf: Configuration): CancelResponse = {
    flinkConf
      .safeSet(DeploymentOptions.TARGET, YarnDeploymentTarget.PER_JOB.getName)
    val response = super.doCancel(cancelRequest, flinkConf)
    val (_: ApplicationId, clusterDescriptor: YarnClusterDescriptor) =
      getYarnClusterDescriptor(cancelRequest.ugi, flinkConf)
    clusterDescriptor.killCluster(ApplicationId.fromString(cancelRequest.clusterId))
    response
  }

}
