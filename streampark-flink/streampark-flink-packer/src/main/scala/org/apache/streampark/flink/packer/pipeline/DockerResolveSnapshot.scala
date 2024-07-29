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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/** Snapshot for docker resolved progress */
@JsonIgnoreProperties(ignoreUnknown = true)
case class DockerResolvedSnapshot(
    pull: DockerPullSnapshot,
    build: DockerBuildSnapshot,
    push: DockerPushSnapshot)

/** snapshot for pulling docker image progress. */
@JsonIgnoreProperties(ignoreUnknown = true)
case class DockerPullSnapshot(
    detail: Seq[DockerLayerProgress],
    error: String,
    emitTime: Long,
    percent: Double) {
  def detailAsJava: JavaList[DockerLayerProgress] = detail
}

/** snapshot for building docker image progress. */
@JsonIgnoreProperties(ignoreUnknown = true)
case class DockerBuildSnapshot(detail: Seq[String], emitTime: Long) {
  def detailAsJava: JavaList[String] = detail
}

/** snapshot for pushing docker image progress. */
@JsonIgnoreProperties(ignoreUnknown = true)
case class DockerPushSnapshot(
    detail: Seq[DockerLayerProgress],
    error: String,
    emitTime: Long,
    percent: Double) {
  def detailAsJava: JavaList[DockerLayerProgress] = detail
}

object DockerPullSnapshot {
  def of(detail: Seq[DockerLayerProgress], error: String, emitTime: Long): DockerPullSnapshot =
    DockerPullSnapshot(
      detail,
      error,
      emitTime,
      Utils.calPercent(detail.map(_.current).sum, detail.map(_.total).sum))
}

object DockerPushSnapshot {
  def of(detail: Seq[DockerLayerProgress], error: String, emitTime: Long): DockerPushSnapshot =
    DockerPushSnapshot(
      detail,
      error,
      emitTime,
      Utils.calPercent(detail.map(_.current).sum, detail.map(_.total).sum))
}
