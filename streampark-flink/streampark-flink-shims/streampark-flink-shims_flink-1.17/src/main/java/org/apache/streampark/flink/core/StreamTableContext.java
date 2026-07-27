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

    @Override
    public <T> org.apache.flink.table.api.Table fromDataStream(
                                                               org.apache.flink.streaming.api.scala.DataStream<T> dataStream,
                                                               org.apache.flink.table.api.Schema schema) {
        return tableEnv.fromDataStream(dataStream, schema);
    }

    @Override
    public org.apache.flink.table.api.Table fromChangelogStream(
                                                                org.apache.flink.streaming.api.scala.DataStream<org.apache.flink.types.Row> dataStream) {
        return tableEnv.fromChangelogStream(dataStream);
    }

    @Override
    public org.apache.flink.table.api.Table fromChangelogStream(
                                                                org.apache.flink.streaming.api.scala.DataStream<org.apache.flink.types.Row> dataStream,
                                                                org.apache.flink.table.api.Schema schema) {
        return tableEnv.fromChangelogStream(dataStream, schema);
    }

    @Override
    public org.apache.flink.table.api.Table fromChangelogStream(
                                                                org.apache.flink.streaming.api.scala.DataStream<org.apache.flink.types.Row> dataStream,
                                                                org.apache.flink.table.api.Schema schema,
                                                                org.apache.flink.table.connector.ChangelogMode changelogMode) {
        return tableEnv.fromChangelogStream(dataStream, schema, changelogMode);
    }

    @Override
    public <T> void createTemporaryView(
                                        String path,
                                        org.apache.flink.streaming.api.scala.DataStream<T> dataStream,
                                        org.apache.flink.table.api.Schema schema) {
        tableEnv.createTemporaryView(path, dataStream, schema);
    }

    @Override
    public org.apache.flink.streaming.api.scala.DataStream<org.apache.flink.types.Row> toDataStream(
                                                                                                    org.apache.flink.table.api.Table table) {
        isConvertedToDataStream = true;
        return tableEnv.toDataStream(table);
    }

    @Override
    public <T> org.apache.flink.streaming.api.scala.DataStream<T> toDataStream(
                                                                               org.apache.flink.table.api.Table table,
                                                                               Class<T> targetClass) {
        isConvertedToDataStream = true;
        return tableEnv.toDataStream(table, targetClass);
    }

    @Override
    public <T> org.apache.flink.streaming.api.scala.DataStream<T> toDataStream(
                                                                               org.apache.flink.table.api.Table table,
                                                                               org.apache.flink.table.types.AbstractDataType<?> targetDataType) {
        isConvertedToDataStream = true;
        return tableEnv.toDataStream(table, targetDataType);
    }

    @Override
    public org.apache.flink.streaming.api.scala.DataStream<org.apache.flink.types.Row> toChangelogStream(
                                                                                                         org.apache.flink.table.api.Table table) {
        isConvertedToDataStream = true;
        return tableEnv.toChangelogStream(table);
    }

    @Override
    public org.apache.flink.streaming.api.scala.DataStream<org.apache.flink.types.Row> toChangelogStream(
                                                                                                         org.apache.flink.table.api.Table table,
                                                                                                         org.apache.flink.table.api.Schema targetSchema) {
        isConvertedToDataStream = true;
        return tableEnv.toChangelogStream(table, targetSchema);
    }

    @Override
    public org.apache.flink.streaming.api.scala.DataStream<org.apache.flink.types.Row> toChangelogStream(
                                                                                                         org.apache.flink.table.api.Table table,
                                                                                                         org.apache.flink.table.api.Schema targetSchema,
                                                                                                         org.apache.flink.table.connector.ChangelogMode changelogMode) {
        isConvertedToDataStream = true;
        return tableEnv.toChangelogStream(table, targetSchema, changelogMode);
    }

    @Override
    public void useModules(String... strings) {
        tableEnv.useModules(strings);
    }

    @Override
    public org.apache.flink.table.module.ModuleEntry[] listFullModules() {
        return tableEnv.listFullModules();
    }

    @Override
    public org.apache.flink.table.api.bridge.scala.StreamStatementSet createStatementSet() {
        return tableEnv.createStatementSet();
    }

    @Override
    public void createTemporaryTable(String path, org.apache.flink.table.api.TableDescriptor descriptor) {
        tableEnv.createTemporaryTable(path, descriptor);
    }

    @Override
    public void createTable(String path, org.apache.flink.table.api.TableDescriptor descriptor) {
        tableEnv.createTable(path, descriptor);
    }

    @Override
    public org.apache.flink.table.api.Table from(org.apache.flink.table.api.TableDescriptor descriptor) {
        return tableEnv.from(descriptor);
    }

    /** @since 1.15 */
    @Override
    public String[] listTables(String s, String s1) {
        return tableEnv.listTables(s, s1);
    }

    /** @since 1.15 */
    @Override
    public org.apache.flink.table.api.CompiledPlan loadPlan(org.apache.flink.table.api.PlanReference planReference) {
        return tableEnv.loadPlan(planReference);
    }

    /** @since 1.15 */
    @Override
    public org.apache.flink.table.api.CompiledPlan compilePlanSql(String s) {
        return tableEnv.compilePlanSql(s);
    }

    /** @since 1.17 */
    @Override
    public void createFunction(
                               String path, String className,
                               java.util.List<org.apache.flink.table.resource.ResourceUri> resourceUris) {
        tableEnv.createFunction(path, className, resourceUris);
    }

    /** @since 1.17 */
    @Override
    public void createFunction(
                               String path,
                               String className,
                               java.util.List<org.apache.flink.table.resource.ResourceUri> resourceUris,
                               boolean ignoreIfExists) {
        tableEnv.createFunction(path, className, resourceUris, ignoreIfExists);
    }

    /** @since 1.17 */
    @Override
    public void createTemporaryFunction(
                                        String path, String className,
                                        java.util.List<org.apache.flink.table.resource.ResourceUri> resourceUris) {
        tableEnv.createTemporaryFunction(path, className, resourceUris);
    }

    /** @since 1.17 */
    @Override
    public void createTemporarySystemFunction(
                                              String name, String className,
                                              java.util.List<org.apache.flink.table.resource.ResourceUri> resourceUris) {
        tableEnv.createTemporarySystemFunction(name, className, resourceUris);
    }

    /** @since 1.17 */
    @Override
    public String explainSql(
                             String statement,
                             org.apache.flink.table.api.ExplainFormat format,
                             org.apache.flink.table.api.ExplainDetail... extraDetails) {
        return tableEnv.explainSql(statement, format, extraDetails);
    }

}
