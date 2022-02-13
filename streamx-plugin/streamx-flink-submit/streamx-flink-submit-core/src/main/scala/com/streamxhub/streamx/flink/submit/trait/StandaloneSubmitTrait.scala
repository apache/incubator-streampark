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

package com.streamxhub.streamx.flink.submit.`trait`

import com.streamxhub.streamx.common.conf.Workspace
import com.streamxhub.streamx.common.util.StandaloneUtils
import com.streamxhub.streamx.flink.submit.domain.{StopRequest, StopResponse, SubmitRequest, SubmitResponse}
import org.apache.flink.client.deployment.{DefaultClusterClientServiceLoader, StandaloneClusterDescriptor, StandaloneClusterId}
import org.apache.flink.configuration._

/**
 * @Description Remote Submit
 */
trait StandaloneSubmitTrait extends FlinkSubmitTrait {

  lazy val workspace: Workspace = Workspace.local

  /**
   * @param submitRequest
   * @param flinkConfig
   */
  override def doConfig(submitRequest: SubmitRequest, flinkConfig: Configuration): Unit = {
    flinkConfig.safeSet(PipelineOptions.NAME, submitRequest.effectiveAppName)
    if (!flinkConfig.contains(JobManagerOptions.ADDRESS) && !flinkConfig.contains(RestOptions.ADDRESS)) {
      logWarn(s"RestOptions Address is not set,use default value : ${StandaloneUtils.DEFAULT_REST_ADDRESS}")
      flinkConfig.setString(RestOptions.ADDRESS, StandaloneUtils.DEFAULT_REST_ADDRESS)
    }
    if (!flinkConfig.contains(RestOptions.PORT)) {
      logWarn(s"RestOptions port is not set,use default value : ${StandaloneUtils.DEFAULT_REST_PORT}")
      flinkConfig.setInteger(RestOptions.PORT, StandaloneUtils.DEFAULT_REST_PORT)
    }
  }

  override def submit(submitRequest: SubmitRequest): SubmitResponse = super.submit(submitRequest)

  override def stop(stopRequest: StopRequest): StopResponse = super.stop(stopRequest)

  /**
   * create StandAloneClusterDescriptor
   *
   * @param flinkConfig
   */
  def getStandAloneClusterDescriptor(flinkConfig: Configuration): (StandaloneClusterId, StandaloneClusterDescriptor) = {
    val serviceLoader = new DefaultClusterClientServiceLoader
    val clientFactory = serviceLoader.getClusterClientFactory(flinkConfig)
    val standaloneClusterId: StandaloneClusterId = clientFactory.getClusterId(flinkConfig)
    val standaloneClusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig).asInstanceOf[StandaloneClusterDescriptor]
    (standaloneClusterId, standaloneClusterDescriptor)
  }

}
