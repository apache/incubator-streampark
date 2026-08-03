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

import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.flink.sql.parser.validate.FlinkSqlConformance;
import org.apache.flink.table.api.SqlDialect;
import org.apache.flink.table.api.config.TableConfigOptions;
import org.apache.flink.table.planner.delegation.FlinkSqlParserFactories;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Validates Flink SQL syntax using Calcite parser. */
public final class FlinkSqlValidator {

    private static final String FLINK112_CALCITE_PARSER_CLASS =
        "org.apache.flink.table.planner.calcite.CalciteParser";

    private static final String FLINK113_PLUS_CALCITE_PARSER_CLASS =
        "org.apache.flink.table.planner.parse.CalciteParser";

    private static final Pattern SYNTAX_ERROR_PATTERN =
        Pattern.compile("at\\sline\\s(\\d+),\\scolumn\\s(\\d+)");

    private static final Map<String, SqlParser.Config> SQL_PARSER_CONFIG_MAP = createSqlParserConfigMap();

    private static final Log LOG = new Log();

    private FlinkSqlValidator() {
    }

    public static FlinkSqlValidationResult verifySql(String sql) {
        AtomicReference<FlinkSqlValidationResult> earlyReturn = new AtomicReference<>();
        List<SqlCommandCall> sqlCommands =
            SqlCommandParser.parseSQL(sql, earlyReturn::set);
        FlinkSqlValidationResult earlyResult = earlyReturn.get();
        if (earlyResult != null) {
            return earlyResult;
        }

        ValidationState state = new ValidationState();
        for (SqlCommandCall call : sqlCommands) {
            FlinkSqlValidationResult failure = processCommand(call, state);
            if (failure != null) {
                return failure;
            }
        }
        return state.toResult(sqlCommands);
    }

    private static FlinkSqlValidationResult processCommand(SqlCommandCall call, ValidationState state) {
        SqlCommand command = call.command();
        switch (command) {
            case SET:
            case RESET:
                state.updateDialect(command, call);
                return null;
            case BEGIN_STATEMENT_SET:
            case END_STATEMENT_SET:
                LOG.warn("SQL Client Syntax: " + command.getCommandName());
                return null;
            default:
                if (command == SqlCommand.INSERT) {
                    state.markInsert();
                }
                try {
                    validateSqlCommand(call, state.sqlDialect);
                } catch (IllegalStateException | UnsupportedOperationException e) {
                    return syntaxErrorResult(call, e);
                }
                return null;
        }
    }

    private static void validateSqlCommand(SqlCommandCall call, String sqlDialect) {
        if ("HIVE".equalsIgnoreCase(sqlDialect)) {
            return;
        }
        if (!"DEFAULT".equalsIgnoreCase(sqlDialect)) {
            throw new UnsupportedOperationException("unsupported dialect: " + sqlDialect);
        }
        try {
            Class<?> calciteClass = resolveCalciteParserClass();
            Constructor<?> constructor = calciteClass.getConstructor(SqlParser.Config.class);
            Object parser =
                constructor.newInstance(SQL_PARSER_CONFIG_MAP.get(sqlDialect.toUpperCase()));
            Method method = parser.getClass().getDeclaredMethod("parse", String.class);
            method.setAccessible(true);
            method.invoke(parser, call.originSql());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to parse SQL with Calcite", e);
        }
    }

    private static Class<?> resolveCalciteParserClass() throws ClassNotFoundException {
        try {
            return Class.forName(FLINK112_CALCITE_PARSER_CLASS);
        } catch (ClassNotFoundException e) {
            return Class.forName(FLINK113_PLUS_CALCITE_PARSER_CLASS);
        }
    }

    private static FlinkSqlValidationResult syntaxErrorResult(SqlCommandCall call, Throwable e) {
        String exception = ExceptionUtils.stringifyException(e);
        int causedByIndex = exception.indexOf("Caused by:");
        String causedBy = causedByIndex >= 0 ? exception.substring(causedByIndex) : exception;
        String cleanUpError = exception.replaceAll("[\r\n]", "");
        Matcher matcher = SYNTAX_ERROR_PATTERN.matcher(cleanUpError);
        if (matcher.find()) {
            int line = Integer.parseInt(matcher.group(1));
            int column = Integer.parseInt(matcher.group(2));
            int errorLine = call.lineStart() + line - 1;
            return FlinkSqlValidationResult.failure(
                FlinkSqlValidationFailedType.SYNTAX_ERROR,
                call.lineStart(),
                call.lineEnd(),
                errorLine,
                column,
                call.originSql(),
                causedBy.replaceAll("at\\sline\\s" + line, "at line " + errorLine));
        }
        return FlinkSqlValidationResult.failure(
            FlinkSqlValidationFailedType.SYNTAX_ERROR,
            call.lineStart(),
            call.lineEnd(),
            0,
            0,
            call.originSql(),
            causedBy);
    }

    private static Map<String, SqlParser.Config> createSqlParserConfigMap() {
        Map<String, SqlParser.Config> map = new HashMap<>();
        map.put(SqlDialect.DEFAULT.name(), getConfig(SqlDialect.DEFAULT));
        map.put(SqlDialect.HIVE.name(), getConfig(SqlDialect.HIVE));
        return map;
    }

    private static SqlParser.Config getConfig(SqlDialect sqlDialect) {
        org.apache.calcite.sql.validate.SqlConformance conformance = FlinkSqlConformance.DEFAULT;
        if (sqlDialect != SqlDialect.DEFAULT && sqlDialect != SqlDialect.HIVE) {
            throw new UnsupportedOperationException("Unsupported sqlDialect: " + sqlDialect);
        }
        return SqlParser.config()
            .withParserFactory(FlinkSqlParserFactories.create(conformance))
            .withConformance(conformance)
            .withLex(Lex.JAVA)
            .withIdentifierMaxLength(256);
    }

    private static final class ValidationState {

        private String sqlDialect = SqlDialect.DEFAULT.name().toLowerCase();
        private boolean hasInsert;

        private void updateDialect(SqlCommand command, SqlCommandCall call) {
            if (command == SqlCommand.SET
                && call.operands()[0].equals(TableConfigOptions.TABLE_SQL_DIALECT.key())) {
                sqlDialect = call.operands()[call.operands().length - 1];
            }
        }

        private void markInsert() {
            hasInsert = true;
        }

        private FlinkSqlValidationResult toResult(List<SqlCommandCall> sqlCommands) {
            if (hasInsert) {
                return new FlinkSqlValidationResult();
            }
            return FlinkSqlValidationResult.failure(
                FlinkSqlValidationFailedType.SYNTAX_ERROR,
                sqlCommands.get(0).lineStart(),
                sqlCommands.get(sqlCommands.size() - 1).lineEnd(),
                0,
                0,
                null,
                "No 'INSERT' statement to trigger the execution of the Flink job.");
        }
    }

    private static final class Log extends org.apache.streampark.common.util.LoggerSupport {

        void warn(String msg) {
            logWarn(msg);
        }
    }
}
