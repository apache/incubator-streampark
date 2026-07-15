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
import org.apache.streampark.flink.core.SqlCommand;
import org.apache.streampark.flink.core.SqlCommandCall;
import org.apache.streampark.flink.core.SqlCommandParser;
import org.apache.streampark.flink.core.scala.FlinkStreamTable;
import org.apache.streampark.flink.core.scala.FlinkTable;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.ExecutionOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Flink SQL client entry point. */
public final class SqlClient {

    private SqlClient() {
    }

    public static void main(String[] args) {
        List<String> arguments = new ArrayList<>();
        for (String arg : args) {
            arguments.add(arg);
        }

        ParameterTool parameterTool = ParameterTool.fromArgs(args);

        String sql = parameterTool.get(ConfigKeys.KEY_FLINK_SQL);
        if (StringUtils.isBlank(sql)) {
            throw new IllegalArgumentException("Usage: flink sql cannot be null");
        }
        String flinkSql;
        try {
            flinkSql = DeflaterUtils.unzipString(sql);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Usage: flink sql is invalid or null, please check", e);
        }

        List<SqlCommandCall> sets = new ArrayList<>();
        for (SqlCommandCall call : SqlCommandParser.parseSQL(flinkSql, null)) {
            if (call.command == SqlCommand.SET) {
                sets.add(call);
            }
        }

        String defaultMode = RuntimeExecutionMode.STREAMING.name();
        String mode;

        java.util.Optional<SqlCommandCall> runtimeModeSet =
            sets.stream()
                .filter(e -> ExecutionOptions.RUNTIME_MODE.key().equals(e.operands[0]))
                .findFirst();

        if (runtimeModeSet.isPresent()) {
            mode = runtimeModeSet.get().operands[1].toUpperCase();
            arguments.add("-D" + ExecutionOptions.RUNTIME_MODE.key() + "=" + mode);
        } else {
            String configuredMode = parameterTool.get(ExecutionOptions.RUNTIME_MODE.key(), null);
            if (configuredMode == null) {
                String appConf = parameterTool.get(ConfigKeys.KEY_APP_CONF, null);
                if (appConf == null) {
                    mode = defaultMode;
                } else {
                    Map<String, String> parameter =
                        PropertiesUtils.fromYamlText(DeflaterUtils.unzipString(appConf.substring(7)));
                    mode =
                        parameter
                            .getOrDefault(ConfigKeys.KEY_FLINK_TABLE_MODE, defaultMode)
                            .toUpperCase();
                }
                arguments.add("-D" + ExecutionOptions.RUNTIME_MODE.key() + "=" + mode);
            } else {
                mode = configuredMode;
            }
        }

        String[] argumentArray = arguments.toArray(new String[0]);
        switch (mode) {
            case "STREAMING":
            case "AUTOMATIC":
                new StreamSqlApp().main(argumentArray);
                break;
            case "BATCH":
                new BatchSqlApp().main(argumentArray);
                break;
            default:
                throw new IllegalArgumentException(
                    "Usage: runtime execution-mode invalid, optional [STREAMING|BATCH|AUTOMATIC]");
        }
    }

    private static final class BatchSqlApp extends FlinkTable {

        @Override
        protected void handle() {
            context.sql(null);
        }
    }

    private static final class StreamSqlApp extends FlinkStreamTable {

        @Override
        protected void handle() {
            context.sql(null);
        }
    }
}
