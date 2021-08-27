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
package com.streamxhub.streamx.flink.k8s

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.k8s.enums.FlinkK8sExecuteMode.SESSION
import com.streamxhub.streamx.flink.k8s.model._

import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.ThreadSafe
import scala.collection.JavaConverters._

/**
 * Tracking info cache pool on flink kubernetes mode.
 * author:Al-assad
 */
class FlinkTRKCachePool extends Logger with AutoCloseable {

  // cache for tracking identifiers
  val trkIds: Cache[TrkId, TrkIdCV] = Caffeine.newBuilder.build()

  // cache for tracking flink job status
  val jobStatuses: Cache[TrkId, JobStatusCV] = Caffeine.newBuilder.build()

  // cache for tracking kubernetes events with Deployment kind
  val k8sDeploymentEvents: Cache[K8sEventKey, K8sDeploymentEventCV] = Caffeine.newBuilder.build()

  // cache for last traking flink cluster metrics, record
  val flinkMetrics: SglValCache[FlinkMetricCV] = SglValCache[FlinkMetricCV](FlinkMetricCV.empty)

  // todo recovery from db

  override def close(): Unit = {
    jobStatuses.cleanUp()
    k8sDeploymentEvents.cleanUp()
    trkIds.cleanUp()
  }

  /**
   * collect all tracking identifiers
   */
  def collectAllTrkIds(): Set[TrkId] = {
    trkIds.asMap().keySet().asScala.toSet
  }

  /**
   * determines whether the specified TrkId is in the trace
   */
  def isInTracking(trkId: TrkId): Boolean = {
    if (trkId == null || !trkId.isLegal) {
      return false
    }
    trkIds.getIfPresent(trkId) != null
  }

  /**
   * collect all legal tracking ids, and remove jobId info of session mode trkId
   */
  private[k8s] def collectDistinctTrkIds(): Set[TrkId] = collectAllTrkIds()
    .map(trkId => if (trkId.executeMode == SESSION) TrkId(trkId.executeMode, trkId.namespace, trkId.clusterId, "") else trkId)
    .filter(_.isLegal)

}

/**
 * Thread-safe signle value cache
 */
@ThreadSafe
case class SglValCache[E <: AnyRef](initElement: E) {
  private val value = new AtomicReference[E](initElement)

  def get: E = value.get()

  def set(newVal: E): Unit = value.set(newVal)

  def update(func: E => E): Unit = value.updateAndGet { case e: E => func(e) }

}
