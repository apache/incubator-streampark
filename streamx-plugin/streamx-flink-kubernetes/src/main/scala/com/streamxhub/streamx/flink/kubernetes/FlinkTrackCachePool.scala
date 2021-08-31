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
package com.streamxhub.streamx.flink.kubernetes

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.kubernetes.enums.FlinkK8sExecuteMode.SESSION
import com.streamxhub.streamx.flink.kubernetes.model._

import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.ThreadSafe
import scala.collection.JavaConverters._

/**
 * Tracking info cache pool on flink kubernetes mode.
 * author:Al-assad
 */
class FlinkTrackCachePool extends Logger with AutoCloseable {

  // cache for tracking identifiers
  val trackIds: Cache[TrackId, TrackIdCV] = Caffeine.newBuilder.build()

  // cache for tracking flink job status
  val jobStatuses: Cache[TrackId, JobStatusCV] = Caffeine.newBuilder.build()

  // cache for tracking kubernetes events with Deployment kind
  val k8sDeploymentEvents: Cache[K8sEventKey, K8sDeploymentEventCV] = Caffeine.newBuilder.build()

  // cache for last tracking flink cluster metrics, record
  val flinkMetrics: SglValCache[FlinkMetricCV] = SglValCache[FlinkMetricCV](FlinkMetricCV.empty)

  // todo recovery from db

  override def close(): Unit = {
    jobStatuses.cleanUp()
    k8sDeploymentEvents.cleanUp()
    trackIds.cleanUp()
  }

  /**
   * collect all tracking identifiers
   */
  def collectAllTrackIds(): Set[TrackId] = {
    trackIds.asMap().keySet().asScala.toSet
  }

  /**
   * determines whether the specified TrkId is in the trace
   */
  def isInTracking(trackId: TrackId): Boolean = {
    if (trackId == null || !trackId.isLegal){
      return false
    }
    trackIds.getIfPresent(trackId) != null
  }

  /**
   * collect all legal tracking ids, and remove jobId info of session mode trkId
   */
  private[kubernetes] def collectDistinctTrackIds(): Set[TrackId] = collectAllTrackIds()
    .filter(_.isLegal)
    .map(id =>
      id match {
        case x if x.executeMode == SESSION => TrackId(id.executeMode, id.namespace, id.clusterId, "")
        case _ => id
      }
    )
}

/**
 * Thread-safe single value cache
 */
@ThreadSafe
case class SglValCache[T](initElement: T)(implicit manifest: Manifest[T]) {

  private val value = new AtomicReference[T](initElement)

  def get: T = value.get()

  def set(newVal: T): Unit = value.set(newVal)

//  def update(func: T => T): Unit = value.updateAndGet { case t: T => func(t) }

}
