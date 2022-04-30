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

import java.util.{Map => JavaMap}
import javax.annotation.Nullable

import com.streamxhub.streamx.common.domain.FlinkVersion
import com.streamxhub.streamx.common.enums.{ExecutionMode, FlinkK8sRestExposedType, ResolveOrder}
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions

case class DeployRequest(flinkVersion: FlinkVersion,
                         clusterId: String,
                         executionMode: ExecutionMode,
                         resolveOrder: ResolveOrder,
                         flameGraph: JavaMap[String, java.io.Serializable],
                         dynamicOption: Array[String],
                         @Nullable k8sDeployParam: KubernetesDeployParam,
                         @Nullable extraParameter: JavaMap[String, Any]
                         )

case class KubernetesDeployParam(clusterId: String,
                                 kubernetesNamespace: String = KubernetesConfigOptions.NAMESPACE.defaultValue(),
                                 kubeConf: String = "~/.kube/config",
                                 serviceAccount: String = KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT.defaultValue(),
                                 flinkImage: String = KubernetesConfigOptions.CONTAINER_IMAGE.defaultValue(),
                                 @Nullable flinkRestExposedType: FlinkK8sRestExposedType = FlinkK8sRestExposedType.ClusterIP)
