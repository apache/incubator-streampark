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

package org.apache.streampark.flink.connector.clickhouse.sink;

import org.apache.streampark.flink.connector.clickhouse.internal.ClickHouseSinkFunction;
import org.apache.streampark.flink.connector.function.TransformFunction;

import org.apache.flink.api.common.io.RichOutputFormat;
import org.apache.flink.configuration.Configuration;

import java.util.Properties;

/** ClickHouse JDBC output format. */
public class ClickHouseOutputFormat<T> extends RichOutputFormat<T> {

    private final ClickHouseSinkFunction<T> sinkFunction;
    private Configuration configuration;

    public ClickHouseOutputFormat(Properties properties, TransformFunction<T, String> sqlFunc) {
        sinkFunction = new ClickHouseSinkFunction<>(properties, sqlFunc);
    }

    @Override
    public void configure(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void open(int taskNumber, int numTasks) {
        try {
            sinkFunction.open(configuration);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeRecord(T record) {
        try {
            sinkFunction.invoke(record, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            sinkFunction.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
