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
package com.streamxhub.streamx.flink.packer.pipeline

/**
 * Result of a BuildPipeline instance.
 *
 * @author Al-assad
 */
sealed trait BuildResult {
  def isPass: Boolean = true
}

sealed trait FlinkBuildResult extends BuildResult {
  def workspacePath: String
}

sealed trait FlinkSessionBuildResult extends FlinkBuildResult {
  def flinkShadedJarPath: String
}

case class ErrorResult(error: PipeErr) extends BuildResult {
  override def isPass: Boolean = false
}

case class FlinkK8sSessionBuildResponse(workspacePath: String,
                                        flinkShadedJarPath: String) extends FlinkSessionBuildResult

case class FlinkK8sApplicationBuildResponse(workspacePath: String,
                                            flinkImageTag: String,
                                            podTemplatePaths: Map[String, String],
                                            dockerInnerMainJarPath: String) extends FlinkBuildResult

// case class FlinkYarnSessionBuildResponse(workspacePath: String, flinkShadedJarPath: String) extends FlinkSessionBuildResult

// case class FlinkYarnApplicationBuildResponse() extends BuildResult

// case class FlinkStandaloneBuildResponse(workspacePath: String, flinkShadedJarPath: String) extends FlinkSessionBuildResult



