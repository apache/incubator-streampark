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

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.StreamParkLoggerFactory;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ExecutionOptions;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.types.Row;
import org.apache.flink.util.ParameterTool;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Executes Flink SQL statements. */
public final class FlinkSqlExecutor {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory()
            .getLogger(FlinkSqlExecutor.class.getName());

    private static final ReentrantReadWriteLock.WriteLock LOCK =
        new ReentrantReadWriteLock().writeLock();

    private static final Map<SqlCommand, CommandHandler> COMMAND_HANDLERS = buildCommandHandlers();

    private FlinkSqlExecutor() {
    }

    public static void executeSql(String sql, ParameterTool parameter, TableEnvironment context) {
        executeSql(sql, parameter, context, null);
    }

    public static void executeSql(
                                  String sql,
                                  ParameterTool parameter,
                                  TableEnvironment context,
                                  Consumer<String> callbackFunc) {
        String flinkSql = resolveSql(sql, parameter);
        ExecutionContext ctx = new ExecutionContext(context, callbackFunc, parameter);
        List<SqlCommandCall> calls = SqlCommandParser.parseSQL(flinkSql, null);
        for (SqlCommandCall call : calls) {
            processCommand(call, ctx);
        }
        finishExecution(flinkSql, ctx);
    }

    private static String resolveSql(String sql, ParameterTool parameter) {
        String flinkSql =
            StringUtils.isBlank(sql)
                ? parameter.get(ConfigKeys.KEY_FLINK_SQL())
                : parameter.get(sql);
        if (StringUtils.isBlank(flinkSql)) {
            throw new IllegalArgumentException("verify failed: flink sql cannot be empty");
        }
        return flinkSql;
    }

    private static void processCommand(SqlCommandCall call, ExecutionContext ctx) {
        COMMAND_HANDLERS.getOrDefault(call.command, FlinkSqlExecutor::executeDefault).handle(call, ctx);
    }

    private static void finishExecution(String flinkSql, ExecutionContext ctx) {
        if (ctx.hasInsert) {
            TableResult result = ctx.statementSet.execute();
            if (result != null) {
                result.getJobClient()
                    .ifPresent(
                        jobClient -> {
                            try {
                                LOG.info("jobId:{}", jobClient.getJobID());
                            } catch (Exception ignored) {
                                // ignore
                            }
                        });
            }
        } else {
            LOG.error("No 'INSERT' statement to trigger the execution of the Flink job.");
            throw new IllegalStateException(
                "No 'INSERT' statement to trigger the execution of the Flink job.");
        }

        LOG.info(
            "\n\n\n==============flinkSql==============\n\n {}\n\n============================\n\n\n",
            flinkSql);
    }

    private static Map<SqlCommand, CommandHandler> buildCommandHandlers() {
        Map<SqlCommand, CommandHandler> handlers = new EnumMap<>(SqlCommand.class);
        handlers.put(SqlCommand.SHOW_CATALOGS,
            (call, ctx) -> showMeta(call, ctx, joinLines(ctx.context.listCatalogs())));
        handlers.put(
            SqlCommand.SHOW_CURRENT_CATALOG,
            (call, ctx) -> showMeta(call, ctx, ctx.context.getCurrentCatalog()));
        handlers.put(
            SqlCommand.SHOW_DATABASES,
            (call, ctx) -> showMeta(call, ctx, joinLines(ctx.context.listDatabases())));
        handlers.put(
            SqlCommand.SHOW_CURRENT_DATABASE,
            (call, ctx) -> showMeta(call, ctx, ctx.context.getCurrentDatabase()));
        handlers.put(SqlCommand.SHOW_TABLES, FlinkSqlExecutor::showTables);
        handlers.put(
            SqlCommand.SHOW_FUNCTIONS,
            (call, ctx) -> showMeta(call, ctx, joinLines(ctx.context.listUserDefinedFunctions())));
        handlers.put(
            SqlCommand.SHOW_MODULES,
            (call, ctx) -> showMeta(call, ctx, joinLines(ctx.context.listModules())));
        handlers.put(SqlCommand.DESC, FlinkSqlExecutor::describeTable);
        handlers.put(SqlCommand.DESCRIBE, FlinkSqlExecutor::describeTable);
        handlers.put(SqlCommand.EXPLAIN, FlinkSqlExecutor::explainSql);
        handlers.put(SqlCommand.SET, FlinkSqlExecutor::setConfig);
        handlers.put(SqlCommand.RESET, FlinkSqlExecutor::resetConfig);
        handlers.put(SqlCommand.RESET_ALL, FlinkSqlExecutor::resetConfig);
        handlers.put(SqlCommand.BEGIN_STATEMENT_SET, FlinkSqlExecutor::warnStatementSet);
        handlers.put(SqlCommand.END_STATEMENT_SET, FlinkSqlExecutor::warnStatementSet);
        handlers.put(SqlCommand.INSERT, FlinkSqlExecutor::addInsert);
        handlers.put(SqlCommand.SELECT, FlinkSqlExecutor::rejectSelect);
        handlers.put(SqlCommand.DELETE, FlinkSqlExecutor::validateBatchCommand);
        handlers.put(SqlCommand.UPDATE, FlinkSqlExecutor::validateBatchCommand);
        return handlers;
    }

