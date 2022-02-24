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

package com.streamxhub.streamx.flink.packer

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.listener.{BuildImageCallbackListener, PullImageCallbackListener, PushImageCallbackListener}
import com.github.dockerjava.api.model.{PullResponseItem, PushResponseItem}
import com.streamxhub.streamx.common.util.Utils.tryWithResource

/**
 * @author Al-assad
 */
package object docker {

  def watchDockerBuildStep(func: String => Unit): BuildImageCallbackListener =
    new BuildImageCallbackListener() {
      def watchBuildStep(buildStepMsg: String): Unit = func(buildStepMsg)
    }

  def watchDockerPullProcess(func: PullResponseItem => Unit): PullImageCallbackListener =
    new PullImageCallbackListener {
      override def watchPullProcess(processDetail: PullResponseItem): Unit = func(processDetail)
    }

  def watchDockerPushProcess(func: PushResponseItem => Unit): PushImageCallbackListener =
    new PushImageCallbackListener {
      override def watchPushProcess(processDetail: PushResponseItem): Unit = func(processDetail)
    }

  def usingDockerClient[R](process: DockerClient => R)(handleException: Throwable => R): R =
    tryWithResource(DockerRetriever.newDockerClient())(process)(handleException)

}
