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

import com.streamxhub.flink.core.StreamEnvConfig;
import com.streamxhub.flink.core.StreamingContext;
import com.streamxhub.flink.core.sink.KafkaJavaSink;
import com.streamxhub.flink.core.source.KafakJavaSource;
import com.streamxhub.flink.core.source.KafkaRecord;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class KafkaJavaApp {

    public static void main(String[] args) {

        StreamEnvConfig javaConfig = new StreamEnvConfig(args, (environment, parameterTool) -> {
            //用户可以给environment设置参数...
            System.out.println("environment argument set...");
        });

        StreamingContext context = new StreamingContext(javaConfig);

        DataStream<LogBean> source = new KafakJavaSource<LogBean>(context)
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
                    public LogBean deserialize(ConsumerRecord<byte[], byte[]> record) throws Exception {
                        String value = new String(record.value());
                        LogBean logBean = new LogBean();
                        //value to logBean....
                        return logBean;
                    }
                }).getDataStream()
                .map((MapFunction<KafkaRecord<LogBean>, LogBean>) KafkaRecord::value);

        new KafkaJavaSink<LogBean>(context).sink(source);

        context.start();
    }


}
