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

import org.apache.streampark.common.conf.Workspace
import org.apache.streampark.common.enums.DevelopmentMode
import org.apache.streampark.common.util.HdfsUtils
import org.apache.streampark.flink.client.`trait`.YarnClientTrait
import org.apache.streampark.flink.client.bean._
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse

import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.configuration._
import org.apache.flink.runtime.security.{SecurityConfiguration, SecurityUtils}
import org.apache.flink.runtime.util.HadoopUtils
import org.apache.flink.yarn.YarnClusterDescriptor
import org.apache.flink.yarn.configuration.{YarnConfigOptions, YarnDeploymentTarget}
import org.apache.hadoop.security.UserGroupInformation
import org.apache.hadoop.yarn.api.records.ApplicationId

import java.util.Collections
import java.util.concurrent.Callable

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/** yarn application mode submit */
object YarnApplicationClient extends YarnClientTrait {

  private[this] lazy val workspace = Workspace.remote

  override def setConfig(submitRequest: SubmitRequest, flinkConfig: Configuration): Unit = {
    super.setConfig(submitRequest, flinkConfig)
    // val flinkDefaultConfiguration = getFlinkDefaultConfiguration(
    //   submitRequest.flinkVersion.flinkHome)
    // val currentUser = UserGroupInformation.getCurrentUser
    // logDebug(s"UserGroupInformation currentUser: $currentUser")
    // if (HadoopUtils.isKerberosSecurityEnabled(currentUser)) {
    //   logDebug(s"kerberos Security is Enabled...")
    //   val useTicketCache =
    //     flinkDefaultConfiguration.get(SecurityOptions.KERBEROS_LOGIN_USETICKETCACHE)
    //   if (!HadoopUtils.areKerberosCredentialsValid(currentUser, useTicketCache)) {
    //     throw new RuntimeException(
    //       s"Hadoop security with Kerberos is enabled but the login user $currentUser does not have Kerberos credentials or delegation tokens!")
    //   }
    // }
    val providedLibs = {
      val array = ListBuffer(
        submitRequest.hdfsWorkspace.flinkLib,
        submitRequest.hdfsWorkspace.appJars
      )
      val jobLib = s"${workspace.APP_WORKSPACE}/${submitRequest.id}/lib"
      if (HdfsUtils.exists(jobLib)) {
        array += jobLib
      }
      if (submitRequest.developmentMode == DevelopmentMode.FLINK_SQL) {
        array += s"${workspace.APP_SHIMS}/flink-${submitRequest.flinkVersion.majorVersion}"
      }
      array.toList
    }

    flinkConfig
      // yarn.provided.lib.dirs
      .safeSet(YarnConfigOptions.PROVIDED_LIB_DIRS, providedLibs.asJava)
      // flinkDistJar
      .safeSet(YarnConfigOptions.FLINK_DIST_JAR, submitRequest.hdfsWorkspace.flinkDistJar)
      // pipeline.jars
      .safeSet(
        PipelineOptions.JARS,
        Collections.singletonList(
          submitRequest.buildResult.asInstanceOf[ShadedBuildResponse].shadedJarPath))

    logInfo(s"""
               |------------------------------------------------------------------
               |Effective submit configuration: $flinkConfig
               |------------------------------------------------------------------
               |""".stripMargin)
  }

  override def doSubmit(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration): SubmitResponse = {
    SecurityUtils.install(new SecurityConfiguration(flinkConfig))
    SecurityUtils.getInstalledContext.runSecured(new Callable[SubmitResponse] {
      override def call(): SubmitResponse = {
        val (clusterSpecification, clusterDescriptor: YarnClusterDescriptor) =
          getYarnClusterDeployDescriptor(flinkConfig)
        logInfo(s"""
                   |------------------------<<specification>>-------------------------
                   |$clusterSpecification
                   |------------------------------------------------------------------
                   |""".stripMargin)

        val applicationConfiguration = ApplicationConfiguration.fromConfiguration(flinkConfig)
        val clusterClient = clusterDescriptor
          .deployApplicationCluster(clusterSpecification, applicationConfiguration)
          .getClusterClient
        val applicationId = clusterClient.getClusterId
        val jobManagerUrl = clusterClient.getWebInterfaceURL
        logInfo(s"""
                   |-------------------------<<applicationId>>------------------------
                   |Flink Job Started: applicationId: $applicationId
                   |__________________________________________________________________
                   |""".stripMargin)

        val resp =
          SubmitResponse(applicationId.toString, flinkConfig.toMap, jobManagerUrl = jobManagerUrl)
        closeSubmit(submitRequest, clusterDescriptor, clusterClient)
        resp
      }
    })
  }

  override def doCancel(cancelRequest: CancelRequest, flinkConf: Configuration): CancelResponse = {
    flinkConf
      .safeSet(DeploymentOptions.TARGET, YarnDeploymentTarget.APPLICATION.getName)
    super.doCancel(cancelRequest, flinkConf)
  }

}
