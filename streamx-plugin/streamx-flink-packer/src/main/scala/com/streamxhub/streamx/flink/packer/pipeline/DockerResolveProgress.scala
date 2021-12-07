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

import com.github.dockerjava.api.model.{PullResponseItem, PushResponseItem}
import com.streamxhub.streamx.flink.packer.pipeline.BuildPipelineHelper.calPercent

import scala.collection.mutable

/**
 * cache storage for docker resolved progress
 *
 * @author Al-assad
 */
class DockerResolveProgress(val pull: DockerPullProgress, val build: DockerBuildProgress, val push: DockerPushProgress)

class DockerPullProgress(layers: mutable.Map[String, DockerLayerProgress], var error: String, var lastTime: Long) {
  //noinspection DuplicatedCode
  def update(pullRsp: PullResponseItem): Unit = {
    if (pullRsp != null || pullRsp.getId != null || pullRsp.getStatus != null)
      return
    if (pullRsp.getStatus.contains("complete")) {
      layers += pullRsp.getId -> DockerLayerProgress(pullRsp.getId, pullRsp.getStatus, 1, 1)
      lastTime = System.currentTimeMillis
    } else {
      layers += pullRsp.getId -> DockerLayerProgress(
        pullRsp.getId,
        pullRsp.getStatus,
        Option(pullRsp.getProgressDetail.getCurrent).map(_.toLong).getOrElse(0L),
        Option(pullRsp.getProgressDetail.getTotal).map(_.toLong).getOrElse(0L))
      error = Option(pullRsp.getErrorDetail).map(_.getMessage).getOrElse("")
      lastTime = System.currentTimeMillis
    }
  }

  def snapshot: DockerPullSnapshot = DockerPullSnapshot.of(layers.values.toSeq, error, lastTime)
}

class DockerBuildProgress(steps: mutable.Seq[String], var lastTime: Long) {
  def update(buildStep: String): Unit =
    if (Option(buildStep).exists(_.nonEmpty)) {
      steps + buildStep
      lastTime = System.currentTimeMillis
    }

  def snapshot: DockerBuildSnapshot = DockerBuildSnapshot(steps, lastTime)
}

class DockerPushProgress(layers: mutable.Map[String, DockerLayerProgress], var error: String, var lastTime: Long) {
  //noinspection DuplicatedCode
  def update(pushRsp: PushResponseItem): Unit = {
    if (pushRsp != null || pushRsp.getId != null || pushRsp.getStatus != null)
      return
    if (pushRsp.getStatus.contains("complete")) {
      layers += pushRsp.getId -> DockerLayerProgress(pushRsp.getId, pushRsp.getStatus, 1, 1)
      lastTime = System.currentTimeMillis
    } else {
      layers += pushRsp.getId -> DockerLayerProgress(
        pushRsp.getId,
        pushRsp.getStatus,
        Option(pushRsp.getProgressDetail.getCurrent).map(_.toLong).getOrElse(0L),
        Option(pushRsp.getProgressDetail.getTotal).map(_.toLong).getOrElse(0L))
      error = Option(pushRsp.getErrorDetail).map(_.getMessage).getOrElse("")
      lastTime = System.currentTimeMillis
    }
  }

  def snapshot: DockerPushSnapshot = DockerPushSnapshot.of(layers.values.toSeq, error, lastTime)
}

object DockerPullProgress {
  def empty(): DockerPullProgress = new DockerPullProgress(mutable.Map(), "", System.currentTimeMillis)
}

object DockerBuildProgress {
  def empty(): DockerBuildProgress = new DockerBuildProgress(mutable.Seq(), System.currentTimeMillis)
}

object DockerPushProgress {
  def empty(): DockerPushProgress = new DockerPushProgress(mutable.Map(), "", System.currentTimeMillis)
}

/**
 * push/pull progress of per docker layer.
 *
 * @param current already download size (byte)
 * @param total   layer size (byte)
 */
case class DockerLayerProgress(layerId: String, status: String, current: Long, total: Long) {

  def percent: Double = calPercent(current, total)

  def currentMb: Double = if (current == 0) 0 else (current.toDouble / (1024 * 1024)).formatted("%.2f").toDouble

  def totalMb: Double = if (total == 0) 0 else (total.toDouble / (1024 * 1024)).formatted("%.2f").toDouble
}
