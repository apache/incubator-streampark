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
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.cache.DistributedCache;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.io.FileInputFormat;
import org.apache.flink.api.common.io.FilePathFilter;
import org.apache.flink.api.common.io.InputFormat;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.api.connector.source.SourceSplit;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.core.execution.JobClient;
import org.apache.flink.core.execution.JobListener;
import org.apache.flink.runtime.state.StateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.FileMonitoringFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.graph.StreamGraph;
import org.apache.flink.table.api.CompiledPlan;
import org.apache.flink.table.api.ExplainDetail;
import org.apache.flink.table.api.ExplainFormat;
import org.apache.flink.table.api.PlanReference;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableConfig;
import org.apache.flink.table.api.TableDescriptor;
import org.apache.flink.table.api.TableException;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.Catalog;
import org.apache.flink.table.catalog.CatalogDescriptor;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.functions.AggregateFunction;
import org.apache.flink.table.functions.ScalarFunction;
import org.apache.flink.table.functions.TableAggregateFunction;
import org.apache.flink.table.functions.TableFunction;
import org.apache.flink.table.functions.UserDefinedFunction;
import org.apache.flink.table.module.Module;
import org.apache.flink.table.module.ModuleEntry;
import org.apache.flink.table.resource.ResourceUri;
import org.apache.flink.table.types.AbstractDataType;
import org.apache.flink.types.Row;
import org.apache.flink.util.SplittableIterator;

import com.esotericsoftware.kryo.Serializer;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Integration API of stream and table environments.
 *
 * <p>Once a Table has been converted to a DataStream, the DataStream job must be executed using the
 * execute method of the StreamExecutionEnvironment.
 */
@SuppressWarnings("java:S100")
public abstract class FlinkStreamTableTrait implements StreamTableEnvironment {

    public final ParameterTool parameter;

    private final StreamExecutionEnvironment streamEnv;

    private final StreamTableEnvironment tableEnv;

    /** Whether a table has been converted to a DataStream. */
    public boolean isConvertedToDataStream;

    protected FlinkStreamTableTrait(
                                    ParameterTool parameter,
                                    StreamExecutionEnvironment streamEnv,
                                    StreamTableEnvironment tableEnv) {
        this.parameter = parameter;
        this.streamEnv = streamEnv;
        this.tableEnv = tableEnv;
    }

    protected StreamExecutionEnvironment getStreamEnv() {
        return streamEnv;
    }

    protected StreamTableEnvironment getStreamTableEnv() {
        return tableEnv;
    }

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

    public void sql(String sql) {
        sql(sql, null);
    }

    public void sql(String sql, Consumer<String> callback) {
        FlinkSqlExecutor.executeSql(sql, parameter, this, callback);
    }

    public StreamExecutionEnvironment getJavaEnv() {
        return streamEnv;
    }

    public List<Tuple2<String, DistributedCache.DistributedCacheEntry>> $getCachedFiles() {
        return streamEnv.getCachedFiles();
    }

    public List<JobListener> $getJobListeners() {
        return streamEnv.getJobListeners();
    }

    public void $setParallelism(int parallelism) {
        streamEnv.setParallelism(parallelism);
    }

    public StreamExecutionEnvironment $setRuntimeMode(RuntimeExecutionMode deployMode) {
        return streamEnv.setRuntimeMode(deployMode);
    }

    public void $setMaxParallelism(int maxParallelism) {
        streamEnv.setMaxParallelism(maxParallelism);
    }

    public int $getParallelism() {
        return streamEnv.getParallelism();
    }

    public int $getMaxParallelism() {
        return streamEnv.getMaxParallelism();
    }

    public StreamExecutionEnvironment $setBufferTimeout(long timeoutMillis) {
        return streamEnv.setBufferTimeout(timeoutMillis);
    }

    public long $getBufferTimeout() {
        return streamEnv.getBufferTimeout();
    }

    public StreamExecutionEnvironment $disableOperatorChaining() {
        return streamEnv.disableOperatorChaining();
    }

    public CheckpointConfig $getCheckpointConfig() {
        return streamEnv.getCheckpointConfig();
    }

    public StreamExecutionEnvironment $enableCheckpointing(long interval, CheckpointingMode mode) {
        return streamEnv.enableCheckpointing(interval, mode);
    }

    public StreamExecutionEnvironment $enableCheckpointing(long interval) {
        return streamEnv.enableCheckpointing(interval);
    }

    public CheckpointingMode $getCheckpointingMode() {
        return streamEnv.getCheckpointConfig().getCheckpointingMode();
    }

