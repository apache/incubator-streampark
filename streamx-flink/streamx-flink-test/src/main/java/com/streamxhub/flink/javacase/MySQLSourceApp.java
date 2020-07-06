package com.streamxhub.flink.javacase;

import com.streamxhub.common.util.ConfigUtils;
import com.streamxhub.flink.core.StreamEnvConfig;
import com.streamxhub.flink.core.StreamingContext;
import com.streamxhub.flink.core.function.MySQLResultFunction;
import com.streamxhub.flink.core.sink.Dialect;
import com.streamxhub.flink.core.source.MySQLJavaSource;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import static com.streamxhub.common.conf.ConfigConst.*;

import java.util.Properties;

public class MySQLSourceApp {

    public static void main(String[] args) {

        StreamingContext context = new StreamingContext(new StreamEnvConfig(args, (environment, parameterTool) -> {
            //用户可以给environment设置参数...
            System.out.println("environment argument set...");
        }));

        //定义jdbc信息,也可以从配置文件读取
        Properties prop = new Properties();
        prop.put(KEY_INSTANCE(), "test");
        prop.put(KEY_JDBC_DRIVER(), "com.mysql.jdbc.Driver");
        prop.put(KEY_JDBC_URL(), "jdbc:mysql://127.0.0.1:3306/test?useSSL=false");
        prop.put(KEY_JDBC_USER(), "root");
        prop.put(KEY_JDBC_PASSWORD(), "123322242");
        prop.put("readOnly", "false");
        prop.put("idleTimeout", "20000");

        //or 从配置文件读取
        Properties jdbc1 = ConfigUtils.getJdbcConf(context.parameter().toMap(), Dialect.MYSQL().toString(), "");

        new MySQLJavaSource<LogBean>(context, prop)
                .getDataStream("select * from orders limit 10", 1000, (MySQLResultFunction<LogBean>) map -> {
                    System.out.println(map);
                    return new LogBean();
                }).returns(TypeInformation.of(LogBean.class))
                .print("Java MySQLSource");

        context.execute();

    }

}
