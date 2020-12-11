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

package com.streamxhub.plugin.profiling.reporters;

import com.streamxhub.plugin.profiling.ArgumentUtils;
import com.streamxhub.plugin.profiling.Reporter;
import com.streamxhub.plugin.profiling.util.HttpClientUtils;
import com.streamxhub.plugin.profiling.util.AgentLogger;
import com.streamxhub.plugin.profiling.util.IOUtils;
import com.streamxhub.plugin.profiling.util.JsonUtils;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpReporter implements Reporter {

    private static final AgentLogger logger = AgentLogger.getLogger(HttpReporter.class.getName());

    private final static String ARG_ID = "id";
    private final static String ARG_TOKEN = "token";
    private final static String ARG_URL = "url";
    private final static String ARG_TYPE = "type";
    private String id;
    private String token;
    private String url;
    private String type;

    public HttpReporter() {
    }

    @Override
    public void updateArguments(Map<String, List<String>> parsedArgs) {
        id = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_ID);
        token = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_TOKEN);
        url = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_URL);
        type = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_TYPE);
    }

    @Override
    public void report(String profilerName, Map<String, Object> metrics) {
        new Thread(() -> {
            metrics.put("id", id);
            metrics.put("token", token);
            metrics.put("type", type);
            String json = JsonUtils.serialize(metrics);
            Map<String, Object> params = new HashMap<>(1);
            params.put("text", IOUtils.zipString(json));
            try {
                HttpClientUtils.httpPostRequest(url, params);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void close() {

    }


}
