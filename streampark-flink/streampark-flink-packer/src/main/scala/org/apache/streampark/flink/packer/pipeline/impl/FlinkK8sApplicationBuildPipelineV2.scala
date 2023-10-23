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

package org.apache.streampark.flink.packer.pipeline.impl

import org.apache.streampark.common.enums.FlinkDevelopmentMode
import org.apache.streampark.common.fs.LfsOperator
import org.apache.streampark.flink.packer.maven.MavenTool
import org.apache.streampark.flink.packer.pipeline._

import scala.language.postfixOps

/**
 * Building pipeline V2(base on kubernetes operator) for flink kubernetes-native application mode
 */
class FlinkK8sApplicationBuildPipelineV2(request: FlinkK8sApplicationBuildRequest)
  extends BuildPipeline {

  override def pipeType: PipelineTypeEnum = PipelineTypeEnum.FLINK_K8S_APPLICATION_V2

  @throws[Throwable]
  override protected def buildProcess(): K8sAppModeBuildResponse = {

    // Step-1: init build workspace of flink job
    // the sub workspace dir like: APP_WORKSPACE/k8s-clusterId@k8s-namespace/
    val buildWorkspace =
      execStep(1) {
        val buildWorkspace = s"${request.workspace}/${request.clusterId}@${request.k8sNamespace}"
        LfsOperator.mkCleanDirs(buildWorkspace)
        logInfo(s"Recreate building workspace: $buildWorkspace")
        buildWorkspace
      }.getOrElse(throw getError.exception)

    // Step-2: build shaded flink job jar and handle extra jars
    // the output shaded jar file name like: streampark-flinkjob_myjob-test.jar
    val (shadedJar, extJarLibs) =
      execStep(2) {
        val shadedJarOutputPath = request.getShadedJarPath(buildWorkspace)
        val extJarLibs = request.developmentMode match {
          case FlinkDevelopmentMode.FLINK_SQL => request.dependencyInfo.extJarLibs
          case FlinkDevelopmentMode.CUSTOM_CODE => Set.empty[String]
          case _ => Set.empty[String]
        }
        val shadedJar =
          MavenTool.buildFatJar(request.mainClass, request.providedLibs, shadedJarOutputPath)
        logInfo(s"Output shaded flink job jar: ${shadedJar.getAbsolutePath}")
        shadedJar -> extJarLibs
      }.getOrElse(throw getError.exception)

    K8sAppModeBuildResponse(
      workspacePath = buildWorkspace,
      flinkBaseImage = request.flinkBaseImage,
      mainJarPath = shadedJar.getAbsolutePath,
      extraLibJarPaths = extJarLibs)
  }

  override protected def offerBuildParam: FlinkK8sApplicationBuildRequest = request
}

object FlinkK8sApplicationBuildPipelineV2 {
  def of(request: FlinkK8sApplicationBuildRequest): FlinkK8sApplicationBuildPipelineV2 =
    new FlinkK8sApplicationBuildPipelineV2(request)

}
