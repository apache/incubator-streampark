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

package org.apache.streampark.flink.connector.jdbc.sink;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.enums.Semantic;
import org.apache.streampark.common.util.ConfigUtils;
import org.apache.streampark.flink.connector.function.TransformFunction;
import org.apache.streampark.flink.connector.jdbc.internal.Jdbc2PCSinkFunction;
import org.apache.streampark.flink.connector.jdbc.internal.JdbcSinkFunction;
import org.apache.streampark.flink.connector.sink.Sink;
import org.apache.streampark.flink.core.scala.StreamingContext;

import org.apache.flink.api.common.io.RichOutputFormat;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.function.Function;

/** JDBC sink connector. */
public class JdbcSink implements Sink {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcSink.class);

    private final StreamingContext ctx;
    private final int parallelism;
    private final String alias;
    private final String name;
    private final String uid;

    public JdbcSink(StreamingContext ctx, int parallelism, String alias, String name, String uid) {
        this.ctx = ctx;
        this.parallelism = parallelism;
        this.alias = alias != null ? alias : "";
        this.name = name;
        this.uid = uid;
    }

    public static JdbcSink of(StreamingContext ctx, int parallelism, String alias, String name, String uid) {
        return new JdbcSink(ctx, parallelism, alias, name, uid);
    }

    public <T> DataStreamSink<T> sink(DataStream<T> stream, Function<T, String> toSQLFn) {
        Properties prop = ConfigUtils.getJdbcConf(ctx.parameter.toMap(), alias);
        Semantic semantic = Semantic.of(prop.getProperty(ConfigKeys.KEY_SEMANTIC(), Semantic.NONE.name()));
        DataStreamSink<T> sink;
        if (Semantic.EXACTLY_ONCE.equals(semantic)) {
            TransformFunction<T, String> func = toSQLFn::apply;
            Jdbc2PCSinkFunction<T> sinkFun = new Jdbc2PCSinkFunction<>(prop, func);
            if (parallelism > 1) {
                LOG.warn("'parallelism':{}, Jdbc Semantic EXACTLY_ONCE,parallelism bust be 1.", parallelism);
            }
            sink = stream.addSink(sinkFun);
        } else {
            TransformFunction<T, String> func = toSQLFn::apply;
            JdbcSinkFunction<T> sinkFun = new JdbcSinkFunction<>(prop, func);
            sink = stream.addSink(sinkFun);
        }
        return afterSink(sink, parallelism, name, uid);
    }

    /** Output format wrapping {@link JdbcSinkFunction}. */
    public static class JdbcOutputFormat<T> extends RichOutputFormat<T> {

        private final JdbcSinkFunction<T> sinkFunction;
        private Configuration configuration;

        public JdbcOutputFormat(Properties prop, Function<T, String> toSQLFn) {
            this.sinkFunction = new JdbcSinkFunction<>(prop, toSQLFn::apply);
        }

        @Override
        public void configure(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public void open(int taskNumber, int numTasks) {
            try {
                sinkFunction.open(this.configuration);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void writeRecord(T record) {
            try {
                sinkFunction.invoke(record, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
            try {
                sinkFunction.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** Output format wrapping {@link Jdbc2PCSinkFunction}. */
    public static class Jdbc2PCOutputFormat<T> extends RichOutputFormat<T> {

        private final Jdbc2PCSinkFunction<T> sinkFunction;
        private Configuration configuration;

        public Jdbc2PCOutputFormat(Properties prop, Function<T, String> toSQLFn) {
            this.sinkFunction = new Jdbc2PCSinkFunction<>(prop, toSQLFn::apply);
        }

        @Override
        public void configure(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public void open(int taskNumber, int numTasks) {
            try {
                sinkFunction.open(this.configuration);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void writeRecord(T record) {
            try {
                sinkFunction.invoke(record, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
            try {
                sinkFunction.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
