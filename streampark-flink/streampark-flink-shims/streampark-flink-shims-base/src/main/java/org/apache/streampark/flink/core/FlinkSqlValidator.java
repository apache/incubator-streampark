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
import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.common.util.StreamParkLoggerFactory;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.flink.sql.parser.validate.FlinkSqlConformance;
import org.apache.flink.table.api.SqlDialect;
import org.apache.flink.table.api.config.TableConfigOptions;
import org.apache.flink.table.planner.delegation.FlinkSqlParserFactories;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Validates Flink SQL syntax. */
public final class FlinkSqlValidator {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory()
            .getLogger(FlinkSqlValidator.class.getName());

    private static final String FLINK112_CALCITE_PARSER_CLASS =
        "org.apache.flink.table.planner.calcite.CalciteParser";

    private static final String FLINK113_PLUS_CALCITE_PARSER_CLASS =
        "org.apache.flink.table.planner.parse.CalciteParser";

    private static final Pattern SYNTAX_ERROR_REGEXP =
        Pattern.compile(".*at\\sline\\s(\\d+),\\scolumn\\s(\\d+).*");

    private static final Map<String, SqlParser.Config> SQL_PARSER_CONFIG_MAP;

    static {
        Map<String, SqlParser.Config> configMap = new HashMap<>();
        configMap.put(SqlDialect.DEFAULT.name(), getConfig(SqlDialect.DEFAULT));
        configMap.put(SqlDialect.HIVE.name(), getConfig(SqlDialect.HIVE));
        SQL_PARSER_CONFIG_MAP = Collections.unmodifiableMap(configMap);
    }

    private FlinkSqlValidator() {
    }

    public static FlinkSqlValidationResult verifySql(String sql) {
        final FlinkSqlValidationResult[] earlyReturn = new FlinkSqlValidationResult[1];
        List<SqlCommandCall> sqlCommands =
            SqlCommandParser.parseSQL(sql, result -> earlyReturn[0] = result);
        if (earlyReturn[0] != null) {
            return earlyReturn[0];
        }

        String sqlDialect = SqlDialect.DEFAULT.name().toLowerCase();
        boolean hasInsert = false;
        for (SqlCommandCall call : sqlCommands) {
            String args = call.operands[0];
            SqlCommand command = call.command;
            switch (command) {
                case SET:
                    if (TableConfigOptions.TABLE_SQL_DIALECT.key().equals(args)) {
                        sqlDialect = call.operands[call.operands.length - 1];
                    }
                    break;
                case RESET:
                    break;
                case BEGIN_STATEMENT_SET:
                case END_STATEMENT_SET:
                    LOG.warn("SQL Client Syntax: {} ", call.command.getName());
                    break;
                default:
                    if (command == SqlCommand.INSERT) {
                        hasInsert = true;
                    }
                    try {
                        Class<?> calciteClass;
                        try {
                            calciteClass = Class.forName(FLINK112_CALCITE_PARSER_CLASS);
                        } catch (ClassNotFoundException e) {
                            calciteClass = Class.forName(FLINK113_PLUS_CALCITE_PARSER_CLASS);
                        }
                        switch (sqlDialect.toUpperCase()) {
                            case "HIVE":
                                break;
                            case "DEFAULT":
                                Object parser =
                                    calciteClass
                                        .getConstructor(SqlParser.Config.class)
                                        .newInstance(
                                            SQL_PARSER_CONFIG_MAP.get(
                                                sqlDialect.toUpperCase()));
                                Method method =
                                    parser.getClass().getDeclaredMethod("parse", String.class);
                                method.setAccessible(true);
                                method.invoke(parser, call.originSql);
                                break;
                            default:
                                throw new UnsupportedOperationException(
                                    "unsupported dialect: " + sqlDialect);
                        }
                    } catch (Exception e) {
                        String exception = ExceptionUtils.stringifyException(e);
                        int causedByIndex = exception.indexOf("Caused by:");
                        String causedBy =
                            causedByIndex >= 0
                                ? exception.substring(causedByIndex)
                                : exception;
                        String cleanUpError = exception.replaceAll("[\r\n]", "");
                        Matcher syntaxMatcher = SYNTAX_ERROR_REGEXP.matcher(cleanUpError);
                        if (syntaxMatcher.find()) {
                            int line = Integer.parseInt(syntaxMatcher.group(1));
                            int column = Integer.parseInt(syntaxMatcher.group(2));
                            int errorLine = call.lineStart + line - 1;
                            return FlinkSqlValidationResult.builder()
                                .success(false)
                                .failedType(FlinkSqlValidationFailedType.SYNTAX_ERROR)
                                .lineStart(call.lineStart)
                                .lineEnd(call.lineEnd)
                                .errorLine(errorLine)
                                .errorColumn(column)
                                .sql(call.originSql)
                                .exception(
                                    causedBy.replaceAll(
                                        "at\\sline\\s" + line,
                                        "at line " + errorLine))
                                .build();
                        }
                        return FlinkSqlValidationResult.builder()
                            .success(false)
                            .failedType(FlinkSqlValidationFailedType.SYNTAX_ERROR)
                            .lineStart(call.lineStart)
                            .lineEnd(call.lineEnd)
                            .sql(call.originSql)
                            .exception(causedBy)
                            .build();
                    }
            }
        }

        if (hasInsert) {
            return FlinkSqlValidationResult.ok();
        }
        return FlinkSqlValidationResult.builder()
            .success(false)
            .failedType(FlinkSqlValidationFailedType.SYNTAX_ERROR)
            .lineStart(sqlCommands.get(0).lineStart)
            .lineEnd(sqlCommands.get(sqlCommands.size() - 1).lineEnd)
            .exception("No 'INSERT' statement to trigger the execution of the Flink job.")
            .build();
    }

    private static SqlParser.Config getConfig(SqlDialect sqlDialect) {
        FlinkSqlConformance conformance;
        if (sqlDialect == SqlDialect.HIVE) {
            try {
                conformance = FlinkSqlConformance.DEFAULT;
            } catch (NoSuchFieldError e) {
                conformance = FlinkSqlConformance.DEFAULT;
            } catch (Throwable e) {
                throw new IllegalArgumentException("Init Flink sql Dialect error: ", e);
            }
        } else if (sqlDialect == SqlDialect.DEFAULT) {
            conformance = FlinkSqlConformance.DEFAULT;
        } else {
            throw new UnsupportedOperationException("Unsupported sqlDialect: " + sqlDialect);
        }
        return SqlParser.config()
            .withParserFactory(FlinkSqlParserFactories.create(conformance))
            .withConformance(conformance)
            .withLex(Lex.JAVA)
            .withIdentifierMaxLength(256);
    }
}
