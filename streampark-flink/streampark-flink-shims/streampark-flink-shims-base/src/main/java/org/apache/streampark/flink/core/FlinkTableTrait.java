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
import org.apache.flink.table.api.ExplainDetail;
import org.apache.flink.table.api.StatementSet;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableConfig;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.catalog.Catalog;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.functions.ScalarFunction;
import org.apache.flink.table.functions.UserDefinedFunction;
import org.apache.flink.table.module.Module;
import org.apache.flink.table.types.AbstractDataType;

import java.util.Optional;

public abstract class FlinkTableTrait implements TableEnvironment {

    public final ParameterTool parameter;
    private final TableEnvironment tableEnv;

    protected FlinkTableTrait(ParameterTool parameter, TableEnvironment tableEnv) {
        this.parameter = parameter;
        this.tableEnv = tableEnv;
    }

    protected TableEnvironment delegate() {
        return tableEnv;
    }

    public JobExecutionResult start() {
        String appName = FlinkEnvironmentUtils.getAppName(parameter, null, true);
        return execute(appName);
    }

    public JobExecutionResult execute(String jobName) {
        Utils.printLogo("FlinkTable " + jobName + " Starting...");
        return null;
    }

    public void sql() {
        sql(null);
    }

    public void sql(String sql) {
        FlinkSqlExecutor.executeSql(sql, parameter, this);
    }

    @Override
    public Table fromValues(Expression... values) {
        return delegate().fromValues(values);
    }

    @Override
    public Table fromValues(AbstractDataType<?> rowType, Expression... values) {
        return delegate().fromValues(rowType, values);
    }

    @Override
    public Table fromValues(Iterable<?> values) {
        return delegate().fromValues(values);
    }

    @Override
    public Table fromValues(AbstractDataType<?> rowType, Iterable<?> values) {
        return delegate().fromValues(rowType, values);
    }

    @Override
    public void registerCatalog(String catalogName, Catalog catalog) {
        delegate().registerCatalog(catalogName, catalog);
    }

    @Override
    public Optional<Catalog> getCatalog(String catalogName) {
        return delegate().getCatalog(catalogName);
    }

    @Override
    public void loadModule(String moduleName, Module module) {
        delegate().loadModule(moduleName, module);
    }

    @Override
    public void unloadModule(String moduleName) {
        delegate().unloadModule(moduleName);
    }

    @Override
    public void createTemporarySystemFunction(
                                              String name, Class<? extends UserDefinedFunction> functionClass) {
        delegate().createTemporarySystemFunction(name, functionClass);
    }

    @Override
    public void createTemporarySystemFunction(String name, UserDefinedFunction functionInstance) {
        delegate().createTemporarySystemFunction(name, functionInstance);
    }

    @Override
    public boolean dropTemporarySystemFunction(String name) {
        return delegate().dropTemporarySystemFunction(name);
    }

    @Override
    public void createFunction(String path, Class<? extends UserDefinedFunction> functionClass) {
        delegate().createFunction(path, functionClass);
    }

    @Override
    public void createFunction(
                               String path,
                               Class<? extends UserDefinedFunction> functionClass,
                               boolean ignoreIfExists) {
        delegate().createFunction(path, functionClass, ignoreIfExists);
    }

    @Override
    public boolean dropFunction(String path) {
        return delegate().dropFunction(path);
    }

    @Override
    public void createTemporaryFunction(String path, Class<? extends UserDefinedFunction> functionClass) {
        delegate().createTemporaryFunction(path, functionClass);
    }

    @Override
    public void createTemporaryFunction(String path, UserDefinedFunction functionInstance) {
        delegate().createTemporaryFunction(path, functionInstance);
    }

    @Override
    public boolean dropTemporaryFunction(String path) {
        return delegate().dropTemporaryFunction(path);
    }

    @Override
    public void createTemporaryView(String path, Table view) {
        delegate().createTemporaryView(path, view);
    }

    @Override
    public Table from(String path) {
        return delegate().from(path);
    }

    @Override
    public String[] listCatalogs() {
        return delegate().listCatalogs();
    }

    @Override
    public String[] listModules() {
        return delegate().listModules();
    }

    @Override
    public String[] listDatabases() {
        return delegate().listDatabases();
    }

    @Override
    public String[] listTables() {
        return delegate().listTables();
    }

    @Override
    public String[] listViews() {
        return delegate().listViews();
    }

    @Override
    public String[] listTemporaryTables() {
        return delegate().listTemporaryTables();
    }

    @Override
    public String[] listTemporaryViews() {
        return delegate().listTemporaryViews();
    }

    @Override
    public String[] listUserDefinedFunctions() {
        return delegate().listUserDefinedFunctions();
    }

    @Override
    public String[] listFunctions() {
        return delegate().listFunctions();
    }

    @Override
    public boolean dropTemporaryTable(String path) {
        return delegate().dropTemporaryTable(path);
    }

    @Override
    public boolean dropTemporaryView(String path) {
        return delegate().dropTemporaryView(path);
    }

    @Override
    public String explainSql(String statement, ExplainDetail... extraDetails) {
        return delegate().explainSql(statement, extraDetails);
    }

    @Override
    public Table sqlQuery(String query) {
        return delegate().sqlQuery(query);
    }

    @Override
    public TableResult executeSql(String statement) {
        return delegate().executeSql(statement);
    }

    @Override
    public String getCurrentCatalog() {
        return delegate().getCurrentCatalog();
    }

    @Override
    public void useCatalog(String catalogName) {
        delegate().useCatalog(catalogName);
    }

    @Override
    public String getCurrentDatabase() {
        return delegate().getCurrentDatabase();
    }

    @Override
    public void useDatabase(String databaseName) {
        delegate().useDatabase(databaseName);
    }

    @Override
    public TableConfig getConfig() {
        return delegate().getConfig();
    }

    @Override
    public StatementSet createStatementSet() {
        return delegate().createStatementSet();
    }

    @Deprecated
    @Override
    public void registerFunction(String name, ScalarFunction function) {
        delegate().registerFunction(name, function);
    }

    @Deprecated
    @Override
    public void registerTable(String name, Table table) {
        delegate().registerTable(name, table);
    }

    @Deprecated
    @Override
    public Table scan(String... tablePath) {
        return delegate().scan(tablePath);
    }

    @Deprecated
    @Override
    public String[] getCompletionHints(String statement, int position) {
        return delegate().getCompletionHints(statement, position);
    }
}
