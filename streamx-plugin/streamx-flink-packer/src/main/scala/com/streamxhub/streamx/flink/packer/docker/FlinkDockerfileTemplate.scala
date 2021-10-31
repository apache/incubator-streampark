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

import com.streamxhub.streamx.flink.packer.docker.FlinkDockerfileTemplate.{DEFAULT_DOCKER_FILE_NAME, DOCKER_FILE_TEMPLATE}
import org.apache.commons.io.FileUtils

import java.io.File

/**
 * flink docker file image template.
 * @author Al-assad
 *
 * @param flinkBaseImage  flink base docker image name, see https://hub.docker.com/_/flink
 * @param flinkFatjarPath path of flink job fat jar
 */
case class FlinkDockerfileTemplate(flinkBaseImage: String, flinkFatjarPath: String) {

  lazy val fatJarName: String = new File(flinkFatjarPath).getName

  /**
   * get content of DockerFile
   */
  def dockerfileContent: String = DOCKER_FILE_TEMPLATE.format(flinkBaseImage, fatJarName, fatJarName)

  /**
   * write content of DockerFile to outputPath.
   * If outputPath is directory, the default output file's name is "Dockerfile".
   *
   * @param outputPath Dockerfile output path
   * @return File Object for actual output Dockerfile
   */
  def writeDockerfile(outputPath: String): File = {
    var output = new File(outputPath)
    if (output.isDirectory) {
      output = new File(output.getAbsolutePath.concat("/").concat(DEFAULT_DOCKER_FILE_NAME))
    }
    FileUtils.write(output, dockerfileContent, "UTF-8")
    output
  }

  /**
   * get flink job jar such as local:///opt/flink/usrlib/flink-fatjar.jar
   */
  def getJobJar: String = s"local:///opt/flink/usrlib/$fatJarName"

}

object FlinkDockerfileTemplate {

  val DEFAULT_DOCKER_FILE_NAME = "Dockerfile"

  /**
   * template of dockerfile
   */
  val DOCKER_FILE_TEMPLATE: String =
    """FROM %s
      |RUN mkdir -p $FLINK_HOME/usrlib
      |COPY %s $FLINK_HOME/usrlib/%s
      |""".stripMargin

}
