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

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.CompiledPlan;
import org.apache.flink.table.api.ExplainDetail;
import org.apache.flink.table.api.ExplainFormat;
import org.apache.flink.table.api.ModelDescriptor;
import org.apache.flink.table.api.PlanReference;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableDescriptor;
import org.apache.flink.table.api.TableException;
import org.apache.flink.table.api.bridge.java.StreamStatementSet;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.CatalogDescriptor;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.functions.UserDefinedFunction;
import org.apache.flink.table.module.ModuleEntry;
import org.apache.flink.table.resource.ResourceUri;
import org.apache.flink.table.types.AbstractDataType;
import org.apache.flink.types.Row;
import org.apache.flink.util.ParameterTool;

import java.util.List;

/** Flink 2.1 stream-table environment context. */
public class StreamTableContext extends FlinkStreamTableTraitV2 {

    public StreamTableContext(
                              ParameterTool parameter,
                              StreamExecutionEnvironment streamEnv,
                              StreamTableEnvironment tableEnv) {
        super(parameter, streamEnv, tableEnv);
    }

    public StreamTableContext(FlinkTableInitializerV2.StreamTableInitResult init) {
        this(init.parameter, init.streamEnv, init.streamTableEnv);
    }

    public StreamTableContext(StreamTableEnvConfig config) {
        this(FlinkTableInitializerV2.initialize(config));
    }

    @Override
    public <T> Table fromDataStream(DataStream<T> dataStream, Schema schema) {
        return getStreamTableEnv().fromDataStream(dataStream, schema);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @Override
    public <T> Table fromDataStream(DataStream<T> dataStream, Expression... expressions) {
        return getStreamTableEnv().fromDataStream(dataStream, expressions);
    }

    @Override
    public Table fromChangelogStream(DataStream<Row> dataStream) {
        return getStreamTableEnv().fromChangelogStream(dataStream);
    }

    @Override
    public Table fromChangelogStream(DataStream<Row> dataStream, Schema schema) {
        return getStreamTableEnv().fromChangelogStream(dataStream, schema);
    }

    @Override
    public Table fromChangelogStream(
                                     DataStream<Row> dataStream, Schema schema, ChangelogMode changelogMode) {
        return getStreamTableEnv().fromChangelogStream(dataStream, schema, changelogMode);
    }

    @Override
    public <T> void createTemporaryView(String path, DataStream<T> dataStream, Schema schema) {
        getStreamTableEnv().createTemporaryView(path, dataStream, schema);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @Override
    public <T> void createTemporaryView(
                                        String path, DataStream<T> dataStream, Expression... expressions) {
        getStreamTableEnv().createTemporaryView(path, dataStream, expressions);
    }

    @Override
    public DataStream<Row> toDataStream(Table table) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toDataStream(table);
    }

    @Override
    public <T> DataStream<T> toDataStream(Table table, Class<T> targetClass) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toDataStream(table, targetClass);
    }

