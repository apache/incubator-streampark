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
import org.apache.streampark.flink.configuration.FlinkJobParameters;
import org.apache.streampark.flink.util.FlinkParameterUtils;

import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamStatementSet;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.CatalogDescriptor;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.types.AbstractDataType;
import org.apache.flink.types.Row;

import java.util.function.Consumer;

/**
 * Shared stream-table adapter for all supported Flink versions.
 *
 * <p>This class deliberately contains only methods whose contracts are compatible across every
 * supported Flink version. APIs added, changed, or removed between Flink releases belong in the
 * concrete version-specific adapters so the shared bytecode remains linkable with each Flink
 * distribution. Once a Table has been converted to a DataStream, the DataStream job must be
 * executed using the execute method of the StreamExecutionEnvironment.
 */
@SuppressWarnings("java:S100")
public abstract class AbstractFlinkStreamTable extends AbstractFlinkTable implements StreamTableEnvironment {

    private final StreamExecutionEnvironment streamEnv;

    private final StreamTableEnvironment tableEnv;

    /** Whether a table has been converted to a DataStream. */
    public boolean isConvertedToDataStream;

    protected AbstractFlinkStreamTable(
                                       FlinkJobParameters parameter,
                                       StreamExecutionEnvironment streamEnv,
                                       StreamTableEnvironment tableEnv) {
        super(parameter, tableEnv);
        this.streamEnv = streamEnv;
        this.tableEnv = tableEnv;
    }

    protected StreamExecutionEnvironment getStreamEnv() {
        return streamEnv;
    }

    protected StreamTableEnvironment getStreamTableEnv() {
        return tableEnv;
    }

    @Override
    public abstract StreamStatementSet createStatementSet();

    /** Recommended API to start tasks. */
    public JobExecutionResult start() {
        return start(null);
    }

    public JobExecutionResult start(String name) {
        String appName = FlinkParameterUtils.getAppName(parameter, name, true);
        return execute(appName);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    public JobExecutionResult execute(String jobName) {
        Utils.printLogo("FlinkStreamTable " + jobName + " Starting...");
        if (isConvertedToDataStream) {
            try {
                return streamEnv.execute(jobName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    /** Executes SQL configured by the default application SQL key. */
    public void sql() {
        sql(null);
    }

    /** Executes SQL configured by the supplied parameter key. */
    public void sql(String sqlKey) {
        sql(sqlKey, null);
    }

    /** Executes SQL configured by the supplied parameter key and reports metadata to a callback. */
    public void sql(String sqlKey, Consumer<String> callback) {
        FlinkSqlExecutor.executeSql(sqlKey, parameter, this, callback);
    }

    @Override
    public <T> Table fromDataStream(DataStream<T> dataStream) {
        return getStreamTableEnv().fromDataStream(dataStream);
    }

    @Override
    public <T> Table fromDataStream(DataStream<T> dataStream, Schema schema) {
        return getStreamTableEnv().fromDataStream(dataStream, schema);
    }

    @Override
    public Table fromChangelogStream(DataStream<Row> dataStream) {
        return getStreamTableEnv().fromChangelogStream(dataStream);
    }

    @Override
    public Table fromChangelogStream(DataStream<Row> dataStream, Schema schema) {
        return getStreamTableEnv().fromChangelogStream(dataStream, schema);
    }

    @Override
    public Table fromChangelogStream(DataStream<Row> dataStream, Schema schema, ChangelogMode changelogMode) {
        return getStreamTableEnv().fromChangelogStream(dataStream, schema, changelogMode);
    }

    @Override
    public <T> void createTemporaryView(String path, DataStream<T> dataStream) {
        getStreamTableEnv().createTemporaryView(path, dataStream);
    }

    @Override
    public <T> void createTemporaryView(String path, DataStream<T> dataStream, Schema schema) {
        getStreamTableEnv().createTemporaryView(path, dataStream, schema);
    }

    @Override
    public DataStream<Row> toDataStream(Table table) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toDataStream(table);
    }

    @Override
    public <T> DataStream<T> toDataStream(Table table, Class<T> targetClass) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toDataStream(table, targetClass);
    }

    @Override
    public <T> DataStream<T> toDataStream(Table table, AbstractDataType<?> targetDataType) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toDataStream(table, targetDataType);
    }

    @Override
    public DataStream<Row> toChangelogStream(Table table) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toChangelogStream(table);
    }

    @Override
    public DataStream<Row> toChangelogStream(Table table, Schema targetSchema) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toChangelogStream(table, targetSchema);
    }

    @Override
    public DataStream<Row> toChangelogStream(Table table, Schema targetSchema, ChangelogMode changelogMode) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toChangelogStream(table, targetSchema, changelogMode);
    }

    @Override
    public <T> Table fromDataStream(DataStream<T> dataStream, Expression... fields) {
        return getStreamTableEnv().fromDataStream(dataStream, fields);
    }

    @Override
    public <T> void createTemporaryView(String path, DataStream<T> dataStream, Expression... fields) {
        getStreamTableEnv().createTemporaryView(path, dataStream, fields);
    }

    @Override
    public <T> DataStream<T> toAppendStream(Table table, Class<T> clazz) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toAppendStream(table, clazz);
    }

    @Override
    public <T> DataStream<T> toAppendStream(Table table, TypeInformation<T> typeInfo) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toAppendStream(table, typeInfo);
    }

    @Override
    public <T> DataStream<Tuple2<Boolean, T>> toRetractStream(Table table, Class<T> clazz) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toRetractStream(table, clazz);
    }

    @Override
    public <T> DataStream<Tuple2<Boolean, T>> toRetractStream(Table table, TypeInformation<T> typeInfo) {
        isConvertedToDataStream = true;
        return getStreamTableEnv().toRetractStream(table, typeInfo);
    }

    @Override
    public void createCatalog(String catalogName, CatalogDescriptor catalogDescriptor) {
        getStreamTableEnv().createCatalog(catalogName, catalogDescriptor);
    }
}
