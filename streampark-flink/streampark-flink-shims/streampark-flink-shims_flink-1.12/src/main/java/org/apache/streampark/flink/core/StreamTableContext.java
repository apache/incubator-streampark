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
import org.apache.flink.table.api.StatementSet;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment;
import org.apache.flink.table.descriptors.ConnectorDescriptor;
import org.apache.flink.table.descriptors.StreamTableDescriptor;
import org.apache.flink.table.sources.TableSource;

import scala.Tuple3;

/** Integration api of stream and table */
public class StreamTableContext extends FlinkStreamTableTrait {

    public StreamTableContext(
                              ParameterTool parameter,
                              StreamExecutionEnvironment streamEnv,
                              StreamTableEnvironment tableEnv) {
        super(parameter, streamEnv, tableEnv);
    }

    public StreamTableContext(
                              Tuple3<ParameterTool, StreamExecutionEnvironment, StreamTableEnvironment> args) {
        this(args._1(), args._2(), args._3());
    }

    public StreamTableContext(StreamTableEnvConfig args) {
        this(FlinkTableInitializer.initialize(args));
    }

    @Override
    public StreamTableDescriptor connect(ConnectorDescriptor connectorDescriptor) {
        return tableEnv().connect(connectorDescriptor);
    }

    public StreamGraph $getStreamGraph(String jobName) {
        return streamEnv().getStreamGraph(jobName);
    }

    public StreamGraph $getStreamGraph(String jobName, boolean clearTransformations) {
        return streamEnv().getStreamGraph(jobName, clearTransformations);
    }

    @Override
    public StatementSet createStatementSet() {
        return tableEnv().createStatementSet();
    }

    @Override
    public Table fromTableSource(TableSource<?> source) {
        return tableEnv().fromTableSource(source);
    }

    @Override
    public void insertInto(Table table, String sinkPath, String... sinkPathContinued) {
        tableEnv().insertInto(table, sinkPath, sinkPathContinued);
    }

    @Override
    public void insertInto(String targetPath, Table table) {
        tableEnv().insertInto(targetPath, table);
    }

    @Override
    public String explain(Table table) {
        return tableEnv().explain(table);
    }

    @Override
    public String explain(Table table, boolean extended) {
        return tableEnv().explain(table, extended);
    }

    @Override
    public String explain(boolean extended) {
        return tableEnv().explain(extended);
    }

    @Override
    public void sqlUpdate(String stmt) {
        tableEnv().sqlUpdate(stmt);
    }
}
