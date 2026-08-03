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
import org.apache.flink.table.api.CompiledPlan;
import org.apache.flink.table.api.ExplainDetail;
import org.apache.flink.table.api.ExplainFormat;
import org.apache.flink.table.api.PlanReference;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableDescriptor;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.module.ModuleEntry;
import org.apache.flink.table.resource.ResourceUri;

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
    public void createTemporaryTable(String path, TableDescriptor descriptor) {
        delegate().createTemporaryTable(path, descriptor);
    }

    @Override
    public void createTable(String path, TableDescriptor descriptor) {
        delegate().createTable(path, descriptor);
    }

    @Override
    public Table from(TableDescriptor tableDescriptor) {
        return delegate().from(tableDescriptor);
    }

    @Override
    public ModuleEntry[] listFullModules() {
        return delegate().listFullModules();
    }

    /** @since 1.15 */
    @Override
    public String[] listTables(String catalogName, String databaseName) {
        return delegate().listTables(catalogName, databaseName);
    }

    /** @since 1.15 */
    @Override
    public CompiledPlan loadPlan(PlanReference planReference) {
        return delegate().loadPlan(planReference);
    }

    /** @since 1.15 */
    @Override
    public CompiledPlan compilePlanSql(String stmt) {
        return delegate().compilePlanSql(stmt);
    }

    /** @since 1.17 */
    @Override
    public void createFunction(String path, String className, java.util.List<ResourceUri> resourceUris) {
        delegate().createFunction(path, className, resourceUris);
    }

    /** @since 1.17 */
    @Override
    public void createFunction(
                               String path, String className, java.util.List<ResourceUri> resourceUris,
                               boolean ignoreIfExists) {
        delegate().createFunction(path, className, resourceUris, ignoreIfExists);
    }

    /** @since 1.17 */
    @Override
    public void createTemporaryFunction(
                                        String path, String className, java.util.List<ResourceUri> resourceUris) {
        delegate().createTemporaryFunction(path, className, resourceUris);
    }

    /** @since 1.17 */
    @Override
    public void createTemporarySystemFunction(
                                              String name, String className, java.util.List<ResourceUri> resourceUris) {
        delegate().createTemporarySystemFunction(name, className, resourceUris);
    }

    /** @since 1.17 */
    @Override
    public String explainSql(String statement, ExplainFormat format, ExplainDetail... extraDetails) {
        return delegate().explainSql(statement, format, extraDetails);
    }

}
