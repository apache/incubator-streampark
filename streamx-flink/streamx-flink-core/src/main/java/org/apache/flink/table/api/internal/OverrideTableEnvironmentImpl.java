/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.flink.table.api.internal;

import com.streamxhub.streamx.common.conf.ConfigConst;
import org.apache.flink.api.dag.Pipeline;
import org.apache.flink.api.dag.Transformation;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.core.execution.JobClient;
import org.apache.flink.table.api.*;
import org.apache.flink.table.catalog.CatalogManager;
import org.apache.flink.table.catalog.FunctionCatalog;
import org.apache.flink.table.catalog.ObjectIdentifier;
import org.apache.flink.table.delegation.Executor;
import org.apache.flink.table.delegation.Planner;
import org.apache.flink.table.module.ModuleManager;
import org.apache.flink.table.operations.CatalogSinkModifyOperation;
import org.apache.flink.table.operations.ModifyOperation;
import org.apache.flink.types.Row;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.flink.configuration.ConfigOptions.key;

/**
 * @author benjobs
 */
public class OverrideTableEnvironmentImpl extends TableEnvironmentImpl implements TableEnvironmentInternal {

    private ClassLoader userClassLoader;

    protected OverrideTableEnvironmentImpl(CatalogManager catalogManager, ModuleManager moduleManager, TableConfig tableConfig, Executor executor, FunctionCatalog functionCatalog, Planner planner, boolean isStreamingMode, ClassLoader userClassLoader) {
        super(catalogManager, moduleManager, tableConfig, executor, functionCatalog, planner, isStreamingMode, userClassLoader);
        this.userClassLoader = userClassLoader;
    }

    @Override
    public TableResult executeInternal(List<ModifyOperation> operations) {
        List<Transformation<?>> transformations = planner.translate(operations);
        List<String> sinkIdentifierNames = extractSinkIdentifierNames(operations);
        String defaultJobName = tableConfig.getConfiguration().getString(PipelineOptions.NAME, "insert-into_" + String.join(",", sinkIdentifierNames));

        ConfigOption<String> name = key(ConfigConst.KEY_FLINK_APP_NAME())
                .stringType()
                .defaultValue(defaultJobName)
                .withDescription("The flink Job name");

        String jobName = tableConfig.getConfiguration().get(name);
        Pipeline pipeline = execEnv.createPipeline(transformations, tableConfig, jobName);
        try {
            JobClient jobClient = execEnv.executeAsync(pipeline);
            TableSchema.Builder builder = TableSchema.builder();
            Object[] affectedRowCounts = new Long[operations.size()];
            for (int i = 0; i < operations.size(); ++i) {
                // use sink identifier name as field name
                builder.field(sinkIdentifierNames.get(i), DataTypes.BIGINT());
                affectedRowCounts[i] = -1L;
            }

            return TableResultImpl.builder()
                    .jobClient(jobClient)
                    .resultKind(ResultKind.SUCCESS_WITH_CONTENT)
                    .tableSchema(builder.build())
                    .data(new InsertResultIterator(jobClient, Row.of(affectedRowCounts), userClassLoader))
                    .build();
        } catch (Exception e) {
            throw new TableException("Failed to execute sql", e);
        }
    }

    private List<String> extractSinkIdentifierNames(List<ModifyOperation> operations) {
        List<String> tableNames = new ArrayList<>(operations.size());
        Map<String, Integer> tableNameToCount = new HashMap<>();
        for (ModifyOperation operation : operations) {
            if (operation instanceof CatalogSinkModifyOperation) {
                ObjectIdentifier identifier = ((CatalogSinkModifyOperation) operation).getTableIdentifier();
                String fullName = identifier.asSummaryString();
                tableNames.add(fullName);
                tableNameToCount.put(fullName, tableNameToCount.getOrDefault(fullName, 0) + 1);
            } else {
                throw new UnsupportedOperationException("Unsupported operation: " + operation);
            }
        }
        Map<String, Integer> tableNameToIndex = new HashMap<>();
        return tableNames.stream().map(tableName -> {
                    if (tableNameToCount.get(tableName) == 1) {
                        return tableName;
                    } else {
                        Integer index = tableNameToIndex.getOrDefault(tableName, 0) + 1;
                        tableNameToIndex.put(tableName, index);
                        return tableName + "_" + index;
                    }
                }
        ).collect(Collectors.toList());
    }

}
