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

import com.streamxhub.streamx.common.util.Utils;
import com.streamxhub.streamx.flink.core.java.function.HBaseQueryFunction;
import com.streamxhub.streamx.flink.core.java.function.HBaseResultFunction;
import com.streamxhub.streamx.flink.core.java.function.RunningFunction;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import com.streamxhub.streamx.flink.core.scala.source.HBaseSourceFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;

import java.util.Properties;


/**
 * @author benjobs
 */
public class HBaseSource<T> {
    private final StreamingContext context;
    private final Properties property;

    public HBaseSource(StreamingContext context, Properties property) {
        this.context = context;
        this.property = property;
    }

    public DataStreamSource<T> getDataStream(HBaseQueryFunction<T> queryFunction,
                                             HBaseResultFunction<T> resultFunction,
                                             RunningFunction runningFunc) {

        Utils.require(queryFunction != null, "queryFunction must not be null");
        Utils.require(resultFunction != null, "resultFunction must not be null");
        HBaseSourceFunction<T> sourceFunction = new HBaseSourceFunction<>(
                property,
                queryFunction,
                resultFunction,
                runningFunc,
                null);
        return context.getJavaEnv().addSource(sourceFunction);
    }
}
