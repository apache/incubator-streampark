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

package com.streamxhub.plugin.profiling.reporter;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import com.streamxhub.plugin.profiling.ArgumentUtils;
import com.streamxhub.plugin.profiling.Reporter;
import com.streamxhub.plugin.profiling.util.AgentLogger;
import com.streamxhub.plugin.profiling.util.Utils;
import scalaj.http.Http;
import scalaj.http.HttpRequest;
import scalaj.http.HttpResponse;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.http.Consts.UTF_8;

/**
 * @author benjobs
 */
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
    public void doArguments(Map<String, List<String>> parsedArgs) {
        id = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_ID);
        token = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_TOKEN);
        url = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_URL);
        type = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_TYPE);
    }

    @Override
    public void report(String profilerName, Map<String, Object> metrics) {
        metrics.put("$id", id);
        metrics.put("$token", token);
        metrics.put("$type", type);
        String json = Utils.toJsonString(metrics);
        String params = "metric=" + Utils.zipString(json);
        HttpResponse response = Http.apply(url).postData(params).asString();
        logger.log("[StreamX] jvm-profiler report:" + response.body());

        /*try {
            logger.debug(String.format("Getting url: %s", url));
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(url);
                List<NameValuePair> pairs = new ArrayList<>(1);
                pairs.add(new BasicNameValuePair("metric", Utils.zipString(json)));
                httpPost.setEntity(new UrlEncodedFormEntity(pairs, UTF_8));
                try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
                    int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode != HttpURLConnection.HTTP_OK) {
                        throw new RuntimeException("Failed response from url: " + url + ", response code: " + statusCode);
                    }
                    byte[] bytes = Utils.toByteArray(httpResponse.getEntity().getContent());
                    String result = new String(bytes, UTF_8.name());
                    logger.log("[StreamX] jvm-profiler report:" + result);
                }
            }
        } catch (Throwable ex) {
            throw new RuntimeException("Failed getting url: " + url, ex);
        }*/

    }

    @Override
    public void close() {

    }


}
