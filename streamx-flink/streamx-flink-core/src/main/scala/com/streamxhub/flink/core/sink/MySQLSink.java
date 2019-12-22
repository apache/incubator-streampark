package com.streamxhub.flink.core.sink;

import com.streamxhub.flink.core.util.MySQLConfig;
import com.streamxhub.flink.core.util.MySQLUtils;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.base.VoidSerializer;
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer;
import org.apache.flink.streaming.api.functions.sink.TwoPhaseCommitSinkFunction;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQLSink extends TwoPhaseCommitSinkFunction<String, Connection, Void> {

    private MySQLConfig config;

    public MySQLSink(MySQLConfig config) {
        super(new KryoSerializer<>(Connection.class, new ExecutionConfig()), VoidSerializer.INSTANCE);
        this.config = config;
    }

    /**
     * Use default {@link ListStateDescriptor} for internal state serialization. Helpful utilities for using this
     * constructor are {@link TypeInformation#of(Class)}, {@link TypeHint} and
     * {@link TypeInformation#of(TypeHint)}. Example:
     * <pre>
     * {@code
     * TwoPhaseCommitSinkFunction(TypeInformation.of(new TypeHint<State<TXN, CONTEXT>>() {}));
     * }
     * </pre>
     *
     * @param transactionSerializer {@link TypeSerializer} for the transaction type of this sink
     * @param contextSerializer     {@link TypeSerializer} for the context type of this sink
     */
    public MySQLSink(TypeSerializer<Connection> transactionSerializer, TypeSerializer<Void> contextSerializer) {
        super(transactionSerializer, contextSerializer);
    }


    @Override
    protected void invoke(Connection connection, String sql, Context context) throws Exception {
        System.out.println(sql);
        //connection.prepareStatement(sql).execute();
    }

    @Override
    protected Connection beginTransaction() throws Exception {
        return MySQLUtils.getConnection(this.config);
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
