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

package org.apache.streampark.flink.kubernetes.v2.model

/**
 * Flink job status snapshot identified by StreamPark app-id.
 *
 * For the logical code to convert a JobSnapshot to a [[org.apache.streampark.console.core.enums.FlinkAppState]],
 * see the [[org.apache.streampark.console.core.utils.FlinkAppStateConverter#dryK8sJobSnapshotToFlinkAppState]]
 *
 * @param appId Ref to [[org.apache.streampark.console.core.entity.Application.id]]
 * @param clusterNs Flink cluster namespace on kubernetes.
 * @param clusterId Flink cluster name on kubernetes.
 * @param crStatus Flink K8s CR status.
 * @param jobStatus Flink job status received from REST API.
 */
case class JobSnapshot(
    appId: Long,
    clusterNs: String,
    clusterId: String,
    crStatus: Option[FlinkCRStatus],
    jobStatus: Option[JobStatus])
