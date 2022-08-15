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

package com.streamxhub.streamx.console.core.service.impl;

import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.log.LogClientService;
import com.streamxhub.streamx.console.core.service.ApplicationService;
import com.streamxhub.streamx.console.core.service.LoggerService;
import com.streamxhub.streamx.flink.kubernetes.FlinkJobWatch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * log service
 */
@Service
@Slf4j
public class LoggerServiceImpl implements LoggerService {
    @Autowired
    private ApplicationService applicationService;

    private final LogClientService logClient;

    public LoggerServiceImpl(){
        logClient = new LogClientService();
    }

    /**
     * view log
     *
     * @param skipLineNum skip line number
     * @param limit       limit
     * @return log string data
     */
    public CompletionStage<String> queryLog(String namespac, String jobName, int skipLineNum, int limit) {
        return CompletableFuture.supplyAsync(() ->
            FlinkJobWatch.jobDeploymentsWatch(namespac, jobName)
        ).thenApply(path -> logClient.rollViewLog(String.valueOf(path), skipLineNum, limit));
    }

    public byte[] getLogBytes(Application app) {
        return null;
    }
}
