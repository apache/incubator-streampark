package com.streamxhub.flink.core.source;

import com.streamxhub.common.util.JdbcUtils;
import com.streamxhub.flink.core.StreamingContext;
import com.streamxhub.flink.core.function.MySQLResultFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.io.Serializable;
import java.util.Properties;
import java.util.stream.Stream;

import static scala.collection.JavaConversions.*;

public class MySQLJavaSource<T> {

    private StreamingContext context;
    private Properties jdbc;

    public MySQLJavaSource(StreamingContext context, Properties jdbc) {
        this.context = context;
        this.jdbc = jdbc;
    }

    public DataStreamSource<T> getDataStream(String sql, int interval, MySQLResultFunction<T> fun) {
        MySQLJavaSourceFunction<T> sourceFunction = new MySQLJavaSourceFunction<>(jdbc, sql, interval, fun);
        return context.getJavaEnv().addSource(sourceFunction);
    }
}


class MySQLJavaSourceFunction<T> implements SourceFunction<T>, Serializable {

    private Properties jdbc;
    private String sql;
    private MySQLResultFunction<T> fun;
    private int interval;

    private boolean isRunning = true;

    public MySQLJavaSourceFunction(Properties jdbc, String sql, int interval, MySQLResultFunction<T> fun) {
        this.jdbc = jdbc;
        this.sql = sql;
        this.fun = fun;
        this.interval = interval;
    }

    @Override
    public void run(SourceContext<T> ctx) throws Exception {
        while (isRunning) {
            Stream.of(asJavaIterable(JdbcUtils.select(sql, jdbc))).forEach((x) -> x.forEach(m -> ctx.collect(fun.result(mapAsJavaMap(m)))));
            Thread.sleep(interval);
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }

}