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

package org.apache.streampark.flink.connector.jdbc.internal;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.JdbcUtils;
import org.apache.streampark.flink.connector.function.TransformFunction;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/** JDBC sink function with optional batch insert support. */
public class JdbcSinkFunction<T> extends RichSinkFunction<T> {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcSinkFunction.class);

    private final Properties jdbc;
    private final TransformFunction<T, String> toSQLFunc;
    private final int batchSize;

    private Connection connection;
    private Statement statement;
    private final AtomicLong offset = new AtomicLong(0L);
    private long timestamp = 0L;

    public JdbcSinkFunction(Properties jdbc, TransformFunction<T, String> toSQLFn) {
        if (toSQLFn == null) {
            throw new IllegalArgumentException("[StreamPark] ToSQLFunction can not be null");
        }
        this.jdbc = jdbc;
        this.toSQLFunc = toSQLFn;
        Object batch = jdbc.remove(ConfigKeys.KEY_JDBC_INSERT_BATCH);
        if (batch == null) {
            this.batchSize = ConfigKeys.DEFAULT_JDBC_INSERT_BATCH;
        } else {
            this.batchSize = Integer.parseInt(batch.toString());
        }
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        if (jdbc == null) {
            throw new IllegalArgumentException("[StreamPark] JdbcSink jdbc can not be null");
        }
        LOG.info("JdbcSink Open....");
        connection = JdbcUtils.getConnection(jdbc);
        connection.setAutoCommit(false);
        if (batchSize > 1) {
            statement = connection.createStatement();
        }
    }

    @Override
    public void invoke(T value, SinkFunction.Context context) throws Exception {
        if (connection == null) {
            throw new IllegalStateException("Connection is null");
        }
        String sql = toSQLFunc.transform(value);
        if (batchSize == 1) {
            try {
                statement = connection.prepareStatement(sql);
                ((PreparedStatement) statement).executeUpdate();
                connection.commit();
            } catch (Exception e) {
                LOG.error("JdbcSink invoke error:{}", sql);
                throw e;
            }
        } else {
            try {
                statement.addBatch(sql);
                long count = offset.incrementAndGet();
                long current = System.currentTimeMillis();
                if (count % batchSize == 0 || current - timestamp > 1000) {
                    execBatch();
                }
            } catch (Exception e) {
                LOG.error("JdbcSink batch invoke error:{}", sql);
                throw e;
            }
        }
    }

    @Override
    public void close() throws Exception {
        execBatch();
        JdbcUtils.close(statement, connection);
    }

    private void execBatch() throws Exception {
        if (offset.get() > 0) {
            offset.set(0L);
            long start = System.currentTimeMillis();
            int[] results = statement.executeBatch();
            int count = 0;
            for (int r : results) {
                count += r;
            }
            statement.clearBatch();
            connection.commit();
            LOG.info("JdbcSink batch {} use {} MS", count, System.currentTimeMillis() - start);
            timestamp = System.currentTimeMillis();
        }
    }
}
