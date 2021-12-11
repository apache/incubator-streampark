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

package com.streamxhub.streamx.console.core.metrics.flink;

import lombok.Data;

import java.io.Serializable;

/**
 * @author benjobs
 */
@Data
public class MailTemplate implements Serializable {
    private String title;
    private String jobName;
    private String status;
    private Integer type;
    private String startTime;
    private String endTime;
    private String duration;
    private String link;
    private String cpFailureRateInterval;
    private Integer cpMaxFailureInterval;
    private Boolean restart;
    private Integer restartIndex;
    private Integer totalRestart;
}
