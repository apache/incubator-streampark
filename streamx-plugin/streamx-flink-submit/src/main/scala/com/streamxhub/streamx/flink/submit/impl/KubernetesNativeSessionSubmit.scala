/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.flink.submit.impl

import com.streamxhub.streamx.common.enums.ExecutionMode
import com.streamxhub.streamx.flink.submit.`trait`.KubernetesNativeSubmitTrait
import com.streamxhub.streamx.flink.submit.{SubmitRequest, SubmitResponse}
import org.apache.flink.client.deployment.application.ApplicationConfiguration
import org.apache.flink.client.program.{PackagedProgram, PackagedProgramUtils}
import org.apache.flink.configuration._
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions

import java.io.File
import java.lang
import scala.collection.JavaConverters._

/**
 * kubernetes native session mode submit
 */
object KubernetesNativeSessionSubmit extends KubernetesNativeSubmitTrait {

  // todo request refactoring of submitRequest
  override def doSubmit(submitRequest: SubmitRequest): SubmitResponse = {
    val flinkConfig = extractEffectiveFlinkConfig(submitRequest)
    assert(flinkConfig.getOptional(KubernetesConfigOptions.CLUSTER_ID).isPresent)

    val clusterDescriptor = getK8sClusterDescriptor(flinkConfig)

    // build JobGraph
    val packageProgram = PackagedProgram.newBuilder()
      .setJarFile(new File(submitRequest.flinkUserJar)) // todo request to refactor StreamX 's file system abstraction
      .setEntryPointClassName(flinkConfig.getOptional(ApplicationConfiguration.APPLICATION_MAIN_CLASS).get())
      .setArguments(flinkConfig.getOptional(ApplicationConfiguration.APPLICATION_ARGS).get().asScala: _*)
      .build()
    val jobGraph = PackagedProgramUtils.createJobGraph(
      packageProgram,
      flinkConfig,
      flinkConfig.getInteger(CoreOptions.DEFAULT_PARALLELISM),
      false)

    // retrieve client and submit JobGraph
    val client = clusterDescriptor.retrieve(flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID)).getClusterClient
    val submitResult = client.submitJob(jobGraph)

    val jobId = submitResult.get().toString

    client.close()
    packageProgram.close()
    clusterDescriptor.close()

    // todo request refactoring of SubmitResponse
    SubmitResponse(null, flinkConfig)
  }


  override def doStop(flinkHome: String, appId: String, jobStringId: String, savePoint: lang.Boolean, drain: lang.Boolean): String = {
    doStop(ExecutionMode.KUBERNETES_NATIVE_SESSION, flinkHome, appId, jobStringId, savePoint, drain)
  }


}
