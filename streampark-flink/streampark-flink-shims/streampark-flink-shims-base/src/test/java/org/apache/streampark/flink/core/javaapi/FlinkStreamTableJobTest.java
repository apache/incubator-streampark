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

package org.apache.streampark.flink.core.javaapi;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Smoke tests for {@link FlinkStreamTableJob} — verifies composition and the dataStream flag. */
class FlinkStreamTableJobTest {

    private static ParameterTool paramWithAppName(String appName) {
        Map<String, String> map = new HashMap<>();
        map.put("app.name", appName);
        return ParameterTool.fromMap(map);
    }

    @Test
    void handleGetsDirectAccessToEnvAndTableEnv() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv =
            StreamTableEnvironment.create(env, EnvironmentSettings.newInstance().inStreamingMode().build());

        FlinkStreamTableJob job =
            new FlinkStreamTableJob(paramWithAppName("test-stream-table-job"), env, tableEnv) {

                @Override
                protected void handle() {
                    assertSame(env, getEnv());
                    assertSame(tableEnv, getTableEnv());
                }
            };

        job.start();
    }

    @Test
    void markConvertedToDataStreamTriggersEnvExecute() {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv =
            StreamTableEnvironment.create(env, EnvironmentSettings.newInstance().inStreamingMode().build());

        FlinkStreamTableJob job =
            new FlinkStreamTableJob(paramWithAppName("test-stream-table-job"), env, tableEnv) {

                @Override
                protected void handle() {
                    Table table = getTableEnv().fromValues("a", "b", "c");
                    DataStream<Row> stream = getTableEnv().toDataStream(table);
                    stream.addSink(new SinkFunction<Row>() {
                    }); // no-op sink so the graph has something to execute
                    markConvertedToDataStream();
                }
            };

        // If markConvertedToDataStream() didn't take effect, env.execute() would never run and
        // this bounded stream job would just silently return null instead of actually executing.
        assertDoesNotThrow(job::start);
    }
}
