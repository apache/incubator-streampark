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
import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;

import java.util.Properties;

/**
 * @param <T>
 * @author wudi
 */
public class DorisSink<T> {

    private final StreamingContext context;
    private String alias = "";
    private Properties streamLoadProp = new Properties();

    public DorisSink(StreamingContext context) {
        this.context = context;
    }

    public DorisSink<T> fenodes(String fenodes) {
        streamLoadProp.put(ConfigConst.DORIS_FENODES(), fenodes);
        return this;
    }

    public DorisSink<T> database(String database) {
        streamLoadProp.put(ConfigConst.DORIS_DATABASE(), database);
        return this;
    }

    public DorisSink<T> table(String table) {
        streamLoadProp.put(ConfigConst.DORIS_TABLE(), table);
        return this;
    }

    public DorisSink<T> user(String user) {
        streamLoadProp.put(ConfigConst.DORIS_USER(), user);
        return this;
    }

    public DorisSink<T> password(String password) {
        streamLoadProp.put(ConfigConst.DORIS_PASSWORD(), password);
        return this;
    }

    public DorisSink<T> batchSize(Integer batchSize) {
        streamLoadProp.put(ConfigConst.DORIS_BATCHSIZE(), batchSize);
        return this;
    }

    public DorisSink<T> intervalMs(Long intervalMs) {
        streamLoadProp.put(ConfigConst.DORIS_INTERVALMS(), intervalMs);
        return this;
    }

    public DorisSink<T> maxRetries(Integer maxRetries) {
        streamLoadProp.put(ConfigConst.DORIS_MAXRETRIES(), maxRetries);
        return this;
    }

    public DorisSink<T> streamLoadProp(Properties streamLoadProp) {
        this.streamLoadProp = streamLoadProp;
        return this;
    }

    public DataStreamSink<T> sink(DataStream<T> source) {
        DorisSinkFunction<T> sinkFunction = new DorisSinkFunction<>(context, streamLoadProp, alias);
        return source.addSink(sinkFunction);
    }
}
