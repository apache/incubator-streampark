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

package org.apache.streampark.flink.connector.http.function;
import org.apache.streampark.flink.connector.conf.ThresholdConf;
import org.apache.streampark.flink.connector.failover.FailoverChecker;
import org.apache.streampark.flink.connector.failover.SinkBuffer;
import org.apache.streampark.flink.connector.http.conf.HttpConfigOption;
import org.apache.streampark.flink.connector.http.internal.HttpSinkWriter;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import java.util.HashMap; import java.util.Map; import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;
public class HttpSinkFunction extends RichSinkFunction<String> {
    private static final Logger LOG = LoggerFactory.getLogger(HttpSinkFunction.class);
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static volatile boolean initialized = false;
    private final Map<String,String> properties; private final Map<String,String> header; private final String method;
    private transient SinkBuffer sinkBuffer; private transient ThresholdConf thresholdConf;
    private transient HttpSinkWriter httpSinkWriter; private transient FailoverChecker failoverChecker;
    private volatile boolean isClosed = false;
    public HttpSinkFunction(Map<String,String> properties, Map<String,String> header, String method) {
        this.properties = properties; this.header = header; this.method = method;
    }
    @Override public void open(Configuration config) throws Exception {
        if (!initialized) { LOCK.lock(); try { if (!initialized) {
            initialized = true;
            Properties prop = new Properties(); properties.forEach(prop::put);
            thresholdConf = new ThresholdConf(HttpConfigOption.HTTP_SINK_PREFIX, prop);
            String table = thresholdConf.failoverTable;
            if (table == null || table.isEmpty()) throw new IllegalArgumentException("Http async insert failoverTable must not null");
            httpSinkWriter = new HttpSinkWriter(thresholdConf, header);
            failoverChecker = new FailoverChecker(thresholdConf.delayTime);
            sinkBuffer = new SinkBuffer(httpSinkWriter, thresholdConf.delayTime, 1);
            failoverChecker.addSinkBuffer(sinkBuffer);
            LOG.info("HttpSink initialize...");
        }} finally { LOCK.unlock(); }}
    }
    @Override public void invoke(String url) { sinkBuffer.put(method + "///" + url); }
    @Override public void close() throws Exception {
        if (!isClosed) synchronized (LOCK) { if (!isClosed) {
            if (sinkBuffer != null) sinkBuffer.close();
            if (httpSinkWriter != null) httpSinkWriter.close();
            if (failoverChecker != null) failoverChecker.close();
            isClosed = true; super.close();
        }}
    }
}
