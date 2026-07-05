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
import org.apache.streampark.flink.connector.clickhouse.util.ClickhouseConvertUtils;
import org.apache.streampark.flink.connector.failover.FailoverChecker;
import org.apache.streampark.flink.connector.failover.SinkBuffer;
import org.apache.streampark.flink.connector.function.TransformFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import java.util.Properties;

public class AsyncClickHouseSinkFunction<T> extends RichSinkFunction<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AsyncClickHouseSinkFunction.class);
    private static final Object LOCK = new Object();
    private static volatile boolean initialized = false;
    private final Properties properties;
    private final TransformFunction<T, String> sqlFunc;
    private transient ClickHouseHttpConfig clickHouseConf;
    private transient SinkBuffer sinkBuffer;
    private transient ClickHouseSinkWriter clickHouseWriter;
    private transient FailoverChecker failoverChecker;
    private volatile boolean isClosed = false;

    public AsyncClickHouseSinkFunction(Properties properties, TransformFunction<T, String> sqlFunc) {
        this.properties = properties; this.sqlFunc = sqlFunc;
    }

    @Override public void open(Configuration config) throws Exception {
        if (!initialized) {
            synchronized (LOCK) {
                if (!initialized) {
                    initialized = true;
                    clickHouseConf = new ClickHouseHttpConfig(properties);
                    clickHouseWriter = new ClickHouseSinkWriter(clickHouseConf);
                    failoverChecker = new FailoverChecker(clickHouseConf.delayTime);
                    sinkBuffer = new SinkBuffer(clickHouseWriter, clickHouseConf.delayTime, clickHouseConf.bufferSize);
                    failoverChecker.addSinkBuffer(sinkBuffer);
                    LOG.info("AsyncClickHouseSink initialize...");
                }
            }
        }
    }

    @Override public void invoke(T value) throws Exception {
        String sql = sqlFunc != null ? sqlFunc.transform(value) : ClickhouseConvertUtils.convert(value);
        try { sinkBuffer.put(sql); }
        catch (Exception e) { LOG.error("Error while sending data to Clickhouse, record = {}", sql, e); throw e; }
    }

    @Override public void close() throws Exception {
        if (!isClosed) synchronized (LOCK) {
            if (!isClosed) {
                if (sinkBuffer != null) sinkBuffer.close();
                if (clickHouseWriter != null) clickHouseWriter.close();
                if (failoverChecker != null) failoverChecker.close();
                isClosed = true; super.close();
            }
        }
    }
}
