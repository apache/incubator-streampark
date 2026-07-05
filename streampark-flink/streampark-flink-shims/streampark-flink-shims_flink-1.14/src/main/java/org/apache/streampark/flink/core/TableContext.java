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
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.sources.TableSource;

/** Flink 1.14 table environment context. */
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

    @Deprecated
    @Override
    public Table fromTableSource(TableSource<?> source) {
        return getTableEnv().fromTableSource(source);
    }

    @Deprecated
    @Override
    public void insertInto(Table table, String sinkPath, String... sinkPathContinued) {
        getTableEnv().insertInto(table, sinkPath, sinkPathContinued);
    }

    @Deprecated
    @Override
    public void insertInto(String targetPath, Table table) {
        getTableEnv().insertInto(targetPath, table);
    }

    @Deprecated
    @Override
    public String explain(Table table) {
        return getTableEnv().explain(table);
    }

    @Deprecated
    @Override
    public String explain(Table table, boolean extended) {
        return getTableEnv().explain(table, extended);
    }

    @Deprecated
    @Override
    public String explain(boolean extended) {
        return getTableEnv().explain(extended);
    }

    @Deprecated
    @Override
    public void sqlUpdate(String stmt) {
        getTableEnv().sqlUpdate(stmt);
    }
}
