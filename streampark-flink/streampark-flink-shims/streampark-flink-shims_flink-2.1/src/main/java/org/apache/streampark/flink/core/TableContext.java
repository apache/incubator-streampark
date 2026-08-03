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

import org.apache.flink.table.api.CompiledPlan;
import org.apache.flink.table.api.ExplainDetail;
import org.apache.flink.table.api.ExplainFormat;
import org.apache.flink.table.api.ModelDescriptor;
import org.apache.flink.table.api.PlanReference;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableDescriptor;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.TableException;
import org.apache.flink.table.catalog.CatalogDescriptor;
import org.apache.flink.table.functions.UserDefinedFunction;
import org.apache.flink.table.module.ModuleEntry;
import org.apache.flink.table.resource.ResourceUri;
import org.apache.flink.util.ParameterTool;

import java.util.List;

/** Flink 2.1 table environment context. */
public class TableContext extends FlinkTableTrait {

    public TableContext(ParameterTool parameter, TableEnvironment tableEnv) {
        super(parameter, tableEnv);
    }

    public TableContext(FlinkTableInitializerV2.TableInitResult init) {
        this(init.parameter, init.tableEnv);
    }

    public TableContext(TableEnvConfig config) {
        this(FlinkTableInitializerV2.initialize(config));
    }

    @Override
    public void useModules(String... moduleNames) {
        getTableEnv().useModules(moduleNames);
    }

    @Override
    public void createTemporaryTable(String path, TableDescriptor descriptor) {
        getTableEnv().createTemporaryTable(path, descriptor);
    }

    @Override
    public void createTable(String path, TableDescriptor descriptor) {
        getTableEnv().createTable(path, descriptor);
    }

    @Override
    public Table from(TableDescriptor descriptor) {
        return getTableEnv().from(descriptor);
    }

    @Override
    public ModuleEntry[] listFullModules() {
        return getTableEnv().listFullModules();
    }

    @Override
    public String[] listTables(String catalogName, String databaseName) {
        return getTableEnv().listTables(catalogName, databaseName);
    }

    @Override
    public CompiledPlan loadPlan(PlanReference planReference) throws TableException {
        return getTableEnv().loadPlan(planReference);
    }

    @Override
    public CompiledPlan compilePlanSql(String statement) throws TableException {
        return getTableEnv().compilePlanSql(statement);
    }

    @Override
    public void createFunction(String path, String className, List<ResourceUri> resourceUris) {
        getTableEnv().createFunction(path, className, resourceUris);
    }

    @Override
    public void createFunction(
                               String path,
                               String className,
                               List<ResourceUri> resourceUris,
                               boolean ignoreIfExists) {
        getTableEnv().createFunction(path, className, resourceUris, ignoreIfExists);
    }

    @Override
    public void createTemporaryFunction(
                                        String path, String className, List<ResourceUri> resourceUris) {
        getTableEnv().createTemporaryFunction(path, className, resourceUris);
    }

    @Override
    public void createTemporarySystemFunction(
                                              String name, String className, List<ResourceUri> resourceUris) {
        getTableEnv().createTemporarySystemFunction(name, className, resourceUris);
    }

    @Override
    public String explainSql(String statement, ExplainFormat format, ExplainDetail... extraDetails) {
        return getTableEnv().explainSql(statement, format, extraDetails);
    }

    @Override
    public void createCatalog(String catalogName, CatalogDescriptor catalogDescriptor) {
        getTableEnv().createCatalog(catalogName, catalogDescriptor);
    }

    @Override
    public boolean createTable(String path, TableDescriptor descriptor, boolean ignoreIfExists) {
        return getTableEnv().createTable(path, descriptor, ignoreIfExists);
    }

    @Override
    public void createTemporaryTable(
                                     String path, TableDescriptor descriptor, boolean ignoreIfExists) {
        getTableEnv().createTemporaryTable(path, descriptor, ignoreIfExists);
    }

    @Override
    public boolean createView(String path, Table view, boolean ignoreIfExists) {
        return getTableEnv().createView(path, view, ignoreIfExists);
    }

    @Override
    public void createView(String path, Table view) {
        getTableEnv().createView(path, view);
    }

    @Override
    public boolean dropTable(String path, boolean ignoreIfNotExists) {
        return getTableEnv().dropTable(path, ignoreIfNotExists);
    }

    @Override
    public boolean dropTable(String path) {
        return getTableEnv().dropTable(path);
    }

    @Override
    public boolean dropView(String path, boolean ignoreIfNotExists) {
        return getTableEnv().dropView(path, ignoreIfNotExists);
    }

    @Override
    public boolean dropView(String path) {
        return getTableEnv().dropView(path);
    }

    @Override
    public void createModel(String path, ModelDescriptor descriptor, boolean ignoreIfExists) {
        getTableEnv().createModel(path, descriptor, ignoreIfExists);
    }

    @Override
    public void createModel(String path, ModelDescriptor descriptor) {
        getTableEnv().createModel(path, descriptor);
    }

    @Override
    public void createTemporaryModel(
                                     String path, ModelDescriptor descriptor, boolean ignoreIfExists) {
        getTableEnv().createTemporaryModel(path, descriptor, ignoreIfExists);
    }

    @Override
    public void createTemporaryModel(String path, ModelDescriptor descriptor) {
        getTableEnv().createTemporaryModel(path, descriptor);
    }

    @Override
    public boolean dropModel(String path, boolean ignoreIfNotExists) {
        return getTableEnv().dropModel(path, ignoreIfNotExists);
    }

    @Override
    public boolean dropModel(String path) {
        return getTableEnv().dropModel(path);
    }

    @Override
    public boolean dropTemporaryModel(String path) {
        return getTableEnv().dropTemporaryModel(path);
    }

    @Override
    public Table fromCall(Class<? extends UserDefinedFunction> functionClass, Object... arguments) {
        return getTableEnv().fromCall(functionClass, arguments);
    }

    @Override
    public Table fromCall(String functionName, Object... arguments) {
        return getTableEnv().fromCall(functionName, arguments);
    }

    @Override
    public String[] listModels() {
        return getTableEnv().listModels();
    }

    @Override
    public String[] listTemporaryModels() {
        return getTableEnv().listTemporaryModels();
    }
}
