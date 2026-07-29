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

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.common.util.SystemPropertyUtils;
import org.apache.streampark.flink.core.FlinkTableInitializer;
import org.apache.streampark.flink.core.SqlCommand;
import org.apache.streampark.flink.core.SqlCommandCall;
import org.apache.streampark.flink.core.SqlCommandParser;
import org.apache.streampark.flink.core.StreamTableContext;
import org.apache.streampark.flink.core.TableContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.ExecutionOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Flink SQL job CLI entry point. */
public final class SqlClient {

    private SqlClient() {
    }

    public static void main(String[] args) {
        List<String> arguments = new ArrayList<>(Arrays.asList(args));

        ParameterTool parameterTool = ParameterTool.fromArgs(args);

        String sqlKey = ConfigKeys.KEY_FLINK_SQL();
        String sql = parameterTool.get(sqlKey);
        if (StringUtils.isBlank(sql)) {
            throw new IllegalArgumentException("Usage: flink sql cannot be null");
        }
        String flinkSql;
        try {
            flinkSql = DeflaterUtils.unzipString(sql);
        } catch (Exception e) {
            throw new IllegalArgumentException("Usage: flink sql is invalid or null, please check");
        }

        List<SqlCommandCall> sets = new ArrayList<>();
        for (SqlCommandCall call : SqlCommandParser.parseSQL(flinkSql)) {
            if (call.command() == SqlCommand.SET) {
                sets.add(call);
            }
        }

        String defaultMode = RuntimeExecutionMode.STREAMING.name();
        String mode =
            resolveExecutionMode(parameterTool, sets, arguments, defaultMode);

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
                                       ParameterTool parameterTool,
                                       List<SqlCommandCall> sets,
                                       List<String> arguments,
                                       String defaultMode) {
        for (SqlCommandCall setCall : sets) {
            if (setCall.operands().length > 0
                && ExecutionOptions.RUNTIME_MODE.key().equals(setCall.operands()[0])) {
                String runtimeMode = setCall.operands()[1].toUpperCase();
                arguments.add("-D" + ExecutionOptions.RUNTIME_MODE.key() + "=" + runtimeMode);
                return runtimeMode;
            }
        }

        String configuredMode = parameterTool.get(ExecutionOptions.RUNTIME_MODE.key(), null);
        if (configuredMode != null) {
            return configuredMode;
        }

        String appConf = parameterTool.get(ConfigKeys.KEY_APP_CONF(), null);
        String runtimeMode;
        if (appConf == null) {
            runtimeMode = defaultMode;
        } else {
            Map<String, String> parameter =
                PropertiesUtils.fromYamlText(DeflaterUtils.unzipString(appConf.substring(7)));
            runtimeMode =
                parameter
                    .getOrDefault(ConfigKeys.KEY_FLINK_TABLE_MODE(), defaultMode)
                    .toUpperCase();
        }
        arguments.add("-D" + ExecutionOptions.RUNTIME_MODE.key() + "=" + runtimeMode);
        return runtimeMode;
    }

    private static final class BatchSqlApp {

        private BatchSqlApp() {
        }

        static void run(String[] args) {
            SystemPropertyUtils.setAppHome(ConfigKeys.KEY_APP_HOME(), SqlClient.class);
            TableContext context = new TableContext(FlinkTableInitializer.initialize(args, null));
            context.sql();
            context.start();
        }
    }

    private static final class StreamSqlApp {

        private StreamSqlApp() {
        }

        static void run(String[] args) {
            SystemPropertyUtils.setAppHome(ConfigKeys.KEY_APP_HOME(), SqlClient.class);
            StreamTableContext context =
                new StreamTableContext(FlinkTableInitializer.initialize(args, null, null));
            context.sql();
            context.start();
        }
    }
}
