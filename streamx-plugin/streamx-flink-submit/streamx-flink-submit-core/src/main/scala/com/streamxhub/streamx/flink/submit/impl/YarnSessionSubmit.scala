/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.submit.impl

import com.streamxhub.streamx.common.conf.ConfigConst.KEY_YARN_APP_ID
import com.streamxhub.streamx.common.util.Utils
import com.streamxhub.streamx.flink.submit.`trait`.YarnSubmitTrait
import com.streamxhub.streamx.flink.submit.bean._
import org.apache.flink.api.common.JobID
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader
import org.apache.flink.client.program.{ClusterClient, PackagedProgram}
import org.apache.flink.configuration._
import org.apache.flink.yarn.YarnClusterDescriptor
import org.apache.flink.yarn.configuration.{YarnConfigOptions, YarnDeploymentTarget}
import org.apache.hadoop.yarn.api.records.{ApplicationId, ApplicationReport, FinalApplicationStatus}
import org.apache.hadoop.yarn.client.api.YarnClient
import org.apache.hadoop.yarn.exceptions.ApplicationNotFoundException
import org.apache.hadoop.yarn.util.ConverterUtils

import scala.collection.JavaConversions._

/**
  * Submit Job to YARN Session Cluster
  */
object YarnSessionSubmit extends YarnSubmitTrait {

  /**
    * @param submitRequest
    * @param flinkConfig
    */
  override def setConfig(submitRequest: SubmitRequest, flinkConfig: Configuration): Unit = {
    flinkConfig
      .safeSet(DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName)
      .safeSet(YarnConfigOptions.APPLICATION_ID, submitRequest.extraParameter.get(KEY_YARN_APP_ID).toString)

    logInfo(
      s"""
         |------------------------------------------------------------------
         |Effective submit configuration: $flinkConfig
         |------------------------------------------------------------------
         |""".stripMargin)
  }

  override def doSubmit(submitRequest: SubmitRequest, flinkConfig: Configuration): SubmitResponse = {
    var clusterDescriptor: YarnClusterDescriptor = null
    var packageProgram: PackagedProgram = null
    var client: ClusterClient[ApplicationId] = null
    try {
      val yarnClusterDescriptor = getYarnSessionClusterDescriptor(flinkConfig)
      clusterDescriptor = yarnClusterDescriptor._2
      val yarnClusterId: ApplicationId = yarnClusterDescriptor._1
      val packageProgramJobGraph = super.getJobGraph(flinkConfig, submitRequest, submitRequest.userJarFile)
      packageProgram = packageProgramJobGraph._1
      val jobGraph = packageProgramJobGraph._2

      client = clusterDescriptor.retrieve(yarnClusterId).getClusterClient
      val jobId = client.submitJob(jobGraph).get().toString

      logInfo(
        s"""
           |-------------------------<<applicationId>>------------------------
           |Flink Job Started: jobId: $jobId , applicationId: ${yarnClusterId.toString}
           |__________________________________________________________________
           |""".stripMargin)
      SubmitResponse(yarnClusterId.toString, flinkConfig.toMap, jobId)
    } catch {
      case e: Exception =>
        logError(s"submit flink job fail in ${submitRequest.executionMode} mode")
        e.printStackTrace()
        throw e
    } finally {
      if (submitRequest.safePackageProgram) {
        Utils.close(packageProgram)
      }
      Utils.close(client, clusterDescriptor)
    }
  }

  override def doStop(stopRequest: StopRequest, flinkConfig: Configuration): StopResponse = {
    flinkConfig.safeSet(YarnConfigOptions.APPLICATION_ID, stopRequest.extraParameter.get(KEY_YARN_APP_ID).toString)
    flinkConfig.safeSet(DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName)
    logInfo(
      s"""
         |------------------------------------------------------------------
         |Effective submit configuration: $flinkConfig
         |------------------------------------------------------------------
         |""".stripMargin)

    var clusterDescriptor: YarnClusterDescriptor = null
    var client: ClusterClient[ApplicationId] = null
    try {
      val yarnClusterDescriptor = getYarnSessionClusterDescriptor(flinkConfig)
      clusterDescriptor = yarnClusterDescriptor._2
      client = clusterDescriptor.retrieve(yarnClusterDescriptor._1).getClusterClient
      val jobID = JobID.fromHexString(stopRequest.jobId)
      val actionResult = cancelJob(stopRequest, jobID, client)
      StopResponse(actionResult)
    } catch {
      case e: Exception => logError(s"stop flink yarn session job fail")
        e.printStackTrace()
        throw e
    } finally {
      Utils.close(client, clusterDescriptor)
    }
  }

