package com.streamxhub.streamx.flink.javacase;

import com.streamxhub.streamx.flink.core.java.function.SQLQueryFunction;
import com.streamxhub.streamx.flink.core.java.function.SQLResultFunction;
import com.streamxhub.streamx.flink.core.java.sink.JdbcSink;
import com.streamxhub.streamx.flink.core.java.source.MySQLSource;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;

import com.streamxhub.streamx.flink.core.scala.util.StreamEnvConfig;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.util.Arrays;
import java.util.Properties;

import static com.streamxhub.streamx.common.conf.ConfigConst.*;

public class MySQLJavaApp {

    public static void main(String[] args) {

        StreamingContext context = new StreamingContext(new StreamEnvConfig(args, (environment, parameterTool) -> {
            System.out.println(environment);
            System.out.println(parameterTool);
            //用户可以给environment设置参数...
            System.out.println("environment argument set...");
        }));

        //定义jdbc信息,也可以从配置文件读取
        Properties prop = new Properties();
        prop.put(KEY_INSTANCE(), "test");
        prop.put(KEY_JDBC_DRIVER(), "com.mysql.cj.jdbc.Driver");
        prop.put(KEY_JDBC_URL(), "jdbc:mysql://127.0.0.1:3306/test?useSSL=false&allowPublicKeyRetrieval=true");
        prop.put(KEY_JDBC_USER(), "root");
        prop.put(KEY_JDBC_PASSWORD(), "123322242");
        prop.put("readOnly", "false");
        prop.put("idleTimeout", "20000");

        //读取MySQL数据源
        DataStream<LogBean> stream = new MySQLSource<LogBean>(context, prop)
                .getDataStream(
                        (SQLQueryFunction<LogBean>) lastOne -> {
                            Thread.sleep(1000);
                            if (lastOne == null) {
                                return "select * from orders where out_time> 2020-10-10 00:00:00 limit 10 ";
                            } else {
                                return "select * from orders where out_time> " + lastOne.getOut_time() + " limit 10 ";
                            }
                        },
                        (SQLResultFunction<LogBean>) map -> {
                            LogBean logBean = new LogBean();
                            logBean.setCard_type("123");
                            logBean.setControlid("345");
                            return Arrays.asList(logBean);
                        })
                .returns(TypeInformation.of(LogBean.class));

        //写入MySQL表...
        new JdbcSink<LogBean>(context)
                .jdbc(prop)
                .sql(bean -> String.format("insert into sink(name,value)value('%s','%s')", bean.getCard_type(), bean.getControlid()))
                .sink(stream);

        context.start();

    }
}
