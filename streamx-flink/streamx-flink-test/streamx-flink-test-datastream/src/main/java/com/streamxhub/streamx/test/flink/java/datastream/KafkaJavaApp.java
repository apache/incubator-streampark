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

package com.streamxhub.streamx.test.flink.java.datastream;

import com.streamxhub.streamx.flink.core.StreamEnvConfig;
import com.streamxhub.streamx.flink.core.java.sink.KafkaSink;
import com.streamxhub.streamx.flink.core.java.source.KafkaSource;
import com.streamxhub.streamx.flink.core.scala.StreamingContext;
import com.streamxhub.streamx.flink.core.scala.source.KafkaRecord;
import com.streamxhub.streamx.test.flink.java.bean.LogBean;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * @author benjobs
 */
public class KafkaJavaApp {

    public static void main(String[] args) {

        StreamEnvConfig javaConfig = new StreamEnvConfig(args, (environment, parameterTool) -> {
            System.out.println("environment argument set...");
            environment.getConfig().enableForceAvro();
        });

        StreamingContext context = new StreamingContext(javaConfig);

        DataStream<LogBean> source = new KafkaSource<LogBean>(context)
                .deserializer(new KafkaDeserializationSchema<LogBean>() {
                    @Override
                    public TypeInformation<LogBean> getProducedType() {
                        return TypeInformation.of(LogBean.class);
                    }

                    @Override
                    public boolean isEndOfStream(LogBean nextElement) {
                        return false;
                    }

                    @Override
                    public LogBean deserialize(ConsumerRecord<byte[], byte[]> record) {
                        String value = new String(record.value());
                        LogBean logBean = new LogBean();
                        logBean.setControlid("benjobs");
                        //value to logBean....
                        return logBean;
                    }
                })
                .getDataStream()
                .map((MapFunction<KafkaRecord<LogBean>, LogBean>) KafkaRecord::value);

        new KafkaSink<LogBean>(context).sink(source);

        context.start();
    }

}
