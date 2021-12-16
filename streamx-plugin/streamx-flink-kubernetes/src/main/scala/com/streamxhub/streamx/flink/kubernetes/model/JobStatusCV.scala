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

package com.streamxhub.streamx.flink.kubernetes.model

import com.streamxhub.streamx.flink.kubernetes.enums.FlinkJobState

/**
 * author:Al-assad
 *
 * @param jobState     state of flink job
 * @param jobId        flink jobId hex string
 * @param jobName      flink job name
 * @param jobStartTime flink job starting timestamp
 * @param pollEmitTime tracking polling emit timestamp
 * @param pollAckTime  traking polling result receive timestamp
 */
case class JobStatusCV(jobState: FlinkJobState.Value,
                       jobId: String,
                       jobName: String = "",
                       jobStartTime: Long = -1,
                       jobEndTime: Long = -1,
                       duration: Long = 0,
                       taskTotal: Int = 0,
                       pollEmitTime: Long,
                       pollAckTime: Long)
