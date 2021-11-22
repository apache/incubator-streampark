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
 * @author Al-assad
 * @param name  name of pipeline used as the identifier of it.
 * @param desc  short description of pipeline.
 * @param steps building steps of pipeline, element => [sorted seq -> step desc].
 */
sealed abstract class PipeType(val name: String,
                               val desc: String,
                               val steps: Seq[(Int, String)]) {

  def isUnknown: Boolean = this == PipeType.Unknown

  lazy val stepsMap: Map[Int, String] = steps.toMap
}

/**
 * Enum of Building Pipeline
 *
 * @author Al-assad
 */
object PipeType {

  case object FlinkNativeK8sSession extends PipeType(
    name = "flink_native_k8s_session",
    desc = "flink native kubernetes session mode task building pipeline",
    steps = Seq(
      1 -> "create building workspace",
      2 -> "output flink kubernetes pod template",
      3 -> "build shaded flink app jar"
    )
  )

  case object FlinkNativeK8sApplication extends PipeType(
    name = "flink_native_k8s_application",
    desc = "flink native kubernetes session mode task building pipeline",
    steps = Seq(
      1 -> "create building workspace",
      2 -> "output kubernetes pod template",
      3 -> "build shaded flink app jar",
      4 -> "copy extra flink libraries",
      5 -> "output flink app dockerfile",
      6 -> "pull flink app base docker image",
      7 -> "build flink app docker image",
      8 -> "push flink app docker image"
    )
  )

  // TODO case object FlinkYarnApplication extends PipelineType

  // TODO case object FlinkYarnSession extends PipelineType

  // TODO case object FlinkStandalone extends PipelineType

  case object Unknown extends PipeType("Unknown", "Unknown", Seq.empty)

  /**
   * Get all pipeline types.
   */
  val allTypes: Seq[PipeType] = Seq(FlinkNativeK8sSession, FlinkNativeK8sApplication)

  /**
   * Get PipelineType with name, when not found, return Unknown type.
   *
   * @param name name of pipeline
   * @return PipelineType
   */
  def withName(name: String): PipeType = allTypes.find(_.name == name).getOrElse(Unknown)

}
