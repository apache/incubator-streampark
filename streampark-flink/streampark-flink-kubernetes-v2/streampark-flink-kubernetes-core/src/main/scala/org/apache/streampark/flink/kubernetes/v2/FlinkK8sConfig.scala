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

package org.apache.streampark.flink.kubernetes.v2

import org.apache.streampark.common.conf.{InternalOption, Workspace}

object FlinkK8sConfig {

  // ----- embedded http file server config -----

  val EMBEDDED_HTTP_FILE_SERVER_LOCAL_MIRROR_DIR: InternalOption = InternalOption(
    key = "streampark.flink-k8s.fs.mirror-dir",
    defaultValue = s"${Workspace.local.WORKSPACE}/mirror",
    classType = classOf[String],
    description = "Local mirror directory for embedded file server"
  )

  val EMBEDDED_HTTP_FILE_SERVER_PORT: InternalOption = InternalOption(
    key = "streampark.flink-k8s.fs-server.port",
    defaultValue = 10030,
    classType = classOf[Integer],
    description = "Port of the embedded http file server"
  )

  // ----- observer config -----

  val EVAL_FLINK_JOB_SNAPSHOT_PARALLELISM: InternalOption = InternalOption(
    key = "streampark.flink-k8s.job-snapshot.eval-parallelism",
    defaultValue = 5,
    classType = classOf[Integer],
    description = "Parallelism of fibers evaluating flink job status"
  )

  val EVAL_FLINK_JOB_SNAP_INTERVAL_MILLIS: InternalOption = InternalOption(
    key = "streampark.flink-k8s.job-snapshot.eval-interval",
    defaultValue = 1000L,
    classType = classOf[Long],
    description = "Interval for evaluating the status of the flink task, in milliseconds"
  )

  val POLL_FLINK_REST_INTERVAL: InternalOption = InternalOption(
    key = "streampark.flink-k8s.flink-rest.poll-interval",
    defaultValue = 1000L,
    classType = classOf[Long],
    description = "Interval for polling the flink rest api, in milliseconds"
  )

  val RETRY_FLINK_REST_INTERVAL: InternalOption = InternalOption(
    key = "streampark.flink-k8s.flink-rest.poll-retry-interval",
    defaultValue = 2000L,
    classType = classOf[Long],
    description = "Polling interval when the flink rest api request fails, in milliseconds"
  )

  val REACH_FLINK_REST_TYPE: InternalOption = InternalOption(
    key = "streampark.flink-k8s.flink-rest.access-type",
    defaultValue = "IP",
    classType = classOf[String],
    description = "The type of the flink rest api, IP or DNS"
  )

  // ----- operator config -----

  val LOG_FLINK_CR_YAML: InternalOption = InternalOption(
    key = "streampark.flink-k8s.log-cr-yaml",
    defaultValue = true,
    classType = classOf[Boolean],
    description = "Whether to log the yaml of the generated flink custom resource when submit flink job & cluster"
  )

}
