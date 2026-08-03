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

package org.apache.streampark.flink.core.test;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.flink.core.FlinkSqlExecutor;
import org.apache.streampark.flink.core.FlinkTableInitializer;
import org.apache.streampark.flink.core.StreamTableContext;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class FlinkSqlExecuteTest {

    private void execute(String sql, Consumer<String> callback) {
        List<String> args = new ArrayList<>();
        args.add(ConfigKeys.KEY_FLINK_SQL(ConfigKeys.PARAM_PREFIX()));
        args.add(DeflaterUtils.zipString(sql));
        args.add(ConfigKeys.KEY_FLINK_CONF(ConfigKeys.PARAM_PREFIX()));
        args.add(DeflaterUtils.zipString("execution.runtime-mode: streaming"));
        StreamTableContext context =
            new StreamTableContext(FlinkTableInitializer.initialize(args.toArray(new String[0]), null, null));
        FlinkSqlExecutor.executeSql(ConfigKeys.KEY_FLINK_SQL(), context.parameter, context);
    }

    @Test
    void execute() {
        execute(
            "-- set -------\n"
                + "set 'table.local-time-zone' = 'GMT+08:00';\n"
                + "\n"
                + "-- reset -----\n"
                + "reset 'table.local-time-zone';\n"
                + "reset;\n"
                + "\n"
                + "CREATE temporary TABLE source_kafka1(\n"
                + "    `id` int,\n"
                + "    `name` string,\n"
                + "    `age` int\n"
                + ") WITH (\n"
                + "    'connector' = 'datagen',\n"
                + "    'rows-per-second' = '1',\n"
                + "    'number-of-rows' = '1'\n"
                + ");\n"
                + "\n"
                + "create table sink_kafka1(\n"
                + "    `id` int,\n"
                + "    `name` string,\n"
                + "    `age` int\n"
                + ") with (\n"
                + "    'connector' = 'print'\n"
                + ");\n"
                + "\n"
                + "insert into sink_kafka1\n"
                + "select id, name, age\n"
                + "from source_kafka1;\n"
                + "\n",
            System.out::println);
    }
}
