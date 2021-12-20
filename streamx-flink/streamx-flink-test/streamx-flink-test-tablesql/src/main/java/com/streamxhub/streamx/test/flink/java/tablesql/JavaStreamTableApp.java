/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.test.flink.java.tablesql;

import com.streamxhub.streamx.flink.core.StreamTableContext;
import com.streamxhub.streamx.flink.core.StreamTableEnvConfig;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.scala.DataStream;
import org.apache.flink.table.api.Table;

import java.time.ZoneId;
import java.util.Arrays;

public class JavaStreamTableApp {

    public static void main(String[] args) {
        StreamTableEnvConfig javaConfig = new StreamTableEnvConfig(args, (environment, parameterTool) -> {
            environment.getConfig().enableForceAvro();
        }, (tableConfig, parameterTool) -> {
            tableConfig.setLocalTimeZone(ZoneId.of("Asia/Shanghai"));
        });

        StreamTableContext context = new StreamTableContext(javaConfig);

        SingleOutputStreamOperator<JavaEntity> source = context.getJavaEnv().fromCollection(
            Arrays.asList(
                "flink,apapche flink",
                "kafka,apapche kafka",
                "spark,spark",
                "zookeeper,apapche zookeeper",
                "hadoop,apapche hadoop"
            )
        ).map((MapFunction<String, JavaEntity>) JavaEntity::new);

        context.createTemporaryView("mySource", new DataStream<>(source));

        Table table = context.from("mySource");
        context.toAppendStream(table, TypeInformation.of(JavaEntity.class)).print();

        context.start("Flink SQl Job");
    }

    public static class JavaEntity {
        public String id;
        public String name;

        public JavaEntity() {
        }

        public JavaEntity(String str) {
            String[] array = str.split(",");
            this.id = array[0];
            this.name = array[1];
        }
    }

}




