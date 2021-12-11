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

package com.streamxhub.streamx.flink.packer.docker

import com.github.dockerjava.api.command.PushImageCmd
import com.github.dockerjava.core.command.HackBuildImageCmd
import com.google.common.collect.Sets
import com.streamxhub.streamx.common.conf.ConfigConst.DOCKER_IMAGE_NAMESPACE
import com.streamxhub.streamx.common.util.Logger

import scala.language.implicitConversions

/**
 * @author Al-assad
 */
object DockerTool extends Logger {

  /**
   * build and push docker image for flink fat-jar.
   *
   * @param dockerFileTemplate flink jar docker build template
   * @param expectImageTag     expect image tag for fat-jar of flink job
   * @param authConf           authentication configuration of remote docker register
   * @param push               whether push image after build image
   * @return actual flink job jar image name
   */
  @throws[Exception] def buildFlinkImage(dockerFileTemplate: FlinkDockerfileTemplateTrait,
                                         expectImageTag: String,
                                         authConf: DockerAuthConf,
                                         push: Boolean = false): String = {

    val baseDirectory = dockerFileTemplate.workspace.toFile
    val dockerfile = dockerFileTemplate.writeDockerfile
    val tagName = compileTag(expectImageTag, authConf.registerAddress)
    val flinkImageTag = dockerFileTemplate.flinkBaseImage

    // pull flink base image
    usingDockerClient {
      dockerClient =>
        val pullImageCmd = {
          if (!flinkImageTag.startsWith(authConf.registerAddress)) dockerClient.pullImageCmd(flinkImageTag)
          else dockerClient.pullImageCmd(flinkImageTag).withAuthConfig(authConf.toDockerAuthConf)
        }
        pullImageCmd.start.awaitCompletion()
        logInfo(s"[streamx-packer] docker pull image ${flinkImageTag} successfully.")
    } {
      err =>
        val msg = s"[streamx] pull flink base docker image failed, imageTag=${dockerFileTemplate.flinkBaseImage}"
        logError(msg, err)
        throw new Exception(msg, err)
    }

    // build flink image
    usingDockerClient {
      dockerClient =>
        val buildImageCmd = dockerClient.buildImageCmd()
          .withBaseDirectory(baseDirectory)
          .withDockerfile(dockerfile)
          .withTags(Sets.newHashSet(tagName))

        val buildCmdCallback = buildImageCmd.asInstanceOf[HackBuildImageCmd]
          .start(watchDockerBuildStep(buildStep =>
            logInfo(s"[streamx-packer] building docker image ${tagName} => ${buildStep}")
          ))
        val imageId = buildCmdCallback.awaitImageId
        logInfo(s"[streamx-packer] docker image built successfully, imageId=${imageId}, tag=${tagName}")
    } {
      err =>
        val msg = "[streamx-packer] build flink job docker image failed."
        logError(msg, err)
        throw new Exception(msg, err)
    }

    // push flink image
    if (push) {
      usingDockerClient {
        dockerClient =>
          val pushCmd: PushImageCmd = dockerClient.pushImageCmd(tagName).withAuthConfig(authConf.toDockerAuthConf)
          pushCmd.start.awaitCompletion
          logInfo(s"[streamx-packer] docker image push successfully, tag=${tagName}, registerAddr=${authConf.registerAddress}")
      } {
        err =>
          val msg = "[streamx-packer] push flink job docker image failed."
          logError(msg, err)
          throw new Exception(msg, err)
      }
    }
    tagName
  }

  /**
   * compile image tag with namespace and remote address.
   * todo support custom namespace of docker remote register
   */
  private[this] def compileTag(tag: String, registerAddress: String): String = {
    var tagName = if (tag.contains("/")) tag else s"$DOCKER_IMAGE_NAMESPACE/$tag"
    if (registerAddress.nonEmpty && !tagName.startsWith(registerAddress)) {
      tagName = s"$registerAddress/$tagName"
    }
    tagName.toLowerCase
  }

}
