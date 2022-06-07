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

package com.streamxhub.streamx.flink.connector.doris.sink;

import com.streamxhub.streamx.common.conf.ConfigConst;
import com.streamxhub.streamx.flink.connector.doris.internal.DorisSinkFunction;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;

import java.util.Properties;

/**
 * @param <T>
 * @author wudi
 */
public class DorisJavaSink<T> {

    private final StreamingContext context;
    private String alias = "";
    private Properties properties = new Properties();

    public DorisJavaSink(StreamingContext context) {
        this.context = context;
    }

    public DorisJavaSink<T> fenodes(String fenodes) {
        properties.put(ConfigConst.DORIS_FENODES(), fenodes);
        return this;
    }

    public DorisJavaSink<T> database(String database) {
        properties.put(ConfigConst.DORIS_DATABASE(), database);
        return this;
    }

    public DorisJavaSink<T> table(String table) {
        properties.put(ConfigConst.DORIS_TABLE(), table);
        return this;
    }

    public DorisJavaSink<T> user(String user) {
        properties.put(ConfigConst.DORIS_USER(), user);
        return this;
    }

    public DorisJavaSink<T> password(String password) {
        properties.put(ConfigConst.DORIS_PASSWORD(), password);
        return this;
    }

    public DorisJavaSink<T> batchSize(Integer batchSize) {
        properties.put(ConfigConst.DORIS_BATCHSIZE(), batchSize);
        return this;
    }

    public DorisJavaSink<T> intervalMs(Long intervalMs) {
        properties.put(ConfigConst.DORIS_INTERVALMS(), intervalMs);
        return this;
    }

    public DorisJavaSink<T> maxRetries(Integer maxRetries) {
        properties.put(ConfigConst.DORIS_MAXRETRIES(), maxRetries);
        return this;
    }

    public DorisJavaSink<T> streamLoadProp(Properties streamLoadProp) {
        this.properties = streamLoadProp;
        return this;
    }

    /**
     * java stream
     * @param source
     * @return
     */
    public DataStreamSink<T> sink(DataStream<T> source) {
        DorisSinkFunction<T> sinkFunction = new DorisSinkFunction<>(context, properties, alias);
        return source.addSink(sinkFunction);
    }

    /**
     * scala stream
     * @param source
     * @return
     */
    public DataStreamSink<T> sink(org.apache.flink.streaming.api.scala.DataStream<T> source) {
        DorisSinkFunction<T> sinkFunction = new DorisSinkFunction<>(context, properties, alias);
        return source.addSink(sinkFunction);
    }

}
