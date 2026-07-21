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
import org.apache.flink.table.api.TableEnvironment;

import java.util.function.Consumer;

/**
 * Java lifecycle base class for Flink Table API (batch) jobs.
 *
 * <p>Unlike the legacy Scala {@code FlinkTableTrait}, this class does NOT extend / mimic {@link
 * TableEnvironment}. Instead it holds a {@link TableEnvironment} by composition — call {@link
 * #getTableEnv()} to access the full Table API surface directly. This avoids re-implementing and
 * maintaining dozens of delegate methods every time the Flink Table API changes.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * public class MyTableJob extends FlinkTableJob {
 *
 *   public MyTableJob(ParameterTool parameter, TableEnvironment tableEnv) {
 *     super(parameter, tableEnv);
 *   }
 *
 *   @Override
 *   protected void handle() throws Exception {
 *     getTableEnv().executeSql("SELECT * FROM my_table");
 *   }
 * }
 * }</pre>
 */
public abstract class FlinkTableJob {

    protected final ParameterTool parameter;

    protected final TableEnvironment tableEnv;

    protected FlinkTableJob(ParameterTool parameter, TableEnvironment tableEnv) {
        this.parameter = parameter;
        this.tableEnv = tableEnv;
    }

    /**
     * Recommended entry point to start the job. Mirrors the legacy trait's {@code start()}: runs
     * the fixed lifecycle and executes under the required {@code app.name} parameter.
     */
    public final JobExecutionResult start() throws Exception {
        ready();
        handle();
        JobExecutionResult result = execute(getAppName());
        destroy();
        return result;
    }

    /** Hook called before {@link #handle()}. Override for pre-job setup (e.g. catalogs, UDFs). */
    protected void ready() {
    }

    /** User job logic goes here — build and register the Table API pipeline. */
    protected abstract void handle() throws Exception;

    /** Hook called after {@link #execute(String)}. Override for cleanup. */
    protected void destroy() {
    }

    /**
     * Executes the job under the given name. Batch Table API jobs are typically already triggered
     * synchronously by {@code executeSql} / {@code StatementSet#execute()} inside {@link
     * #handle()}, so this returns {@code null} by default — override if a specific job result is
     * needed.
     */
    protected JobExecutionResult execute(String jobName) throws Exception {
        // TODO: replace with Utils.printLogo(...) once streampark-common Java migration (Phase 1) lands
        System.out.println("[StreamPark] FlinkTable " + jobName + " Starting...");
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

    /** Direct access to the underlying Table API — use this instead of delegate methods. */
    public TableEnvironment getTableEnv() {
        return tableEnv;
    }

    public ParameterTool getParameter() {
        return parameter;
    }

    private String getAppName() {
        return FlinkJobSupport.requireAppName(parameter);
    }
}
