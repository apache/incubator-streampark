/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.kubernetes

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.kubernetes.model._

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import javax.annotation.concurrent.ThreadSafe
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.util.Try

/**
 * Tracking info cache pool on flink kubernetes mode.
 * author:Al-assad
 */
class FlinkTrkCachePool extends Logger with AutoCloseable {

  // cache for tracking identifiers
  val trackIds: Cache[TrkId, TrkIdCV] = Caffeine.newBuilder.build()

  // cache for flink Jobmanager rest url
  val clusterRestUrls: Cache[ClusterKey, String] = Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build()

  // cache for tracking flink job status
  val jobStatuses: Cache[TrkId, JobStatusCV] = Caffeine.newBuilder.build()

  // cache for tracking kubernetes events with Deployment kind
  val k8sDeploymentEvents: Cache[K8sEventKey, K8sDeploymentEventCV] = Caffeine.newBuilder.build()

  // cache for last each flink cluster metrics (such as a session cluster or a application cluster)
  val flinkMetrics: Cache[ClusterKey, FlinkMetricCV] = Caffeine.newBuilder().build()

  // cache for last aggregate flink cluster metrics
  // val flinkMetricsAgg: SglValCache[FlinkMetricCV] = SglValCache[FlinkMetricCV](FlinkMetricCV.empty)

  override def close(): Unit = {
    jobStatuses.cleanUp()
    k8sDeploymentEvents.cleanUp()
    trackIds.cleanUp()
  }

  /**
   * collect all tracking identifiers
   */
  def collectAllTrackIds(): Set[TrkId] = {
    trackIds.asMap().keySet().asScala.toSet
  }

  /**
   * determines whether the specified TrkId is in the trace
   */
  def isInTracking(trkId: TrkId): Boolean = {
    if (Try(trkId.nonLegal).getOrElse(true)) false; else {
      trackIds.getIfPresent(trkId) != null
    }
  }

  /**
   * collect all legal tracking ids, and covert to ClusterKey
   */
  private[kubernetes] def collectTrkClusterKeys(): Set[ClusterKey] = collectAllTrackIds().filter(_.isLegal).map(_.toClusterKey)

  /**
   * collect the aggregation of flink metrics that in tracking
   */
  def collectAccMetric(): FlinkMetricCV = {
    // get cluster metrics that in tracking
    collectTrkClusterKeys() match {
      case k if k.isEmpty => FlinkMetricCV.empty
      case k =>
        flinkMetrics.getAllPresent(k) match {
          case m if m.isEmpty => FlinkMetricCV.empty
          case m =>
            // aggregate metrics
            m.values.fold(FlinkMetricCV.empty)((x, y) => x + y)
        }
    }
  }

  /**
   * get flink jobmanager rest url from cache which will auto refresh when it it empty.
   */
  def getClusterRestUrl(clusterKey: ClusterKey): Option[String] =
    Option(clusterRestUrls.getIfPresent(clusterKey)).filter(_.nonEmpty).orElse(refreshClusterRestUrl(clusterKey))

  /**
   * refresh flink jobmanager rest url from remote flink cluster, and cache it.
   */
  def refreshClusterRestUrl(clusterKey: ClusterKey): Option[String] = {
    val restUrl = KubernetesRetriever.retrieveFlinkRestUrl(clusterKey)
    if (restUrl.nonEmpty) clusterRestUrls.put(clusterKey, restUrl.get)
    restUrl
  }

}

/**
 * Thread-safe single value cache
 */
@ThreadSafe
case class SglValCache[T](initElement: T)(implicit manifest: Manifest[T]) {

  private val value = new AtomicReference[T](initElement)

  def get: T = value.get()

  def set(newVal: T): Unit = value.set(newVal)

  def update(func: T => T): Unit = value.updateAndGet(new UnaryOperator[T] {
    override def apply(t: T): T = func(t)
  })

}
