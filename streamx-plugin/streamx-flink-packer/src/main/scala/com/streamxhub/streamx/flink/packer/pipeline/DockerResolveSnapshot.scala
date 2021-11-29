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

/**
 * Snapshot for docker resolved progress
 *
 * @author Al-assad
 */

/**
 * snapshot for pulling docker image progress.
 */
case class DockerPullSnapshot(detail: Seq[DockerLayerProgress],
                              error: String,
                              emitTime: Long,
                              percent: Double)

object DockerPullSnapshot {
  def apply(detail: Seq[DockerLayerProgress], error: String, emitTime: Long): DockerPullSnapshot =
    DockerPullSnapshot(detail, error, emitTime, calPercent(detail.map(_.current).sum, detail.map(_.total).sum))
}

/**
 * snapshot for building docker image progress.
 */
case class DockerBuildSnapshot(detail: Seq[String],
                               error: String,
                               emitTime: Long)

/**
 * snapshot for pushing docker image progress.
 */
case class DockerPushSnapshot(detail: Seq[DockerLayerProgress],
                              error: String,
                              emitTime: Long,
                              percent: Double)

object DockerPushSnapshot {
  def apply(detail: Seq[DockerLayerProgress], error: String, emitTime: Long): DockerPushSnapshot =
    DockerPushSnapshot(detail, error, emitTime, calPercent(detail.map(_.current).sum, detail.map(_.total).sum))
}

/**
 * push/pull progress of per docker layer.
 */
case class DockerLayerProgress(layerId: String, current: Long, total: Long, percent: Double)

object DockerLayerProgress {
  def apply(layerId: String, current: Long, total: Long): DockerLayerProgress =
    DockerLayerProgress(layerId, current, total, calPercent(current, total))
}