    private static void showMeta(SqlCommandCall call, ExecutionContext ctx, String payload) {
        ctx.callback.accept(call.command.getName() + ": " + payload);
    }

    private static void showTables(SqlCommandCall call, ExecutionContext ctx) {
        String tables =
            Arrays.stream(ctx.context.listTables())
                .filter(t -> !t.startsWith("UnnamedTable"))
                .collect(Collectors.joining("\n"));
        showMeta(call, ctx, tables);
    }

    private static void describeTable(SqlCommandCall call, ExecutionContext ctx) {
        String args = firstOperand(call);
        ResolvedSchema schema = ctx.context.from(args).getResolvedSchema();
        StringBuilder builder = new StringBuilder();
        builder.append("Column\tType\n");
        for (int i = 0; i < schema.getColumnCount(); i++) {
            builder.append(schema.getColumnNames().get(i))
                .append("\t")
                .append(schema.getColumnDataTypes().get(i))
                .append("\n");
        }
        ctx.callback.accept(builder.toString());
    }

    private static void explainSql(SqlCommandCall call, ExecutionContext ctx) {
        TableResult tableResult = ctx.context.executeSql(call.originSql);
        Row row = tableResult.collect().next();
        ctx.callback.accept(row.getField(0).toString());
    }

    private static void setConfig(SqlCommandCall call, ExecutionContext ctx) {
        AssertUtils.required(
            call.operands != null && call.operands.length >= 2,
            "SET command requires key and value operands");
        String args = call.operands[0];
        String operand = call.operands[1];
        LOG.info("{}: {} --> {}", call.command.getName(), args, operand);
        ctx.context.getConfig().getConfiguration().setString(args, operand);
    }

    private static void resetConfig(SqlCommandCall call, ExecutionContext ctx) {
        String args = firstOperand(call);
        try {
            java.lang.reflect.Field confDataField =
                Configuration.class.getDeclaredField("confData");
            confDataField.setAccessible(true);
            @SuppressWarnings("unchecked")
            HashMap<String, Object> confData =
                (HashMap<String, Object>) confDataField.get(ctx.context.getConfig().getConfiguration());
            synchronized (confData) {
                if (call.command == SqlCommand.RESET) {
                    confData.remove(args);
                } else {
                    confData.clear();
                }
            }
            LOG.info("{}: {}", call.command.getName(), args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to reset Flink table configuration", e);
        }
    }

    private static void warnStatementSet(SqlCommandCall call, ExecutionContext ctx) {
        LOG.warn("SQL Client Syntax: {} ", call.command.getName());
    }

    private static void addInsert(SqlCommandCall call, ExecutionContext ctx) {
        ctx.statementSet.addInsertSql(call.originSql);
        ctx.hasInsert = true;
    }

    private static void rejectSelect(SqlCommandCall call, ExecutionContext ctx) {
        LOG.error("StreamPark dose not support 'SELECT' statement now!");
        throw new UnsupportedOperationException("StreamPark dose not support 'select' statement now!");
    }

    private static void validateBatchCommand(SqlCommandCall call, ExecutionContext ctx) {
        String runMode = ctx.parameter.get(ExecutionOptions.RUNTIME_MODE.key());
        AssertUtils.required(
            !"STREAMING".equals(runMode),
            "Currently, "
                + call.command.getName().toUpperCase()
                + " statement only supports in batch mode, "
                + "and it requires the target table connector implements the SupportsRowLevelDelete, "
                + "For more details please refer to: https://nightlies.apache.org/flink/flink-docs-release-1.18/docs/dev/table/sql/"
                + call.command.getName());
    }

    private static void executeDefault(SqlCommandCall call, ExecutionContext ctx) {
        String args = firstOperand(call);
        try {
            LOCK.lock();
            ctx.context.executeSql(call.originSql);
            LOG.info("{}:{}", call.command.getName(), args);
        } finally {
            if (LOCK.isHeldByCurrentThread()) {
                LOCK.unlock();
            }
        }
    }

    private static String firstOperand(SqlCommandCall call) {
        return call.operands.length == 0 ? null : call.operands[0];
    }

    private static String joinLines(String[] values) {
        return String.join("\n", values);
    }

    @FunctionalInterface
    private interface CommandHandler {

        void handle(SqlCommandCall call, ExecutionContext ctx);
    }

    private static final class ExecutionContext {

        private final TableEnvironment context;
        private final Consumer<String> callback;
        private final ParameterTool parameter;
        private final org.apache.flink.table.api.StatementSet statementSet;
        private boolean hasInsert;

        private ExecutionContext(
                                 TableEnvironment context, Consumer<String> callbackFunc,
                                 ParameterTool parameter) {
            this.context = context;
            this.parameter = parameter;
            this.statementSet = context.createStatementSet();
            this.callback =
                r -> {
                    if (callbackFunc != null) {
                        callbackFunc.accept(r);
                    } else {
                        LOG.info(r);
                    }
                };
        }
    }
}
