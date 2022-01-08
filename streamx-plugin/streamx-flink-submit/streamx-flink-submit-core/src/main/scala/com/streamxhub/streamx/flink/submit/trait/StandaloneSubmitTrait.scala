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

package com.streamxhub.streamx.flink.submit.`trait`

import com.streamxhub.streamx.common.conf.ConfigConst.KEY_FLINK_PARALLELISM
import com.streamxhub.streamx.common.conf.Workspace
import com.streamxhub.streamx.common.enums.DevelopmentMode
import com.streamxhub.streamx.common.fs.FsOperator
import com.streamxhub.streamx.common.util.StandaloneUtils
import com.streamxhub.streamx.flink.submit.FlinkSubmitHelper.extractDynamicOption
import com.streamxhub.streamx.flink.submit.domain.{StopRequest, StopResponse, SubmitRequest, SubmitResponse}
import org.apache.commons.collections.MapUtils
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.deployment.{DefaultClusterClientServiceLoader, StandaloneClusterDescriptor, StandaloneClusterId}
import org.apache.flink.configuration._

import javax.annotation.Nonnull
import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

/**
 * @Description Remote Submit
 */
trait StandaloneSubmitTrait extends FlinkSubmitTrait {

  lazy val workspace: Workspace = Workspace.local

  override def submit(submitRequest: SubmitRequest): SubmitResponse = super.submit(submitRequest)

  override def stop(stopRequest: StopRequest): StopResponse = super.stop(stopRequest)

  /**
   * create StandAloneClusterDescriptor
   * @param flinkConfig
   */
  def getStandAloneClusterDescriptor(flinkConfig: Configuration): (StandaloneClusterId, StandaloneClusterDescriptor) = {
    val serviceLoader = new DefaultClusterClientServiceLoader
    val clientFactory = serviceLoader.getClusterClientFactory(flinkConfig)
    val standaloneClusterId: StandaloneClusterId = clientFactory.getClusterId(flinkConfig)
    val standaloneClusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig).asInstanceOf[StandaloneClusterDescriptor]
    (standaloneClusterId, standaloneClusterDescriptor)
  }

  /**
   * submitRequest to flinkConfig
   * @param submitRequest
   * @return
   */
  @Nonnull def extractEffectiveFlinkConfig(@Nonnull submitRequest: SubmitRequest): Configuration = {
    // load base flink config from flinkHome config
    val flinkConfig = Try(GlobalConfiguration.loadConfiguration(submitRequest.flinkVersion.flinkHome + "/conf")).getOrElse(new Configuration)
    flinkConfig.set(DeploymentOptions.TARGET, submitRequest.executionMode.getName)
      .set(PipelineOptions.NAME, submitRequest.appName)
      .set(CoreOptions.CLASSLOADER_RESOLVE_ORDER, submitRequest.resolveOrder.getName)
    // developmentMode select
    if (DevelopmentMode.FLINKSQL == submitRequest.developmentMode) {
      flinkConfig.set(ApplicationConfiguration.APPLICATION_MAIN_CLASS, "com.streamxhub.streamx.flink.cli.SqlClient")
    } else {
      flinkConfig.set(ApplicationConfiguration.APPLICATION_MAIN_CLASS, submitRequest.appMain)
    }
    // copy from submitRequest.dynamicOption
    extractDynamicOption(submitRequest.dynamicOption)
      .foreach(e => flinkConfig.setString(e._1, e._2))
    // if not set jm address and port, use the default set jm address and warn
    checkAndReplaceRestOptions(flinkConfig)
    // copy from submitRequest.property
    if (MapUtils.isNotEmpty(submitRequest.property)) {
      submitRequest.property
        .filter(_._2 != null)
        .foreach(e => flinkConfig.setString(e._1, e._2.toString))
    }
    val args = extractProgramArgs(submitRequest)
    flinkConfig.set(ApplicationConfiguration.APPLICATION_ARGS, args.toList.asJava)
    if (submitRequest.property.containsKey(KEY_FLINK_PARALLELISM())) {
      flinkConfig.set(CoreOptions.DEFAULT_PARALLELISM, Integer.valueOf(submitRequest.property.get(KEY_FLINK_PARALLELISM()).toString))
    } else {
      flinkConfig.set(CoreOptions.DEFAULT_PARALLELISM,
        CoreOptions.DEFAULT_PARALLELISM.defaultValue())
    }
    flinkConfig
  }

  private[submit] def extractProgramArgs(submitRequest: SubmitRequest): ArrayBuffer[String] = {
    val programArgs = new ArrayBuffer[String]()
    Try(submitRequest.args.split("\\s+")).getOrElse(Array()).foreach(x => if (x.nonEmpty) programArgs += x)
    programArgs += PARAM_KEY_FLINK_CONF
    programArgs += submitRequest.flinkYaml
    programArgs += PARAM_KEY_APP_NAME
    programArgs += submitRequest.effectiveAppName
    submitRequest.developmentMode match {
      case DevelopmentMode.FLINKSQL =>
        programArgs += PARAM_KEY_FLINK_SQL
        programArgs += submitRequest.flinkSQL
        if (submitRequest.appConf != null) {
          programArgs += PARAM_KEY_APP_CONF
          programArgs += submitRequest.appConf
        }
      case _ =>
        // Custom Code 必传配置文件...
        programArgs += PARAM_KEY_APP_CONF
        programArgs += submitRequest.appConf
    }
    programArgs
  }

  private[submit] def checkAndReplaceRestOptions(flinkConfig: Configuration): Unit = {
    if (!flinkConfig.contains(JobManagerOptions.ADDRESS) && !flinkConfig.contains(RestOptions.ADDRESS)) {
      logWarn("RestOptions Address is not set,use default value : localhost to replace it")
      flinkConfig.setString(RestOptions.ADDRESS, StandaloneUtils.DEFAULT_REST_ADDRESS)
    }
    if (!flinkConfig.contains(RestOptions.PORT)) {
      logWarn("RestOptions port is not set,use default value : 8081 to replace it")
      flinkConfig.setInteger(RestOptions.PORT, StandaloneUtils.DEFAULT_REST_PORT)
    }
  }


  private[submit] def extractProvidedLibs(submitRequest: SubmitRequest): Set[String] = {
    val providedLibs = ArrayBuffer(
      // flinkLib,
      workspace.APP_JARS,
      workspace.APP_PLUGINS,
      submitRequest.flinkUserJar
    )
    providedLibs += {
      val version = submitRequest.flinkVersion.version.split("\\.").map(_.trim.toInt)
      version match {
        case Array(1, 12, _) => s"${workspace.APP_SHIMS}/flink-1.12"
        case Array(1, 13, _) => s"${workspace.APP_SHIMS}/flink-1.13"
        case Array(1, 14, _) => s"${workspace.APP_SHIMS}/flink-1.14"
        case _ => throw new UnsupportedOperationException(s"Unsupported flink version: ${submitRequest.flinkVersion}")
      }
    }
    val jobLib = s"${workspace.APP_WORKSPACE}/${submitRequest.jobID}/lib"
    if (FsOperator.lfs.exists(jobLib)) {
      providedLibs += jobLib
    }
    val libSet = providedLibs.toSet
    libSet
  }

}