  def deploy(deployRequest: DeployRequest): DeployResponse = {
    logInfo(
      s"""
         |--------------------------------------- flink yarn sesion start ---------------------------------------
         |    userFlinkHome    : ${deployRequest.flinkVersion.flinkHome}
         |    flinkVersion     : ${deployRequest.flinkVersion.version}
         |    execMode         : ${deployRequest.executionMode.name()}
         |    clusterId        : ${deployRequest.clusterId}
         |    resolveOrder     : ${deployRequest.resolveOrder.getName}
         |    flameGraph       : ${deployRequest.flameGraph != null}
         |    dynamicOption    : ${deployRequest.dynamicOption.mkString(" ")}
         |-------------------------------------------------------------------------------------------
         |""".stripMargin)
    var clusterDescriptor: YarnClusterDescriptor = null
    var client: ClusterClient[ApplicationId] = null
    try {
      val flinkConfig = extractConfiguration(deployRequest.flinkVersion.flinkHome,
        null,
        deployRequest.flameGraph,
        deployRequest.dynamicOption,
        deployRequest.extraParameter,
        deployRequest.resolveOrder)
      flinkConfig.safeSet(DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName)
      val yarnClusterDescriptor = getSessionClusterDeployDescriptor(flinkConfig)
      clusterDescriptor = yarnClusterDescriptor._2
      if (null != deployRequest.clusterId && deployRequest.clusterId.nonEmpty) {
        try {
          val applicationStatus = clusterDescriptor.getYarnClient.getApplicationReport(ConverterUtils.toApplicationId(deployRequest.clusterId)).getFinalApplicationStatus
          if (FinalApplicationStatus.UNDEFINED.equals(applicationStatus)) {
            //application is running
            val yarnClient = clusterDescriptor.retrieve(ApplicationId.fromString(deployRequest.clusterId)).getClusterClient
            if (yarnClient.getWebInterfaceURL != null) {
              return DeployResponse(yarnClient.getWebInterfaceURL, yarnClient.getClusterId.toString)
            }
          }
        } catch {
          case _: ApplicationNotFoundException => logInfo("this applicationId have not managed by yarn ,need deploy ...")
        }
      }
      val clientProvider = clusterDescriptor.deploySessionCluster(yarnClusterDescriptor._1)
      client = clientProvider.getClusterClient
      if (client.getWebInterfaceURL != null) {
        DeployResponse(client.getWebInterfaceURL, client.getClusterId.toString)
      } else {
        null
      }
    } catch {
      case e: Exception => logError(s"start flink session fail in ${deployRequest.executionMode} mode")
        e.printStackTrace()
        throw e
    } finally {
      Utils.close(client, clusterDescriptor)
    }
  }

  def shutdown(shutDownRequest: ShutDownRequest): ShutDownResponse = {
    var clusterDescriptor: YarnClusterDescriptor = null
    var client: ClusterClient[ApplicationId] = null
    try {
      val flinkConfig = getFlinkDefaultConfiguration(shutDownRequest.flinkVersion.flinkHome)
      shutDownRequest.extraParameter.foreach(m => m._2 match {
        case v if v != null => flinkConfig.setString(m._1, m._2.toString)
        case _ =>
      })
      flinkConfig.safeSet(YarnConfigOptions.APPLICATION_ID, shutDownRequest.clusterId)
      flinkConfig.safeSet(DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName)
      val yarnClusterDescriptor = getSessionClusterDescriptor(flinkConfig)
      clusterDescriptor = yarnClusterDescriptor._2
      if (FinalApplicationStatus.UNDEFINED.equals(clusterDescriptor.getYarnClient.getApplicationReport(ApplicationId.fromString(shutDownRequest.clusterId)).getFinalApplicationStatus)) {
        val clientProvider = clusterDescriptor.retrieve(yarnClusterDescriptor._1)
        clientProvider.getClusterClient.shutDownCluster()
      }
      logInfo(s"the ${shutDownRequest.clusterId}'s final status is ${clusterDescriptor.getYarnClient.getApplicationReport(ConverterUtils.toApplicationId(shutDownRequest.clusterId)).getFinalApplicationStatus}")
      ShutDownResponse()
    } catch {
      case e: Exception => logError(s"shutdown flink session fail in ${shutDownRequest.executionMode} mode")
        e.printStackTrace()
        throw e
    } finally {
      Utils.close(client, clusterDescriptor)
    }
  }

  private[this] def getYarnSessionClusterDescriptor(flinkConfig: Configuration): (ApplicationId, YarnClusterDescriptor) = {
    val serviceLoader = new DefaultClusterClientServiceLoader
    val clientFactory = serviceLoader.getClusterClientFactory[ApplicationId](flinkConfig)
    val yarnClusterId: ApplicationId = clientFactory.getClusterId(flinkConfig)
    require(yarnClusterId != null)
    val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig).asInstanceOf[YarnClusterDescriptor]
    (yarnClusterId, clusterDescriptor)
  }

}
