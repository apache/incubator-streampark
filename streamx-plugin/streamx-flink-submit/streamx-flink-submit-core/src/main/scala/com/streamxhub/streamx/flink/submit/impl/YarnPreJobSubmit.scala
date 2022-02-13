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

import com.streamxhub.streamx.common.util.FlinkUtils
import com.streamxhub.streamx.flink.submit.`trait`.YarnSubmitTrait
import com.streamxhub.streamx.flink.submit.domain._
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.{PackagedProgram, PackagedProgramUtils}
import org.apache.flink.configuration.{Configuration, DeploymentOptions}
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings
import org.apache.flink.yarn.YarnClusterDescriptor
import org.apache.flink.yarn.configuration.YarnDeploymentTarget
import org.apache.flink.yarn.entrypoint.YarnJobClusterEntrypoint
import org.apache.hadoop.fs.{Path => HadoopPath}
import org.apache.hadoop.yarn.api.records.ApplicationId

import java.io.File
import scala.collection.JavaConversions._
import scala.util.Try

/**
 * yarn PreJob mode submit
 */
@deprecated
object YarnPreJobSubmit extends YarnSubmitTrait {

  override def doSubmit(submitRequest: SubmitRequest, flinkConfig: Configuration): SubmitResponse = {

    setJobSpecificConfig(submitRequest, flinkConfig)

    val flinkHome = submitRequest.flinkVersion.flinkHome

    val clusterClientServiceLoader = new DefaultClusterClientServiceLoader
    val clientFactory = clusterClientServiceLoader.getClusterClientFactory[ApplicationId](flinkConfig)

    val clusterDescriptor = {
      val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig).asInstanceOf[YarnClusterDescriptor]
      val flinkDistJar = FlinkUtils.getFlinkDistJar(flinkHome)
      clusterDescriptor.setLocalJarPath(new HadoopPath(flinkDistJar))
      clusterDescriptor.addShipFiles(List(new File(s"${flinkHome}/plugins")))
      clusterDescriptor
    }

    try {
      val clusterClient = {
        val clusterSpecification = clientFactory.getClusterSpecification(flinkConfig)
        logInfo(
          s"""
             |------------------------<<specification>>-------------------------
             |$clusterSpecification
             |------------------------------------------------------------------
             |""".stripMargin)

        val savepointRestoreSettings = {
          // 判断参数 submitRequest.option 中是否包涵 -n 参数；赋值 allowNonRestoredState: true or false
          lazy val allowNonRestoredState = Try(submitRequest.option.split("\\s+").contains("-n")).getOrElse(false)
          submitRequest.savePoint match {
            case sp if Try(sp.isEmpty).getOrElse(true) => SavepointRestoreSettings.none
            case sp => SavepointRestoreSettings.forPath(sp, allowNonRestoredState)
          }
        }

        logInfo(
          s"""
             |------------------------<<savepointRestoreSettings>>--------------
             |$savepointRestoreSettings
             |------------------------------------------------------------------
             |""".stripMargin)

        val packagedProgram = PackagedProgram
          .newBuilder
          .setSavepointRestoreSettings(savepointRestoreSettings)
          .setJarFile(new File(submitRequest.flinkUserJar))
          .setEntryPointClassName(flinkConfig.getOptional(ApplicationConfiguration.APPLICATION_MAIN_CLASS).get())
          .setArguments(
            flinkConfig
              .getOptional(ApplicationConfiguration.APPLICATION_ARGS)
              .get()
              : _*
          ).build

        val jobGraph = PackagedProgramUtils.createJobGraph(
          packagedProgram,
          flinkConfig,
          getParallelism(submitRequest),
          false
        )
        logInfo(
          s"""
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
          false
        ).getClusterClient

      }
      val applicationId = clusterClient.getClusterId
      logInfo(
        s"""
           |-------------------------<<applicationId>>------------------------
           |Flink Job Started: applicationId: $applicationId
           |__________________________________________________________________
           |""".stripMargin)

      SubmitResponse(applicationId.toString, flinkConfig.toMap)
    } finally if (clusterDescriptor != null) {
      clusterDescriptor.close()
    }
  }

  private def setJobSpecificConfig[T](submitRequest: SubmitRequest, flinkConfig: Configuration) = {
    //execution.target
    flinkConfig.set(DeploymentOptions.TARGET, YarnDeploymentTarget.PER_JOB.getName)
    logInfo(
      s"""
         |------------------------------------------------------------------
         |Effective executor configuration: $flinkConfig
         |------------------------------------------------------------------
         |""".stripMargin)

    flinkConfig
  }

}
