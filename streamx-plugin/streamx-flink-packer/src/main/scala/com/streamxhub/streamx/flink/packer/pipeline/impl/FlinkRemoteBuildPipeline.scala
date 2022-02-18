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
 * Building pipeline for flink standalone session mode
 *
 * @author czy006
 */
class FlinkRemoteBuildPipeline(params: FlinkRemoteBuildRequest) extends BuildPipeline {

  override def pipeType: PipeType = PipeType.FLINK_STANDALONE

  override def offerBuildParam: FlinkRemoteBuildRequest = params

  /**
   * The construction logic needs to be implemented by subclasses
   */
  @throws[Throwable]
  override protected def buildProcess(): FlinkRemoteBuildResponse = {
    val appName = BuildPipelineHelper.letAppNameSafe(params.appName)

    // create workspace.
    // the sub workspace path like: APP_WORKSPACE/jobName
    val buildWorkspace =
    execStep(1) {
      val buildWorkspace = s"${Workspace.local.APP_WORKSPACE}/$appName"
      LfsOperator.mkCleanDirs(buildWorkspace)
      logInfo(s"recreate building workspace: $buildWorkspace")
      buildWorkspace
    }.getOrElse(throw getError.exception)

    // build flink job shaded jar
    val shadedJar =
      execStep(2) {
        val providedLibs = BuildPipelineHelper.extractFlinkProvidedLibs(params)
        val shadedJarOutputPath = s"$buildWorkspace/streamx-flinkjob_${appName}_${DateUtils.now(fullCompact)}.jar"
        val flinkLibs = params.dependencyInfo.merge(providedLibs)
        val output = MavenTool.buildFatJar(params.mainClass, flinkLibs, shadedJarOutputPath)
        logInfo(s"output shaded flink job jar: ${output.getAbsolutePath}")
        output
      }.getOrElse(throw getError.exception)

    FlinkRemoteBuildResponse(buildWorkspace, shadedJar.getAbsolutePath)
  }
}

object FlinkRemoteBuildPipeline {
  def of(params: FlinkRemoteBuildRequest): FlinkRemoteBuildPipeline = new FlinkRemoteBuildPipeline(params)
}


