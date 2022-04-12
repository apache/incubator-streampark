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

package com.streamxhub.streamx.flink.connector.doris.bean;

import org.apache.commons.codec.binary.Base64;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * doris streamLoad util
 */
public class DorisStreamLoad implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DorisStreamLoad.class);
    private static final List<String> DORIS_SUCCESS_STATUS = Arrays.asList("Success", "Publish Timeout");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String LOAD_URL_PATTERN = "http://%s/api/%s/%s/_stream_load?";
    private final Properties streamLoadProp;
    private String hostPort;
    private String db;
    private String tbl;
    private String user;
    private String password;
    private String loadUrlStr;

    public DorisStreamLoad(String hostPort, String db, String tbl, String user, String password, Properties streamLoadProp) {
        this.hostPort = hostPort;
        this.db = db;
        this.tbl = tbl;
        this.user = user;
        this.password = password;
        this.loadUrlStr = String.format(LOAD_URL_PATTERN, hostPort, db, tbl);
        this.streamLoadProp = streamLoadProp;
    }

    public void load(String value) {
        LoadResponse loadResponse = loadBatch(value);
        LOGGER.info("Streamload Response:{}", loadResponse);
        if (loadResponse.status != 200) {
            throw new RuntimeException("stream load error: " + loadResponse.respContent);
        } else {
            try {
                RespContent respContent = OBJECT_MAPPER.readValue(loadResponse.respContent, RespContent.class);
                if (!DORIS_SUCCESS_STATUS.contains(respContent.getStatus())) {
                    String errMsg = String.format("stream load error: %s, see more in %s", respContent.getMessage(), respContent.getErrorURL());
                    throw new RuntimeException(errMsg);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private LoadResponse loadBatch(String data) {
        final HttpClientBuilder httpClientBuilder = HttpClients.custom()
            .setRedirectStrategy(new DefaultRedirectStrategy() {
                @Override
                protected boolean isRedirectable(String method) {
                    return true;
                }
            });
        String formatDate = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String label = String.format("doris_%s_%s", formatDate,
            UUID.randomUUID().toString().replaceAll("-", ""));

        try (CloseableHttpClient httpclient = httpClientBuilder.build()) {
            HttpPut put = new HttpPut(loadUrlStr);
            put.setHeader(HttpHeaders.EXPECT, "100-continue");
            put.setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthHeader(user, password));
            put.setHeader("label", label);
            for (Map.Entry<Object, Object> entry : streamLoadProp.entrySet()) {
                put.setHeader(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            StringEntity entity = new StringEntity(data, "UTF-8");
            put.setEntity(entity);
            CloseableHttpResponse response = httpclient.execute(put);
            final int statusCode = response.getStatusLine().getStatusCode();
            final String reasonPhrase = response.getStatusLine().getReasonPhrase();
            String loadResult = "";
            if (response.getEntity() != null) {
                loadResult = EntityUtils.toString(response.getEntity());
            }
            return new LoadResponse(statusCode, reasonPhrase, loadResult);
        } catch (Exception e) {
            String err = "failed to stream load data with label: " + label;
            LOGGER.warn(err, e);
            return new LoadResponse(-1, e.getMessage(), err);
        }
    }

    private String getBasicAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
        return "Basic " + new String(encodedAuth);
    }

    public static class LoadResponse {
        public int status;
        public String respMsg;
        public String respContent;

        public LoadResponse(int status, String respMsg, String respContent) {
            this.status = status;
            this.respMsg = respMsg;
            this.respContent = respContent;
        }

        @Override
        public String toString() {
            return "status: " + status +
                ", resp msg: " + respMsg +
                ", resp content: " + respContent;
        }
    }
}
