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

import org.apache.streampark.flink.core.SqlCommand;
import org.apache.streampark.flink.core.SqlCommandCall;

import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.ExecutionOptions;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                ParameterTool.fromArgs(new String[0]),
                sets,
                arguments,
                RuntimeExecutionMode.STREAMING.name());

        assertEquals("BATCH", mode);
        assertTrue(
            arguments.contains("-D" + ExecutionOptions.RUNTIME_MODE.key() + "=BATCH"));
    }

    @Test
    void resolveExecutionModeFromDynamicProperty() {
        List<String> arguments = new ArrayList<>();
        Map<String, String> properties = new HashMap<>();
        properties.put(ExecutionOptions.RUNTIME_MODE.key(), "BATCH");
        ParameterTool parameterTool =
            ParameterTool.fromSystemProperties().mergeWith(ParameterTool.fromMap(properties));

        String mode =
            SqlClient.resolveExecutionMode(
                parameterTool,
                Collections.emptyList(),
                arguments,
                RuntimeExecutionMode.STREAMING.name());

        assertEquals("BATCH", mode);
        assertTrue(arguments.isEmpty());
    }
}
