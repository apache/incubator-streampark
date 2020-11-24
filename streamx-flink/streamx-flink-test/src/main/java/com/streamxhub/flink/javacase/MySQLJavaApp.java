package com.streamxhub.flink.javacase;

import com.streamxhub.flink.core.StreamEnvConfig;
import com.streamxhub.flink.core.scala.StreamingContext;

import com.streamxhub.flink.core.sink.JdbcJavaSink;
import com.streamxhub.flink.core.source.MySQLJavaSource;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.util.Arrays;
import java.util.Properties;

import static com.streamxhub.common.conf.ConfigConst.*;

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
        DataStream<LogBean> stream = new MySQLJavaSource<LogBean>(context, prop)
                .sql(() -> {
                    Thread.sleep(1000);
                    return "select * from orders limit 10";
                })
                .result(map -> {
                    LogBean logBean = new LogBean();
                    logBean.setCard_type("123");
                    logBean.setControlid("345");
                    return Arrays.asList(logBean);
                })
                .getDataStream()
                .returns(TypeInformation.of(LogBean.class));

        //写入MySQL表...
        new JdbcJavaSink<LogBean>(context)
                .jdbc(prop)
                .sql(bean -> String.format("insert into sink(name,value)value('%s','%s')", bean.getCard_type(), bean.getControlid()))
                .sink(stream);

        context.start();

    }
}
