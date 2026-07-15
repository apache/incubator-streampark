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

package org.apache.streampark.flink.connector.clickhouse.internal;
import org.apache.streampark.flink.connector.clickhouse.conf.ClickHouseHttpConfig;
import org.apache.streampark.flink.connector.failover.FailoverWriter;
import org.apache.streampark.flink.connector.failover.SinkRequest;
import org.asynchttpclient.*;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import java.util.List; import java.util.concurrent.*;
public class ClickHouseWriterTask implements Runnable, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ClickHouseWriterTask.class);
    private final int id; private final ClickHouseHttpConfig clickHouseConf;
    private final AsyncHttpClient asyncHttpClient; private final BlockingQueue<SinkRequest> queue;
    private final ExecutorService callbackService; private volatile boolean isWorking = false;
    private final FailoverWriter failoverWriter;
    public ClickHouseWriterTask(int id, ClickHouseHttpConfig conf, AsyncHttpClient client, BlockingQueue<SinkRequest> queue, ExecutorService cb) {
        this.id = id; this.clickHouseConf = conf; this.asyncHttpClient = client; this.queue = queue; this.callbackService = cb;
        this.failoverWriter = new FailoverWriter(clickHouseConf.storageType, clickHouseConf.getFailoverConfig());
    }
    @Override public void run() {
        try {
            isWorking = true; LOG.info("Start writer task, id = {}", id);
            while (isWorking || !queue.isEmpty()) {
                SinkRequest req = queue.poll(300, TimeUnit.MILLISECONDS);
                if (req != null) send(req);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) {
            LOG.error("Error while inserting data", e);
            throw new RuntimeException(e);
        }
        finally { LOG.info("Task id = {} is finished", id); }
    }
    void send(SinkRequest sinkRequest) {
        List<String> stmts = sinkRequest.getSqlStatement();
        if (stmts == null || stmts.isEmpty()) { LOG.warn("Skip empty sql statement"); return; }
        for (String statement : stmts) {
            if (statement == null || statement.isEmpty()) continue;
            String host = clickHouseConf.getRandomHostUrl();
            BoundRequestBuilder builder = asyncHttpClient.preparePost(host)
                .setRequestTimeout(clickHouseConf.timeout)
                .setHeader(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8").setBody(statement);
            if (clickHouseConf.credentials != null) builder.setHeader(HttpHeaderNames.AUTHORIZATION, "Basic " + clickHouseConf.credentials);
            ListenableFuture<Response> future = asyncHttpClient.executeRequest(builder.build());
            future.addListener(() -> {
                try {
                    Response resp = future.get();
                    if (resp == null || resp.getStatusCode() != 200) handleFailedResponse(resp, sinkRequest);
                } catch (Exception e) { handleFailedResponse(null, sinkRequest); }
            }, callbackService);
        }
    }
    void handleFailedResponse(Response response, SinkRequest sinkRequest) {
        if (sinkRequest.getAttemptCounter() > clickHouseConf.maxRetries) {
            LOG.warn("Failed to send data to ClickHouse, flushing to {}", clickHouseConf.storageType);
            failoverWriter.write(sinkRequest);
        } else {
            sinkRequest.incrementCounter();
            try { queue.put(sinkRequest); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
    @Override public void close() { isWorking = false; failoverWriter.close(); }
}
