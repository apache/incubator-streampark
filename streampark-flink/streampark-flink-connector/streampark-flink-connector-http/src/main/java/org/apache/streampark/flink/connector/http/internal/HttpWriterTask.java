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

package org.apache.streampark.flink.connector.http.internal;
import org.apache.streampark.common.util.JsonUtils;
import org.apache.streampark.flink.connector.conf.ThresholdConf;
import org.apache.streampark.flink.connector.failover.FailoverWriter;
import org.apache.streampark.flink.connector.failover.SinkRequest;
import org.apache.http.client.methods.*;
import org.asynchttpclient.*;
import io.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import java.util.*; import java.util.concurrent.*;
public class HttpWriterTask implements Runnable, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(HttpWriterTask.class);
    private static final List<String> HTTP_METHODS = Arrays.asList(
        HttpGet.METHOD_NAME, HttpPost.METHOD_NAME, HttpPut.METHOD_NAME, HttpPatch.METHOD_NAME,
        HttpDelete.METHOD_NAME, HttpOptions.METHOD_NAME, HttpTrace.METHOD_NAME);
    private final int id; private final ThresholdConf thresholdConf; private final AsyncHttpClient asyncHttpClient;
    private final Map<String,String> header; private final BlockingQueue<SinkRequest> queue;
    private final ExecutorService callbackService; private volatile boolean isWorking = false;
    private final FailoverWriter failoverWriter;
    public HttpWriterTask(int id, ThresholdConf tc, AsyncHttpClient client, Map<String,String> header, BlockingQueue<SinkRequest> queue, ExecutorService cb) {
        this.id = id; this.thresholdConf = tc; this.asyncHttpClient = client; this.header = header; this.queue = queue; this.callbackService = cb;
        failoverWriter = new FailoverWriter(thresholdConf.storageType, thresholdConf.getFailoverConfig());
    }
    @Override public void run() {
        try { isWorking = true;
            while (isWorking || !queue.isEmpty()) {
                SinkRequest req = queue.poll(100, TimeUnit.MILLISECONDS);
                if (req != null && !req.getRecords().isEmpty()) {
                    String url = req.getRecords().get(0);
                    SinkRequest sinkRequest = new SinkRequest(Collections.singletonList(url), req.getAttemptCounter());
                    ListenableFuture<Response> future = asyncHttpClient.executeRequest(buildRequest(url));
                    future.addListener(() -> { try { Response resp = future.get(); if (resp == null || resp.getStatusCode() != 200) handleFailedResponse(resp, sinkRequest); } catch (Exception e) { handleFailedResponse(null, sinkRequest); }}, callbackService);
                }
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        finally { LOG.info("Task id = {} is finished", id); }
    }
    Request buildRequest(String url) {
        String method = HTTP_METHODS.stream().filter(url::startsWith).findFirst().orElse(HttpGet.METHOD_NAME);
        String[] uriAndParams = url.substring(method.length() + 3).split("\\?", 2);
        String uri = uriAndParams[0];
        BoundRequestBuilder builder;
        switch (method) {
            case HttpDelete.METHOD_NAME: builder = asyncHttpClient.prepareDelete(uri); break;
            case HttpOptions.METHOD_NAME: builder = asyncHttpClient.prepareOptions(uri); break;
            case HttpTrace.METHOD_NAME: builder = asyncHttpClient.prepareTrace(uri); break;
            case HttpPost.METHOD_NAME: builder = asyncHttpClient.preparePost(uri); break;
            case HttpPatch.METHOD_NAME: builder = asyncHttpClient.preparePatch(uri); break;
            case HttpPut.METHOD_NAME: builder = asyncHttpClient.preparePut(uri); break;
            default: builder = asyncHttpClient.prepareGet(uri);
        }
        if (header != null) header.forEach(builder::setHeader);
        if (uriAndParams.length > 1) {
            Map<String,String> paramMap = new HashMap<>();
            for (String x : uriAndParams[1].trim().split("&")) {
                String[] param = x.split("=", 2);
                if (param.length == 2) paramMap.put(param[0], param[1]);
            }
            if (!paramMap.isEmpty()) {
                builder.setHeader(HttpHeaders.Names.CONTENT_TYPE, HttpHeaders.Values.APPLICATION_JSON);
                try {
                    builder.setBody(JsonUtils.write(paramMap).getBytes());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return builder.setRequestTimeout(thresholdConf.timeout).build();
    }
    void handleFailedResponse(Response response, SinkRequest sinkRequest) {
        try {
            if (sinkRequest.getAttemptCounter() >= thresholdConf.maxRetries) {
                List<String> cleaned = new ArrayList<>();
                for (String r : sinkRequest.getRecords()) cleaned.add(r.replaceFirst("^[A-Z]+///", ""));
                failoverWriter.write(new SinkRequest(cleaned, sinkRequest.getAttemptCounter()));
            } else { sinkRequest.incrementCounter(); queue.put(sinkRequest); }
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    @Override public void close() { isWorking = false; failoverWriter.close(); }
}
