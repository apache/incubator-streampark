package com.streamxhub.flink.core.sink;

import com.streamxhub.flink.core.util.MySQLUtils;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.base.VoidSerializer;
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer;
import org.apache.flink.streaming.api.functions.sink.TwoPhaseCommitSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class JavaMySQLSink extends TwoPhaseCommitSinkFunction<String, Connection, Void> {

    private static final Logger log = LoggerFactory.getLogger(JavaMySQLSink.class);

    private Properties jdbcProp;

    public JavaMySQLSink() {
        super(new KryoSerializer<>(Connection.class, new ExecutionConfig()), VoidSerializer.INSTANCE);
    }

    public JavaMySQLSink(Properties jdbcProp) {
        this();
        this.jdbcProp = jdbcProp;
    }

    @Override
    protected void invoke(Connection connection, String sql, Context context) throws Exception {
        log.info("start invoke...");
        System.out.println(sql);
        connection.prepareStatement(sql).execute();
    }

    @Override
    protected Connection beginTransaction() throws Exception {
        log.info("start beginTransaction.......");
        return MySQLUtils.getConnection(jdbcProp);
    }

    @Override
    protected void preCommit(Connection connection) throws Exception {
        log.info("start preCommit...");
    }


    @Override
    protected void commit(Connection connection) {
        log.info("start commit...");
        if (connection != null) {
            try {
                connection.commit();
            } catch (SQLException e) {
                log.error("提交事物失败,Connection:" + connection);
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void abort(Connection connection) {
        log.info("start abort rollback...");
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                log.error("事物回滚失败,Connection:" + connection);
                e.printStackTrace();
            }
        }
    }

}

