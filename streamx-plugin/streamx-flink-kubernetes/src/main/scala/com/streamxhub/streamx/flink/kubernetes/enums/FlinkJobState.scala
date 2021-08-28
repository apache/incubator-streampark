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
package com.streamxhub.streamx.flink.kubernetes.enums

import org.apache.flink.api.common.JobStatus

/**
 * author:Al-assad
 * flink job status on kubernetes
 */
object FlinkJobState extends Enumeration {

  val K8S_DEPLOYING, LOST, OTHER = Value

  // @see org.apache.flink.api.common.JobStatus
  val INITIALIZING, CREATED, RUNNING, FAILING, FAILED, CANCELLING, CANCELED, FINISHED, RESTARTING, SUSPENDED, RECONCILING = Value

  def of(value: String): FlinkJobState.Value = {
    this.values.find(_.toString == value).getOrElse(OTHER)
  }

  def of(jobStatus: JobStatus): FlinkJobState.Value = {
    val jobStatusStr = jobStatus.toString
    this.values.find(_.toString == jobStatusStr).getOrElse(OTHER)
  }

}
