/*
 * Copyright (c) 2021 The StreamX Project
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
package com.streamxhub.streamx.flink.packer

import com.github.dockerjava.api.command.PushImageCmd
import com.google.common.collect.Sets
import com.streamxhub.streamx.common.conf.K8sConfigConst.{IMAGE_NAMESPACE, K8S_IMAGE_REGISTER_ADDRESS}
import com.streamxhub.streamx.common.util.Utils.tryWithResource
import org.apache.commons.io.FileUtils

import java.io.File
import javax.annotation.Nonnull

/**
 * author: Al-assad
 */
object DockerTool {

  /**
   * build and push docker image for flink fat-jar.
   *
   * @param projectPath project workspace path
   * @param template    flink jar docker build template
   * @param tag         image tag
   * @param push        whether push image after build image
   * @return actual flink job jar image name
   */
  @Nonnull
  def buildFlinkImage(projectPath: String, template: FlinkDockerfileTemplate, tag: String, push: Boolean = false): String = {
    // organize project path and write docker file
    val projectDir = new File(projectPath)
    if (!projectDir.exists()) {
      projectDir.mkdir()
    }
    val flinkFatJar = new File(template.flinkFatjarPath)
    if (flinkFatJar.getParentFile.getAbsolutePath != projectDir.getAbsolutePath) {
      FileUtils.copyFile(flinkFatJar, new File(s"${projectDir.getAbsolutePath}/${flinkFatJar.getName}"))
    }
    val dockerfile = template.writeDockerfile(projectPath)
    // build and push docker image
    buildImage(projectDir, dockerfile, tag) match {
      case x if push => pushImage(x)
      case y => y
    }
  }

  /**
   * build docker image.
   * this is sync call api.
   *
   * @param baseDir    base directory
   * @param dockerfile dockerfile
   * @param tag        image tag
   * @return actually image tag
   */
  @Nonnull
  def buildImage(baseDir: File, dockerfile: File, tag: String): String = {
    val tagName = compileTag(tag)
    tryWithResource(DockerRetriever.newDockerClient()) {
      client => {
        // build docker image
        val buildImageCmd = client.buildImageCmd()
          .withPull(true)
          .withBaseDirectory(baseDir)
          .withDockerfile(dockerfile)
          .withTags(Sets.newHashSet(tagName))
        buildImageCmd.start().awaitCompletion()
      }
    }
    tagName
  }

  /**
   * push image to remote repository.
   * this is sync call api.
   *
   * @param tag tag name
   * @return actually image tag
   */
  def pushImage(tag: String): String = {
    val tagName = compileTag(tag)
    tryWithResource(DockerRetriever.newDockerClient()) {
      client => {
        val pushCmd: PushImageCmd = client.pushImageCmd(tagName)
          .withAuthConfig(DockerRetriever.remoteImageRegisterAuthConfig)
          .withTag(tagName)
        pushCmd.start().awaitCompletion()
      }
    }
    tagName
  }

  /**
   * compile image tag with namespace and remote address.
   */
  private[packer] def compileTag(tag: String): String = {
    var tagName = {
      if (tag.contains("/")) tag
      else s"$IMAGE_NAMESPACE/$tag"
    }
    if (K8S_IMAGE_REGISTER_ADDRESS.nonEmpty && !tagName.startsWith(K8S_IMAGE_REGISTER_ADDRESS)) {
      tagName = s"$K8S_IMAGE_REGISTER_ADDRESS/$tagName"
    }
    tagName
  }


}
