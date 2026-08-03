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
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Parses Flink SQL scripts into {@link SqlCommandCall} instances. */
public final class SqlCommandParser extends org.apache.streampark.common.util.LoggerSupport {

    private SqlCommandParser() {
    }

    public static List<SqlCommandCall> parseSQL(String sql) {
        return parseSQL(sql, null);
    }

    public static List<SqlCommandCall> parseSQL(
                                                String sql,
                                                Consumer<FlinkSqlValidationResult> validationCallback) {
        String sqlEmptyError = "verify failed: flink sql cannot be empty.";
        if (StringUtils.isBlank(sql)) {
            throw new IllegalArgumentException(sqlEmptyError);
        }
        List<SqlSegment> sqlSegments = SqlSplitter.splitSql(sql);
        if (sqlSegments.isEmpty()) {
            if (validationCallback != null) {
                validationCallback.accept(
                    new FlinkSqlValidationResult()
                        .withSuccess(false)
                        .withFailedType(FlinkSqlValidationFailedType.VERIFY_FAILED)
                        .withException(sqlEmptyError));
                return null;
            }
            throw new IllegalArgumentException(sqlEmptyError);
        }

        List<SqlCommandCall> calls = new ArrayList<>();
        for (SqlSegment segment : sqlSegments) {
            Optional<SqlCommandCall> call = parseLine(segment);
            if (call.isPresent()) {
                calls.add(call.get());
            } else if (validationCallback != null) {
                validationCallback.accept(
                    new FlinkSqlValidationResult()
                        .withSuccess(false)
                        .withFailedType(FlinkSqlValidationFailedType.UNSUPPORTED_SQL)
                        .withLineStart(segment.start())
                        .withLineEnd(segment.end())
                        .withException("unsupported sql")
                        .withSql(segment.sql()));
                return null;
            } else {
                throw new UnsupportedOperationException("unsupported sql: " + segment.sql());
            }
        }

        if (calls.isEmpty()) {
            if (validationCallback != null) {
                validationCallback.accept(
                    new FlinkSqlValidationResult()
                        .withSuccess(false)
                        .withFailedType(FlinkSqlValidationFailedType.VERIFY_FAILED)
                        .withException("flink sql syntax error, no executable sql"));
                return null;
            }
            throw new UnsupportedOperationException("flink sql syntax error, no executable sql");
        }
        return calls;
    }

    private static Optional<SqlCommandCall> parseLine(SqlSegment sqlSegment) {
        SqlCommand sqlCommand = SqlCommand.get(sqlSegment.sql().trim());
        if (sqlCommand == null) {
            return Optional.empty();
        }
        String[] groups = new String[sqlCommand.matcher().groupCount()];
        for (int i = 0; i < groups.length; i++) {
            groups[i] = sqlCommand.matcher().group(i + 1);
        }
        return sqlCommand
            .convert(groups)
            .map(
                operands -> new SqlCommandCall(
                    sqlSegment.start(),
                    sqlSegment.end(),
                    sqlCommand,
                    operands,
                    sqlSegment.sql().trim()));
    }
}
