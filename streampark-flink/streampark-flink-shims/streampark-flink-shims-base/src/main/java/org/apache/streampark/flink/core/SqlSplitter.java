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

import org.apache.streampark.common.conf.ConfigKeys;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/** Splits multi-statement Flink SQL scripts into individual segments. */
public final class SqlSplitter {

    private static final Set<String> SINGLE_LINE_COMMENT_PREFIX_LIST;

    static {
        Set<String> prefixes = new HashSet<>();
        prefixes.add(ConfigKeys.PARAM_PREFIX());
        SINGLE_LINE_COMMENT_PREFIX_LIST = Collections.unmodifiableSet(prefixes);
    }

    private SqlSplitter() {
    }

    /**
     * Split whole text into multiple sql statements. Two Steps: Step 1, split the whole text into
     * multiple sql statements. Step 2, refine the results. Replace the preceding sql statements with
     * empty lines, so that we can get the correct line number in the parsing error message.
     */
    public static List<SqlSegment> splitSql(String sql) {
        Map<Integer, Boolean> lineDescriptor = buildLineDescriptor(sql);
        Map<Integer, int[]> lineNumMap = new HashMap<>();
        List<String> queries = splitRawQueries(sql, lineDescriptor, lineNumMap);
        Map<Integer, String> refinedQueries = refineQueries(queries);
        return toSegments(refinedQueries, lineNumMap);
    }

    private static List<String> splitRawQueries(
                                                String sql, Map<Integer, Boolean> lineDescriptor,
                                                Map<Integer, int[]> lineNumMap) {
        List<String> queries = new ArrayList<>();
        if (StringUtils.isBlank(sql)) {
            return queries;
        }

        int lastIndex = sql.length() - 1;
        StringBuilder query = new StringBuilder();
        ParseState state = new ParseState();

        for (int idx = 0; idx < sql.length(); idx++) {
            if (sql.charAt(idx) == '\n') {
                state.lineNum++;
            }
            char ch = sql.charAt(idx);
            processCharacter(
                sql, idx, lastIndex, ch, query, queries, state, lineDescriptor, lineNumMap);
        }
        return queries;
    }

    private static void processCharacter(
                                         String sql,
                                         int idx,
                                         int lastIndex,
                                         char ch,
                                         StringBuilder query,
                                         List<String> queries,
                                         ParseState state,
                                         Map<Integer, Boolean> lineDescriptor,
                                         Map<Integer, int[]> lineNumMap) {
        if (state.endSingleLineComment(ch)) {
            query.append(ch);
            appendTrailingQuery(query, queries, idx, lastIndex);
            return;
        }

        state.endMultiLineComment(sql, idx);
        state.toggleQuoteState(ch, idx);
        state.startCommentIfNeeded(sql, idx, lastIndex);

        if (state.isStatementDelimiter(ch)) {
            finishStatement(query, queries, state, lineDescriptor, lineNumMap);
            return;
        }

        if (idx == lastIndex) {
            finishLastCharacter(sql, idx, lastIndex, ch, query, queries, state, lineDescriptor, lineNumMap);
            return;
        }

        if (state.shouldAppendChar(ch)) {
            query.append(ch);
        } else if (ch == '\n') {
            query.append(ch);
        }
    }

    private static void appendTrailingQuery(
                                            StringBuilder query, List<String> queries, int idx, int lastIndex) {
        if (idx == lastIndex && StringUtils.isNotBlank(query.toString().trim())) {
            queries.add(query.toString());
        }
    }

    private static void finishStatement(
                                        StringBuilder query,
                                        List<String> queries,
                                        ParseState state,
                                        Map<Integer, Boolean> lineDescriptor,
                                        Map<Integer, int[]> lineNumMap) {
        markLineNumber(state.lineNum, lineDescriptor, lineNumMap);
        if (StringUtils.isNotBlank(query.toString().trim())) {
            queries.add(query.toString());
        }
        query.setLength(0);
    }

    private static void finishLastCharacter(
                                            String sql,
                                            int idx,
                                            int lastIndex,
                                            char ch,
                                            StringBuilder query,
                                            List<String> queries,
                                            ParseState state,
                                            Map<Integer, Boolean> lineDescriptor,
                                            Map<Integer, int[]> lineNumMap) {
        markLineNumber(state.lineNum, lineDescriptor, lineNumMap);
        if (!state.singleLineComment && !state.multiLineComment) {
            query.append(ch);
        }
        if (StringUtils.isNotBlank(query.toString().trim())) {
            queries.add(query.toString());
        }
        query.setLength(0);
    }

    private static Map<Integer, String> refineQueries(List<String> queries) {
        Map<Integer, String> refinedQueries = new HashMap<>();
        for (int i = 0; i < queries.size(); i++) {
            String currStatement = queries.get(i);
            if (isSingleLineComment(currStatement) || isMultipleLineComment(currStatement)) {
                appendCommentLineBreaks(refinedQueries, currStatement);
            } else {
                refinedQueries.put(
                    refinedQueries.size(),
                    leadingLineBreaks(refinedQueries, i) + currStatement);
            }
        }
        return refinedQueries;
    }

    private static void appendCommentLineBreaks(Map<Integer, String> refinedQueries, String currStatement) {
        if (refinedQueries.isEmpty()) {
            return;
        }
        int lastKey = refinedQueries.size() - 1;
        refinedQueries.put(lastKey, refinedQueries.get(lastKey) + extractLineBreaks(currStatement));
    }

