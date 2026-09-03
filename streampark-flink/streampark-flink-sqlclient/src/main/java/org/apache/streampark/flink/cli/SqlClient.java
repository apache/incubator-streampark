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

package org.apache.streampark.flink.cli;

import org.apache.streampark.common.configuration.CommandLineParser;
import org.apache.streampark.common.configuration.Configuration;
import org.apache.streampark.common.configuration.ConfigurationFormat;
import org.apache.streampark.common.configuration.ConfigurationParser;
import org.apache.streampark.common.configuration.FlinkOptions;
import org.apache.streampark.common.configuration.option.ApplicationOptions;
import org.apache.streampark.common.configuration.option.CoreOptions;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.SystemPropertyUtils;
import org.apache.streampark.flink.configuration.FlinkJobParameters;
import org.apache.streampark.flink.core.FlinkTableInitializer;
import org.apache.streampark.flink.core.SqlCommand;
import org.apache.streampark.flink.core.SqlCommandCall;
import org.apache.streampark.flink.core.SqlCommandParser;
import org.apache.streampark.flink.core.StreamTableContext;
import org.apache.streampark.flink.core.TableContext;
import org.apache.streampark.flink.core.bean.StreamTableContextSpec;
import org.apache.streampark.flink.core.bean.TableContextSpec;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.configuration.ExecutionOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Flink SQL job CLI entry point. */
public final class SqlClient {

    private SqlClient() {
    }

    public static void main(String[] args) {
        List<String> arguments = new ArrayList<>(Arrays.asList(args));

        FlinkJobParameters parameters = FlinkJobParameters.of(CommandLineParser.parse(args));

        String sql = parameters.get(ApplicationOptions.SQL);
        if (StringUtils.isBlank(sql)) {
            throw new IllegalArgumentException("Usage: flink sql cannot be null");
        }
        String flinkSql = DeflaterUtils.unzipString(sql);
        if (StringUtils.isBlank(flinkSql)) {
            throw new IllegalArgumentException("Usage: flink sql is invalid or null, please check");
        }

        List<SqlCommandCall> sets = new ArrayList<>();
        for (SqlCommandCall call : SqlCommandParser.parseSQL(flinkSql, null)) {
            if (call.command == SqlCommand.SET) {
                sets.add(call);
            }
        }

        String defaultMode = RuntimeExecutionMode.STREAMING.name();
        String mode = resolveExecutionMode(parameters, sets, arguments, defaultMode);

        switch (mode) {
            case "STREAMING":
            case "AUTOMATIC":
                StreamSqlApp.run(arguments.toArray(new String[0]));
                break;
            case "BATCH":
                BatchSqlApp.run(arguments.toArray(new String[0]));
                break;
            default:
                throw new IllegalArgumentException(
                    "Usage: runtime execution-mode invalid, optional [STREAMING|BATCH|AUTOMATIC]");
        }
    }

    static String resolveExecutionMode(
                                       FlinkJobParameters parameters,
                                       List<SqlCommandCall> sets,
                                       List<String> arguments,
                                       String defaultMode) {
        for (SqlCommandCall setCall : sets) {
            if (setCall.operands.length >= 2
                && ExecutionOptions.RUNTIME_MODE.key().equals(setCall.operands[0])) {
                String runtimeMode = setCall.operands[1].toUpperCase();
                setArgument(arguments, ExecutionOptions.RUNTIME_MODE.key(), runtimeMode);
                return runtimeMode;
            }
        }

        String configuredMode = parameters.get(ExecutionOptions.RUNTIME_MODE.key(), null);
        if (configuredMode != null) {
            return configuredMode;
        }

        String appConf = parameters.getOptional(FlinkOptions.APPLICATION_CONFIG).orElse(null);
        String runtimeMode;
        if (appConf == null) {
            runtimeMode = defaultMode;
        } else {
            Configuration applicationConfiguration = parseApplicationConfiguration(appConf);
            runtimeMode =
                applicationConfiguration
                    .getOptional(FlinkOptions.TABLE_MODE)
                    .orElse(defaultMode)
                    .toUpperCase();
        }
        setArgument(arguments, ExecutionOptions.RUNTIME_MODE.key(), runtimeMode);
        return runtimeMode;
    }

    private static Configuration parseApplicationConfiguration(String resource) {
        if (resource.startsWith("yaml://")) {
            return ConfigurationParser.parse(
                DeflaterUtils.unzipString(resource.substring("yaml://".length())),
                ConfigurationFormat.YAML,
                "inline SQL application configuration");
        }
        return ConfigurationParser.parse(java.nio.file.Path.of(resource));
    }

    private static void setArgument(List<String> arguments, String key, String value) {
        String option = CommandLineParser.LONG_OPTION_PREFIX + key;
        for (int index = 0; index < arguments.size(); index++) {
            if (option.equals(arguments.get(index))) {
                if (index + 1 < arguments.size()) {
                    arguments.set(index + 1, value);
                    return;
                }
                break;
            }
            if (arguments.get(index).startsWith(option + "=")) {
                arguments.set(index, option + "=" + value);
                return;
            }
        }
        arguments.add(option);
        arguments.add(value);
    }

    private static final class BatchSqlApp {

        private BatchSqlApp() {
        }

        static void run(String[] args) {
            SystemPropertyUtils.setAppHome(CoreOptions.APP_HOME.key(), SqlClient.class);
            TableContextSpec contextConfig = FlinkTableInitializer.initializeTable(args);
            TableContext context = new TableContext(contextConfig);
            context.sql();
            context.start();
        }
    }

    private static final class StreamSqlApp {

        private StreamSqlApp() {
        }

        static void run(String[] args) {
            SystemPropertyUtils.setAppHome(CoreOptions.APP_HOME.key(), SqlClient.class);
            StreamTableContextSpec contextConfig = FlinkTableInitializer.initializeStreamTable(args);
            StreamTableContext context = new StreamTableContext(contextConfig);
            context.sql();
            context.start();
        }
    }
}
