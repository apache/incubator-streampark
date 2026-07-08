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

package org.apache.streampark.flink.core;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.graph.StreamGraph;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamStatementSet;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.sources.TableSource;

/** Flink 1.14 stream-table environment context. */
public class StreamTableContext extends FlinkStreamTableTrait {

    public StreamTableContext(
                              ParameterTool parameter,
                              StreamExecutionEnvironment streamEnv,
                              StreamTableEnvironment tableEnv) {
        super(parameter, streamEnv, tableEnv);
    }

    public StreamTableContext(FlinkTableInitializer.StreamTableInitResult init) {
        this(init.parameter, init.streamEnv, init.streamTableEnv);
    }

    public StreamTableContext(StreamTableEnvConfig config) {
        this(FlinkTableInitializer.initialize(config));
    }

    @Override
    public <T> Table fromDataStream(DataStream<T> dataStream, String field) {
        return getStreamTableEnv().fromDataStream(dataStream, field);
    }

    public StreamGraph $getStreamGraph(boolean clearTransformations) {
        return getStreamEnv().getStreamGraph(clearTransformations);
    }

    @Override
    public StreamStatementSet createStatementSet() {
        return getStreamTableEnv().createStatementSet();
    }

    @Override
    public <T> void createTemporaryView(
                                        String path, DataStream<T> dataStream, String field) {
        getStreamTableEnv().createTemporaryView(path, dataStream, field);
    }

    @Override
    public <T> void registerDataStream(
                                       String name, DataStream<T> dataStream, String field) {
        getStreamTableEnv().registerDataStream(name, dataStream, field);
    }

    @Deprecated
    @Override
    public Table fromTableSource(TableSource<?> source) {
        return getStreamTableEnv().fromTableSource(source);
    }

    @Deprecated
    @Override
    public void insertInto(Table table, String sinkPath, String... sinkPathContinued) {
        getStreamTableEnv().insertInto(table, sinkPath, sinkPathContinued);
    }

    @Deprecated
    @Override
    public void insertInto(String targetPath, Table table) {
        getStreamTableEnv().insertInto(targetPath, table);
    }

    @Deprecated
    @Override
    public String explain(Table table) {
        return getStreamTableEnv().explain(table);
    }

    @Deprecated
    @Override
    public String explain(Table table, boolean extended) {
        return getStreamTableEnv().explain(table, extended);
    }

    @Deprecated
    @Override
    public String explain(boolean extended) {
        return getStreamTableEnv().explain(extended);
    }

    @Deprecated
    @Override
    public void sqlUpdate(String stmt) {
        getStreamTableEnv().sqlUpdate(stmt);
    }
}
