/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.flink.core.java.sink;

import com.streamxhub.streamx.common.util.AssertUtils;
import com.streamxhub.streamx.common.util.ConfigUtils;
import com.streamxhub.streamx.flink.core.java.function.SQLFromFunction;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import com.streamxhub.streamx.flink.core.scala.sink.JdbcSinkFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;

import java.util.Properties;

/**
 * @author benjobs
 */
public class JdbcSink<T> {

    private final StreamingContext context;
    private Properties jdbc;
    private SQLFromFunction<T> sqlFunc;
    private String alias = "";

    public JdbcSink(StreamingContext context) {
        this.context = context;
    }

    public JdbcSink<T> alias(String alias) {
        this.alias = alias;
        return this;
    }

    public JdbcSink<T> jdbc(Properties jdbc) {
        this.jdbc = jdbc;
        return this;
    }

    public JdbcSink<T> sql(SQLFromFunction<T> func) {
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
