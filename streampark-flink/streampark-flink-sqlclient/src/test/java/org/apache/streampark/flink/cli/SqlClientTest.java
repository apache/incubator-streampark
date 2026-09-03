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
import org.apache.streampark.common.configuration.FlinkOptions;
import org.apache.streampark.common.configuration.option.ApplicationOptions;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.flink.configuration.FlinkJobParameters;
import org.apache.streampark.flink.core.SqlCommand;
import org.apache.streampark.flink.core.SqlCommandCall;

import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.configuration.ExecutionOptions;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlClientTest {

    @Test
    void resolveExecutionModeFromSetCommand() {
        List<String> arguments = new ArrayList<>();
        List<SqlCommandCall> sets =
            Collections.singletonList(
                new SqlCommandCall(
                    1,
                    1,
                    SqlCommand.SET,
                    new String[]{ExecutionOptions.RUNTIME_MODE.key(), "batch"},
                    "SET 'execution.runtime-mode' = 'batch'"));

        String mode =
            SqlClient.resolveExecutionMode(
                FlinkJobParameters.of(CommandLineParser.parse(new String[0])),
                sets,
                arguments,
                RuntimeExecutionMode.STREAMING.name());

        assertEquals("BATCH", mode);
        assertTrue(arguments.contains("--" + ExecutionOptions.RUNTIME_MODE.key()));
        assertTrue(arguments.contains("BATCH"));
    }

    @Test
    void resolveExecutionModeFromDynamicProperty() {
        List<String> arguments = new ArrayList<>();
        FlinkJobParameters parameters =
            FlinkJobParameters.of(
                CommandLineParser.parse(
                    new String[]{"--" + ExecutionOptions.RUNTIME_MODE.key(), "BATCH"}));

        String mode =
            SqlClient.resolveExecutionMode(
                parameters,
                Collections.emptyList(),
                arguments,
                RuntimeExecutionMode.STREAMING.name());

        assertEquals("BATCH", mode);
        assertTrue(arguments.isEmpty());
    }

    @Test
    void resolveExecutionModeFromAppConfYaml() {
        List<String> arguments = new ArrayList<>();
        String yamlContent = FlinkOptions.TABLE_MODE.key() + ": batch\n";
        String appConf = "yaml://" + DeflaterUtils.zipString(yamlContent);
        FlinkJobParameters parameters =
            FlinkJobParameters.of(
                CommandLineParser.parse(
                    new String[]{"--" + ApplicationOptions.CONFIG.key(), appConf}));

        String mode =
            SqlClient.resolveExecutionMode(
                parameters,
                Collections.emptyList(),
                arguments,
                RuntimeExecutionMode.STREAMING.name());

        assertEquals("BATCH", mode);
        assertTrue(arguments.contains("--" + ExecutionOptions.RUNTIME_MODE.key()));
        assertTrue(arguments.contains("BATCH"));
    }
}
