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

import com.streamxhub.streamx.common.enums.DevelopmentMode
import com.streamxhub.streamx.common.fs.{FsOperator, LfsOperator}
import com.streamxhub.streamx.common.util.Utils
import com.streamxhub.streamx.flink.packer.maven.MavenTool
import com.streamxhub.streamx.flink.packer.pipeline._
import org.apache.commons.codec.digest.DigestUtils

import java.io.{File, FileInputStream, IOException}

/**
 * Building pipeline for flink yarn application mode
 *
 * @author benjobs
 */
class FlinkYarnApplicationBuildPipeline(request: FlinkYarnApplicationBuildRequest) extends BuildPipeline {

  private[this] val fsOperator = FsOperator.hdfs

  /**
   * the type of pipeline
   */
  override def pipeType: PipelineType = PipelineType.FLINK_YARN_APPLICATION

  override def offerBuildParam: FlinkYarnApplicationBuildRequest = request

  /**
   * the actual build process.
   * the effective steps progress should be implemented in
   * multiple BuildPipeline.execStep() functions.
   */
  @throws[Throwable] override protected def buildProcess(): SimpleBuildResponse = {
    execStep(1) {
      LfsOperator.mkCleanDirs(request.yarnProvidedPath)
      logInfo(s"recreate building workspace: ${request.yarnProvidedPath}")
    }.getOrElse(throw getError.exception)

    val mavenJars =
      execStep(2) {
        request.developmentMode match {
          case DevelopmentMode.FLINKSQL =>
            val mavenArts = MavenTool.resolveArtifacts(request.dependencyInfo.mavenArts)
            mavenArts.map(_.getAbsolutePath) ++ request.dependencyInfo.extJarLibs
          case _ => Set[String]()
        }
      }.getOrElse(throw getError.exception)

    execStep(3) {
      mavenJars.foreach(jar => uploadToHdfs(jar, request.yarnProvidedPath))
    }.getOrElse(throw getError.exception)

    SimpleBuildResponse()
  }

  @throws[IOException] private[this] def uploadToHdfs(origin: String, target: String): Unit = {
    val originFile = new File(origin)
    if (!fsOperator.exists(target)) {
      fsOperator.mkdirs(target)
    }
    if (originFile.isFile) {
      val targetFile = s"$target/${originFile.getName}"
      if (fsOperator.exists(targetFile)) {
        Utils.tryWithResource(new FileInputStream(originFile))(inputStream => {
          if (DigestUtils.md5Hex(inputStream) != fsOperator.fileMd5(targetFile)) {
            fsOperator.upload(originFile.getAbsolutePath, targetFile)
          }
        })
      } else {
        fsOperator.upload(originFile.getAbsolutePath, targetFile)
      }
    } else {
      fsOperator.upload(originFile.getAbsolutePath, target)
    }
  }

}

object FlinkYarnApplicationBuildPipeline {
  def of(request: FlinkYarnApplicationBuildRequest): FlinkYarnApplicationBuildPipeline = new FlinkYarnApplicationBuildPipeline(request)
}
