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

/** Splits multi-statement Flink SQL scripts into segments with line numbers. */
final class SqlSplitter {

    private static final Set<String> SINGLE_LINE_COMMENT_PREFIX_LIST;

    static {
        Set<String> prefixes = new HashSet<>();
        prefixes.add(ConfigKeys.PARAM_PREFIX());
        SINGLE_LINE_COMMENT_PREFIX_LIST = Collections.unmodifiableSet(prefixes);
    }

    private SqlSplitter() {
    }

    static List<SqlSegment> splitSql(String sql) {
        QueryExtractor extractor = new QueryExtractor(sql);
        Map<Integer, String> refinedQueries = refineQueries(extractor.queries());
        return buildSegments(refinedQueries, extractor.lineNumMap());
    }

    private static Map<Integer, String> refineQueries(List<String> queries) {
        Map<Integer, String> refinedQueries = new HashMap<>();
        for (int i = 0; i < queries.size(); i++) {
            String currStatement = queries.get(i);
            if (isSingleLineComment(currStatement) || isMultipleLineComment(currStatement)) {
                mergeCommentIntoPrevious(refinedQueries, currStatement);
            } else {
                appendStatement(refinedQueries, i, currStatement);
            }
        }
        return refinedQueries;
    }

    private static void mergeCommentIntoPrevious(Map<Integer, String> refinedQueries, String comment) {
        if (!refinedQueries.isEmpty()) {
            int lastKey = refinedQueries.size() - 1;
            refinedQueries.put(lastKey, refinedQueries.get(lastKey) + extractLineBreaks(comment));
        }
    }

    private static void appendStatement(
                                        Map<Integer, String> refinedQueries,
                                        int index,
                                        String statement) {
        String linesPlaceholder = "";
        if (index > 0) {
            linesPlaceholder = extractLineBreaks(refinedQueries.get(index - 1));
        }
        refinedQueries.put(refinedQueries.size(), linesPlaceholder + statement);
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

    private static List<SqlSegment> buildSegments(
                                                  Map<Integer, String> refinedQueries,
                                                  Map<Integer, int[]> lineNumMap) {
        List<SqlSegment> segments = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : refinedQueries.entrySet()) {
            int[] line = lineNumMap.get(entry.getKey());
            segments.add(new SqlSegment(line[0], line[1], entry.getValue()));
        }
        segments.sort(Comparator.comparingInt(SqlSegment::start));
        return segments;
    }

    private static boolean isSingleLineComment(String text) {
        return text.trim().startsWith(ConfigKeys.PARAM_PREFIX());
    }

    private static boolean isMultipleLineComment(String text) {
        return text.trim().startsWith("/*") && text.trim().endsWith("*/");
    }

    private static boolean isSingleLineComment(char curChar, char nextChar) {
        for (String prefix : SINGLE_LINE_COMMENT_PREFIX_LIST) {
            if (prefix.length() == 1 && curChar == prefix.charAt(0)) {
                return true;
            }
            if (prefix.length() == 2
                && curChar == prefix.charAt(0)
                && nextChar == prefix.charAt(1)) {
                return true;
            }
        }
        return false;
    }

    private static final class QueryExtractor {

        private final String sql;
        private final int lastIndex;
        private final Map<Integer, int[]> lineNumMap = new HashMap<>();
        private final Map<Integer, Boolean> lineDescriptor;
        private final List<String> queries = new ArrayList<>();

        private final StringBuilder query = new StringBuilder();
        private boolean multiLineComment;
        private boolean singleLineComment;
        private boolean singleQuoteString;
        private boolean doubleQuoteString;
        private int lineNum;

        private QueryExtractor(String sql) {
            this.sql = sql;
            this.lastIndex = StringUtils.isNotBlank(sql) ? sql.length() - 1 : 0;
            this.lineDescriptor = buildLineDescriptor(sql);
            for (int idx = 0; idx < sql.length(); idx++) {
                processCharacter(idx);
            }
        }

        private List<String> queries() {
            return queries;
        }

        private Map<Integer, int[]> lineNumMap() {
            return lineNumMap;
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
                                           int lineNum,
                                           Map<Integer, int[]> lineNumMap,
                                           Map<Integer, Boolean> lineDescriptor) {
            int line = lineNum + 1;
            if (lineNumMap.isEmpty()) {
                lineNumMap.put(0, new int[]{findStartLine(1, lineDescriptor), line});
            } else {
                int index = lineNumMap.size();
                int start = lineNumMap.get(lineNumMap.size() - 1)[1] + 1;
                lineNumMap.put(index, new int[]{findStartLine(start, lineDescriptor), line});
            }
        }

        private static boolean hasNonBlankQuery(StringBuilder query) {
            return !query.toString().trim().isEmpty();
        }

        private void processCharacter(int idx) {
            if (sql.charAt(idx) == '\n') {
                lineNum++;
            }
            char ch = sql.charAt(idx);
            if (handleSingleLineCommentEnd(idx, ch)) {
                return;
            }
            updateMultiLineCommentEnd(idx);
            updateQuoteState(idx, ch);
            updateCommentStart(idx);
            handleStatementBoundary(idx, ch);
        }

        private boolean handleSingleLineCommentEnd(int idx, char ch) {
            if (!singleLineComment || ch != '\n') {
                return false;
            }
            singleLineComment = false;
            query.append(ch);
            if (idx == lastIndex && hasNonBlankQuery(query)) {
                queries.add(query.toString());
            }
            return true;
        }

        private void updateMultiLineCommentEnd(int idx) {
            if (multiLineComment && idx - 1 >= 0 && sql.charAt(idx - 1) == '/'
                && idx - 2 >= 0 && sql.charAt(idx - 2) == '*') {
                multiLineComment = false;
            }
        }

        private void updateQuoteState(int idx, char ch) {
            if (ch == '\'' && !singleLineComment && !multiLineComment) {
                if (singleQuoteString) {
                    singleQuoteString = false;
                } else if (!doubleQuoteString) {
                    singleQuoteString = true;
                }
            }
            if (ch == '"' && !singleLineComment && !multiLineComment) {
                if (doubleQuoteString && idx > 0) {
                    doubleQuoteString = false;
                } else if (!singleQuoteString) {
                    doubleQuoteString = true;
                }
            }
        }

        private void updateCommentStart(int idx) {
            if (singleQuoteString || doubleQuoteString || multiLineComment || singleLineComment
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

        private void handleStatementBoundary(int idx, char ch) {
            if (ch == ';' && !singleQuoteString && !doubleQuoteString && !multiLineComment
                && !singleLineComment) {
                markLineNumber(lineNum, lineNumMap, lineDescriptor);
                if (hasNonBlankQuery(query)) {
                    queries.add(query.toString());
                    query.setLength(0);
                }
                return;
            }
            if (idx == lastIndex) {
                handleLastCharacter(ch);
                return;
            }
            appendNonCommentCharacter(ch);
        }

        private void handleLastCharacter(char ch) {
            markLineNumber(lineNum, lineNumMap, lineDescriptor);
            if (!singleLineComment && !multiLineComment) {
                query.append(ch);
            }
            if (hasNonBlankQuery(query)) {
                queries.add(query.toString());
            }
        }

        private void appendNonCommentCharacter(char ch) {
            if (!singleLineComment && !multiLineComment || ch == '\n') {
                query.append(ch);
            }
        }
    }
}
