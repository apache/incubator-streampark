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

package org.apache.streampark.flink.core.lineage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the table name and {@code WITH (...)} connector options from one {@code CREATE TABLE}
 * or {@code CREATE TEMPORARY TABLE} statement.
 *
 * <p>Why this exists: a Flink {@code CompiledPlan} gives an accurate {@code
 * `catalog`.`database`.`table`} identifier only for tables that live in a real, attached Flink
 * catalog (e.g. a Paimon catalog registered via {@code CREATE CATALOG}). For a table created
 * per-job via {@code CREATE TABLE ... WITH (...)} — the common shape for StreamPark Flink SQL jobs,
 * which are typically self-contained scripts with no attached catalog — the plan JSON reports only
 * "session default catalog/database + local table name", carrying no connector/host/physical-table
 * info at all. That information lives in the SQL text itself, so it is extracted here instead.
 *
 * <p>Deliberately not scoped to {@code TEMPORARY} tables only (unlike the reference implementation
 * this was ported from): a plain {@code CREATE TABLE} without an attached persistent catalog is
 * exactly as ephemeral as a {@code CREATE TEMPORARY TABLE} from Gravitino's point of view, and
 * StreamPark SQL jobs commonly omit the {@code TEMPORARY} keyword. Restricting to {@code TEMPORARY}
 * would silently lose lineage for the common case.
 */
public final class SqlWithOptionsParser {

    private static final Pattern TABLE_NAME =
        Pattern.compile(
            "^\\s*CREATE\\s+(?:TEMPORARY\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?([A-Za-z_][A-Za-z0-9_]*)`?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern WITH_CLAUSE = Pattern.compile("\\bWITH\\s*\\(", Pattern.CASE_INSENSITIVE);
    /**
     * {@code 'key' = 'value'}. A literal's embedded {@code ''} must count as an escaped single quote,
     * not the end of the literal — a connector option value (e.g. a password) may legitimately
     * contain a quote escaped this way, and treating it as the literal's end would truncate the
     * value at that point.
     */
    private static final Pattern OPTION_ENTRY =
        Pattern.compile("'((?:[^'\\\\]|\\\\.|'')*)'\\s*=\\s*'((?:[^'\\\\]|\\\\.|'')*)'");

    private SqlWithOptionsParser() {
    }

    /** One {@code CREATE TABLE}'s local name and its {@code WITH (...)} connector options. */
    public static final class TableOptions {

        private final String name;
        private final Map<String, String> options;

        TableOptions(String name, Map<String, String> options) {
            this.name = name;
            this.options = options;
        }

        public String name() {
            return name;
        }

        public Map<String, String> options() {
            return options;
        }
    }

    /**
     * Parses one {@code CREATE [TEMPORARY] TABLE} statement. Returns {@code null} (not an
     * exception — this is production submission-path code, not a validator) when the statement
     * does not match the expected shape or carries no {@code WITH (...)} clause, e.g. {@code CREATE
     * TABLE ... LIKE ...} or a catalog-backed table with no inline connector options.
     */
    public static TableOptions parse(String createTableStatement) {
        Matcher nameMatcher = TABLE_NAME.matcher(createTableStatement);
        if (!nameMatcher.find()) {
            return null;
        }
        String name = nameMatcher.group(1);

        Matcher withStart = WITH_CLAUSE.matcher(createTableStatement);
        if (!withStart.find()) {
            return null;
        }
        String body = extractParenthesizedBody(createTableStatement, withStart.end());
        if (body == null) {
            return null;
        }

        Map<String, String> options = new LinkedHashMap<>();
        Matcher entry = OPTION_ENTRY.matcher(body);
        while (entry.find()) {
            options.put(unescapeLiteral(entry.group(1)), unescapeLiteral(entry.group(2)));
        }
        return new TableOptions(name, options);
    }

    /** Balanced-parenthesis scan from just past the opening {@code (}, quote-aware. */
    private static String extractParenthesizedBody(String sql, int bodyStart) {
        int depth = 1;
        int idx = bodyStart;
        boolean inSingleQuote = false;
        while (idx < sql.length() && depth > 0) {
            char c = sql.charAt(idx);
            if (c == '\'') {
                inSingleQuote = !inSingleQuote;
            } else if (!inSingleQuote && c == '(') {
                depth++;
            } else if (!inSingleQuote && c == ')') {
                depth--;
            }
            idx++;
        }
        if (depth != 0) {
            return null;
        }
        return sql.substring(bodyStart, idx - 1);
    }

    private static String unescapeLiteral(String literal) {
        return literal.replace("''", "'");
    }
}
