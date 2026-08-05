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
        Pattern.compile("at\\sline\\s(\\d+),\\scolumn\\s(\\d+)");

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
        if (sqlCommands == null || sqlCommands.isEmpty()) {
            return FlinkSqlValidationResult.builder()
                .success(false)
                .failedType(FlinkSqlValidationFailedType.VERIFY_FAILED)
                .exception("verify failed: flink sql cannot be empty.")
                .build();
        }

        ValidationContext context = new ValidationContext();
        for (SqlCommandCall call : sqlCommands) {
            FlinkSqlValidationResult validationError = validateCall(call, context);
            if (validationError != null) {
                return validationError;
            }
        }

        if (context.hasInsert) {
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

    private static FlinkSqlValidationResult validateCall(SqlCommandCall call, ValidationContext context) {
        switch (call.command) {
            case SET:
                context.updateDialect(call);
                return null;
            case RESET:
                return null;
            case BEGIN_STATEMENT_SET:
            case END_STATEMENT_SET:
                LOG.warn("SQL Client Syntax: {} ", call.command.getName());
                return null;
            default:
                if (call.command == SqlCommand.INSERT) {
                    context.hasInsert = true;
                }
                return parseWithCalcite(call, context.sqlDialect);
        }
    }

    private static FlinkSqlValidationResult parseWithCalcite(SqlCommandCall call, String sqlDialect) {
        try {
            if ("HIVE".equalsIgnoreCase(sqlDialect)) {
                return null;
            }
            if (!"DEFAULT".equalsIgnoreCase(sqlDialect)) {
                throw new UnsupportedOperationException("unsupported dialect: " + sqlDialect);
            }
            Class<?> calciteClass = loadCalciteParserClass();
            Object parser =
                calciteClass
                    .getConstructor(SqlParser.Config.class)
                    .newInstance(SQL_PARSER_CONFIG_MAP.get(sqlDialect.toUpperCase()));
            Method method = parser.getClass().getDeclaredMethod("parse", String.class);
            method.setAccessible(true);
            method.invoke(parser, call.originSql);
            return null;
        } catch (Exception e) {
            return toSyntaxErrorResult(call, e);
        }
    }

    private static FlinkSqlValidationResult toSyntaxErrorResult(SqlCommandCall call, Exception e) {
        String exception = ExceptionUtils.stringifyException(e);
        int causedByIndex = exception.indexOf("Caused by:");
        String causedBy =
            causedByIndex >= 0 ? exception.substring(causedByIndex) : exception;
        Matcher syntaxMatcher = SYNTAX_ERROR_REGEXP.matcher(exception.replaceAll("[\r\n]", ""));
        if (!syntaxMatcher.find()) {
            return FlinkSqlValidationResult.builder()
                .success(false)
                .failedType(FlinkSqlValidationFailedType.SYNTAX_ERROR)
                .lineStart(call.lineStart)
                .lineEnd(call.lineEnd)
                .sql(call.originSql)
                .exception(causedBy)
                .build();
        }
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
            .exception(causedBy.replaceAll("at\\sline\\s" + line, "at line " + errorLine))
            .build();
    }

    private static Class<?> loadCalciteParserClass() throws ClassNotFoundException {
        try {
            return Class.forName(FLINK112_CALCITE_PARSER_CLASS);
        } catch (ClassNotFoundException e) {
            return Class.forName(FLINK113_PLUS_CALCITE_PARSER_CLASS);
        }
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

    private static final class ValidationContext {

        private String sqlDialect = SqlDialect.DEFAULT.name().toLowerCase();
        private boolean hasInsert;

        private void updateDialect(SqlCommandCall call) {
            String args =
                call.operands == null || call.operands.length == 0 ? null : call.operands[0];
            if (args != null
                && TableConfigOptions.TABLE_SQL_DIALECT.key().equals(args)
                && call.operands.length > 1) {
                sqlDialect = call.operands[call.operands.length - 1];
            }
        }
    }
}
