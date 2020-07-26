/**
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
package com.streamxhub.flink.core.sink;

import com.streamxhub.common.util.ConfigUtils;
import com.streamxhub.flink.core.StreamingContext;
import com.streamxhub.flink.core.function.ToSQLFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;


import java.util.Properties;

/**
 * @author benjobs
 */
public class JdbcJavaSink<T> {

    private StreamingContext context;
    private Properties jdbc;
    private ToSQLFunction<T> toSQLFunc;
    private String dialect = Dialect.MYSQL().toString().toLowerCase();
    private String alias = "";

    public JdbcJavaSink(StreamingContext context) {
        this.context = context;
        this.jdbc = ConfigUtils.getJdbcConf(context.parameter().toMap(), dialect, alias);
    }

    public JdbcJavaSink<T> dialect(String dialect) {
        this.dialect = dialect;
        return this;
    }

    public JdbcJavaSink<T> alias(String alias) {
        this.alias = alias;
        this.jdbc = ConfigUtils.getJdbcConf(context.parameter().toMap(), dialect, alias);
        return this;
    }

    public JdbcJavaSink<T> jdbc(Properties jdbc) {
        this.jdbc = jdbc;
        return this;
    }

    public JdbcJavaSink<T> sql(ToSQLFunction<T> func) {
        this.toSQLFunc = func;
        return this;
    }

    public DataStreamSink<T> sink(DataStream<T> dataStream) {
        JdbcSinkFunction<T> sinkFun = new JdbcSinkFunction<>(this.jdbc, this.toSQLFunc);
        return dataStream.addSink(sinkFun);
    }

    public DataStreamSink<T> towPcSink(DataStream<T> dataStream) {
        Jdbc2PCSinkFunction<T> sinkFun = new Jdbc2PCSinkFunction<>(this.jdbc, this.toSQLFunc);
        return dataStream.addSink(sinkFun);
    }

}
