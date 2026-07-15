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

package org.apache.streampark.spark.core.util;

import org.apache.streampark.common.enums.SparkSqlValidationFailedType;
import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.common.util.StreamParkLoggerFactory;

import org.apache.streampark.shaded.org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Validates Spark SQL syntax. */
public final class SparkSqlValidator {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory()
            .getLogger(SparkSqlValidator.class.getName());

    private static final String SPARK_SQL_PARSER_CLASS =
        "org.apache.spark.sql.execution.SparkSqlParser";

    private static final Pattern SYNTAX_ERROR_REGEXP =
        Pattern.compile(".*\\(line\\s(\\d+),\\spos\\s(\\d+)\\).*");

    private SparkSqlValidator() {
    }

    public static SparkSqlValidationResult verifySql(String sql) {
        final SparkSqlValidationResult[] earlyReturn = new SparkSqlValidationResult[1];
        List<SqlCommandCall> sqlCommands =
            SqlCommandParser.parseSQL(sql, result -> earlyReturn[0] = result);
        if (earlyReturn[0] != null) {
            return earlyReturn[0];
        }

        try {
            Class<?> parserClass = Class.forName(SPARK_SQL_PARSER_CLASS);
            Object parser = parserClass.getConstructor().newInstance();
            Method method = parser.getClass().getMethod("parsePlan", String.class);
            method.setAccessible(true);
            for (SqlCommandCall call : sqlCommands) {
                try {
                    method.invoke(parser, call.originSql);
                } catch (Exception e) {
                    String exception = ExceptionUtils.stringifyException(e);
                    int causedByIndex = exception.indexOf("Caused by:");
                    String causedBy =
                        causedByIndex >= 0 ? exception.substring(causedByIndex) : exception;
                    String cleanUpError = exception.replaceAll("[\r\n]", "");
                    Matcher syntaxMatcher = SYNTAX_ERROR_REGEXP.matcher(cleanUpError);
                    if (syntaxMatcher.find()) {
                        int line = Integer.parseInt(syntaxMatcher.group(1));
                        int column = Integer.parseInt(syntaxMatcher.group(2));
                        int errorLine = call.lineStart + line - 1;
                        return SparkSqlValidationResult.builder()
                            .success(false)
                            .failedType(SparkSqlValidationFailedType.SYNTAX_ERROR)
                            .lineStart(call.lineStart)
                            .lineEnd(call.lineEnd)
                            .errorLine(errorLine)
                            .errorColumn(column)
                            .sql(call.originSql)
                            .exception(
                                causedBy.replaceAll(
                                    "at\\sline\\s" + line, "at line " + errorLine))
                            .build();
                    }
                    return SparkSqlValidationResult.builder()
                        .success(false)
                        .failedType(SparkSqlValidationFailedType.SYNTAX_ERROR)
                        .lineStart(call.lineStart)
                        .lineEnd(call.lineEnd)
                        .sql(call.originSql)
                        .exception(causedBy)
                        .build();
                }
            }
        } catch (Exception e) {
            return SparkSqlValidationResult.builder()
                .success(false)
                .failedType(SparkSqlValidationFailedType.CLASS_ERROR)
                .exception(ExceptionUtils.stringifyException(e))
                .build();
        }
        return SparkSqlValidationResult.ok();
    }
}
