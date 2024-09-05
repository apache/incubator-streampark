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

package org.apache.streampark.flink.packer.docker

import org.apache.streampark.common.constants.Constants
import org.apache.streampark.common.fs.LfsOperator

import org.apache.commons.io.FileUtils

import java.io.File
import java.nio.file.{Path, Paths}

/** Flink image dockerfile template. */
trait FlinkDockerfileTemplateTrait {

  /** Path of dockerfile workspace, it should be a directory. */
  def workspacePath: String

  /** Flink base docker image name, see https://hub.docker.com/_/flink. */
  def flinkBaseImage: String

  /** Path of flink job main jar which would copy to $FLINK_HOME/usrlib/ */
  def flinkMainJarPath: String

  /** Path of additional flink lib path which would copy to $FLINK_HOME/lib/ */
  def flinkExtraLibPaths: Set[String]

  /** Offer content of DockerFile. */
  def offerDockerfileContent: String

  /** Startup flink main jar path inner Docker */
  def innerMainJarPath: String = s"local:///opt/flink/usrlib/$mainJarName"

  /** output dockerfile name */
  protected val DEFAULT_DOCKER_FILE_NAME = "Dockerfile"
  protected val FLINK_LIB_PATH = "lib"
  protected val FLINK_HOME: String = "$FLINK_HOME"

  /** Dockerfile building workspace. */
  lazy val workspace: Path = {
    val path = Paths.get(workspacePath).toAbsolutePath
    if (!LfsOperator.exists(workspacePath)) LfsOperator.mkdirs(workspacePath)
    path
  }

  /**
   * flink main jar name, the main jar would copy from `flinkMainjarPath` to
   * `workspacePath/mainJarName.jar`.
   */
  lazy val mainJarName: String = {
    val mainJarPath = Paths.get(flinkMainJarPath).toAbsolutePath
    if (mainJarPath.getParent != workspace) {
      LfsOperator.copy(
        mainJarPath.toString,
        s"${workspace.toString}/${mainJarPath.getFileName.toString}")
    }
    mainJarPath.getFileName.toString
  }

  /**
   * flink extra jar lib, the jar file in `flinkExtraLibPaths` would be copyed into
   * `FLINK_LIB_PATH`.
   */
  lazy val extraLibName: String = {
    LfsOperator.mkCleanDirs(s"${workspace.toString}/$FLINK_LIB_PATH")
    flinkExtraLibPaths
      .map(new File(_))
      .filter(_.exists())
      .filter(_.getName.endsWith(Constants.JAR_SUFFIX))
      .flatMap {
        case f if f.isDirectory =>
          f.listFiles
            .filter(_.isFile)
            .filter(_.getName.endsWith(Constants.JAR_SUFFIX))
            .map(_.getAbsolutePath)
        case f if f.isFile => Array(f.getAbsolutePath)
      }
      .foreach(LfsOperator.copy(_, s"${workspace.toString}/$FLINK_LIB_PATH"))
    FLINK_LIB_PATH
  }

  /**
   * write content of DockerFile to outputPath, the output dockerfile name is "dockerfile".
   *
   * @return
   *   File Object for actual output Dockerfile
   */
  def writeDockerfile: File = {
    val output = new File(s"$workspacePath/$DEFAULT_DOCKER_FILE_NAME")
    FileUtils.write(output, offerDockerfileContent, "UTF-8")
    output
  }

  /**
   * write content of DockerFile to outputPath using specified output dockerfile name.
   *
   * @return
   *   File Object for actual output Dockerfile
   */
  def writeDockerfile(dockerfileName: String): File = {
    val output = new File(s"$workspacePath/$dockerfileName")
    FileUtils.write(output, offerDockerfileContent, "UTF-8")
    output
  }

}
