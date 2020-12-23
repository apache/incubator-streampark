/**
 * s * Copyright (c) 2019 The StreamX Project
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
package com.streamxhub.console.core.controller;

import com.streamxhub.console.base.domain.RestResponse;
import com.streamxhub.console.core.entity.FlameGraph;
import com.streamxhub.console.core.metrics.flink.JvmProfiler;
import com.streamxhub.console.core.service.FlameGraphService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@Slf4j
@Validated
@RestController
@RequestMapping("metrics")
public class MetricsController {

    private final String STACKTRACE_PROFILER_NAME = "Stacktrace";

    @Autowired
    private FlameGraphService flameGraphService;

    @PostMapping("report")
    public RestResponse report(@RequestBody JvmProfiler jvmProfiler) {
        try {
            if (jvmProfiler != null && jvmProfiler.getProfiler().equals(STACKTRACE_PROFILER_NAME)) {
                System.out.println("id:" + jvmProfiler.getId() + ",token:" + jvmProfiler.getToken() + ",type:" + jvmProfiler.getType());
                String json = jvmProfiler.getMetrics();
                FlameGraph flameGraph = new FlameGraph();
                flameGraph.setAppId(jvmProfiler.getId());
                flameGraph.setProfiler(jvmProfiler.getProfiler());
                flameGraph.setTimeline(new Date());
                flameGraph.setContent(json);
                flameGraphService.save(flameGraph);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RestResponse.create();
    }
}
