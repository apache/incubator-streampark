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
package com.streamxhub.flink.core.source;

import com.streamxhub.common.util.ConfigUtils;
import com.streamxhub.flink.core.StreamingContext;
import com.streamxhub.flink.core.function.ResultSetFunction;
import com.streamxhub.flink.core.function.SQLFunction;
import com.streamxhub.flink.core.sink.Dialect;
import org.apache.flink.streaming.api.datastream.DataStreamSource;

import java.util.Properties;

public class MySQLJavaSource<T> {

    private StreamingContext context;
    private Properties jdbc;
    private SQLFunction sqlFunc;
    private ResultSetFunction<T> resultSetFunc;

    public MySQLJavaSource(StreamingContext context) {
        this(context, (String) null);
    }

    public MySQLJavaSource(StreamingContext context, String alias) {
        this.context = context;
        this.jdbc = ConfigUtils.getJdbcConf(context.parameter().toMap(), Dialect.MYSQL().toString(), alias);
    }

    public MySQLJavaSource(StreamingContext context, Properties jdbc) {
        this.context = context;
        this.jdbc = jdbc;
    }

    public DataStreamSource<T> getDataStream() {
        return getDataStream(this.sqlFunc, this.resultSetFunc);
    }

    public DataStreamSource<T> getDataStream(SQLFunction sqlFun, ResultSetFunction<T> resultSetFunc) {
        MySQLSourceFunction<T> sourceFunction = new MySQLSourceFunction(jdbc, sqlFun, resultSetFunc, null);
        return context.getJavaEnv().addSource(sourceFunction);
    }

    public MySQLJavaSource<T> sql(SQLFunction func) {
        this.sqlFunc = func;
        return this;
    }

    public MySQLJavaSource<T> result(ResultSetFunction<T> func) {
        this.resultSetFunc = func;
        return this;
    }
}