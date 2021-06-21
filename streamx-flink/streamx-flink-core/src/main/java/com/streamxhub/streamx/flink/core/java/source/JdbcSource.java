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
package com.streamxhub.streamx.flink.core.java.source;

import com.streamxhub.streamx.common.util.ConfigUtils;
import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.flink.core.java.function.RunningFunction;
import com.streamxhub.streamx.flink.core.java.function.SQLQueryFunction;
import com.streamxhub.streamx.flink.core.java.function.SQLResultFunction;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import com.streamxhub.streamx.flink.core.scala.source.JdbcSourceFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;

import java.util.Properties;

/**
 * @author benjobs
 */
public class JdbcSource<T> {

    private final StreamingContext context;
    private Properties jdbc;
    private String alias = null;

    public JdbcSource(StreamingContext context) {
        this.context = context;
    }

    /**
     * 允许手动指定一个jdbc的连接信息
     *
     * @param jdbc: jdbc connection info
     * @return JdbcSource: JdbcSource
     */
    public JdbcSource<T> jdbc(Properties jdbc) {
        this.jdbc = jdbc;
        return this;
    }

    public JdbcSource<T> alias(String alias) {
        this.alias = alias;
        return this;
    }

    public DataStreamSource<T> getDataStream(SQLQueryFunction<T> queryFunction,
                                             SQLResultFunction<T> resultFunction,
                                             RunningFunction runningFunc) {

        Utils.require(queryFunction != null, "queryFunction must not be null");
        Utils.require(resultFunction != null, "resultFunction must not be null");
        this.jdbc = this.jdbc == null ? ConfigUtils.getJdbcConf(context.parameter().toMap(), alias) : this.jdbc;
        JdbcSourceFunction<T> sourceFunction = new JdbcSourceFunction<>(jdbc, queryFunction, resultFunction, runningFunc, null);
        return context.getJavaEnv().addSource(sourceFunction);

    }

}
