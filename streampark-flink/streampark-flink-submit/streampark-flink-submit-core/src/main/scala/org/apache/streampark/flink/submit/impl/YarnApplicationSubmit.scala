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

package org.apache.streampark.flink.submit.impl

import org.apache.streampark.common.conf.Workspace
import org.apache.streampark.common.enums.DevelopmentMode
import org.apache.streampark.common.util.{HdfsUtils, Utils}
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse
import org.apache.streampark.flink.submit.`trait`.YarnSubmitTrait
import org.apache.streampark.flink.submit.bean._
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration._
import org.apache.flink.runtime.security.{SecurityConfiguration, SecurityUtils}
import org.apache.flink.runtime.util.HadoopUtils
import org.apache.flink.yarn.configuration.YarnConfigOptions
import org.apache.hadoop.security.UserGroupInformation
import org.apache.hadoop.yarn.api.records.ApplicationId

import java.util.Collections
import java.util.concurrent.Callable
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/**
 * yarn application mode submit
 */
object YarnApplicationSubmit extends YarnSubmitTrait {

  private[this] lazy val workspace = Workspace.remote

  override def setConfig(submitRequest: SubmitRequest, flinkConfig: Configuration): Unit = {
    val flinkDefaultConfiguration = getFlinkDefaultConfiguration(submitRequest.flinkVersion.flinkHome)
    val currentUser = UserGroupInformation.getCurrentUser
    logDebug(s"UserGroupInformation currentUser: $currentUser")
    if (HadoopUtils.isKerberosSecurityEnabled(currentUser)) {
      logDebug(s"kerberos Security is Enabled...")
      val useTicketCache = flinkDefaultConfiguration.get(SecurityOptions.KERBEROS_LOGIN_USETICKETCACHE)
      if (!HadoopUtils.areKerberosCredentialsValid(currentUser, useTicketCache)) {
        throw new RuntimeException(s"Hadoop security with Kerberos is enabled but the login user ${currentUser} does not have Kerberos credentials or delegation tokens!")
      }
    }
    val providedLibs = {
      val array = ListBuffer(
        submitRequest.hdfsWorkspace.flinkLib,
        submitRequest.hdfsWorkspace.flinkPlugins,
        submitRequest.hdfsWorkspace.appJars,
        submitRequest.hdfsWorkspace.appPlugins
      )
      submitRequest.developmentMode match {
        case DevelopmentMode.FLINKSQL =>
          val version = submitRequest.flinkVersion.version.split("\\.").map(_.trim.toInt)
          version match {
            case Array(1, 12, _) => array += s"${workspace.APP_SHIMS}/flink-1.12"
            case Array(1, 13, _) => array += s"${workspace.APP_SHIMS}/flink-1.13"
            case Array(1, 14, _) => array += s"${workspace.APP_SHIMS}/flink-1.14"
            case Array(1, 15, _) => array += s"${workspace.APP_SHIMS}/flink-1.15"
            case Array(1, 16, _) => array += s"${workspace.APP_SHIMS}/flink-1.16"
            case _ => throw new UnsupportedOperationException(s"Unsupported flink version: ${submitRequest.flinkVersion}")
          }
          val jobLib = s"${workspace.APP_WORKSPACE}/${submitRequest.id}/lib"
          if (HdfsUtils.exists(jobLib)) {
            array += jobLib
          }
        case _ =>
      }
      array.toList
    }

    flinkConfig
      //yarn.provided.lib.dirs
      .safeSet(YarnConfigOptions.PROVIDED_LIB_DIRS, providedLibs.asJava)
      //flinkDistJar
      .safeSet(YarnConfigOptions.FLINK_DIST_JAR, submitRequest.hdfsWorkspace.flinkDistJar)
      //pipeline.jars
      .safeSet(PipelineOptions.JARS, Collections.singletonList(submitRequest.buildResult.asInstanceOf[ShadedBuildResponse].shadedJarPath))
      //yarn application name
      .safeSet(YarnConfigOptions.APPLICATION_NAME, submitRequest.effectiveAppName)
      //yarn application Type
      .safeSet(YarnConfigOptions.APPLICATION_TYPE, submitRequest.applicationType.getName)

    logInfo(
      s"""
         |------------------------------------------------------------------
         |Effective submit configuration: $flinkConfig
         |------------------------------------------------------------------
         |""".stripMargin)
  }

  override def doSubmit(submitRequest: SubmitRequest, flinkConfig: Configuration): SubmitResponse = {
    SecurityUtils.install(new SecurityConfiguration(flinkConfig))
    SecurityUtils.getInstalledContext.runSecured(new Callable[SubmitResponse] {
      override def call(): SubmitResponse = {
        val clusterClientServiceLoader = new DefaultClusterClientServiceLoader
        val clientFactory = clusterClientServiceLoader.getClusterClientFactory[ApplicationId](flinkConfig)
        val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig)
        var clusterClient: ClusterClient[ApplicationId] = null
        try {
          val clusterSpecification = clientFactory.getClusterSpecification(flinkConfig)
          logInfo(
            s"""
               |------------------------<<specification>>-------------------------
               |$clusterSpecification
               |------------------------------------------------------------------
               |""".stripMargin)

          val applicationConfiguration = ApplicationConfiguration.fromConfiguration(flinkConfig)
          var applicationId: ApplicationId = null
          var jobManagerUrl: String = null
          clusterClient = clusterDescriptor.deployApplicationCluster(clusterSpecification, applicationConfiguration).getClusterClient
          applicationId = clusterClient.getClusterId
          jobManagerUrl = clusterClient.getWebInterfaceURL
          logInfo(
            s"""
               |-------------------------<<applicationId>>------------------------
               |Flink Job Started: applicationId: $applicationId
               |__________________________________________________________________
               |""".stripMargin)

          SubmitResponse(applicationId.toString, flinkConfig.toMap, jobManagerUrl = jobManagerUrl)
        } finally {
          Utils.close(clusterDescriptor, clusterClient)
        }
      }
    })
  }

}
