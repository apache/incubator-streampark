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

import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableDescriptor;
import org.apache.flink.table.api.TableEnvironment;

/** Flink 2.0 table environment context. */
public class TableContext extends AbstractFlinkTable {

    public TableContext(FlinkJobParameters parameter, TableEnvironment tableEnv) {
        super(parameter, tableEnv);
    }

    public TableContext(TableContextSpec contextConfig) {
        this(contextConfig.parameter, contextConfig.tableEnv);
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
}