    private static String leadingLineBreaks(Map<Integer, String> refinedQueries, int index) {
        if (index == 0) {
            return "";
        }
        return extractLineBreaks(refinedQueries.get(index - 1));
    }

    private static List<SqlSegment> toSegments(
                                               Map<Integer, String> refinedQueries, Map<Integer, int[]> lineNumMap) {
        List<SqlSegment> segments = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : refinedQueries.entrySet()) {
            int[] line = lineNumMap.get(entry.getKey());
            segments.add(new SqlSegment(line[0], line[1], entry.getValue()));
        }
        segments.sort(Comparator.comparingInt(a -> a.start));
        return segments;
    }

    private static final class ParseState {

        private boolean multiLineComment;
        private boolean singleLineComment;
        private boolean singleQuoteString;
        private boolean doubleQuoteString;
        private int lineNum;

        private boolean endSingleLineComment(char ch) {
            if (singleLineComment && ch == '\n') {
                singleLineComment = false;
                return true;
            }
            return false;
        }

        private void endMultiLineComment(String sql, int idx) {
            if (multiLineComment
                && idx - 1 >= 0
                && sql.charAt(idx - 1) == '/'
                && idx - 2 >= 0
                && sql.charAt(idx - 2) == '*') {
                multiLineComment = false;
            }
        }

        private void toggleQuoteState(char ch, int idx) {
            if (ch == '\'' && !(singleLineComment || multiLineComment)) {
                if (singleQuoteString) {
                    singleQuoteString = false;
                } else if (!doubleQuoteString) {
                    singleQuoteString = true;
                }
            }
            if (ch == '"' && !(singleLineComment || multiLineComment)) {
                if (doubleQuoteString && idx > 0) {
                    doubleQuoteString = false;
                } else if (!singleQuoteString) {
                    doubleQuoteString = true;
                }
            }
        }

        private void startCommentIfNeeded(String sql, int idx, int lastIndex) {
            if (singleQuoteString
                || doubleQuoteString
                || multiLineComment
                || singleLineComment
                || idx >= lastIndex) {
                return;
            }
            if (isSingleLineComment(sql.charAt(idx), sql.charAt(idx + 1))) {
                singleLineComment = true;
            } else if (sql.charAt(idx) == '/'
                && sql.length() > idx + 2
                && sql.charAt(idx + 1) == '*'
                && sql.charAt(idx + 2) != '+') {
                multiLineComment = true;
            }
        }

        private boolean isStatementDelimiter(char ch) {
            return ch == ';'
                && !singleQuoteString
                && !doubleQuoteString
                && !multiLineComment
                && !singleLineComment;
        }

        private boolean shouldAppendChar(char ch) {
            return !singleLineComment && !multiLineComment;
        }
    }

    private static Map<Integer, Boolean> buildLineDescriptor(String sql) {
        Map<Integer, Boolean> descriptor = new HashMap<>();
        Scanner scanner = new Scanner(sql);
        int lineNumber = 0;
        boolean startComment = false;
        boolean hasComment = false;

        while (scanner.hasNextLine()) {
            lineNumber++;
            String line = scanner.nextLine().trim();
            boolean nonEmpty =
                StringUtils.isNotBlank(line) && !line.startsWith(ConfigKeys.PARAM_PREFIX());
            if (line.startsWith("/*")) {
                startComment = true;
                hasComment = true;
            }

            descriptor.put(lineNumber, nonEmpty && !hasComment);

            if (startComment && line.endsWith("*/")) {
                startComment = false;
                hasComment = false;
            }
        }
        scanner.close();
        return descriptor;
    }

    private static int findStartLine(int num, Map<Integer, Boolean> lineDescriptor) {
        if (num >= lineDescriptor.size() || Boolean.TRUE.equals(lineDescriptor.get(num))) {
            return num;
        }
        return findStartLine(num + 1, lineDescriptor);
    }

    private static void markLineNumber(
                                       int lineNum, Map<Integer, Boolean> lineDescriptor,
                                       Map<Integer, int[]> lineNumMap) {
        int line = lineNum + 1;
        if (lineNumMap.isEmpty()) {
            lineNumMap.put(0, new int[]{findStartLine(1, lineDescriptor), line});
        } else {
            int index = lineNumMap.size();
            int[] previous = lineNumMap.get(lineNumMap.size() - 1);
            int start = previous[1] + 1;
            lineNumMap.put(index, new int[]{findStartLine(start, lineDescriptor), line});
        }
    }

    private static String extractLineBreaks(String text) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private static boolean isSingleLineComment(String text) {
        return text.trim().startsWith(ConfigKeys.PARAM_PREFIX());
    }

    private static boolean isMultipleLineComment(String text) {
        return text.trim().startsWith("/*") && text.trim().endsWith("*/");
    }

    private static boolean isSingleLineComment(char curChar, char nextChar) {
        for (String singleCommentPrefix : SINGLE_LINE_COMMENT_PREFIX_LIST) {
            switch (singleCommentPrefix.length()) {
                case 1:
                    if (curChar == singleCommentPrefix.charAt(0)) {
                        return true;
                    }
                    break;
                case 2:
                    if (curChar == singleCommentPrefix.charAt(0)
                        && nextChar == singleCommentPrefix.charAt(1)) {
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }
        return false;
    }
}
