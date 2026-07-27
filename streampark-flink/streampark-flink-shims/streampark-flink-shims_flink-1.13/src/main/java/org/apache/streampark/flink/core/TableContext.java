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

import scala.Tuple2;

public class TableContext extends FlinkTableTrait {

    public TableContext(ParameterTool parameter, TableEnvironment tableEnv) {
        super(parameter, tableEnv);
    }

    public TableContext(Tuple2<ParameterTool, TableEnvironment> args) {
        this(args._1(), args._2());
    }

    public TableContext(TableEnvConfig args) {
        this(FlinkTableInitializer.initialize(args));
    }

    @Override
    public void useModules(String... strings) {
        delegate().useModules(strings);
    }

    @Override
    public ModuleEntry[] listFullModules() {
        return delegate().listFullModules();
    }

    @Deprecated
    @Override
    public ConnectTableDescriptor connect(ConnectorDescriptor connectorDescriptor) {
        return delegate().connect(connectorDescriptor);
    }

    @Override
    public JobExecutionResult execute(String jobName) {
        Utils.printLogo("FlinkTable " + jobName + " Starting...");
        return null;
    }

    @Deprecated
    @Override
    public Table fromTableSource(TableSource<?> source) {
        return delegate().fromTableSource(source);
    }

    @Deprecated
    @Override
    public void insertInto(Table table, String sinkPath, String... sinkPathContinued) {
        delegate().insertInto(table, sinkPath, sinkPathContinued);
    }

    @Deprecated
    @Override
    public void insertInto(String targetPath, Table table) {
        delegate().insertInto(targetPath, table);
    }

    @Deprecated
    @Override
    public String explain(Table table) {
        return delegate().explain(table);
    }

    @Deprecated
    @Override
    public String explain(Table table, boolean extended) {
        return delegate().explain(table, extended);
    }

    @Deprecated
    @Override
    public String explain(boolean extended) {
        return delegate().explain(extended);
    }

    @Deprecated
    @Override
    public void sqlUpdate(String stmt) {
        delegate().sqlUpdate(stmt);
    }

}