    public StreamExecutionEnvironment $setStateBackend(StateBackend backend) {
        return streamEnv.setStateBackend(backend);
    }

    public StateBackend $getStateBackend() {
        return streamEnv.getStateBackend();
    }

    public void $setRestartStrategy(
                                    RestartStrategies.RestartStrategyConfiguration restartStrategyConfiguration) {
        streamEnv.setRestartStrategy(restartStrategyConfiguration);
    }

    public RestartStrategies.RestartStrategyConfiguration $getRestartStrategy() {
        return streamEnv.getRestartStrategy();
    }

    public void $setNumberOfExecutionRetries(int numRetries) {
        streamEnv.setNumberOfExecutionRetries(numRetries);
    }

    public int $getNumberOfExecutionRetries() {
        return streamEnv.getNumberOfExecutionRetries();
    }

    public <T extends Serializer<?> & Serializable> void $addDefaultKryoSerializer(
                                                                                   Class<?> type, T serializer) {
        streamEnv.addDefaultKryoSerializer(type, serializer);
    }

    public void $addDefaultKryoSerializer(
                                          Class<?> type, Class<? extends Serializer<?>> serializerClass) {
        streamEnv.addDefaultKryoSerializer(type, serializerClass);
    }

    public <T extends Serializer<?> & Serializable> void $registerTypeWithKryoSerializer(
                                                                                         Class<?> clazz, T serializer) {
        streamEnv.registerTypeWithKryoSerializer(clazz, serializer);
    }

    public void $registerTypeWithKryoSerializer(
                                                Class<?> clazz, Class<? extends Serializer<?>> serializer) {
        streamEnv.registerTypeWithKryoSerializer(clazz, serializer);
    }

    public void $registerType(Class<?> typeClass) {
        streamEnv.registerType(typeClass);
    }

    public TimeCharacteristic $getStreamTimeCharacteristic() {
        return streamEnv.getStreamTimeCharacteristic();
    }

    public void $configure(ReadableConfig configuration, ClassLoader classLoader) {
        streamEnv.configure(configuration, classLoader);
    }

    public DataStream<Long> $fromSequence(long from, long to) {
        return streamEnv.fromSequence(from, to);
    }

    public <T> DataStream<T> $fromElements(T... data) {
        return streamEnv.fromElements(data);
    }

    public <T> DataStream<T> $fromCollection(Collection<T> data) {
        return streamEnv.fromCollection(data);
    }

    public <T> DataStream<T> $fromCollection(Iterator<T> data) {
        java.util.List<T> list = new java.util.ArrayList<>();
        data.forEachRemaining(list::add);
        return streamEnv.fromCollection(list);
    }

    public <T> DataStream<T> $fromParallelCollection(
                                                     SplittableIterator<T> data, TypeInformation<T> typeInfo) {
        return streamEnv.fromParallelCollection(data, typeInfo);
    }

    public DataStream<String> $readTextFile(String filePath) {
        return streamEnv.readTextFile(filePath);
    }

    public DataStream<String> $readTextFile(String filePath, String charsetName) {
        return streamEnv.readTextFile(filePath, charsetName);
    }

    public <T> DataStream<T> $readFile(FileInputFormat<T> inputFormat, String filePath) {
        return streamEnv.readFile(inputFormat, filePath);
    }

    public <T> DataStream<T> $readFile(
                                       FileInputFormat<T> inputFormat,
                                       String filePath,
                                       org.apache.flink.streaming.api.functions.source.FileProcessingMode watchType,
                                       long interval) {
        return streamEnv.readFile(inputFormat, filePath, watchType, interval);
    }

    public DataStream<String> $socketTextStream(
                                                String hostname, int port, char delimiter, long maxRetry) {
        return streamEnv.socketTextStream(hostname, port, delimiter, maxRetry);
    }

    public <T> DataStream<T> $createInput(InputFormat<T, ?> inputFormat) {
        return streamEnv.createInput(inputFormat);
    }

    public <T> DataStream<T> $addSource(SourceFunction<T> function) {
        return streamEnv.addSource(function);
    }

    public <T> DataStream<T> $fromSource(
                                         Source<T, ? extends SourceSplit, ?> source,
                                         WatermarkStrategy<T> watermarkStrategy,
                                         String sourceName) {
        return streamEnv.fromSource(source, watermarkStrategy, sourceName);
    }

    public void $registerJobListener(JobListener jobListener) {
        streamEnv.registerJobListener(jobListener);
    }

    public void $clearJobListeners() {
        streamEnv.clearJobListeners();
    }

