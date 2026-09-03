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

import org.apache.streampark.flink.configuration.FlinkJobParameters;
import org.apache.streampark.flink.core.bean.TableContextSpec;

import org.apache.flink.table.api.FunctionDescriptor;
import org.apache.flink.table.api.Model;
import org.apache.flink.table.api.ModelDescriptor;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableDescriptor;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.functions.UserDefinedFunction;

/** Flink 2.2 table environment context. */
public class TableContext extends AbstractFlinkTable {

    public TableContext(FlinkJobParameters parameter, TableEnvironment tableEnv) {
        super(parameter, tableEnv);
    }

    public TableContext(TableContextSpec contextConfig) {
        this(contextConfig.parameter, contextConfig.tableEnv);
    }

    @Override
    public void createFunction(String path, FunctionDescriptor functionDescriptor) {
        getTableEnv().createFunction(path, functionDescriptor);
    }

    @Override
    public void createFunction(String path, FunctionDescriptor functionDescriptor, boolean ignoreIfExists) {
        getTableEnv().createFunction(path, functionDescriptor, ignoreIfExists);
    }

    @Override
    public void createTemporaryFunction(String path, FunctionDescriptor functionDescriptor) {
        getTableEnv().createTemporaryFunction(path, functionDescriptor);
    }

    @Override
    public void createTemporarySystemFunction(String name, FunctionDescriptor functionDescriptor) {
        getTableEnv().createTemporarySystemFunction(name, functionDescriptor);
    }

    @Override
    public void createTemporaryTable(String path, TableDescriptor descriptor, boolean ignoreIfExists) {
        getTableEnv().createTemporaryTable(path, descriptor, ignoreIfExists);
    }

    @Override
    public boolean createTable(String path, TableDescriptor descriptor, boolean ignoreIfExists) {
        return getTableEnv().createTable(path, descriptor, ignoreIfExists);
    }

    @Override
    public void createView(String path, Table view) {
        getTableEnv().createView(path, view);
    }

    @Override
    public boolean createView(String path, Table view, boolean ignoreIfExists) {
        return getTableEnv().createView(path, view, ignoreIfExists);
    }

    @Override
    public void createModel(String path, ModelDescriptor descriptor) {
        getTableEnv().createModel(path, descriptor);
    }

    @Override
    public void createModel(String path, ModelDescriptor descriptor, boolean ignoreIfExists) {
        getTableEnv().createModel(path, descriptor, ignoreIfExists);
    }

    @Override
    public void createTemporaryModel(String path, ModelDescriptor descriptor) {
        getTableEnv().createTemporaryModel(path, descriptor);
    }

    @Override
    public void createTemporaryModel(String path, ModelDescriptor descriptor, boolean ignoreIfExists) {
        getTableEnv().createTemporaryModel(path, descriptor, ignoreIfExists);
    }

    @Override
    public Table fromCall(String path, Object... arguments) {
        return getTableEnv().fromCall(path, arguments);
    }

    @Override
    public Table fromCall(Class<? extends UserDefinedFunction> function, Object... arguments) {
        return getTableEnv().fromCall(function, arguments);
    }

    @Override
    public Model fromModel(String modelPath) {
        return getTableEnv().fromModel(modelPath);
    }

    @Override
    public Model fromModel(ModelDescriptor descriptor) {
        return getTableEnv().fromModel(descriptor);
    }

    @Override
    public String[] listMaterializedTables() {
        return getTableEnv().listMaterializedTables();
    }

    @Override
    public String[] listModels() {
        return getTableEnv().listModels();
    }

    @Override
    public String[] listTemporaryModels() {
        return getTableEnv().listTemporaryModels();
    }

    @Override
    public boolean dropTable(String path) {
        return getTableEnv().dropTable(path);
    }

    @Override
    public boolean dropTable(String path, boolean ignoreIfNotExists) {
        return getTableEnv().dropTable(path, ignoreIfNotExists);
    }

    @Override
    public boolean dropView(String path) {
        return getTableEnv().dropView(path);
    }

    @Override
    public boolean dropView(String path, boolean ignoreIfNotExists) {
        return getTableEnv().dropView(path, ignoreIfNotExists);
    }

    @Override
    public boolean dropModel(String path) {
        return getTableEnv().dropModel(path);
    }

    @Override
    public boolean dropModel(String path, boolean ignoreIfNotExists) {
        return getTableEnv().dropModel(path, ignoreIfNotExists);
    }

    @Override
    public boolean dropTemporaryModel(String path) {
        return getTableEnv().dropTemporaryModel(path);
    }
}
