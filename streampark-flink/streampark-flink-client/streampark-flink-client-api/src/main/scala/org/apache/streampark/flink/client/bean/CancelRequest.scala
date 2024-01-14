/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.flink.client.bean

import org.apache.streampark.common.conf.{FlinkVersion, K8sFlinkConfig}
import org.apache.streampark.common.enums.ExecutionMode

import javax.annotation.Nullable

import java.util.{Map => JavaMap}

case class CancelRequest(
    override val flinkVersion: FlinkVersion,
    override val executionMode: ExecutionMode,
    @Nullable override val properties: JavaMap[String, Any],
    override val clusterId: String,
    override val jobId: String,
    override val withSavepoint: Boolean,
    withDrain: Boolean,
    override val savepointPath: String,
    override val kubernetesNamespace: String = K8sFlinkConfig.DEFAULT_KUBERNETES_NAMESPACE)
  extends SavepointRequestTrait