    @Override
    public <T> DataStream<T> toDataStream(Table table, AbstractDataType<?> targetDataType) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toDataStream(table, targetDataType);
    }

    @Override
    public DataStream<Row> toChangelogStream(Table table) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toChangelogStream(table);
    }

    @Override
    public DataStream<Row> toChangelogStream(Table table, Schema targetSchema) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toChangelogStream(table, targetSchema);
    }

    @Override
    public DataStream<Row> toChangelogStream(
                                             Table table, Schema targetSchema, ChangelogMode changelogMode) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toChangelogStream(table, targetSchema, changelogMode);
    }

    @Override
    public StreamStatementSet createStatementSet() {
        return getStreamTableEnv().createStatementSet();
    }

    @Override
    public void useModules(String... moduleNames) {
        getStreamTableEnv().useModules(moduleNames);
    }

    @Override
    public void createTemporaryTable(String path, TableDescriptor descriptor) {
        getStreamTableEnv().createTemporaryTable(path, descriptor);
    }

    @Override
    public void createTable(String path, TableDescriptor descriptor) {
        getStreamTableEnv().createTable(path, descriptor);
    }

    @Override
    public Table from(TableDescriptor descriptor) {
        return getStreamTableEnv().from(descriptor);
    }

    @Override
    public ModuleEntry[] listFullModules() {
        return getStreamTableEnv().listFullModules();
    }

    @Override
    public String[] listTables(String catalogName, String databaseName) {
        return getStreamTableEnv().listTables(catalogName, databaseName);
    }

    @Override
    public CompiledPlan loadPlan(PlanReference planReference) throws TableException {
        return getStreamTableEnv().loadPlan(planReference);
    }

    @Override
    public CompiledPlan compilePlanSql(String statement) throws TableException {
        return getStreamTableEnv().compilePlanSql(statement);
    }

    @Override
    public void createFunction(String path, String className, List<ResourceUri> resourceUris) {
        getStreamTableEnv().createFunction(path, className, resourceUris);
    }

    @Override
    public void createFunction(
                               String path,
                               String className,
                               List<ResourceUri> resourceUris,
                               boolean ignoreIfExists) {
        getStreamTableEnv().createFunction(path, className, resourceUris, ignoreIfExists);
    }

    @Override
    public void createTemporaryFunction(
                                        String path, String className, List<ResourceUri> resourceUris) {
        getStreamTableEnv().createTemporaryFunction(path, className, resourceUris);
    }

    @Override
    public void createTemporarySystemFunction(
                                              String name, String className, List<ResourceUri> resourceUris) {
        getStreamTableEnv().createTemporarySystemFunction(name, className, resourceUris);
    }

    @Override
    public String explainSql(String statement, ExplainFormat format, ExplainDetail... extraDetails) {
        return getStreamTableEnv().explainSql(statement, format, extraDetails);
    }

    @Override
    public void createCatalog(String catalogName, CatalogDescriptor catalogDescriptor) {
        getStreamTableEnv().createCatalog(catalogName, catalogDescriptor);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @Override
    public <T> DataStream<T> toAppendStream(Table table, TypeInformation<T> typeInformation) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toAppendStream(table, typeInformation);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @Override
    public <T> DataStream<Tuple2<Boolean, T>> toRetractStream(
                                                              Table table, TypeInformation<T> typeInformation) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toRetractStream(table, typeInformation);
    }

    @Override
    public <T> DataStream<T> toAppendStream(Table table, Class<T> clazz) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toAppendStream(table, clazz);
    }

    @Override
    public <T> DataStream<Tuple2<Boolean, T>> toRetractStream(Table table, Class<T> clazz) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toRetractStream(table, clazz);
    }

    @Override
    public boolean createTable(String path, TableDescriptor descriptor, boolean ignoreIfExists) {
        return getStreamTableEnv().createTable(path, descriptor, ignoreIfExists);
    }

    @Override
    public void createTemporaryTable(
                                     String path, TableDescriptor descriptor, boolean ignoreIfExists) {
        getStreamTableEnv().createTemporaryTable(path, descriptor, ignoreIfExists);
    }

    @Override
    public boolean createView(String path, Table view, boolean ignoreIfExists) {
        return getStreamTableEnv().createView(path, view, ignoreIfExists);
    }

    @Override
    public void createView(String path, Table view) {
        getStreamTableEnv().createView(path, view);
    }

    @Override
    public boolean dropTable(String path, boolean ignoreIfNotExists) {
        return getStreamTableEnv().dropTable(path, ignoreIfNotExists);
    }

    @Override
    public boolean dropTable(String path) {
        return getStreamTableEnv().dropTable(path);
    }

    @Override
    public boolean dropView(String path, boolean ignoreIfNotExists) {
        return getStreamTableEnv().dropView(path, ignoreIfNotExists);
    }

    @Override
    public boolean dropView(String path) {
        return getStreamTableEnv().dropView(path);
    }

    @Override
    public void createModel(String path, ModelDescriptor descriptor, boolean ignoreIfExists) {
        getStreamTableEnv().createModel(path, descriptor, ignoreIfExists);
    }

    @Override
    public void createModel(String path, ModelDescriptor descriptor) {
        getStreamTableEnv().createModel(path, descriptor);
    }

    @Override
    public void createTemporaryModel(
                                     String path, ModelDescriptor descriptor, boolean ignoreIfExists) {
        getStreamTableEnv().createTemporaryModel(path, descriptor, ignoreIfExists);
    }

    @Override
    public void createTemporaryModel(String path, ModelDescriptor descriptor) {
        getStreamTableEnv().createTemporaryModel(path, descriptor);
    }

    @Override
    public boolean dropModel(String path, boolean ignoreIfNotExists) {
        return getStreamTableEnv().dropModel(path, ignoreIfNotExists);
    }

    @Override
    public boolean dropModel(String path) {
        return getStreamTableEnv().dropModel(path);
    }

    @Override
    public boolean dropTemporaryModel(String path) {
        return getStreamTableEnv().dropTemporaryModel(path);
    }

    @Override
    public Table fromCall(Class<? extends UserDefinedFunction> functionClass, Object... arguments) {
        return getStreamTableEnv().fromCall(functionClass, arguments);
    }

    @Override
    public Table fromCall(String functionName, Object... arguments) {
        return getStreamTableEnv().fromCall(functionName, arguments);
    }

    @Override
    public String[] listModels() {
        return getStreamTableEnv().listModels();
    }

    @Override
    public String[] listTemporaryModels() {
        return getStreamTableEnv().listTemporaryModels();
    }
}
