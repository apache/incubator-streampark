/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.flink.connector.doris.sink;

import org.apache.streampark.flink.connector.doris.internal.DorisSinkFunction;
import org.apache.streampark.flink.core.scala.StreamingContext;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;

public class DorisSink<T> {

    private final StreamingContext context;
    public DorisSink(StreamingContext context) {
        this.context = context;
    }

    /**
     * java stream
     * @param source
     * @return
     */
    public DataStreamSink<T> sink(DataStream<T> source) {
        DorisSinkFunction<T> sinkFunction = new DorisSinkFunction<>(context);
        return source.addSink(sinkFunction);
    }

    /**
     * scala stream
     * @param source
     * @return
     */
    public DataStreamSink<T> sink(org.apache.flink.streaming.api.scala.DataStream<T> source) {
        DorisSinkFunction<T> sinkFunction = new DorisSinkFunction<>(context);
        return source.addSink(sinkFunction);
    }

}
