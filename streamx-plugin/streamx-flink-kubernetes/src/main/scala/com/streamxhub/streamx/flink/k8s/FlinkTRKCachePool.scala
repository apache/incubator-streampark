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
import com.streamxhub.streamx.flink.k8s.model.{JobStatusCV, K8sDeploymentEventCV, K8sEventKey, TrkId, TrkIdCV}
import scala.collection.JavaConverters._

/**
 * auther:Al-assad
 */
class FlinkTRKCachePool {

  // tracking identifiers cache
  val trkIds: Cache[TrkId, TrkIdCV] = Caffeine.newBuilder.build()
  // tracking flink job status cache
  val jobStatuses: Cache[TrkId, JobStatusCV] = Caffeine.newBuilder.build()
  // tracking kubernetes events cache with Deployment kind
  val k8sDeploymentEvents: Cache[K8sEventKey, K8sDeploymentEventCV] = Caffeine.newBuilder.build()

  // todo recovery from db

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

}
