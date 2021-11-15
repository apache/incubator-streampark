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
package com.streamxhub.streamx.flink.packer.docker

import com.github.dockerjava.api.command.PushImageCmd
import com.google.common.collect.Sets
import com.streamxhub.streamx.common.conf.ConfigConst.DOCKER_IMAGE_NAMESPACE
import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.common.util.Utils.tryWithResourceException
import org.apache.commons.io.FileUtils

import java.io.File

/**
 * @author Al-assad
 */
object DockerTool extends Logger {


  /**
   * build and push docker image for flink fat-jar.
   *
   * @author Al-assad
   * @param authConf           authentication configuration of remote docker register
   * @param projectBaseDir     project workspace dir of flink job
   * @param dockerFileTemplate flink jar docker build template
   * @param expectImageTag     expect image tag for fat-jar of flink job
   * @param push               whether push image after build image
   * @return actual flink job jar image name
   */
  @throws[Exception] def buildFlinkImage(authConf: DockerAuthConf,
                                         projectBaseDir: String,
                                         dockerFileTemplate: FlinkDockerfileTemplate,
                                         expectImageTag: String,
                                         push: Boolean = false): String = {
    // organize project path and write docker file
    val projectDir = new File(projectBaseDir)
    if (!projectDir.exists()) {
      projectDir.mkdir()
    }
    val flinkFatJar = new File(dockerFileTemplate.flinkFatjarPath)
    if (flinkFatJar.getParentFile.getAbsolutePath != projectDir.getAbsolutePath) {
      FileUtils.copyFile(flinkFatJar, new File(s"${projectDir.getAbsolutePath}/${flinkFatJar.getName}"))
    }
    // generate dockerfile
    val dockerfile = dockerFileTemplate.writeDockerfile(projectBaseDir)
    val tagName = compileTag(expectImageTag, authConf.registerAddress)

    // pull flink base image
    usingDockerClient {
      dockerClient =>
        val pullImageCmd = {
          if (!dockerFileTemplate.flinkBaseImage.startsWith(authConf.registerAddress)) dockerClient.pullImageCmd(dockerFileTemplate.flinkBaseImage)
          else dockerClient.pullImageCmd(dockerFileTemplate.flinkBaseImage).withAuthConfig(authConf.toDockerAuthConf)
        }
        pullImageCmd.start.awaitCompletion()
        logInfo(s"streamx-packer: docker pull image ${dockerFileTemplate.flinkBaseImage} successfully.")
    } {
      err =>
        val msg = s"streamx-packer: pull flink base docker image failed, imageTag=${dockerFileTemplate.flinkBaseImage}"
        logError(msg, err)
        throw new Exception(msg, err)
    }

    // build flink image
    usingDockerClient {
      dockerClient =>
        // build docker image
        val buildImageCmd = dockerClient.buildImageCmd()
          .withBaseDirectory(projectDir)
          .withDockerfile(dockerfile)
          .withTags(Sets.newHashSet(tagName))
        val imageId = buildImageCmd.start().awaitImageId()
        logInfo(s"docker image built successfully, imageId=$imageId, tag=$tagName")
    } {
      err =>
        val msg = "streamx-packer: build flink job docker image failed."
        logError(msg, err)
        throw new Exception(msg, err)
    }

    // push flink image
    if (push) {
      usingDockerClient {
        dockerClient =>
          val pushCmd: PushImageCmd = dockerClient.pushImageCmd(tagName).withAuthConfig(authConf.toDockerAuthConf)
          pushCmd.start.awaitCompletion
          logInfo(s"streamx-packer: docker image push successfully, tag=$tagName, registerAddress=${authConf.registerAddress}")
      } {
        err =>
          val msg = "streamx-packer: push flink job docker image failed."
          logError(msg, err)
          throw new Exception(msg, err)
      }
    }
    tagName
  }


  /**
   * push docker image to remote regoster
   *
   * @return successful or failed
   */
  @throws[Exception] def pushImage(imageTag: String, authConf: DockerAuthConf): Boolean = {
    tryWithResourceException(DockerRetriever.newDockerClient()) {
      client =>
        val pushCmd: PushImageCmd = client.pushImageCmd(imageTag).withAuthConfig(authConf.toDockerAuthConf)
        pushCmd.start().awaitCompletion()
        true
    } {
      exception =>
        logError(s"push docker image fail, tag=$imageTag, registerAddress=${authConf.registerAddress}," +
          s" exception=${exception.getMessage}")
        false
    }
  }

  /**
   * compile image tag with namespace and remote address.
   */
  private def compileTag(tag: String, registerAddress: String): String = {
    formatTag(if (tag.contains("/")) tag else s"$DOCKER_IMAGE_NAMESPACE/$tag", registerAddress)
  }

  /**
   * format image tag with namespace and remote address.
   *
   * e.g.
   * image:tag             registry-1.docker.io -> registry-1.docker.io/library/image:tag
   * repository/image:tag  registry-1.docker.io -> registry-1.docker.io/repository/image:tag
   */
  def formatTag(tag: String, registerAddress: String): String = {
    if (registerAddress.nonEmpty && !tag.startsWith(registerAddress)) {
      s"$registerAddress${if (tag.contains("/")) "/" else "/library/"}$tag".toLowerCase
    } else tag.toLowerCase
  }


}
