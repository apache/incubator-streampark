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

package com.streamxhub.streamx.flink.packer.pipeline.impl

import com.streamxhub.streamx.common.conf.Workspace
import com.streamxhub.streamx.common.fs.LfsOperator
import com.streamxhub.streamx.common.util.DateUtils
import com.streamxhub.streamx.common.util.DateUtils.fullCompact
import com.streamxhub.streamx.flink.packer.maven.MavenTool
import com.streamxhub.streamx.flink.packer.pipeline._

/**
 * Building pipeline for flink kubernetes-native session mode
 *
 * @author Al-assad
 */
class FlinkK8sSessionBuildPipeline(params: FlinkK8sSessionBuildRequest) extends BuildPipeline {

  override def pipeType: PipeType = PipeType.FLINK_NATIVE_K8S_SESSION

  override def offerBuildParam: FlinkK8sSessionBuildRequest = params

  /**
   * The construction logic needs to be implemented by subclasses
   */
  @throws[Throwable]
  override protected def buildProcess(): FlinkK8sSessionBuildResponse = {
    val appName = BuildPipelineHelper.letAppNameSafe(params.appName)

    // create workspace.
    // the sub workspace path like: APP_WORKSPACE/k8s-clusterId@k8s-namespace/job-name/
    val buildWorkspace =
    execStep(1) {
      val buildWorkspace = s"${Workspace.local.APP_WORKSPACE}/${params.clusterId}@${params.k8sNamespace}/$appName"
      LfsOperator.mkCleanDirs(buildWorkspace)
      logInfo(s"recreate building workspace: $buildWorkspace")
      buildWorkspace
    }.getOrElse(throw getError.exception)

    // build flink job shaded jar.
    // the output shaded file name like: streamx-flinkjob_myjob_20211024134822
    val shadedJar =
    execStep(2) {
      val providedLibs = BuildPipelineHelper.extractFlinkProvidedLibs(params)
      val shadedJarOutputPath = s"$buildWorkspace/streamx-flinkjob_${appName}_${DateUtils.now(fullCompact)}.jar"
      val flinkLibs = params.jarPackDeps.merge(providedLibs)
      val output = MavenTool.buildFatJar(flinkLibs, shadedJarOutputPath)
      logInfo(s"output shaded flink job jar: ${output.getAbsolutePath}")
      output
    }.getOrElse(throw getError.exception)

    FlinkK8sSessionBuildResponse(buildWorkspace, shadedJar.getAbsolutePath)
  }
}

object FlinkK8sSessionBuildPipeline {
  def of(params: FlinkK8sSessionBuildRequest): FlinkK8sSessionBuildPipeline = new FlinkK8sSessionBuildPipeline(params)
}
