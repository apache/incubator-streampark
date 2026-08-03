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

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.FunctionDescriptor;
import org.apache.flink.table.api.Model;
import org.apache.flink.table.api.ModelDescriptor;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.functions.UserDefinedFunction;
import org.apache.flink.util.ParameterTool;

/** Flink 2.2 stream-table environment context. */
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

    @Override
    public void createFunction(
                               String path, FunctionDescriptor descriptor, boolean ignoreIfExists) {
        getStreamTableEnv().createFunction(path, descriptor, ignoreIfExists);
    }

    @Override
    public void createFunction(String path, FunctionDescriptor descriptor) {
        getStreamTableEnv().createFunction(path, descriptor);
    }

    @Override
    public void createTemporaryFunction(String path, FunctionDescriptor descriptor) {
        getStreamTableEnv().createTemporaryFunction(path, descriptor);
    }

    @Override
    public void createTemporarySystemFunction(String name, FunctionDescriptor descriptor) {
        getStreamTableEnv().createTemporarySystemFunction(name, descriptor);
    }

    @Override
    public Model fromModel(ModelDescriptor descriptor) {
        return getStreamTableEnv().fromModel(descriptor);
    }

    @Override
    public Model fromModel(String path) {
        return getStreamTableEnv().fromModel(path);
    }

    @Override
    public String[] listMaterializedTables() {
        return getStreamTableEnv().listMaterializedTables();
    }
}
