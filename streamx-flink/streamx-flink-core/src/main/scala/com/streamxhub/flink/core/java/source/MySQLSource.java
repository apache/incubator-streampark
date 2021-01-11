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
package com.streamxhub.flink.core.java.source;

import com.streamxhub.common.util.ConfigUtils;
import com.streamxhub.flink.core.java.function.SQLResultFunction;
import com.streamxhub.flink.core.scala.StreamingContext;
import com.streamxhub.flink.core.java.function.SQLQueryFunction;
import com.streamxhub.flink.core.scala.sink.Dialect;
import com.streamxhub.flink.core.scala.source.MySQLSourceFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;

import java.util.Properties;

/**
 * @author benjobs
 */
public class MySQLSource<T> {

    private final StreamingContext context;
    private final Properties jdbc;

    public MySQLSource(StreamingContext context) {
        this(context, (String) null);
    }

    public MySQLSource(StreamingContext context, String alias) {
        this.context = context;
        this.jdbc = ConfigUtils.getJdbcConf(context.parameter().toMap(), Dialect.MYSQL().toString(), alias);
    }

    public MySQLSource(StreamingContext context, Properties jdbc) {
        this.context = context;
        this.jdbc = jdbc;
    }

    public DataStreamSource<T> getDataStream(SQLQueryFunction<T> queryFunc, SQLResultFunction<T> resultFunc) {
        assert queryFunc != null;
        assert resultFunc != null;
        MySQLSourceFunction<T> sourceFunction = new MySQLSourceFunction(jdbc, queryFunc, resultFunc, null);
        return context.getJavaEnv().addSource(sourceFunction);
    }

}
