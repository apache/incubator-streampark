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
import com.streamxhub.console.core.metrics.flink.JvmProfiler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("metrics")
public class MetricsController {


    @PostMapping("report")
    public RestResponse report(@RequestBody JvmProfiler jvmProfiler) {
        try {
            if (jvmProfiler != null) {
                System.out.println("id:" + jvmProfiler.getId() + ",token:" + jvmProfiler.getToken() + ",type:" + jvmProfiler.getType());
                Map<String, Object> map = jvmProfiler.getMetrics();
                if (map != null) {
                    map.forEach((k, v) -> System.out.println(k + "=====>" + v));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RestResponse.create();
    }
}
