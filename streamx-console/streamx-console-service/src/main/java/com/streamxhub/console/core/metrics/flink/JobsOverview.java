/**
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
package com.streamxhub.console.core.metrics.flink;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * @author benjobs
 */
@Data
public class JobsOverview {

    private List<Job> jobs;

    @Data
    public static class Job {
        @JsonProperty("jid")
        private String id;
        private String name;
        private String state;
        @JsonProperty("start-time")
        private Long startTime;
        @JsonProperty("end-time")
        private Long endTime;
        private Long duration;
        @JsonProperty("last-modification")
        private Long lastModification;
        private Task tasks;
    }

    @Data
    public static class Task {
        private int total;
        private int created;
        private int scheduled;
        private int deploying;
        private int running;
        private int finished;
        private int canceling;
        private int canceled;
        private int failed;
        private int reconciling;
    }
}

