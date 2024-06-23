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

import org.apache.streampark.common.Constant
import org.apache.streampark.common.conf.Workspace
import org.apache.streampark.common.enums.FlinkDevelopmentMode
import org.apache.streampark.common.fs.FsOperator
import org.apache.streampark.common.util.{AssertUtils, FileUtils, HdfsUtils, Utils}
import org.apache.streampark.flink.client.`trait`.YarnClientTrait
import org.apache.streampark.flink.client.bean._
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse

import org.apache.commons.lang3.StringUtils
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration._
import org.apache.flink.python.PythonOptions
import org.apache.flink.runtime.util.HadoopUtils
import org.apache.flink.yarn.configuration.YarnConfigOptions
import org.apache.hadoop.security.UserGroupInformation
import org.apache.hadoop.yarn.api.records.ApplicationId

import java.security.PrivilegedAction
import java.util
import java.util.Collections

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/** yarn application mode submit */
object YarnApplicationClient extends YarnClientTrait {

  private[this] lazy val workspace = Workspace.remote

  override def setConfig(submitRequest: SubmitRequest, flinkConfig: Configuration): Unit = {
    val flinkDefaultConfiguration = getFlinkDefaultConfiguration(
      submitRequest.flinkVersion.flinkHome)
    val currentUser = UserGroupInformation.getCurrentUser
    logDebug(s"UserGroupInformation currentUser: $currentUser")
    if (HadoopUtils.isKerberosSecurityEnabled(currentUser)) {
      logDebug(s"kerberos Security is Enabled...")
      val useTicketCache =
        flinkDefaultConfiguration.get(SecurityOptions.KERBEROS_LOGIN_USETICKETCACHE)
      AssertUtils.required(
        HadoopUtils.areKerberosCredentialsValid(currentUser, useTicketCache),
        s"Hadoop security with Kerberos is enabled but the login user $currentUser does not have Kerberos credentials or delegation tokens!"
      )
    }
    val providedLibs = {
      val array = ListBuffer(
        submitRequest.hdfsWorkspace.flinkLib,
        submitRequest.hdfsWorkspace.flinkPlugins,
        submitRequest.hdfsWorkspace.appJars,
        submitRequest.hdfsWorkspace.appPlugins
      )
      submitRequest.developmentMode match {
        case FlinkDevelopmentMode.FLINK_SQL =>
          array += s"${workspace.APP_SHIMS}/flink-${submitRequest.flinkVersion.majorVersion}"
          val jobLib = s"${workspace.APP_WORKSPACE}/${submitRequest.id}/lib"
          if (HdfsUtils.exists(jobLib)) {
            array += jobLib
          }
        case _ =>
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
      // yarn application name
      .safeSet(YarnConfigOptions.APPLICATION_NAME, submitRequest.effectiveAppName)
      // yarn application Type
      .safeSet(YarnConfigOptions.APPLICATION_TYPE, submitRequest.applicationType.getName)

    if (submitRequest.developmentMode == FlinkDevelopmentMode.PYFLINK) {
      val pyVenv: String = workspace.APP_PYTHON_VENV
      AssertUtils.required(FsOperator.hdfs.exists(pyVenv), s"$pyVenv File does not exist")

      val localLib: String = s"${Workspace.local.APP_WORKSPACE}/${submitRequest.id}/lib"
      if (FileUtils.exists(localLib) && FileUtils.directoryNotBlank(localLib)) {
        flinkConfig.safeSet(PipelineOptions.JARS, util.Arrays.asList(localLib))
      }

      // yarn.ship-files
      val shipFiles = new util.ArrayList[String]()
      shipFiles.add(submitRequest.userJarFile.getParentFile.getAbsolutePath)

      flinkConfig
        .safeSet(YarnConfigOptions.SHIP_FILES, shipFiles)
        // python.files
        .safeSet(PythonOptions.PYTHON_FILES, submitRequest.userJarFile.getParentFile.getName)
        // python.archives
        .safeSet(PythonOptions.PYTHON_ARCHIVES, pyVenv)
        // python.client.executable
        .safeSet(PythonOptions.PYTHON_CLIENT_EXECUTABLE, Constant.PYTHON_EXECUTABLE)
        // python.executable
        .safeSet(PythonOptions.PYTHON_EXECUTABLE, Constant.PYTHON_EXECUTABLE)

      val args: util.List[String] = flinkConfig.get(ApplicationConfiguration.APPLICATION_ARGS)
      // Caused by: java.lang.UnsupportedOperationException
      val argsList: util.ArrayList[String] = new util.ArrayList[String](args)
      argsList.add("-pym")
      argsList.add(submitRequest.userJarFile.getName.dropRight(Constant.PYTHON_SUFFIX.length))
      flinkConfig.safeSet(ApplicationConfiguration.APPLICATION_ARGS, argsList)
    }

    logInfo(s"""
               |------------------------------------------------------------------
               |Effective submit configuration: $flinkConfig
               |------------------------------------------------------------------
               |""".stripMargin)
  }

  override def doSubmit(
      submitRequest: SubmitRequest,
      flinkConfig: Configuration): SubmitResponse = {
    var proxyUserUgi: UserGroupInformation = UserGroupInformation.getCurrentUser
    val currentUser = UserGroupInformation.getCurrentUser
    val enableProxyState =
      !HadoopUtils.isKerberosSecurityEnabled(currentUser) && StringUtils.isNotEmpty(
        submitRequest.hadoopUser)
    if (enableProxyState) {
      proxyUserUgi = UserGroupInformation.createProxyUser(
        submitRequest.hadoopUser,
        currentUser
      )
    }

    proxyUserUgi.doAs[SubmitResponse](new PrivilegedAction[SubmitResponse] {
      override def run(): SubmitResponse = {
        val clusterClientServiceLoader = new DefaultClusterClientServiceLoader
        val clientFactory =
          clusterClientServiceLoader.getClusterClientFactory[ApplicationId](flinkConfig)
        val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig)
        val clusterSpecification = clientFactory.getClusterSpecification(flinkConfig)
        logInfo(s"""
                   |------------------------<<specification>>-------------------------
                   |$clusterSpecification
                   |------------------------------------------------------------------
                   |""".stripMargin)

        val applicationConfiguration = ApplicationConfiguration.fromConfiguration(flinkConfig)
        var applicationId: ApplicationId = null
        var jobManagerUrl: String = null
        val clusterClient = clusterDescriptor
          .deployApplicationCluster(clusterSpecification, applicationConfiguration)
          .getClusterClient
        applicationId = clusterClient.getClusterId
        jobManagerUrl = clusterClient.getWebInterfaceURL
        logInfo(s"""
                   |-------------------------<<applicationId>>------------------------
                   |Flink Job Started: applicationId: $applicationId
                   |__________________________________________________________________
                   |""".stripMargin)

        val resp =
          SubmitResponse(applicationId.toString, flinkConfig.toMap, jobManagerUrl = jobManagerUrl)
        closeSubmit(submitRequest, clusterClient, clusterDescriptor)
        resp
      }
    })
  }

}
