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
package org.apache.streampark.flink.packer.pipeline

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/** Result of a BuildPipeline instance. */
sealed trait BuildResult {

  /** is pass aka is successfully */
  def pass: Boolean

  def as[T <: BuildResult](implicit clz: Class[T]): T = this.asInstanceOf[T]
}

sealed trait FlinkBuildResult extends BuildResult {
  def workspacePath: String
}

@JsonIgnoreProperties(ignoreUnknown = true)
case class ErrorResult(pass: Boolean = false) extends BuildResult {}

@JsonIgnoreProperties(ignoreUnknown = true)
case class SimpleBuildResponse(workspacePath: String = null, pass: Boolean = true)
  extends FlinkBuildResult {
  override def toString: String =
    s"{ workspacePath: $workspacePath, pass: $pass }"
}

@JsonIgnoreProperties(ignoreUnknown = true)
case class ShadedBuildResponse(workspacePath: String, shadedJarPath: String, pass: Boolean = true)
  extends FlinkBuildResult {
  override def toString: String =
    s"{ workspacePath: $workspacePath, " +
      s"shadedJarPath: $shadedJarPath, " +
      s"pass: $pass }"

}

@JsonIgnoreProperties(ignoreUnknown = true)
case class DockerImageBuildResponse(
    workspacePath: String,
    flinkImageTag: String,
    podTemplatePaths: Map[String, String],
    dockerInnerMainJarPath: String,
    pass: Boolean = true)
  extends FlinkBuildResult {
  override def toString: String =
    s"{ workspacePath: $workspacePath, " +
      s"flinkImageTag: $flinkImageTag, " +
      s"podTemplatePaths: ${podTemplatePaths.mkString(",")}, " +
      s"dockerInnerMainJarPath: $dockerInnerMainJarPath, " +
      s"pass: $pass }"
}

@JsonIgnoreProperties(ignoreUnknown = true)
case class K8sAppModeBuildResponse(
    workspacePath: String,
    flinkBaseImage: String,
    mainJarPath: String,
    extraLibJarPaths: Set[String],
    pass: Boolean = false) extends FlinkBuildResult {
  override def toString: String =
    s"{ workspacePath: $workspacePath, " +
      s"flinkBaseImage: $flinkBaseImage, " +
      s"mainJarPath: $mainJarPath, " +
      s"extraLibJarPaths: ${extraLibJarPaths.mkString(",")}, " +
      s"pass: $pass }"
}
