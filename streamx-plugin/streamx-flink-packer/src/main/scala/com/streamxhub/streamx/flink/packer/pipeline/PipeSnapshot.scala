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

import com.streamxhub.streamx.flink.packer.pipeline.BuildPipelineHelper.calPercent

import java.util.{Map => JMap}
import scala.collection.JavaConverters._

/**
 * Snapshot for a BuildPipeline instance.
 * see com.streamxhub.streamx.flink.packer.pipeline.BuildPipeline
 *
 * @param emitTime snapshot interception time
 * @author Al-assad
 */
case class PipeSnapshot(appName: String,
                        pipeType: PipeType,
                        pipeStatus: PipeStatus,
                        curStep: Int,
                        allSteps: Int,
                        stepStatus: Map[Int, PipeStepStatus],
                        error: PipeErr,
                        emitTime: Long) {

  def percent(): Double = calPercent(curStep, allSteps)

  def stepStatusAsJava: JMap[Integer, PipeStepStatus] = stepStatus.toSeq.map(e => new Integer(e._1) -> e._2).toMap.asJava
}
