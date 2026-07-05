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
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ExecutionOptions;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.types.Row;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
        String flinkSql =
            StringUtils.isBlank(sql)
                ? parameter.get(ConfigKeys.KEY_FLINK_SQL())
                : parameter.get(sql);
        if (StringUtils.isBlank(flinkSql)) {
            throw new IllegalArgumentException("verify failed: flink sql cannot be empty");
        }

        Consumer<String> callback =
            r -> {
                if (callbackFunc != null) {
                    callbackFunc.accept(r);
                } else {
                    LOG.info(r);
                }
            };

        String runMode = parameter.get(ExecutionOptions.RUNTIME_MODE.key());

        boolean hasInsert = false;
        org.apache.flink.table.api.StatementSet statementSet = context.createStatementSet();
        List<SqlCommandCall> calls = SqlCommandParser.parseSQL(flinkSql, null);
        for (SqlCommandCall call : calls) {
            String args = call.operands.length == 0 ? null : call.operands[0];
            String command = call.command.getName();
            switch (call.command) {
                case SHOW_CATALOGS:
                    callback.accept(
                        command + ": " + String.join("\n", context.listCatalogs()));
                    break;
                case SHOW_CURRENT_CATALOG:
                    callback.accept(command + ": " + context.getCurrentCatalog());
                    break;
                case SHOW_DATABASES:
                    callback.accept(
                        command + ": " + String.join("\n", context.listDatabases()));
                    break;
                case SHOW_CURRENT_DATABASE:
                    callback.accept(command + ": " + context.getCurrentDatabase());
                    break;
                case SHOW_TABLES:
                    String tables =
                        Arrays.stream(context.listTables())
                            .filter(t -> !t.startsWith("UnnamedTable"))
                            .collect(Collectors.joining("\n"));
                    callback.accept(command + ": " + tables);
                    break;
                case SHOW_FUNCTIONS:
                    callback.accept(
                        command
                            + ": "
                            + String.join("\n", context.listUserDefinedFunctions()));
                    break;
                case SHOW_MODULES:
                    callback.accept(
                        command + ": " + String.join("\n", context.listModules()));
                    break;
                case DESC:
                case DESCRIBE:
                    TableSchema schema = context.scan(args).getSchema();
                    StringBuilder builder = new StringBuilder();
                    builder.append("Column\tType\n");
                    for (int i = 0; i <= schema.getFieldCount(); i++) {
                        builder.append(schema.getFieldName(i).get())
                            .append("\t")
                            .append(schema.getFieldDataType(i).get())
                            .append("\n");
                    }
                    callback.accept(builder.toString());
                    break;
                case EXPLAIN:
                    TableResult tableResult = context.executeSql(call.originSql);
                    Row row = tableResult.collect().next();
                    callback.accept(row.getField(0).toString());
                    break;
                case SET:
                    String operand = call.operands[1];
                    LOG.info("{}: {} --> {}", command, args, operand);
                    context.getConfig().getConfiguration().setString(args, operand);
                    break;
                case RESET:
                case RESET_ALL:
                    try {
                        java.lang.reflect.Field confDataField =
                            Configuration.class.getDeclaredField("confData");
                        confDataField.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        HashMap<String, Object> confData =
                            (HashMap<String, Object>) confDataField.get(
                                context.getConfig().getConfiguration());
                        synchronized (confData) {
                            if (call.command == SqlCommand.RESET) {
                                confData.remove(args);
                            } else {
                                confData.clear();
                            }
                        }
                        LOG.info("{}: {}", command, args);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case BEGIN_STATEMENT_SET:
                case END_STATEMENT_SET:
                    LOG.warn("SQL Client Syntax: {} ", call.command.getName());
                    break;
                case INSERT:
                    statementSet.addInsertSql(call.originSql);
                    hasInsert = true;
                    break;
                case SELECT:
                    LOG.error("StreamPark dose not support 'SELECT' statement now!");
                    throw new RuntimeException(
                        "StreamPark dose not support 'select' statement now!");
                case DELETE:
                case UPDATE:
                    AssertUtils.required(
                        !"STREAMING".equals(runMode),
                        "Currently, "
                            + command.toUpperCase()
                            + " statement only supports in batch mode, "
                            + "and it requires the target table connector implements the SupportsRowLevelDelete, "
                            + "For more details please refer to: https://nightlies.apache.org/flink/flink-docs-release-1.18/docs/dev/table/sql/"
                            + command);
                    break;
                default:
                    try {
                        LOCK.lock();
                        context.executeSql(call.originSql);
                        LOG.info("{}:{}", command, args);
                    } finally {
                        if (LOCK.isHeldByCurrentThread()) {
                            LOCK.unlock();
                        }
                    }
            }
        }

        if (hasInsert) {
            TableResult result = statementSet.execute();
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
            throw new RuntimeException(
                "No 'INSERT' statement to trigger the execution of the Flink job.");
        }

        LOG.info(
            "\n\n\n==============flinkSql==============\n\n {}\n\n============================\n\n\n",
            flinkSql);
    }
}
