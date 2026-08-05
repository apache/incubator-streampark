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

import org.apache.streampark.common.util.Utils;

import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.table.api.CompiledPlan;
import org.apache.flink.table.api.ExplainDetail;
import org.apache.flink.table.api.ExplainFormat;
import org.apache.flink.table.api.PlanReference;
import org.apache.flink.table.api.StatementSet;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableConfig;
import org.apache.flink.table.api.TableDescriptor;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.TableException;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.catalog.Catalog;
import org.apache.flink.table.catalog.CatalogDescriptor;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.functions.ScalarFunction;
import org.apache.flink.table.functions.UserDefinedFunction;
import org.apache.flink.table.module.Module;
import org.apache.flink.table.module.ModuleEntry;
import org.apache.flink.table.resource.ResourceUri;
import org.apache.flink.table.types.AbstractDataType;

import java.util.List;
import java.util.Optional;

/** Base table environment trait with SQL execution helpers. */
public abstract class FlinkTableTrait implements TableEnvironment {

    public final ParameterTool parameter;

    private final TableEnvironment tableEnv;

    protected FlinkTableTrait(ParameterTool parameter, TableEnvironment tableEnv) {
        this.parameter = parameter;
        this.tableEnv = tableEnv;
    }

    protected TableEnvironment getTableEnv() {
        return tableEnv;
    }

    public JobExecutionResult start() {
        String appName = FlinkParameterUtils.getAppName(parameter, true);
        return execute(appName);
    }

    public JobExecutionResult execute(String jobName) {
        Utils.printLogo("FlinkTable " + jobName + " Starting...");
        return null;
    }

    public void sql(String sql) {
        FlinkSqlExecutor.executeSql(sql, parameter, this);
    }

    @Override
    public Table fromValues(Expression... values) {
        return tableEnv.fromValues(values);
    }

    @Override
    public Table fromValues(AbstractDataType<?> rowType, Expression... values) {
        return tableEnv.fromValues(rowType, values);
    }

    @Override
    public Table fromValues(Iterable<?> values) {
        return tableEnv.fromValues(values);
    }

    @Override
    public Table fromValues(AbstractDataType<?> rowType, Iterable<?> values) {
        return tableEnv.fromValues(rowType, values);
    }

    @Override
    public void createCatalog(String catalogName, CatalogDescriptor catalogDescriptor) {
        tableEnv.createCatalog(catalogName, catalogDescriptor);
    }

    @Override
    public void useModules(String... moduleNames) {
        tableEnv.useModules(moduleNames);
    }

    @Override
    public void createFunction(
                               String path, String className, List<ResourceUri> resourceUris) {
        tableEnv.createFunction(path, className, resourceUris);
    }

    @Override
    public void createFunction(
                               String path,
                               String className,
                               List<ResourceUri> resourceUris,
                               boolean ignoreIfExists) {
        tableEnv.createFunction(path, className, resourceUris, ignoreIfExists);
    }

    @Override
    public void createTemporaryFunction(
                                        String path, String className, List<ResourceUri> resourceUris) {
        tableEnv.createTemporaryFunction(path, className, resourceUris);
    }

    @Override
    public void createTemporarySystemFunction(
                                              String name, String className, List<ResourceUri> resourceUris) {
        tableEnv.createTemporarySystemFunction(name, className, resourceUris);
    }

    @Override
    public void createTemporaryTable(String path, TableDescriptor descriptor) {
        tableEnv.createTemporaryTable(path, descriptor);
    }

    @Override
    public void createTable(String path, TableDescriptor descriptor) {
        tableEnv.createTable(path, descriptor);
    }

    @Override
    public Table from(TableDescriptor descriptor) {
        return tableEnv.from(descriptor);
    }

    @Override
    public ModuleEntry[] listFullModules() {
        return tableEnv.listFullModules();
    }

    @Override
    public String[] listTables(String catalogName, String databaseName) {
        return tableEnv.listTables(catalogName, databaseName);
    }

    @Override
    public String explainSql(
                             String statement, ExplainFormat format, ExplainDetail... extraDetails) {
        return tableEnv.explainSql(statement, format, extraDetails);
    }

    @Override
    public CompiledPlan loadPlan(PlanReference planReference) throws TableException {
        return tableEnv.loadPlan(planReference);
    }

    @Override
    public CompiledPlan compilePlanSql(String statement) throws TableException {
        return tableEnv.compilePlanSql(statement);
    }

    @Override
    public void registerCatalog(String catalogName, Catalog catalog) {
        tableEnv.registerCatalog(catalogName, catalog);
    }

