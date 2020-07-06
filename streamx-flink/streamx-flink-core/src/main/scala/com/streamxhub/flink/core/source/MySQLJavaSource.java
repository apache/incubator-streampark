package com.streamxhub.flink.core.source;

import com.streamxhub.common.util.ConfigUtils;
import com.streamxhub.common.util.JdbcUtils;
import com.streamxhub.flink.core.StreamingContext;
import com.streamxhub.flink.core.function.ResultSetFunction;
import com.streamxhub.flink.core.function.SQLFunction;
import com.streamxhub.flink.core.sink.Dialect;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.io.Serializable;
import java.util.Properties;
import java.util.stream.Stream;

import static scala.collection.JavaConversions.*;

public class MySQLJavaSource<T> {

    private StreamingContext context;
    private Properties jdbc;

    public MySQLJavaSource(StreamingContext context) {
        this(context,(String) null);
    }

    public MySQLJavaSource(StreamingContext context, String alias) {
        this.context = context;
        this.jdbc = ConfigUtils.getJdbcConf(context.parameter().toMap(), Dialect.MYSQL().toString(),alias);
    }

    public MySQLJavaSource(StreamingContext context, Properties jdbc) {
        this.context = context;
        this.jdbc = jdbc;
    }

    public DataStreamSource<T> getDataStream(SQLFunction sqlFun,ResultSetFunction<T> fun) {
        MySQLJavaSourceFunction<T> sourceFunction = new MySQLJavaSourceFunction<>(jdbc, sqlFun, fun);
        return context.getJavaEnv().addSource(sourceFunction);
    }
}


class MySQLJavaSourceFunction<T> implements SourceFunction<T>, Serializable {

    private Properties jdbc;
    private SQLFunction sqlFun;
    private ResultSetFunction<T> fun;

    private boolean isRunning = true;

    public MySQLJavaSourceFunction(Properties jdbc, SQLFunction sqlFun,ResultSetFunction<T> fun) {
        this.jdbc = jdbc;
        this.sqlFun = sqlFun;
        this.fun = fun;
    }

    @Override
    public void run(SourceContext<T> ctx) throws Exception {
        while (isRunning) {
            Stream.of(asJavaIterable(JdbcUtils.select(sqlFun.getSQL(), jdbc))).forEach((x) -> x.forEach(m -> ctx.collect(fun.result(mapAsJavaMap(m)))));
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }

}