/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.flink.javacase;

import com.streamxhub.common.util.ConfigUtils;
import com.streamxhub.flink.core.StreamEnvConfig;
import com.streamxhub.flink.core.StreamingContext;
import com.streamxhub.flink.core.function.ResultSetFunction;
import com.streamxhub.flink.core.function.SQLFunction;
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
                .sql((SQLFunction) () -> "select * from orders limit 10")
                .result((ResultSetFunction<LogBean>) map -> {
                    System.out.println(map);
                    return new LogBean();
                })
                .getDataStream().returns(TypeInformation.of(LogBean.class))
                .print("Java MySQLSource");

        context.execute();

    }

}
