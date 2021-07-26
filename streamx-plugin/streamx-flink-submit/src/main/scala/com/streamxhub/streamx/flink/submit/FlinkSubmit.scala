/*
 * Copyright (c) 2019 The StreamX Project
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
package com.streamxhub.streamx.flink.submit

import com.streamxhub.streamx.common.enums.ExecutionMode
import com.streamxhub.streamx.flink.submit.impl._

import java.lang.{Boolean => JavaBool}

object FlinkSubmit {

  def submit(submitInfo: SubmitRequest): SubmitResponse = {
    submitInfo.executionMode match {
      case ExecutionMode.APPLICATION => YarnApplicationSubmit.submit(submitInfo)
      case ExecutionMode.YARN_PRE_JOB => YarnPreJobSubmit.submit(submitInfo)
      case ExecutionMode.LOCAL => LocalSubmit.submit(submitInfo)
      case ExecutionMode.KUBERNETES_NATIVE_SESSION => KubernetesNativeSessionSubmit.submit(submitInfo)
      case ExecutionMode.KUBERNETES_NATIVE_APPLICATION => KubernetesNativeApplicationSubmit.submit(submitInfo)
      case _ => throw new UnsupportedOperationException(s"Unsupported ${submitInfo.executionMode} Submit ")
    }
  }

  def stop(executionMode: ExecutionMode, stopInfo: StopRequest): StopResponse = {
    executionMode match {
      case ExecutionMode.APPLICATION | ExecutionMode.YARN_PRE_JOB | ExecutionMode.YARN_SESSION =>
        YarnPreJobSubmit.stop(stopInfo)
      case ExecutionMode.LOCAL => LocalSubmit.stop(stopInfo)
      case ExecutionMode.KUBERNETES_NATIVE_SESSION => KubernetesNativeSessionSubmit.stop(stopInfo)
      case ExecutionMode.KUBERNETES_NATIVE_APPLICATION => KubernetesNativeApplicationSubmit.stop(stopInfo)
      case _ => throw new UnsupportedOperationException(s"Unsupported ${executionMode} Submit ")
    }
  }

}
