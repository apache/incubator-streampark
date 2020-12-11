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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamxhub.common.util.DeflaterUtils;
import com.streamxhub.console.base.domain.RestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("metrics")
public class MetricsController {

    private ObjectMapper mapper = new ObjectMapper();

    @PostMapping("report")
    public RestResponse report(String text) {
        try {
            String content = DeflaterUtils.unzipString(text);
            if (content != null) {
                Map<String, Object> map = mapper.readValue(content, Map.class);
                if (map != null) {
                    String id = map.remove("$id").toString();
                    String token = map.remove("$token").toString();
                    String type = map.remove("$type").toString();
                    System.out.println("id:" + id + ",token:" + token + ",type:" + type);
                    map.forEach((k, v) -> System.out.println(k + "=====>" + v));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RestResponse.create();
    }
}
