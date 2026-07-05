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

import org.apache.streampark.common.util.JdbcUtils;
import org.apache.streampark.flink.connector.function.TransformFunction;
import org.apache.streampark.flink.connector.jdbc.bean.Transaction;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.base.VoidSerializer;
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.sink.TwoPhaseCommitSinkFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/** JDBC two-phase commit sink for exactly-once semantics. */
public class Jdbc2PCSinkFunction<T>
        extends TwoPhaseCommitSinkFunction<T, Transaction, Void> {

    private static final Logger LOG = LoggerFactory.getLogger(Jdbc2PCSinkFunction.class);

    private final Properties jdbc;
    private final TransformFunction<T, String> toSQLFunc;
    private final Map<String, Transaction> buffer = new HashMap<>();

    public Jdbc2PCSinkFunction(Properties jdbc, TransformFunction<T, String> toSQLFn) {
        super(
                new KryoSerializer<>(Transaction.class, new ExecutionConfig()),
                VoidSerializer.INSTANCE);
        if (toSQLFn == null) {
            throw new IllegalArgumentException("[StreamPark] ToSQLFunction can not be null");
        }
        this.jdbc = jdbc;
        this.toSQLFunc = toSQLFn;
    }

    @Override
    protected Optional<Void> initializeUserContext() {
        return Optional.empty();
    }

    @Override
    protected Transaction beginTransaction() {
        LOG.info("Jdbc2PCSink beginTransaction.");
        return new Transaction();
    }

    @Override
    protected void invoke(Transaction transaction, T value, SinkFunction.Context context)
            throws Exception {
        String sql = toSQLFunc.transform(value);
        if (!sql.toUpperCase().trim().startsWith("INSERT")) {
            transaction.setInsertMode(false);
        }
        transaction.setInvoked(true);
        transaction.addSql(sql);
    }

    @Override
    protected void preCommit(Transaction transaction) throws Exception {
        if (transaction.isInvoked()) {
            LOG.info("Jdbc2PCSink preCommit.TransactionId:{}", transaction.getTransactionId());
            buffer.put(transaction.getTransactionId(), transaction);
        }
    }

    @Override
    protected void commit(Transaction transaction) {
        if (transaction.isInvoked() && !transaction.getSql().isEmpty()) {
            LOG.info("Jdbc2PCSink commit,TransactionId:{}", transaction.getTransactionId());
            Connection connection = null;
            Statement statement = null;
            try {
                connection = JdbcUtils.getConnection(jdbc);
                connection.setAutoCommit(false);
                statement = connection.createStatement();
                if (transaction.isInsertMode()) {
                    for (String sql : transaction.getSql()) {
                        statement.addBatch(sql);
                    }
                    statement.executeBatch();
                    statement.clearBatch();
                } else {
                    for (String sql : transaction.getSql()) {
                        statement.executeUpdate(sql);
                    }
                }
                connection.commit();
                buffer.remove(transaction.getTransactionId());
            } catch (Throwable t) {
                LOG.error("Jdbc2PCSink commit Exception:{}", t.getMessage());
                throw new RuntimeException(t);
            } finally {
                JdbcUtils.close(statement, connection);
            }
        }
    }

    @Override
    protected void abort(Transaction transaction) {
        LOG.info("Jdbc2PCSink abort,TransactionId:{}", transaction.getTransactionId());
        buffer.remove(transaction.getTransactionId());
    }
}
