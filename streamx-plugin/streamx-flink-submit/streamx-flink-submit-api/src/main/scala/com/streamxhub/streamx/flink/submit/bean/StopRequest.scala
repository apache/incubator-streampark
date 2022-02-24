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

package com.streamxhub.streamx.flink.submit.bean

import com.streamxhub.streamx.common.conf.K8sFlinkConfig
import com.streamxhub.streamx.common.domain.FlinkVersion
import com.streamxhub.streamx.common.enums.ExecutionMode

import java.util.{Map => JavaMap}
import javax.annotation.Nullable

case class StopRequest(flinkVersion: FlinkVersion,
                       executionMode: ExecutionMode,
                       clusterId: String,
                       jobId: String,
                       withSavePoint: Boolean,
                       withDrain: Boolean,
                       customSavePointPath: String,
                       kubernetesNamespace: String = K8sFlinkConfig.DEFAULT_KUBERNETES_NAMESPACE,
                       @Nullable dynamicOption: String,
                       @Nullable extraParameter: JavaMap[String, Any]
                      ) {

}