    @Override
    public Optional<Catalog> getCatalog(String catalogName) {
        return tableEnv.getCatalog(catalogName);
    }

    @Override
    public void loadModule(String moduleName, Module module) {
        tableEnv.loadModule(moduleName, module);
    }

    @Override
    public void unloadModule(String moduleName) {
        tableEnv.unloadModule(moduleName);
    }

    @Override
    public void createTemporarySystemFunction(
                                              String name, Class<? extends UserDefinedFunction> functionClass) {
        tableEnv.createTemporarySystemFunction(name, functionClass);
    }

    @Override
    public void createTemporarySystemFunction(
                                              String name, UserDefinedFunction functionInstance) {
        tableEnv.createTemporarySystemFunction(name, functionInstance);
    }

    @Override
    public boolean dropTemporarySystemFunction(String name) {
        return tableEnv.dropTemporarySystemFunction(name);
    }

    @Override
    public void createFunction(String path, Class<? extends UserDefinedFunction> functionClass) {
        tableEnv.createFunction(path, functionClass);
    }

    @Override
    public void createFunction(
                               String path,
                               Class<? extends UserDefinedFunction> functionClass,
                               boolean ignoreIfExists) {
        tableEnv.createFunction(path, functionClass, ignoreIfExists);
    }

    @Override
    public boolean dropFunction(String path) {
        return tableEnv.dropFunction(path);
    }

    @Override
    public void createTemporaryFunction(
                                        String path, Class<? extends UserDefinedFunction> functionClass) {
        tableEnv.createTemporaryFunction(path, functionClass);
    }

    @Override
    public void createTemporaryFunction(String path, UserDefinedFunction functionInstance) {
        tableEnv.createTemporaryFunction(path, functionInstance);
    }

    @Override
    public boolean dropTemporaryFunction(String path) {
        return tableEnv.dropTemporaryFunction(path);
    }

    @Override
    public void createTemporaryView(String path, Table view) {
        tableEnv.createTemporaryView(path, view);
    }

    @Override
    public Table from(String path) {
        return tableEnv.from(path);
    }

    @Override
    public String[] listCatalogs() {
        return tableEnv.listCatalogs();
    }

    @Override
    public String[] listModules() {
        return tableEnv.listModules();
    }

    @Override
    public String[] listDatabases() {
        return tableEnv.listDatabases();
    }

    @Override
    public String[] listTables() {
        return tableEnv.listTables();
    }

    @Override
    public String[] listViews() {
        return tableEnv.listViews();
    }

    @Override
    public String[] listTemporaryTables() {
        return tableEnv.listTemporaryTables();
    }

    @Override
    public String[] listTemporaryViews() {
        return tableEnv.listTemporaryViews();
    }

    @Override
    public String[] listUserDefinedFunctions() {
        return tableEnv.listUserDefinedFunctions();
    }

    @Override
    public String[] listFunctions() {
        return tableEnv.listFunctions();
    }

    @Override
    public boolean dropTemporaryTable(String path) {
        return tableEnv.dropTemporaryTable(path);
    }

    @Override
    public boolean dropTemporaryView(String path) {
        return tableEnv.dropTemporaryView(path);
    }

    @Override
    public String explainSql(String statement, ExplainDetail... extraDetails) {
        return tableEnv.explainSql(statement, extraDetails);
    }

    @Override
    public Table sqlQuery(String query) {
        return tableEnv.sqlQuery(query);
    }

    @Override
    public TableResult executeSql(String statement) {
        return tableEnv.executeSql(statement);
    }

    @Override
    public String getCurrentCatalog() {
        return tableEnv.getCurrentCatalog();
    }

    @Override
    public void useCatalog(String catalogName) {
        tableEnv.useCatalog(catalogName);
    }

    @Override
    public String getCurrentDatabase() {
        return tableEnv.getCurrentDatabase();
    }

    @Override
    public void useDatabase(String databaseName) {
        tableEnv.useDatabase(databaseName);
    }

    @Override
    public TableConfig getConfig() {
        return tableEnv.getConfig();
    }

    @Override
    public StatementSet createStatementSet() {
        return tableEnv.createStatementSet();
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @Override
    public void registerFunction(String name, ScalarFunction function) {
        tableEnv.registerFunction(name, function);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @Override
    public void registerTable(String name, Table table) {
        tableEnv.registerTable(name, table);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @Override
    public Table scan(String... tablePath) {
        return tableEnv.scan(tablePath);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @Override
    public String[] getCompletionHints(String statement, int position) {
        return tableEnv.getCompletionHints(statement, position);
    }
}
