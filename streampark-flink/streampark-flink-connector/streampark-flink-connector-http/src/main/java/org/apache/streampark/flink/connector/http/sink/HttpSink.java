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

package org.apache.streampark.flink.connector.http.sink;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.connector.http.function.HttpSinkFunction;
import org.apache.streampark.flink.connector.sink.Sink;
import org.apache.streampark.flink.core.scala.StreamingContext;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.http.client.methods.*;
import java.util.HashMap; import java.util.Map; import java.util.Properties;
public class HttpSink implements Sink {
    private final StreamingContext ctx; private final Properties property;
    private final Map<String,String> header; private final int parallelism; private final String name; private final String uid;
    private final Properties prop;
    public HttpSink(StreamingContext ctx, Properties property, Map<String,String> header, int parallelism, String name, String uid) {
        this.ctx = ctx; this.property = property != null ? property : new Properties();
        this.header = header != null ? header : new HashMap<>();
        this.parallelism = parallelism; this.name = name; this.uid = uid;
        this.prop = Utils.toProperties(ctx.parameter.toMap()); Utils.copyProperties(this.property, prop);
    }
    public static HttpSink of(StreamingContext ctx, Map<String,String> header, Properties property, int parallelism, String name, String uid) {
        return new HttpSink(ctx, property, header, parallelism, name, uid);
    }
    public DataStreamSink<String> get(DataStream<String> stream) { return sink(stream, HttpGet.METHOD_NAME); }
    public DataStreamSink<String> post(DataStream<String> stream) { return sink(stream, HttpPost.METHOD_NAME); }
    public DataStreamSink<String> put(DataStream<String> stream) { return sink(stream, HttpPut.METHOD_NAME); }
    public DataStreamSink<String> patch(DataStream<String> stream) { return sink(stream, HttpPatch.METHOD_NAME); }
    public DataStreamSink<String> delete(DataStream<String> stream) { return sink(stream, HttpDelete.METHOD_NAME); }
    private DataStreamSink<String> sink(DataStream<String> stream, String method) {
        Map<String,String> propMap = new HashMap<>();
        for (String k : prop.stringPropertyNames()) propMap.put(k, prop.getProperty(k));
        return afterSink(stream.addSink(new HttpSinkFunction(propMap, header, method)), parallelism, name, uid);
    }
}
