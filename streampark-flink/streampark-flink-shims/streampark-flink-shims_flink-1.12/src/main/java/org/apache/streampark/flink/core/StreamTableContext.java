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
import org.apache.flink.streaming.api.graph.StreamGraph;
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment;

import scala.Tuple3;

/**
 * Integration api of stream and table
 */
public class StreamTableContext extends FlinkStreamTableTrait {

    private final StreamExecutionEnvironment streamEnv;
    private final StreamTableEnvironment tableEnv;

    public StreamTableContext(
                              ParameterTool parameter,
                              StreamExecutionEnvironment streamEnv,
                              StreamTableEnvironment tableEnv) {
        super(parameter, streamEnv, tableEnv);
        this.streamEnv = streamEnv;
        this.tableEnv = tableEnv;
    }

    public StreamTableContext(
                              Tuple3<ParameterTool, StreamExecutionEnvironment, StreamTableEnvironment> args) {
        this(args._1(), args._2(), args._3());
    }

    public StreamTableContext(StreamTableEnvConfig args) {
        this(FlinkTableInitializer.initialize(args));
    }

    @Deprecated
    @Override
    public org.apache.flink.table.descriptors.StreamTableDescriptor connect(
                                                                            org.apache.flink.table.descriptors.ConnectorDescriptor connectorDescriptor) {
        return tableEnv.connect(connectorDescriptor);
    }

    public StreamGraph $getStreamGraph(String jobName) {
        return streamEnv.getStreamGraph(jobName);
    }

    public StreamGraph $getStreamGraph(String jobName, boolean clearTransformations) {
        return streamEnv.getStreamGraph(jobName, clearTransformations);
    }

    @Override
    public org.apache.flink.table.api.StatementSet createStatementSet() {
        return tableEnv.createStatementSet();
    }

    @Deprecated
    @Override
    public org.apache.flink.table.api.Table fromTableSource(org.apache.flink.table.sources.TableSource<?> source) {
        return tableEnv.fromTableSource(source);
    }

    @Deprecated
    @Override
    public void insertInto(org.apache.flink.table.api.Table table, String sinkPath, String... sinkPathContinued) {
        tableEnv.insertInto(table, sinkPath, sinkPathContinued);
    }

    @Deprecated
    @Override
    public void insertInto(String targetPath, org.apache.flink.table.api.Table table) {
        tableEnv.insertInto(targetPath, table);
    }

    @Deprecated
    @Override
    public String explain(org.apache.flink.table.api.Table table) {
        return tableEnv.explain(table);
    }

    @Deprecated
    @Override
    public String explain(org.apache.flink.table.api.Table table, boolean extended) {
        return tableEnv.explain(table, extended);
    }

    @Deprecated
    @Override
    public String explain(boolean extended) {
        return tableEnv.explain(extended);
    }

    @Deprecated
    @Override
    public void sqlUpdate(String stmt) {
        tableEnv.sqlUpdate(stmt);
    }

}
