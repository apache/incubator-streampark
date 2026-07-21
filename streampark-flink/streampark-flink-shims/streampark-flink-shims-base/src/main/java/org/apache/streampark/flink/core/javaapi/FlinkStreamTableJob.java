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

package org.apache.streampark.flink.core.javaapi;

import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.util.function.Consumer;

/**
 * Java lifecycle base class for jobs that mix the DataStream API and the Table API.
 *
 * <p>Unlike the legacy Scala {@code FlinkStreamTableTrait}, this class does NOT extend / mimic
 * {@link StreamTableEnvironment} or {@link StreamExecutionEnvironment}. Instead it holds both by
 * composition — call {@link #getEnv()} / {@link #getTableEnv()} directly instead of relying on
 * delegate methods (the legacy trait needed a {@code $}-prefix naming hack to avoid clashes
 * between the two APIs; composition removes that need entirely).
 *
 * <p><b>Note on {@code toDataStream} conversions:</b> the legacy trait auto-detected when a
 * {@code Table} was converted back to a {@code DataStream} (via an override) so {@code start()}
 * knew whether to also call {@code env.execute()}. Because this class no longer intercepts calls
 * made directly on {@link #getTableEnv()}, that auto-detection is not possible — call {@link
 * #markConvertedToDataStream()} explicitly after such a conversion in {@link #handle()}.
 */
public abstract class FlinkStreamTableJob {

    protected final ParameterTool parameter;

    protected final StreamExecutionEnvironment env;

    protected final StreamTableEnvironment tableEnv;

    private boolean convertedToDataStream = false;

    protected FlinkStreamTableJob(
                                  ParameterTool parameter, StreamExecutionEnvironment env,
                                  StreamTableEnvironment tableEnv) {
        this.parameter = parameter;
        this.env = env;
        this.tableEnv = tableEnv;
    }

    /** Recommended entry point to start the job. */
    public final JobExecutionResult start() throws Exception {
        ready();
        handle();
        JobExecutionResult result = execute(getAppName());
        destroy();
        return result;
    }

    /** Hook called before {@link #handle()}. Override for pre-job setup. */
    protected void ready() {
    }

    /** User job logic goes here — build the DataStream / Table pipeline. */
    protected abstract void handle() throws Exception;

    /** Hook called after {@link #execute(String)}. Override for cleanup. */
    protected void destroy() {
    }

    /**
     * Executes the job under the given name. Only triggers {@link
     * StreamExecutionEnvironment#execute(String)} if the pipeline was converted back to a {@code
     * DataStream} (see {@link #markConvertedToDataStream()}); pure Table/SQL pipelines are already
     * triggered inside {@link #handle()} via {@code executeSql} / {@code StatementSet#execute()}.
     */
    protected JobExecutionResult execute(String jobName) throws Exception {
        // TODO: replace with Utils.printLogo(...) once streampark-common Java migration (Phase 1) lands
        System.out.println("[StreamPark] FlinkStreamTable " + jobName + " Starting...");
        if (convertedToDataStream) {
            return env.execute(jobName);
        }
        return null;
    }

    /**
     * Convenience shortcut for running a single SQL statement, matching legacy {@code sql(...)}.
     * Statement result lines (e.g. from {@code SHOW TABLES}, {@code EXPLAIN}) are logged by
     * default — use {@link #sql(String, Consumer)} to receive them instead.
     */
    public void sql(String sql) {
        sql(sql, null);
    }

    /**
     * Runs a single SQL statement, routing any result lines to the given callback instead of the
     * default log output.
     *
     * <p>{@code FlinkSqlExecutor.executeSql} (not yet migrated off Scala — see Phase 3) takes a
     * Scala {@code String => Unit} as its 4th argument; this bridges a plain Java {@link Consumer}
     * to that type so the public surface of this class stays Scala-free.
     */
    public void sql(String sql, Consumer<String> callback) {
        FlinkJobSupport.executeSql(sql, parameter, tableEnv, callback);
    }

    /**
     * Call this after converting a {@code Table} back to a {@code DataStream} (e.g. via {@code
     * getTableEnv().toDataStream(table)}) so that {@link #execute(String)} knows to also run the
     * underlying {@link StreamExecutionEnvironment}.
     */
    protected void markConvertedToDataStream() {
        this.convertedToDataStream = true;
    }

    /** Direct access to the DataStream API — use this instead of {@code $}-prefixed delegates. */
    public StreamExecutionEnvironment getEnv() {
        return env;
    }

    /** Direct access to the Table API — use this instead of delegate methods. */
    public StreamTableEnvironment getTableEnv() {
        return tableEnv;
    }

    public ParameterTool getParameter() {
        return parameter;
    }

    private String getAppName() {
        return FlinkJobSupport.requireAppName(parameter);
    }
}
