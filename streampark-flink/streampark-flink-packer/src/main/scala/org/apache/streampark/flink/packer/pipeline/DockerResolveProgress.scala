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

import org.apache.streampark.common.util.Utils

import com.github.dockerjava.api.model.{PullResponseItem, PushResponseItem}
import org.apache.commons.lang3.StringUtils

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/** cache storage for docker resolved progress */
class DockerResolveProgress(
    val pull: DockerPullProgress,
    val build: DockerBuildProgress,
    val push: DockerPushProgress)

class DockerPullProgress(
    val layers: mutable.Map[String, DockerLayerProgress],
    var error: String,
    var lastTime: Long) {
  // noinspection DuplicatedCode
  def update(pullRsp: PullResponseItem): Unit = {
    val nonPullUpdateState =
      pullRsp == null || StringUtils.isBlank(pullRsp.getId) || StringUtils
        .isBlank(pullRsp.getStatus)
    if (nonPullUpdateState) {
      return
    }
    if (pullRsp.getStatus.contains("complete")) {
      layers += pullRsp.getId -> DockerLayerProgress(pullRsp.getId, pullRsp.getStatus, 1, 1)
      lastTime = System.currentTimeMillis
    } else {
      val cur =
        if (pullRsp.getProgressDetail == null || pullRsp.getProgressDetail.getCurrent == null) 0
        else pullRsp.getProgressDetail.getCurrent.toLong
      val total =
        if (pullRsp.getProgressDetail == null || pullRsp.getProgressDetail.getTotal == null) 0
        else pullRsp.getProgressDetail.getTotal.toLong
      layers += pullRsp.getId -> DockerLayerProgress(pullRsp.getId, pullRsp.getStatus, cur, total)
      error = Option(pullRsp.getErrorDetail).map(_.getMessage).getOrElse("")
      lastTime = System.currentTimeMillis
    }
  }

  def snapshot: DockerPullSnapshot =
    DockerPullSnapshot.of(layers.values.toSeq, error, lastTime)
}

class DockerBuildProgress(val steps: ArrayBuffer[String], var lastTime: Long) {
  def update(buildStep: String): Unit = {
    if (StringUtils.isNotBlank(buildStep)) {
      steps += buildStep
      lastTime = System.currentTimeMillis
    }
  }

  def snapshot: DockerBuildSnapshot = DockerBuildSnapshot(steps, lastTime)
}

class DockerPushProgress(
    val layers: mutable.Map[String, DockerLayerProgress],
    var error: String,
    var lastTime: Long) {
  // noinspection DuplicatedCode
  def update(pushRsp: PushResponseItem): Unit = {
    val nonPushUpdateState =
      pushRsp == null || StringUtils.isBlank(pushRsp.getId) || StringUtils
        .isBlank(pushRsp.getStatus)
    if (nonPushUpdateState) {
      return
    }
    if (pushRsp.getStatus.contains("complete")) {
      layers += pushRsp.getId -> DockerLayerProgress(pushRsp.getId, pushRsp.getStatus, 1, 1)
      lastTime = System.currentTimeMillis
    } else {
      val cur =
        if (pushRsp.getProgressDetail == null || pushRsp.getProgressDetail.getCurrent == null) 0L
        else pushRsp.getProgressDetail.getCurrent.toLong
      val total =
        if (pushRsp.getProgressDetail == null || pushRsp.getProgressDetail.getTotal == null) 0L
        else pushRsp.getProgressDetail.getTotal.toLong
      layers += pushRsp.getId -> DockerLayerProgress(pushRsp.getId, pushRsp.getStatus, cur, total)
      error = Option(pushRsp.getErrorDetail).map(_.getMessage).getOrElse("")
      lastTime = System.currentTimeMillis
    }
  }

  def snapshot: DockerPushSnapshot =
    DockerPushSnapshot.of(layers.values.toSeq, error, lastTime)
}

object DockerPullProgress {
  def empty(): DockerPullProgress =
    new DockerPullProgress(mutable.Map(), "", System.currentTimeMillis)
}

object DockerBuildProgress {
  def empty(): DockerBuildProgress =
    new DockerBuildProgress(ArrayBuffer(), System.currentTimeMillis)
}

object DockerPushProgress {
  def empty(): DockerPushProgress =
    new DockerPushProgress(mutable.Map(), "", System.currentTimeMillis)
}

/**
 * push/pull progress of per docker layer.
 *
 * @param current
 *   already download size (byte)
 * @param total
 *   layer size (byte)
 */
case class DockerLayerProgress(layerId: String, status: String, current: Long, total: Long) {

  def percent: Double = Utils.calPercent(current, total)

  def currentMb: Double =
    if (current == 0) 0
    else "%.2f".format(current.toDouble / (1024 * 1024)).toDouble

  def totalMb: Double =
    if (total == 0) 0
    else "%.2f".format(total.toDouble / (1024 * 1024)).toDouble
}
