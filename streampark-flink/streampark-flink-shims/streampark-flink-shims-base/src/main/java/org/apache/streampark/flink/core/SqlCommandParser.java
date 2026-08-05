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

import org.apache.streampark.common.enums.FlinkSqlValidationFailedType;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;

/** Parser for Flink SQL commands. */
public final class SqlCommandParser {

    private static final String SQL_EMPTY_ERROR = "verify failed: flink sql cannot be empty.";

    private SqlCommandParser() {
    }

    public static List<SqlCommandCall> parseSQL(String sql, Consumer<FlinkSqlValidationResult> callback) {
        if (StringUtils.isBlank(sql)) {
            if (callback != null) {
                callback.accept(
                    FlinkSqlValidationResult.builder()
                        .success(false)
                        .failedType(FlinkSqlValidationFailedType.VERIFY_FAILED)
                        .exception(SQL_EMPTY_ERROR)
                        .build());
                return Collections.emptyList();
            }
            throw new IllegalArgumentException(SQL_EMPTY_ERROR);
        }

        List<SqlSegment> sqlSegments = SqlSplitter.splitSql(sql);
        if (sqlSegments.isEmpty()) {
            if (callback != null) {
                callback.accept(
                    FlinkSqlValidationResult.builder()
                        .success(false)
                        .failedType(FlinkSqlValidationFailedType.VERIFY_FAILED)
                        .exception(SQL_EMPTY_ERROR)
                        .build());
                return Collections.emptyList();
            }
            throw new IllegalArgumentException(SQL_EMPTY_ERROR);
        }

        List<SqlCommandCall> calls = new ArrayList<>();
        for (SqlSegment segment : sqlSegments) {
            Optional<SqlCommandCall> parsed = parseLine(segment);
            if (parsed.isPresent()) {
                calls.add(parsed.get());
            } else if (callback != null) {
                callback.accept(
                    FlinkSqlValidationResult.builder()
                        .success(false)
                        .failedType(FlinkSqlValidationFailedType.UNSUPPORTED_SQL)
                        .lineStart(segment.start)
                        .lineEnd(segment.end)
                        .exception("unsupported sql")
                        .sql(segment.sql)
                        .build());
            } else {
                throw new UnsupportedOperationException("unsupported sql: " + segment.sql);
            }
        }

        if (calls.isEmpty()) {
            if (callback != null) {
                callback.accept(
                    FlinkSqlValidationResult.builder()
                        .success(false)
                        .failedType(FlinkSqlValidationFailedType.VERIFY_FAILED)
                        .exception("flink sql syntax error, no executable sql")
                        .build());
                return Collections.emptyList();
            }
            throw new UnsupportedOperationException("flink sql syntax error, no executable sql");
        }
        return calls;
    }

    private static Optional<SqlCommandCall> parseLine(SqlSegment sqlSegment) {
        SqlCommand sqlCommand = SqlCommand.get(sqlSegment.sql.trim());
        if (sqlCommand == null) {
            return Optional.empty();
        }

        Matcher matcher = sqlCommand.getMatcher();
        String[] groups = new String[matcher.groupCount()];
        for (int i = 0; i < groups.length; i++) {
            groups[i] = matcher.group(i + 1);
        }

        return sqlCommand
            .getConverter()
            .convert(groups)
            .map(
                operands -> new SqlCommandCall(
                    sqlSegment.start,
                    sqlSegment.end,
                    sqlCommand,
                    operands,
                    sqlSegment.sql.trim()));
    }
}
