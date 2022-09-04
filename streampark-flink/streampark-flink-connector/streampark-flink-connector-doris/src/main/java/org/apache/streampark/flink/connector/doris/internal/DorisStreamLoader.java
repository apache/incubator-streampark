/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.flink.connector.doris.internal;

import org.apache.streampark.connector.doris.conf.DorisConfig;
import org.apache.streampark.flink.connector.doris.bean.DorisSinkBufferEntry;
import org.apache.streampark.flink.connector.doris.bean.LoadStatusFailedException;
import org.apache.streampark.flink.connector.doris.bean.RespContent;
import org.apache.streampark.flink.connector.doris.util.DorisDelimiterParser;

import org.apache.commons.codec.binary.Base64;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class DorisStreamLoader implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String LOAD_URL_PATTERN = "%s/api/%s/%s/_stream_load?";
    private static final String GET_LOAD_STATUS_URL = "%s/api/%s/get_load_state?label=%s";
    private static final Logger LOG = LoggerFactory.getLogger(DorisStreamLoader.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final DorisConfig dorisConfig;
    private static final String RESULT_FAILED = "Fail";
    private static final String RESULT_LABEL_EXISTED = "Label Already Exists";
    private static final String LAEBL_STATE_VISIBLE = "VISIBLE";
    private static final String LAEBL_STATE_COMMITTED = "COMMITTED";
    private static final String RESULT_LABEL_PREPARE = "PREPARE";
    private static final String RESULT_LABEL_ABORTED = "ABORTED";
    private static final String RESULT_LABEL_UNKNOWN = "UNKNOWN";

    public DorisStreamLoader(DorisConfig dorisConfig) {
        this.dorisConfig = dorisConfig;
    }

    public RespContent doStreamLoad(DorisSinkBufferEntry bufferEntity) throws IOException {
        String host = getWorkerHost();
        if (null == host) {
            throw new IOException("None of the hosts in `load_url` could be connected.");
        }

        LOG.info(String.format("Start to join batch data: label[%s].", bufferEntity.getLabel()));
        String loadUrl = String.format(LOAD_URL_PATTERN, host, bufferEntity.getDatabase(), bufferEntity.getTable());
        LoadResponse loadResponse = doHttpPut(loadUrl, bufferEntity.getLabel(), joinRows(bufferEntity.getBuffer(), (int) bufferEntity.getBatchSize()));
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Stream Load response: \n%s\n", loadResponse.respContent));
        }
        if (loadResponse.status != 200) {
            throw new RuntimeException("stream load error: " + loadResponse.respContent);
        } else {
            try {
                RespContent respContent = OBJECT_MAPPER.readValue(loadResponse.respContent, RespContent.class);
                if (RESULT_FAILED.equals(respContent.getStatus())) {
                    String errMsg = String.format("stream load error: %s, see more in %s", respContent.getMessage(), respContent.getErrorURL());
                    throw new RuntimeException(errMsg);
                } else if (RESULT_LABEL_EXISTED.equals(respContent.getStatus())) {
                    LOG.error(String.format("Stream Load response: \n%s\n", loadResponse.respContent));
                    checkLableState(host, bufferEntity.getDatabase(), bufferEntity.getLabel());
                }
                return respContent;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void checkLableState(String host, String database, String label) throws IOException {
        int tries = 0;
        while (tries < 10) {
            try {
                TimeUnit.SECONDS.sleep(Math.min(++tries, 5));
            } catch (InterruptedException e) {
                return;
            }
            try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet(String.format(GET_LOAD_STATUS_URL, host, database, label));
                httpGet.setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthHeader(dorisConfig.user(), dorisConfig.password()));
                httpGet.setHeader("Connection", "close");
                try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
                    final int statusCode = response.getStatusLine().getStatusCode();
                    String loadResult = "{}";
                    if (response.getEntity() != null) {
                        loadResult = EntityUtils.toString(response.getEntity());
                    }
                    if (statusCode != 200) {
                        throw new LoadStatusFailedException(String.format("Failed to flush data to doris, Error " +
                            "could not get the final state of label[%s].\n", label), null);
                    }
                    Map<String, Object> result = OBJECT_MAPPER.readValue(loadResult, HashMap.class);
                    String labelState = (String) result.get("state");
                    if (null == labelState) {
                        throw new LoadStatusFailedException(String.format("Failed to flush data to doris, Error " +
                            "could not get the final state of label[%s]. response[%s]\n", label, loadResult), null);
                    }
                    LOG.info(String.format("Checking label[%s] state[%s]\n", label, labelState));
                    switch (labelState) {
                        case LAEBL_STATE_VISIBLE:
                            return;
                        case LAEBL_STATE_COMMITTED:
                            return;
                        case RESULT_LABEL_PREPARE:
                            continue;
                        case RESULT_LABEL_ABORTED:
                            throw new LoadStatusFailedException(String.format("Failed to flush data to doris, Error " +
                                "label[%s] state[%s]\n", label, labelState), null, true);
                        case RESULT_LABEL_UNKNOWN:
                        default:
                            throw new LoadStatusFailedException(String.format("Failed to flush data to doris, Error " +
                                "label[%s] state[%s]\n", label, labelState), null);
                    }
                }
            }

        }
    }

    private String getBasicAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
        return "Basic " + new String(encodedAuth);
    }

    private LoadResponse doHttpPut(String loadUrl, String label, byte[] data) throws IOException {
        LOG.info(String.format("Executing stream load to: '%s', size: '%s', thread: %d", loadUrl, data.length, Thread.currentThread().getId()));
        final HttpClientBuilder httpClientBuilder = HttpClients.custom()
            .setRedirectStrategy(new DefaultRedirectStrategy() {
                @Override
                protected boolean isRedirectable(String method) {
                    return true;
                }
            });
        try (CloseableHttpClient httpclient = httpClientBuilder.build()) {
            final HttpPut put = new HttpPut(loadUrl);
            final Properties properties = dorisConfig.loadProperties();
            properties.forEach((k, v) -> put.setHeader(k.toString(), v.toString()));
            if (properties.containsKey("columns")) {
                put.setHeader("timeout", dorisConfig.timeout() + "");
            }
            put.setHeader(HttpHeaders.EXPECT, "100-continue");
            put.setHeader(HttpHeaders.AUTHORIZATION, getBasicAuthHeader(dorisConfig.user(), dorisConfig.password()));
            put.setHeader("label", label);
            put.setEntity(new ByteArrayEntity(data));
            try (CloseableHttpResponse response = httpclient.execute(put)) {
                final int statusCode = response.getStatusLine().getStatusCode();
                final String reasonPhrase = response.getStatusLine().getReasonPhrase();
                String loadResult = "";
                if (response.getEntity() != null) {
                    loadResult = EntityUtils.toString(response.getEntity());
                }
                return new LoadResponse(statusCode, reasonPhrase, loadResult);
            }
        }
    }

    private byte[] joinRows(List<byte[]> rows, int totalBytes) {
        if (DorisConfig.CSV().equalsIgnoreCase(dorisConfig.loadFormat())) {
            byte[] lineDelimiter = DorisDelimiterParser.parse(dorisConfig.rowDelimiter()).getBytes(StandardCharsets.UTF_8);
            ByteBuffer bos = ByteBuffer.allocate(totalBytes + rows.size() * lineDelimiter.length);
            for (byte[] row : rows) {
                bos.put(row);
                bos.put(lineDelimiter);
            }
            return bos.array();
        }

        if (DorisConfig.JSON().equalsIgnoreCase(dorisConfig.loadFormat())) {
            ByteBuffer bos = ByteBuffer.allocate(totalBytes + (rows.isEmpty() ? 2 : rows.size() + 1));
            bos.put("[".getBytes(StandardCharsets.UTF_8));
            byte[] jsonDelimiter = ",".getBytes(StandardCharsets.UTF_8);
            boolean isFirstElement = true;
            for (byte[] row : rows) {
                if (!isFirstElement) {
                    bos.put(jsonDelimiter);
                }
                bos.put(row);
                isFirstElement = false;
            }
            bos.put("]".getBytes(StandardCharsets.UTF_8));
            return bos.array();
        }
        throw new RuntimeException("Failed to join rows data, unsupported `format` from stream load properties:");
    }

    private String getWorkerHost() {
        for (int pos = 0; pos < dorisConfig.getLoadUrlSize(); pos++) {
            String host = dorisConfig.getHostUrl();
            if (tryHttpConnection(host)) {
                return host;
            }
        }
        return null;
    }

    private boolean tryHttpConnection(String host) {
        try {
            URL url = new URL(host);
            HttpURLConnection co = (HttpURLConnection) url.openConnection();
            co.setConnectTimeout(dorisConfig.timeout());
            co.connect();
            co.disconnect();
            return true;
        } catch (Exception e1) {
            LOG.warn("Failed to connect to address:{}", host, e1);
            return false;
        }
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
