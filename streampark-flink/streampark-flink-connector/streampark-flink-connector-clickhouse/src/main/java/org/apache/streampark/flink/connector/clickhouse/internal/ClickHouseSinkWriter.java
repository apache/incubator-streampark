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
import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.flink.connector.clickhouse.conf.ClickHouseHttpConfig;
import org.apache.streampark.flink.connector.failover.SinkRequest;
import org.apache.streampark.flink.connector.failover.SinkWriter;
import org.asynchttpclient.AsyncHttpClient; import org.asynchttpclient.DefaultAsyncHttpClientConfig; import org.asynchttpclient.Dsl;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import java.util.ArrayList; import java.util.List;
import java.util.concurrent.*;
public class ClickHouseSinkWriter implements SinkWriter {
    private static final Logger LOG = LoggerFactory.getLogger(ClickHouseSinkWriter.class);
    private final ClickHouseHttpConfig clickHouseConfig;
    private final ExecutorService callbackService;
    private final List<ClickHouseWriterTask> tasks = new ArrayList<>();
    private final BlockingQueue<SinkRequest> recordQueue;
    private final AsyncHttpClient asyncHttpClient;
    private final ExecutorService service;
    public ClickHouseSinkWriter(ClickHouseHttpConfig clickHouseConfig) {
        this.clickHouseConfig = clickHouseConfig;
        ThreadFactory cbFactory = ThreadUtils.threadFactory("ClickHouse-writer-callback-executor");
        ThreadFactory tf = ThreadUtils.threadFactory("ClickHouse-writer");
        callbackService = new ThreadPoolExecutor(Math.max(Runtime.getRuntime().availableProcessors()/4,2), Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), cbFactory);
        recordQueue = new LinkedBlockingQueue<>(clickHouseConfig.queueCapacity);
        asyncHttpClient = Dsl.asyncHttpClient(new DefaultAsyncHttpClientConfig.Builder()
            .setRequestTimeout(clickHouseConfig.sinkOption.requestTimeout.get())
            .setConnectTimeout(clickHouseConfig.sinkOption.connectTimeout.get())
            .setMaxRequestRetry(clickHouseConfig.sinkOption.maxRequestRetry.get())
            .setMaxConnections(clickHouseConfig.sinkOption.maxConnections.get()).build());
        service = Executors.newFixedThreadPool(clickHouseConfig.numWriters, tf);
        for (int i = 0; i < clickHouseConfig.numWriters; i++) {
            ClickHouseWriterTask task = new ClickHouseWriterTask(i, clickHouseConfig, asyncHttpClient, recordQueue, callbackService);
            tasks.add(task); service.submit(task);
        }
    }
    @Override public void write(SinkRequest request) {
        try { recordQueue.put(request); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
    }
    @Override
    public void close() {
        LOG.info("Closing ClickHouse-writer...");
        for (ClickHouseWriterTask t : tasks) {
            t.close();
        }
        try {
            ThreadUtils.shutdownExecutorService(service);
            ThreadUtils.shutdownExecutorService(callbackService);
            asyncHttpClient.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        LOG.info("{} is closed", ClickHouseSinkWriter.class.getSimpleName());
    }
}
