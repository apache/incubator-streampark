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

package org.apache.streampark.flink.connector.influx.function;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.flink.connector.influx.bean.InfluxEntity;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/** InfluxDB sink function. */
public class InfluxFunction<T> extends RichSinkFunction<T> {

    private final Properties config;
    private final InfluxEntity<T> endpoint;
    private transient InfluxDB influxDB;

    public InfluxFunction(Properties config, InfluxEntity<T> endpoint) {
        this.config = config;
        this.endpoint = endpoint;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        String url = config.getProperty(ConfigKeys.KEY_JDBC_URL());
        if (url == null) {
            throw new IllegalArgumentException("Influx url must not be null");
        }
        String username = config.getProperty(ConfigKeys.KEY_JDBC_USER());
        String password = config.getProperty(ConfigKeys.KEY_JDBC_PASSWORD());
        if (username == null) {
            influxDB = InfluxDBFactory.connect(url);
        } else {
            influxDB = InfluxDBFactory.connect(url, username, password);
        }
        influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void invoke(T value, SinkFunction.Context context) throws Exception {
        Map<String, String> tag = endpoint.getTags(value);
        Map<String, Object> fields = endpoint.getFields(value);
        Point point =
                Point.measurement(endpoint.getMeasurement())
                        .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                        .tag(tag)
                        .fields(fields)
                        .build();
        influxDB.write(endpoint.getDatabase(), endpoint.getRetentionPolicy(), point);
    }

    @Override
    public void close() throws Exception {
        if (influxDB != null) {
            influxDB.flush();
            influxDB.close();
        }
    }
}
