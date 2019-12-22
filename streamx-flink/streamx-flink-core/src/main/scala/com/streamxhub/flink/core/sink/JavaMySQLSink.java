package com.streamxhub.flink.core.sink;

import com.streamxhub.flink.core.util.MySQLConfig;
import com.streamxhub.flink.core.util.MySQLUtils;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.base.VoidSerializer;
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer;
import org.apache.flink.streaming.api.functions.sink.TwoPhaseCommitSinkFunction;

import java.sql.Connection;
import java.sql.SQLException;

public class JavaMySQLSink extends TwoPhaseCommitSinkFunction<String, Connection, Void> {

    private MySQLConfig config;

    public JavaMySQLSink() {
        super(new KryoSerializer<>(Connection.class, new ExecutionConfig()), VoidSerializer.INSTANCE);
    }

    public JavaMySQLSink(MySQLConfig config) {
        super(new KryoSerializer<>(Connection.class, new ExecutionConfig()), VoidSerializer.INSTANCE);
        this.config = config;
    }

    public JavaMySQLSink(TypeSerializer<Connection> transactionSerializer, TypeSerializer<Void> contextSerializer) {
        super(transactionSerializer, contextSerializer);
    }

    @Override
    protected void invoke(Connection connection, String sql, Context context) throws Exception {
        connection.prepareStatement(sql).execute();
    }

    @Override
    protected Connection beginTransaction() throws Exception {
        Connection connection = MySQLUtils.getConnection(this.config);
        connection.setAutoCommit(false);
        return connection;
    }

    @Override
    protected void preCommit(Connection transaction) throws Exception {

    }

    @Override
    protected void commit(Connection connection) {
        if (connection != null) {
            try {
                connection.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                close(connection);
            }
        }
    }

    @Override
    protected void abort(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                close(connection);
            }
        }
    }

    private void close(Connection conn) {
        MySQLUtils.close(this.config, conn, null, null);
    }
}