    public JobClient $executeAsync() throws Exception {
        return streamEnv.executeAsync();
    }

    public JobClient $executeAsync(String jobName) throws Exception {
        return streamEnv.executeAsync(jobName);
    }

    public String $getExecutionPlan() {
        return streamEnv.getExecutionPlan();
    }

    public StreamGraph $getStreamGraph() {
        return streamEnv.getStreamGraph();
    }

    public StreamExecutionEnvironment $getWrappedStreamExecutionEnvironment() {
        return streamEnv;
    }

    public void $registerCachedFile(String filePath, String name) {
        streamEnv.registerCachedFile(filePath, name);
    }

    public void $registerCachedFile(String filePath, String name, boolean executable) {
        streamEnv.registerCachedFile(filePath, name, executable);
    }

    public boolean $isUnalignedCheckpointsEnabled() {
        return streamEnv.getCheckpointConfig().isUnalignedCheckpointsEnabled();
    }

    public boolean $isForceUnalignedCheckpoints() {
        return streamEnv.getCheckpointConfig().isForceUnalignedCheckpoints();
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    public StreamExecutionEnvironment $enableCheckpointing(
                                                           long interval, CheckpointingMode mode, boolean force) {
        return streamEnv.enableCheckpointing(interval, mode);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    public DataStream<Long> $generateSequence(long from, long to) {
        return streamEnv.fromSequence(from, to);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    public DataStream<String> $readFileStream(
                                              String streamPath,
                                              long intervalMillis,
                                              FileMonitoringFunction.WatchType watchType) {
        return streamEnv.readFileStream(streamPath, intervalMillis, watchType);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    public <T> DataStream<T> $readFile(
                                       FileInputFormat<T> inputFormat,
                                       String filePath,
                                       org.apache.flink.streaming.api.functions.source.FileProcessingMode watchType,
                                       long interval,
                                       FilePathFilter filter) {
        return streamEnv.readFile(inputFormat, filePath, watchType, interval, filter);
    }

    @Override
    public <T> Table fromDataStream(DataStream<T> dataStream, Schema schema) {
        return tableEnv.fromDataStream(dataStream, schema);
    }

    @Override
    public Table fromChangelogStream(DataStream<Row> dataStream) {
        return tableEnv.fromChangelogStream(dataStream);
    }

    @Override
    public Table fromChangelogStream(DataStream<Row> dataStream, Schema schema) {
        return tableEnv.fromChangelogStream(dataStream, schema);
    }

    @Override
    public Table fromChangelogStream(
                                     DataStream<Row> dataStream, Schema schema, ChangelogMode changelogMode) {
        return tableEnv.fromChangelogStream(dataStream, schema, changelogMode);
    }

    @Override
    public <T> void createTemporaryView(String path, DataStream<T> dataStream, Schema schema) {
        tableEnv.createTemporaryView(path, dataStream, schema);
    }

    @Override
    public DataStream<Row> toDataStream(Table table) {
        isConvertedToDataStream = true;
        return tableEnv.toDataStream(table);
    }

    @Override
    public <T> DataStream<T> toDataStream(Table table, Class<T> targetClass) {
        isConvertedToDataStream = true;
        return tableEnv.toDataStream(table, targetClass);
    }

    @Override
    public <T> DataStream<T> toDataStream(Table table, AbstractDataType<?> targetDataType) {
        isConvertedToDataStream = true;
        return tableEnv.toDataStream(table, targetDataType);
    }

    @Override
    public DataStream<Row> toChangelogStream(Table table) {
        isConvertedToDataStream = true;
        return tableEnv.toChangelogStream(table);
    }

    @Override
    public DataStream<Row> toChangelogStream(Table table, Schema targetSchema) {
        isConvertedToDataStream = true;
        return tableEnv.toChangelogStream(table, targetSchema);
    }

    @Override
    public DataStream<Row> toChangelogStream(
                                             Table table, Schema targetSchema, ChangelogMode changelogMode) {
        isConvertedToDataStream = true;
        return tableEnv.toChangelogStream(table, targetSchema, changelogMode);
    }

    @Override
    public <T> DataStream<T> toAppendStream(Table table, TypeInformation<T> typeInfo) {
        isConvertedToDataStream = true;
        return tableEnv.toAppendStream(table, typeInfo);
    }

    @Override
    public <T> DataStream<Tuple2<Boolean, T>> toRetractStream(Table table, Class<T> clazz) {
        isConvertedToDataStream = true;
        return tableEnv.toRetractStream(table, clazz);
    }

    @Override
    public <T> DataStream<Tuple2<Boolean, T>> toRetractStream(
                                                              Table table, TypeInformation<T> typeInfo) {
        isConvertedToDataStream = true;
        return tableEnv.toRetractStream(table, typeInfo);
    }

    @Override
    public void createCatalog(String catalogName, CatalogDescriptor catalogDescriptor) {
        tableEnv.createCatalog(catalogName, catalogDescriptor);
    }

    @Override
    public void useModules(String... moduleNames) {
        tableEnv.useModules(moduleNames);
    }

    @Override
    public void createFunction(
                               String path, String className, List<ResourceUri> resourceUris) {
        tableEnv.createFunction(path, className, resourceUris);
    }

    @Override
    public void createFunction(
                               String path,
                               String className,
                               List<ResourceUri> resourceUris,
                               boolean ignoreIfExists) {
        tableEnv.createFunction(path, className, resourceUris, ignoreIfExists);
    }

    @Override
    public void createTemporaryFunction(
                                        String path, String className, List<ResourceUri> resourceUris) {
        tableEnv.createTemporaryFunction(path, className, resourceUris);
    }

    @Override
    public void createTemporarySystemFunction(
                                              String name, String className, List<ResourceUri> resourceUris) {
        tableEnv.createTemporarySystemFunction(name, className, resourceUris);
    }

    @Override
    public void createTemporaryTable(String path, TableDescriptor descriptor) {
        tableEnv.createTemporaryTable(path, descriptor);
    }

    @Override
    public void createTable(String path, TableDescriptor descriptor) {
        tableEnv.createTable(path, descriptor);
    }

    @Override
    public Table from(TableDescriptor descriptor) {
        return tableEnv.from(descriptor);
    }

    @Override
    public ModuleEntry[] listFullModules() {
        return tableEnv.listFullModules();
    }

    @Override
    public String[] listTables(String catalogName, String databaseName) {
        return tableEnv.listTables(catalogName, databaseName);
    }

    @Override
    public String explainSql(
                             String statement, ExplainFormat format, ExplainDetail... extraDetails) {
        return tableEnv.explainSql(statement, format, extraDetails);
    }

    @Override
    public CompiledPlan loadPlan(PlanReference planReference) throws TableException {
        return tableEnv.loadPlan(planReference);
    }

    @Override
    public CompiledPlan compilePlanSql(String statement) throws TableException {
        return tableEnv.compilePlanSql(statement);
    }

    @Override
    public <T> Table fromDataStream(DataStream<T> dataStream) {
        return tableEnv.fromDataStream(dataStream);
    }

    @Override
    public <T> Table fromDataStream(DataStream<T> dataStream, Expression... fields) {
        return tableEnv.fromDataStream(dataStream, fields);
    }

    @Override
    public <T> void createTemporaryView(String path, DataStream<T> dataStream) {
        tableEnv.createTemporaryView(path, dataStream);
    }

    @Override
    public <T> void createTemporaryView(
                                        String path, DataStream<T> dataStream, Expression... fields) {
        tableEnv.createTemporaryView(path, dataStream, fields);
    }

    @Override
    public <T> DataStream<T> toAppendStream(Table table, Class<T> clazz) {
        isConvertedToDataStream = true;
        return tableEnv.toAppendStream(table, clazz);
    }

    @Override
    public Table fromValues(Expression... values) {
        return tableEnv.fromValues(values);
    }

    @Override
    public Table fromValues(AbstractDataType<?> rowType, Expression... values) {
        return tableEnv.fromValues(rowType, values);
    }

    @Override
    public Table fromValues(Iterable<?> values) {
        return tableEnv.fromValues(values);
    }

    @Override
    public Table fromValues(AbstractDataType<?> rowType, Iterable<?> values) {
        return tableEnv.fromValues(rowType, values);
    }

    @Override
    public void registerCatalog(String catalogName, Catalog catalog) {
        tableEnv.registerCatalog(catalogName, catalog);
    }

    @Override
    public Optional<Catalog> getCatalog(String catalogName) {
        return tableEnv.getCatalog(catalogName);
    }

    @Override
    public void loadModule(String moduleName, Module module) {
        tableEnv.loadModule(moduleName, module);
    }

    @Override
    public void unloadModule(String moduleName) {
        tableEnv.unloadModule(moduleName);
    }

    @Override
    public void createTemporarySystemFunction(
                                              String name, Class<? extends UserDefinedFunction> functionClass) {
        tableEnv.createTemporarySystemFunction(name, functionClass);
    }

    @Override
    public void createTemporarySystemFunction(
                                              String name, UserDefinedFunction functionInstance) {
        tableEnv.createTemporarySystemFunction(name, functionInstance);
    }

    @Override
    public boolean dropTemporarySystemFunction(String name) {
        return tableEnv.dropTemporarySystemFunction(name);
    }

    @Override
    public void createFunction(String path, Class<? extends UserDefinedFunction> functionClass) {
        tableEnv.createFunction(path, functionClass);
    }

    @Override
    public void createFunction(
                               String path,
                               Class<? extends UserDefinedFunction> functionClass,
                               boolean ignoreIfExists) {
        tableEnv.createFunction(path, functionClass, ignoreIfExists);
    }

    @Override
    public boolean dropFunction(String path) {
        return tableEnv.dropFunction(path);
    }

    @Override
    public void createTemporaryFunction(
                                        String path, Class<? extends UserDefinedFunction> functionClass) {
        tableEnv.createTemporaryFunction(path, functionClass);
    }

    @Override
    public void createTemporaryFunction(String path, UserDefinedFunction functionInstance) {
        tableEnv.createTemporaryFunction(path, functionInstance);
    }

    @Override
    public boolean dropTemporaryFunction(String path) {
        return tableEnv.dropTemporaryFunction(path);
    }

    @Override
    public void createTemporaryView(String path, Table view) {
        tableEnv.createTemporaryView(path, view);
    }

    @Override
    public Table from(String path) {
        return tableEnv.from(path);
    }

    @Override
    public String[] listCatalogs() {
        return tableEnv.listCatalogs();
    }

    @Override
    public String[] listModules() {
        return tableEnv.listModules();
    }

    @Override
    public String[] listDatabases() {
        return tableEnv.listDatabases();
    }

    @Override
    public String[] listTables() {
        return tableEnv.listTables();
    }

    @Override
    public String[] listViews() {
        return tableEnv.listViews();
    }

    @Override
    public String[] listTemporaryTables() {
        return tableEnv.listTemporaryTables();
    }

    @Override
    public String[] listTemporaryViews() {
        return tableEnv.listTemporaryViews();
    }

    @Override
    public String[] listUserDefinedFunctions() {
        return tableEnv.listUserDefinedFunctions();
    }

    @Override
    public String[] listFunctions() {
        return tableEnv.listFunctions();
    }

    @Override
    public boolean dropTemporaryTable(String path) {
        return tableEnv.dropTemporaryTable(path);
    }

    @Override
    public boolean dropTemporaryView(String path) {
        return tableEnv.dropTemporaryView(path);
    }

    @Override
    public String explainSql(String statement, ExplainDetail... extraDetails) {
        return tableEnv.explainSql(statement, extraDetails);
    }

    @Override
    public Table sqlQuery(String query) {
        return tableEnv.sqlQuery(query);
    }

    @Override
    public TableResult executeSql(String statement) {
        return tableEnv.executeSql(statement);
    }

    @Override
    public String getCurrentCatalog() {
        return tableEnv.getCurrentCatalog();
    }

    @Override
    public void useCatalog(String catalogName) {
        tableEnv.useCatalog(catalogName);
    }

    @Override
    public String getCurrentDatabase() {
        return tableEnv.getCurrentDatabase();
    }

    @Override
    public void useDatabase(String databaseName) {
        tableEnv.useDatabase(databaseName);
    }

    @Override
    public TableConfig getConfig() {
        return tableEnv.getConfig();
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @Override
    public <T> void registerFunction(String name, TableFunction<T> function) {
        tableEnv.registerFunction(name, function);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @Override
    public <T, ACC> void registerFunction(String name, AggregateFunction<T, ACC> function) {
        tableEnv.registerFunction(name, function);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @Override
    public <T, ACC> void registerFunction(String name, TableAggregateFunction<T, ACC> function) {
        tableEnv.registerFunction(name, function);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @Override
    public <T> void registerDataStream(String name, DataStream<T> dataStream) {
        tableEnv.registerDataStream(name, dataStream);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @Override
    public void registerFunction(String name, ScalarFunction function) {
        tableEnv.registerFunction(name, function);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @Override
    public void registerTable(String name, Table table) {
        tableEnv.registerTable(name, table);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @Override
    public Table scan(String... tablePath) {
        return tableEnv.scan(tablePath);
    }

    /** @deprecated Retained for backward compatibility with legacy Flink Table API. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    @Override
    public String[] getCompletionHints(String statement, int position) {
        return tableEnv.getCompletionHints(statement, position);
    }
}
