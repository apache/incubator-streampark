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

import org.apache.streampark.common.util.Implicits._
import org.apache.streampark.common.util.Utils

import java.lang.{Long => JavaLong}

/**
 * Snapshot for a BuildPipeline instance. see
 * org.apache.streampark.flink.packer.pipeline.BuildPipeline
 *
 * @param emitTime
 *   snapshot interception time
 * @param stepStatus
 *   StepSeq -> (PipeStepStatus -> status update timestamp)
 */

case class PipelineSnapshot(
    appName: String,
    pipeType: PipelineTypeEnum,
    pipeStatus: PipelineStatusEnum,
    curStep: Int,
    allSteps: Int,
    stepStatus: Map[Int, (PipelineStepStatusEnum, Long)],
    error: PipeError,
    emitTime: Long) {

  def percent(): Double = Utils.calPercent(curStep, allSteps)

  def stepStatusAsJava: JavaMap[Integer, (PipelineStepStatusEnum, JavaLong)] = {
    stepStatus.toSeq
      .map(e => Integer.valueOf(e._1) -> (e._2._1 -> JavaLong.valueOf(e._2._2)))
      .toMap
      .asJava
  }

  def pureStepStatusAsJava: JavaMap[Integer, PipelineStepStatusEnum] = {
    stepStatus.toSeq.map(e => Integer.valueOf(e._1) -> e._2._1).toMap.asJava
  }

  def stepStatusTimestampAsJava: JavaMap[Integer, JavaLong] = {
    stepStatus.toSeq
      .map(e => Integer.valueOf(e._1) -> JavaLong.valueOf(e._2._2))
      .toMap
      .asJava
  }

}
