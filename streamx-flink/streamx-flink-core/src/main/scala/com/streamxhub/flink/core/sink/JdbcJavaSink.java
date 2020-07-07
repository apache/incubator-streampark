package com.streamxhub.flink.core.sink;

import com.streamxhub.common.util.ConfigUtils;
import com.streamxhub.flink.core.StreamingContext;
import com.streamxhub.flink.core.function.ToSQLFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;

import java.util.Properties;

public class JdbcJavaSink<T> {

    private StreamingContext context;
    private Properties jdbc;
    private ToSQLFunction<T> toSQLFunc;
    private String dialect = Dialect.MYSQL().toString().toLowerCase();
    private String alias = "";
    private Integer isoLevel = null;

    public JdbcJavaSink(StreamingContext context) {
        this.context = context;
        this.jdbc = ConfigUtils.getJdbcConf(context.parameter().toMap(), dialect, alias);
    }

    public JdbcJavaSink<T> dialect(String dialect) {
        this.dialect = dialect;
        return this;
    }

    public JdbcJavaSink<T> isoLevel(Integer isoLevel) {
        this.isoLevel = isoLevel;
        return this;
    }

    public JdbcJavaSink<T> alias(String alias) {
        this.alias = alias;
        this.jdbc = ConfigUtils.getJdbcConf(context.parameter().toMap(), dialect, alias);
        return this;
    }

    public JdbcJavaSink<T> jdbc(Properties jdbc) {
        this.jdbc = jdbc;
        return this;
    }

    public JdbcJavaSink<T> toSQL(ToSQLFunction<T> func) {
        this.toSQLFunc = func;
        return this;
    }

    public DataStreamSink<T> sink(DataStream<T> dataStream) {
        JdbcSinkFunction<T> sinkFun = new JdbcSinkFunction<T>(this.jdbc, toSQLFunc, this.isoLevel);
        return dataStream.addSink(sinkFun);
    }

}
