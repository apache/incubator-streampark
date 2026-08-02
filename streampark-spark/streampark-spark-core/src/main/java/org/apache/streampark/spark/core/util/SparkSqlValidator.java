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
import org.apache.streampark.common.util.LoggerSupport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Validates Spark SQL syntax using Spark's internal parser. */
public final class SparkSqlValidator extends LoggerSupport {

    private static final String SPARK_SQL_PARSER_CLASS = "org.apache.spark.sql.execution.SparkSqlParser";

    private static final Pattern SYNTAX_ERROR_REGEXP =
        Pattern.compile("\\(line\\s+(\\d+),\\s+pos\\s+(\\d+)\\)");

    private SparkSqlValidator() {
    }

    public static SparkSqlValidationResult verifySql(String sql) {
        SparkSqlValidationResult[] earlyResult = new SparkSqlValidationResult[1];
        List<SqlCommandCall> sqlCommands =
            SqlCommandParser.parseSQL(sql, result -> earlyResult[0] = result);
        if (earlyResult[0] != null) {
            return earlyResult[0];
        }

        try {
            Class<?> parserClass = Class.forName(SPARK_SQL_PARSER_CLASS);
            Object parser = parserClass.getConstructor().newInstance();
            Method method = parser.getClass().getMethod("parsePlan", String.class);
            method.setAccessible(true);

            for (SqlCommandCall call : sqlCommands) {
                SparkSqlValidationResult syntaxError = validateCommand(method, parser, call);
                if (syntaxError != null) {
                    return syntaxError;
                }
            }
        } catch (ReflectiveOperationException e) {
            return new SparkSqlValidationResult()
                .withSuccess(false)
                .withFailedType(SparkSqlValidationFailedType.CLASS_ERROR)
                .withException(ExceptionUtils.stringifyException(e));
        }
        return new SparkSqlValidationResult();
    }

    private static SparkSqlValidationResult validateCommand(
                                                            Method method,
                                                            Object parser,
                                                            SqlCommandCall call) {
        try {
            method.invoke(parser, call.originSql());
            return null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            Throwable cause = e instanceof InvocationTargetException
                ? ((InvocationTargetException) e).getTargetException()
                : e;
            String exception = ExceptionUtils.stringifyException(cause);
            int causedByIndex = exception.indexOf("Caused by:");
            String causedBy = causedByIndex >= 0 ? exception.substring(causedByIndex) : exception;
            String cleanUpError = exception.replaceAll("[\r\n]", "");
            Matcher matcher = SYNTAX_ERROR_REGEXP.matcher(cleanUpError);
            if (matcher.find()) {
                int line = Integer.parseInt(matcher.group(1));
                int column = Integer.parseInt(matcher.group(2));
                int errorLine = call.lineStart() + line - 1;
                return new SparkSqlValidationResult()
                    .withSuccess(false)
                    .withFailedType(SparkSqlValidationFailedType.SYNTAX_ERROR)
                    .withLineStart(call.lineStart())
                    .withLineEnd(call.lineEnd())
                    .withErrorLine(errorLine)
                    .withErrorColumn(column)
                    .withSql(call.originSql())
                    .withException(
                        causedBy.replaceAll("at\\sline\\s" + line, "at line " + errorLine));
            }
            return new SparkSqlValidationResult()
                .withSuccess(false)
                .withFailedType(SparkSqlValidationFailedType.SYNTAX_ERROR)
                .withLineStart(call.lineStart())
                .withLineEnd(call.lineEnd())
                .withSql(call.originSql())
                .withException(causedBy);
        }
    }
}
