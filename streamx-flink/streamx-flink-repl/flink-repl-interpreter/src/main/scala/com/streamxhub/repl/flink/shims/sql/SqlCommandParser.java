/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.repl.flink.shims.sql;

import com.streamxhub.repl.flink.shims.FlinkShims;

import java.util.Optional;

/**
 * Simple parser for determining the type of command and its parameters.
 * All the SqlCommands should be put into this class, and the parsing logic needs to be put ito FlinkShims
 * because each version of flink has different sql syntax support.
 * @author benjobs
 */
public final class SqlCommandParser {

    private FlinkShims flinkShims;
    private Object tableEnv;

    public SqlCommandParser(FlinkShims flinkShims, Object tableEnv) {
        this.flinkShims = flinkShims;
        this.tableEnv = tableEnv;
    }

    public Optional<SqlCommandCall> parse(String stmt) {
        return flinkShims.parseSql(tableEnv, stmt);
    }

}
