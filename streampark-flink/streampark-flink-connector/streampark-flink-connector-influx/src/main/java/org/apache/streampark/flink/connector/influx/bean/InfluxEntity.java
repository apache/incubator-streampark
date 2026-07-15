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

package org.apache.streampark.flink.connector.influx.bean;

import org.apache.streampark.flink.connector.influx.function.InfluxFieldFunction;
import org.apache.streampark.flink.connector.influx.function.InfluxTagFunction;

import java.io.Serializable;
import java.util.Map;

/** InfluxDB endpoint configuration. */
public class InfluxEntity<T> implements Serializable {

    private final String database;
    private final String measurement;
    private final String retentionPolicy;
    private final InfluxTagFunction<T> tagFun;
    private final InfluxFieldFunction<T> fieldFun;

    public InfluxEntity(
            String database,
            String measurement,
            String retentionPolicy,
            InfluxTagFunction<T> tagFun,
            InfluxFieldFunction<T> fieldFun) {
        this.database = database;
        this.measurement = measurement;
        this.retentionPolicy = retentionPolicy;
        this.tagFun = tagFun;
        this.fieldFun = fieldFun;
    }

    public String getDatabase() {
        return database;
    }

    public String getMeasurement() {
        return measurement;
    }

    public String getRetentionPolicy() {
        return retentionPolicy;
    }

    public Map<String, String> getTags(T value) {
        return tagFun.transform(value);
    }

    public Map<String, Object> getFields(T value) {
        return fieldFun.transform(value);
    }
}
