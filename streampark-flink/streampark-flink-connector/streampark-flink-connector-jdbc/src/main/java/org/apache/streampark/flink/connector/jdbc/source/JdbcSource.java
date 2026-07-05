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

package org.apache.streampark.flink.connector.jdbc.source;

import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.connector.jdbc.internal.JdbcSourceFunction;
import org.apache.streampark.flink.core.scala.StreamingContext;

import org.apache.flink.streaming.api.datastream.DataStreamSource;

import org.apache.streampark.flink.connector.function.RunningFunction;
import org.apache.streampark.flink.connector.function.SQLQueryFunction;
import org.apache.streampark.flink.connector.function.SQLResultFunction;

import java.util.Properties;

/** JDBC source connector. */
public class JdbcSource {

    private final StreamingContext ctx;
    private final Properties property;

    public JdbcSource(StreamingContext ctx, Properties property) {
        this.ctx = ctx;
        this.property = property != null ? property : new Properties();
    }

    public static JdbcSource of(StreamingContext ctx, Properties property) {
        return new JdbcSource(ctx, property);
    }

    public <R> DataStreamSource<R> getDataStream(
            SQLQueryFunction<R> sqlFun,
            SQLResultFunction<R> fun,
            RunningFunction running,
            Properties jdbc) {
        Utils.copyProperties(property, jdbc);
        JdbcSourceFunction<R> sourceFunction =
                new JdbcSourceFunction<>(jdbc, sqlFun, fun, running, null);
        return ctx.getJavaEnv().addSource(sourceFunction);
    }
}
