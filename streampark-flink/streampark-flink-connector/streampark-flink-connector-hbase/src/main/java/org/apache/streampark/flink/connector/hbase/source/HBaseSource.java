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

package org.apache.streampark.flink.connector.hbase.source;

import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.connector.function.RunningFunction;
import org.apache.streampark.flink.connector.hbase.function.HBaseQueryFunction;
import org.apache.streampark.flink.connector.hbase.function.HBaseResultFunction;
import org.apache.streampark.flink.connector.hbase.internal.HBaseSourceFunction;
import org.apache.streampark.flink.core.scala.StreamingContext;

import org.apache.flink.streaming.api.datastream.DataStreamSource;

import java.util.Properties;

/** HBase source connector. */
public class HBaseSource {

    private final StreamingContext ctx;
    private final Properties property;

    public HBaseSource(StreamingContext ctx, Properties property) {
        this.ctx = ctx;
        this.property = property != null ? property : new Properties();
    }

    public static HBaseSource of(StreamingContext ctx, Properties property) {
        return new HBaseSource(ctx, property);
    }

    public <R> DataStreamSource<R> getDataStream(
            HBaseQueryFunction<R> queryFunction,
            HBaseResultFunction<R> resultFunction,
            RunningFunction runningFunc,
            Properties prop) {
        Utils.copyProperties(property, prop);
        HBaseSourceFunction<R> sourceFunction =
                new HBaseSourceFunction<>(prop, queryFunction, resultFunction, runningFunc, null);
        return ctx.getJavaEnv().addSource(sourceFunction);
    }
}
