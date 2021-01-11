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

import com.streamxhub.flink.core.java.function.MongoQueryFunction;
import com.streamxhub.flink.core.java.function.MongoResultFunction;
import com.streamxhub.flink.core.scala.StreamingContext;
import com.streamxhub.flink.core.scala.source.MongoSourceFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;

import java.util.Properties;

public class MongoSource<T> {
    private StreamingContext ctx;
    private Properties prop;

    public MongoSource(StreamingContext ctx, Properties prop) {
        this.ctx = ctx;
        this.prop = prop;
    }

    public DataStreamSource<T> getDataStream(String collectionName, MongoQueryFunction<T> queryFunction, MongoResultFunction<T> resultFunction) {
        MongoSourceFunction<T> sourceFunction = new MongoSourceFunction(collectionName,prop, queryFunction,resultFunction, null);
        return ctx.getJavaEnv().addSource(sourceFunction);
    }

}
