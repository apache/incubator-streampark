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
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.descriptors.ConnectTableDescriptor;
import org.apache.flink.table.descriptors.ConnectorDescriptor;
import org.apache.flink.table.module.ModuleEntry;
import org.apache.flink.table.sources.TableSource;

/** Flink 1.13 table environment context. */
public class TableContext extends FlinkTableTrait {

    public TableContext(ParameterTool parameter, TableEnvironment tableEnv) {
        super(parameter, tableEnv);
    }

    public TableContext(FlinkTableInitializer.TableInitResult init) {
        this(init.parameter, init.tableEnv);
    }

    public TableContext(TableEnvConfig config) {
        this(FlinkTableInitializer.initialize(config));
    }

    @Override
    public void useModules(String... moduleNames) {
        getTableEnv().useModules(moduleNames);
    }

    @Override
    public ModuleEntry[] listFullModules() {
        return getTableEnv().listFullModules();
    }

    @Deprecated(since = "3.0.0", forRemoval = true)
    @Override
    public ConnectTableDescriptor connect(ConnectorDescriptor connectorDescriptor) {
        return getTableEnv().connect(connectorDescriptor);
    }

    @Override
    public JobExecutionResult execute(String jobName) {
        Utils.printLogo(String.format("FlinkTable %s Starting...", jobName));
        return null;
    }

    @Deprecated(since = "3.0.0", forRemoval = true)
    @Override
    public Table fromTableSource(TableSource<?> source) {
        return getTableEnv().fromTableSource(source);
    }

    @Deprecated(since = "3.0.0", forRemoval = true)
    @Override
    public void insertInto(Table table, String sinkPath, String... sinkPathContinued) {
        getTableEnv().insertInto(table, sinkPath, sinkPathContinued);
    }

    @Deprecated(since = "3.0.0", forRemoval = true)
    @Override
    public void insertInto(String targetPath, Table table) {
        getTableEnv().insertInto(targetPath, table);
    }

    @Deprecated(since = "3.0.0", forRemoval = true)
    @Override
    public String explain(Table table) {
        return getTableEnv().explain(table);
    }

    @Deprecated(since = "3.0.0", forRemoval = true)
    @Override
    public String explain(Table table, boolean extended) {
        return getTableEnv().explain(table, extended);
    }

    @Deprecated(since = "3.0.0", forRemoval = true)
    @Override
    public String explain(boolean extended) {
        return getTableEnv().explain(extended);
    }

    @Deprecated(since = "3.0.0", forRemoval = true)
    @Override
    public void sqlUpdate(String stmt) {
        getTableEnv().sqlUpdate(stmt);
    }
}
