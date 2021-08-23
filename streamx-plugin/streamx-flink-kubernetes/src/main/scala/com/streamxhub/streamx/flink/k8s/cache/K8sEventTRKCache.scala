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
package com.streamxhub.streamx.flink.k8s.cache

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import com.streamxhub.streamx.flink.k8s.cache.K8sEventTRKCache.{DeploymentVal, Key, ServiceVal}
import com.streamxhub.streamx.flink.k8s.enums.K8sEventAction
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.apps.Deployment

/**
 * kubernetes event tracking cache, just for flink-application mode
 */
class K8sEventTRKCache {
  /**
   * last k8s events of Service kind
   */
  val lastServiceEvents: Cache[Key, ServiceVal] = Caffeine.newBuilder.build()
  /**
   * last k8s events of Deployment kind
   */
  val lastDeploymentEvents: Cache[Key, DeploymentVal] = Caffeine.newBuilder.build()

}

object K8sEventTRKCache {
  case class Key(namespace: String, clusterId: String)

  case class ServiceVal(action: K8sEventAction.Value, event: Service)

  case class DeploymentVal(action: K8sEventAction.Value, event: Deployment)
}





