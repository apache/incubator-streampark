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
import scala.collection.JavaConversions._
import scala.util.Try

/**
 * Tracking info cache pool on flink kubernetes mode.
 * author:Al-assad
 */
class FlinkTrackCachePool extends Logger with AutoCloseable {

  // cache for tracking identifiers
  val trackIds: TrackIdCache = TrackIdCache.build()

  // cache for flink Job-manager rest url
  val clusterRestUrls: Cache[ClusterKey, String] = Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build()

  // cache for tracking flink job status
  val jobStatuses: Cache[TrackId, JobStatusCV] = Caffeine.newBuilder.build()

  // cache for tracking kubernetes events with Deployment kind
  val k8sDeploymentEvents: Cache[K8sEventKey, K8sDeploymentEventCV] = Caffeine.newBuilder.build()

  // cache for last each flink cluster metrics (such as a session cluster or a application cluster)
  val flinkMetrics: Cache[ClusterKey, FlinkMetricCV] = Caffeine.newBuilder().build()

  override def close(): Unit = {
    jobStatuses.cleanUp()
    k8sDeploymentEvents.cleanUp()
    trackIds.cleanUp()
  }

  /**
   * collect all tracking identifiers
   */
  def collectAllTrackIds(): Set[TrackId] = trackIds.getAll()

  /**
   * determines whether the specified TrackId is in the trace
   */
  def isInTracking(trackId: TrackId): Boolean = {
    if (Try(trackId.nonLegal).getOrElse(true)) false; else {
      trackIds.get(trackId) != null
    }
  }

  /**
   * collect all legal tracking ids, and covert to ClusterKey
   */
  private[kubernetes] def collectTracks(): Set[TrackId] = collectAllTrackIds().filter(_.isLegal)

  /**
   * collect the aggregation of flink metrics that in tracking
   */
  def collectAccMetric(): FlinkMetricCV = {
    // get cluster metrics that in tracking
    collectTracks() match {
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
   * get flink job-manager rest url from cache which will auto refresh when it it empty.
   */
  def getClusterRestUrl(clusterKey: ClusterKey): Option[String] =
    Option(clusterRestUrls.getIfPresent(clusterKey)).filter(_.nonEmpty).orElse(refreshClusterRestUrl(clusterKey))

  /**
   * refresh flink job-manager rest url from remote flink cluster, and cache it.
   */
  def refreshClusterRestUrl(clusterKey: ClusterKey): Option[String] = {
    val restUrl = KubernetesRetriever.retrieveFlinkRestUrl(clusterKey)
    if (restUrl.nonEmpty) {
      clusterRestUrls.put(clusterKey, restUrl.get)
    }
    restUrl
  }

}


class TrackIdCache {
  case class CacheKey(key: java.lang.Long) extends Serializable

  lazy val cache: Cache[CacheKey, TrackId] = Caffeine.newBuilder.build()

  def update(k: TrackId): Unit = {
    cache.invalidate(k)
    cache.put(CacheKey(k.appId), k)
  }

  def set(k: TrackId): Unit = cache.put(CacheKey(k.appId), k)

  def invalidate(k: TrackId): Unit = cache.invalidate(CacheKey(k.appId))

  def get(k: TrackId): TrackId = cache.getIfPresent(CacheKey(k.appId))

  def getAll(): Set[TrackId] = cache.asMap().values().toSet

  def cleanUp(): Unit = cache.cleanUp()

}

object TrackIdCache {
  def build(): TrackIdCache = {
    new TrackIdCache()
  }
}
