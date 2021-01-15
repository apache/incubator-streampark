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
package com.streamxhub.console.core.metrics.yarn;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author benjobs
 */
@Data
public class AppInfo {

    private App app;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class App {
        private String id;
        private String user;
        private String name;
        private String queue;
        private String state;
        private String finalStatus;
        private Float progress;
        private String trackingUI;
        private String trackingUrl;
        private String clusterId;
        private String applicationType;
        private Long startedTime;
        private Long finishedTime;
        private Long elapsedTime;
        private String amContainerLogs;
        private String amHostHttpAddress;
        private String allocatedMB;
        private String allocatedVCores;
        private String reservedMB;
        private String reservedVCores;
        private String runningContainers;
        private Long memorySeconds;
        private Long vcoreSeconds;
        private Long preemptedResourceMB;
        private Long preemptedResourceVCores;
        private Long numNonAMContainerPreempted;
        private Long numAMContainerPreempted;
        private String logAggregationStatus;
    }
}
