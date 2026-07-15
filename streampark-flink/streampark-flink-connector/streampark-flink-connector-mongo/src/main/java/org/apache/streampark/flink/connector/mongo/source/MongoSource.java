/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.flink.connector.mongo.source;

import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.connector.function.RunningFunction;
import org.apache.streampark.flink.connector.mongo.function.MongoQueryFunction;
import org.apache.streampark.flink.connector.mongo.function.MongoResultFunction;
import org.apache.streampark.flink.connector.mongo.internal.MongoSourceFunction;
import org.apache.streampark.flink.core.scala.StreamingContext;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.bson.Document;

import java.util.List;
import java.util.Properties;

/** MongoDB source connector. */
public class MongoSource {

    private final StreamingContext ctx;
    private final Properties property;

    public MongoSource(StreamingContext ctx, Properties property) {
        this.ctx = ctx;
        this.property = property != null ? property : new Properties();
    }

    public static MongoSource of(StreamingContext ctx, Properties property) {
        return new MongoSource(ctx, property);
    }

    public <R> DataStreamSource<R> getDataStream(
            String collection,
            MongoQueryFunction<R> queryFun,
            MongoResultFunction<R> resultFun,
            RunningFunction running,
            Properties prop) {
        Utils.copyProperties(property, prop);
        MongoSourceFunction<R> mongoFun =
                new MongoSourceFunction<>(collection, prop, queryFun, resultFun, running, null);
        return ctx.getJavaEnv().addSource(mongoFun);
    }
}
