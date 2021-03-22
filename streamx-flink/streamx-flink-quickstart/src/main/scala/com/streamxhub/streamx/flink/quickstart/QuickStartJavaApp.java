/*
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
package com.streamxhub.streamx.flink.quickstart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamxhub.streamx.flink.core.java.function.SQLFromFunction;
import com.streamxhub.streamx.flink.core.java.function.StreamEnvConfigFunction;
import com.streamxhub.streamx.flink.core.java.sink.JdbcSink;
import com.streamxhub.streamx.flink.core.java.source.KafkaSource;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import com.streamxhub.streamx.flink.core.scala.source.KafkaRecord;
import com.streamxhub.streamx.flink.core.scala.util.StreamEnvConfig;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.io.Serializable;

/**
 * @author benjobs
 */
public class QuickStartJavaApp {

    public static void main(String[] args) {

        StreamEnvConfig envConfig = new StreamEnvConfig(args, new StreamEnvConfigFunction.NoneConfig());

        StreamingContext context = new StreamingContext(envConfig);

        ObjectMapper mapper = new ObjectMapper();

        DataStream<JavaUser> source = new KafkaSource<String>(context)
                .getDataStream()
                .map((MapFunction<KafkaRecord<String>, JavaUser>) value ->
                        mapper.readValue(value.value(), JavaUser.class))
                .filter((FilterFunction<JavaUser>) value -> value.age < 30);


        new JdbcSink<JavaUser>(context)
                .sql((SQLFromFunction<JavaUser>) bean -> String.format(
                        "insert into t_user(`name`,`age`,`gender`,`address`) " +
                                "value('%s',%d,%d,'%s')",
                        bean.name,
                        bean.age,
                        bean.gender,
                        bean.address
                ))
                .towPCSink(source);


        context.start();
    }

}

class JavaUser implements Serializable {
    String name;
    Integer age;
    Integer gender;
    String address;

}

