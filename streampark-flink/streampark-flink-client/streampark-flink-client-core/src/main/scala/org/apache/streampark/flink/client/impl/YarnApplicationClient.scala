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

import org.apache.streampark.common.conf.{ConfigConst, Workspace}
import org.apache.streampark.common.enums.DevelopmentMode
import org.apache.streampark.common.fs.FsOperator
import org.apache.streampark.common.util.{HdfsUtils, Utils}
import org.apache.streampark.flink.client.`trait`.YarnClientTrait
import org.apache.streampark.flink.client.bean._
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse
import org.apache.streampark.flink.util.FlinkUtils

import org.apache.commons.lang3.StringUtils
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.ClusterClient
import org.apache.flink.configuration._
import org.apache.flink.python.PythonOptions
import org.apache.flink.runtime.security.{SecurityConfiguration, SecurityUtils}
import org.apache.flink.runtime.util.HadoopUtils
import org.apache.flink.yarn.configuration.YarnConfigOptions
import org.apache.hadoop.security.UserGroupInformation
import org.apache.hadoop.yarn.api.records.ApplicationId

import java.io.File
import java.util
import java.util.Collections
import java.util.concurrent.Callable

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
      if (!HadoopUtils.areKerberosCredentialsValid(currentUser, useTicketCache)) {
        throw new RuntimeException(
          s"Hadoop security with Kerberos is enabled but the login user $currentUser does not have Kerberos credentials or delegation tokens!")
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
        case DevelopmentMode.FLINK_SQL =>
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

    if (StringUtils.isNotBlank(submitRequest.pyflinkFilePath)) {
      val pythonVenv: String = workspace.APP_PYTHON_VENV
      if (!FsOperator.hdfs.exists(pythonVenv)) {
        throw new RuntimeException(s"$pythonVenv File does not exist")
      }
      val pyflinkFile: File = new File(submitRequest.pyflinkFilePath)

      val argList = new util.ArrayList[String]()
      argList.add("-pym")
      argList.add(pyflinkFile.getName.replace(ConfigConst.PYTHON_SUFFIX, ""))

      val pythonFlinkconnectorJars: String =
        FlinkUtils.getPythonFlinkconnectorJars(submitRequest.flinkVersion.flinkHome)
      if (StringUtils.isNotBlank(pythonFlinkconnectorJars)) {
        flinkConfig.setString(PipelineOptions.JARS.key(), pythonFlinkconnectorJars)
      }

      // yarn.ship-files
      flinkConfig.setString(
        YarnConfigOptions.SHIP_FILES.key(),
        pyflinkFile.getParentFile.getAbsolutePath)

      flinkConfig
        // python.archives
        .safeSet(PythonOptions.PYTHON_ARCHIVES, pythonVenv)
        // python.client.executable
        .safeSet(PythonOptions.PYTHON_CLIENT_EXECUTABLE, ConfigConst.PYTHON_EXECUTABLE)
        // python.executable
        .safeSet(PythonOptions.PYTHON_EXECUTABLE, ConfigConst.PYTHON_EXECUTABLE)
        // python.files
        .safeSet(PythonOptions.PYTHON_FILES, pyflinkFile.getParentFile.getName)
        .safeSet(
          ApplicationConfiguration.APPLICATION_MAIN_CLASS,
          ConfigConst.PYTHON_DRIVER_CLASS_NAME)
        .safeSet(ApplicationConfiguration.APPLICATION_ARGS, argList)
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
    SecurityUtils.install(new SecurityConfiguration(flinkConfig))
    SecurityUtils.getInstalledContext.runSecured(
      () => {
        val clusterClientServiceLoader = new DefaultClusterClientServiceLoader
        val clientFactory =
          clusterClientServiceLoader.getClusterClientFactory[ApplicationId](flinkConfig)
        val clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig)
        var clusterClient: ClusterClient[ApplicationId] = null
        try {
          val clusterSpecification = clientFactory.getClusterSpecification(flinkConfig)
          logInfo(s"""
                     |------------------------<<specification>>-------------------------
                     |$clusterSpecification
                     |------------------------------------------------------------------
                     |""".stripMargin)

          val applicationConfiguration = ApplicationConfiguration.fromConfiguration(flinkConfig)
          var applicationId: ApplicationId = null
          var jobManagerUrl: String = null
          clusterClient = clusterDescriptor
            .deployApplicationCluster(clusterSpecification, applicationConfiguration)
            .getClusterClient
          applicationId = clusterClient.getClusterId
          jobManagerUrl = clusterClient.getWebInterfaceURL
          logInfo(s"""
                     |-------------------------<<applicationId>>------------------------
                     |Flink Job Started: applicationId: $applicationId
                     |__________________________________________________________________
                     |""".stripMargin)

          SubmitResponse(applicationId.toString, flinkConfig.toMap, jobManagerUrl = jobManagerUrl)
        } finally {
          Utils.close(clusterDescriptor, clusterClient)
        }
      })
  }

}
