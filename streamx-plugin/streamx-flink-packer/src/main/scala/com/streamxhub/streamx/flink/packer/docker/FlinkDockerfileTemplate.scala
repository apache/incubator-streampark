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

import org.apache.commons.io.FileUtils

import java.io.File
import java.nio.file.{Path, Paths}

/**
 * Base flink docker file image template.
 * @author Al-assad
 *
 * @param workspacePath      Absolute path of dockerfile workspace.
 * @param flinkBaseImage     Flink base docker image name, see https://hub.docker.com/_/flink.
 * @param flinkFatjarPath    Absolute path or relative path to workspace of flink job fat jar
 *                           which would copy to $FLINK_HOME/usrlib/.It should be under the
 *                           workspacePath.
 * @param flinkExtraLibsPath Absolute path or relative path of additional flink lib path which
 *                           would copy to $FLINK_HOME/lib/.It should be under the workspacePath.
 */
case class FlinkDockerfileTemplate(workspacePath: String,
                                   flinkBaseImage: String,
                                   flinkFatjarPath: String,
                                   flinkExtraLibsPath: String) {

  val DEFAULT_DOCKER_FILE_NAME = "Dockerfile"

  protected val FLINK_HOME: String = "$FLINK_HOME"

  protected val workspacePaths: Path = Paths.get(workspacePath)

  /**
   * fatJar: relative path of fat-jar to workspace path
   * fatJarName: file name of fat-jar
   */
  val (fatJar, fatJarName) = {
    val jarPath = workspacePaths.relativize(Paths.get(flinkFatjarPath))
    jarPath.toString -> jarPath.getFileName
  }

  /**
   * relative path of extra jar lib to workspace path
   */
  lazy val extraLib: String = workspacePaths.relativize(Paths.get(flinkExtraLibsPath)).toString

  /**
   * file path of startup flink jar inner container
   */
  def startupJarFilePath: String = s"local:///opt/flink/usrlib/$fatJarName"

  /**
   * offer content of DockerFile
   */
  def offerDockerfileContent: String = {
    s"""FROM ${flinkBaseImage}
       |RUN mkdir -p $FLINK_HOME/usrlib
       |COPY ${fatJar} $FLINK_HOME/usrlib/${fatJarName}
       |COPY ${extraLib} $FLINK_HOME/lib/
       |""".stripMargin
  }

  /**
   * write content of DockerFile to outputPath.
   * If outputPath is directory, the default output file's name is "Dockerfile".
   *
   * @return File Object for actual output Dockerfile
   */
  def writeDockerfile: File = {
    val output = new File(s"$workspacePath/$DEFAULT_DOCKER_FILE_NAME")
    FileUtils.write(output, offerDockerfileContent, "UTF-8")
    output
  }

}
