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

package com.streamxhub.streamx.flink.core.java.sink.doris;

import com.streamxhub.streamx.common.conf.ConfigConst;
import com.streamxhub.streamx.common.util.ConfigUtils;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;

import java.util.Properties;

/**
 * Doris sink
 **/
public class DorisSink<T> {

    private final StreamingContext context;

    private String fenodes;
    private String database;
    private String table;
    private String user;
    private String password;
    private Integer batchSize = 100;
    private Long intervalMs = 3000L;
    private Integer maxRetries = 1;
    private Properties streamLoadProp = new Properties();

    public DorisSink(StreamingContext context) {
        this.context = context;
    }

    public DorisSink<T> fenodes(String fenodes) {
        this.fenodes = fenodes;
        return this;
    }

    public DorisSink<T> database(String database) {
        this.database = database;
        return this;
    }

    public DorisSink<T> table(String table) {
        this.table = table;
        return this;
    }

    public DorisSink<T> user(String user) {
        this.user = user;
        return this;
    }

    public DorisSink<T> password(String password) {
        this.password = password;
        return this;
    }

    public DorisSink<T> batchSize(Integer batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public DorisSink<T> intervalMs(Long intervalMs) {
        this.intervalMs = intervalMs;
        return this;
    }

    public DorisSink<T> maxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public DorisSink<T> streamLoadProp(Properties streamLoadProp) {
        this.streamLoadProp = streamLoadProp;
        return this;
    }

    public DataStreamSink<T> sink(DataStream<T> source) {
        Properties config = ConfigUtils.getConf(context.parameter().toMap(), ConfigConst.DORIS_SINK_PREFIX(), "", "");
        return source.addSink(new DorisSinkFunction<>(config));
    }
}
