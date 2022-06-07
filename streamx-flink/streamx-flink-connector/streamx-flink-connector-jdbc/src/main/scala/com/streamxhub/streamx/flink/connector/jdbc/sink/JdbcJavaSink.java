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

package com.streamxhub.streamx.flink.connector.jdbc.sink;

import com.streamxhub.streamx.common.util.AssertUtils;
import com.streamxhub.streamx.common.util.ConfigUtils;
import com.streamxhub.streamx.flink.connector.function.TransformFunction;
import com.streamxhub.streamx.flink.connector.jdbc.internal.JdbcSinkFunction;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;

import java.util.Properties;

/**
 * @author benjobs
 */
public class JdbcJavaSink<T> {

    private final StreamingContext context;
    private Properties jdbc;
    private TransformFunction<T, String> sqlFunc;
    private String alias = "";

    public JdbcJavaSink(StreamingContext context) {
        this.context = context;
    }

    public JdbcJavaSink<T> alias(String alias) {
        this.alias = alias;
        return this;
    }

    public JdbcJavaSink<T> jdbc(Properties jdbc) {
        this.jdbc = jdbc;
        return this;
    }

    public JdbcJavaSink<T> sql(TransformFunction<T, String> func) {
        this.sqlFunc = func;
        return this;
    }

    public DataStreamSink<T> sink(DataStream<T> dataStream) {
        AssertUtils.notNull(sqlFunc);
        this.jdbc = this.jdbc == null ? ConfigUtils.getJdbcConf(context.parameter().toMap(), alias) : this.jdbc;
        JdbcSinkFunction<T> sinkFun = new JdbcSinkFunction<>(this.jdbc, this.sqlFunc);
        return dataStream.addSink(sinkFun);
    }

}
