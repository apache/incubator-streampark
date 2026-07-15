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
        prefixes.add(ConfigKeys.PARAM_PREFIX);
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
        List<String> queries = new ArrayList<>();
        int lastIndex = StringUtils.isNotBlank(sql) ? sql.length() - 1 : 0;
        StringBuilder query = new StringBuilder();

        boolean multiLineComment = false;
        boolean singleLineComment = false;
        boolean singleQuoteString = false;
        boolean doubleQuoteString = false;
        int lineNum = 0;
        Map<Integer, int[]> lineNumMap = new HashMap<>();

        Map<Integer, Boolean> lineDescriptor = buildLineDescriptor(sql);

        for (int idx = 0; idx < sql.length(); idx++) {
            if (sql.charAt(idx) == '\n') {
                lineNum++;
            }

            char ch = sql.charAt(idx);

            // end of single line comment
            if (singleLineComment && ch == '\n') {
                singleLineComment = false;
                query.append(ch);
                if (idx == lastIndex && StringUtils.isNotBlank(query.toString().trim())) {
                    queries.add(query.toString());
                }
                continue;
            }

            // end of multiple line comment
            if (multiLineComment
                && idx - 1 >= 0
                && sql.charAt(idx - 1) == '/'
                && idx - 2 >= 0
                && sql.charAt(idx - 2) == '*') {
                multiLineComment = false;
            }

            // single quote start or end mark
            if (ch == '\'' && !(singleLineComment || multiLineComment)) {
                if (singleQuoteString) {
                    singleQuoteString = false;
                } else if (!doubleQuoteString) {
                    singleQuoteString = true;
                }
            }

            // double quote start or end mark
            if (ch == '"' && !(singleLineComment || multiLineComment)) {
                if (doubleQuoteString && idx > 0) {
                    doubleQuoteString = false;
                } else if (!singleQuoteString) {
                    doubleQuoteString = true;
                }
            }

            // single line comment or multiple line comment start mark
            if (!singleQuoteString
                && !doubleQuoteString
                && !multiLineComment
                && !singleLineComment
                && idx < lastIndex) {
                if (isSingleLineComment(sql.charAt(idx), sql.charAt(idx + 1))) {
                    singleLineComment = true;
                } else if (sql.charAt(idx) == '/'
                    && sql.length() > idx + 2
                    && sql.charAt(idx + 1) == '*'
                    && sql.charAt(idx + 2) != '+') {
                    multiLineComment = true;
                }
            }

            if (ch == ';'
                && !singleQuoteString
                && !doubleQuoteString
                && !multiLineComment
                && !singleLineComment) {
                markLineNumber(lineNum, lineDescriptor, lineNumMap);
                if (StringUtils.isNotBlank(query.toString().trim())) {
                    queries.add(query.toString());
                    query = new StringBuilder();
                }
            } else if (idx == lastIndex) {
                markLineNumber(lineNum, lineDescriptor, lineNumMap);

                if (!singleLineComment && !multiLineComment) {
                    query.append(ch);
                }

                if (StringUtils.isNotBlank(query.toString().trim())) {
                    queries.add(query.toString());
                    query = new StringBuilder();
                }
            } else if (!singleLineComment && !multiLineComment) {
                query.append(ch);
            } else if (ch == '\n') {
                query.append(ch);
            }
        }

        Map<Integer, String> refinedQueries = new HashMap<>();
        for (int i = 0; i < queries.size(); i++) {
            String currStatement = queries.get(i);
            if (isSingleLineComment(currStatement) || isMultipleLineComment(currStatement)) {
                if (!refinedQueries.isEmpty()) {
                    int lastKey = refinedQueries.size() - 1;
                    String lastRefinedQuery = refinedQueries.get(lastKey);
                    refinedQueries.put(lastKey, lastRefinedQuery + extractLineBreaks(currStatement));
                }
            } else {
                String linesPlaceholder = "";
                if (i > 0) {
                    linesPlaceholder = extractLineBreaks(refinedQueries.get(i - 1));
                }
                refinedQueries.put(refinedQueries.size(), linesPlaceholder + currStatement);
            }
        }

        List<SqlSegment> segments = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : refinedQueries.entrySet()) {
            int[] line = lineNumMap.get(entry.getKey());
            segments.add(new SqlSegment(line[0], line[1], entry.getValue()));
        }
        segments.sort(Comparator.comparingInt(a -> a.start));
        return segments;
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
                StringUtils.isNotBlank(line) && !line.startsWith(ConfigKeys.PARAM_PREFIX);
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
        return text.trim().startsWith(ConfigKeys.PARAM_PREFIX);
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
