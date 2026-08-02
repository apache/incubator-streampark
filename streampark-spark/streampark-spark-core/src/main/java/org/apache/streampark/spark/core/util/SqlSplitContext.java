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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Mutable parsing state for {@link SqlSplitter}. */
final class SqlSplitContext {

    private final List<String> queries = new ArrayList<>();
    private final Map<Integer, int[]> lineNumMap = new HashMap<>();
    private final Map<Integer, Boolean> lineDescriptor;

    private StringBuilder query = new StringBuilder();
    private boolean multiLineComment;
    private boolean singleLineComment;
    private boolean singleQuoteString;
    private boolean doubleQuoteString;
    private int lineNum;

    SqlSplitContext(Map<Integer, Boolean> lineDescriptor) {
        this.lineDescriptor = lineDescriptor;
    }

    List<String> queries() {
        return queries;
    }

    Map<Integer, int[]> lineNumMap() {
        return lineNumMap;
    }

    void onNewline() {
        lineNum++;
    }

    void processCharacter(String sql, int idx, int lastIndex) {
        char ch = sql.charAt(idx);
        if (handleSingleLineCommentEnd(ch, idx, lastIndex)) {
            return;
        }
        updateMultiLineCommentEnd(sql, idx);
        updateQuoteState(ch);
        updateCommentStart(sql, idx, lastIndex);
        handleStatementBoundary(ch, idx, lastIndex);
    }

    private boolean handleSingleLineCommentEnd(char ch, int idx, int lastIndex) {
        if (!singleLineComment || ch != '\n') {
            return false;
        }
        singleLineComment = false;
        query.append(ch);
        if (idx == lastIndex && !query.toString().trim().isEmpty()) {
            queries.add(query.toString());
        }
        return true;
    }

    private void updateMultiLineCommentEnd(String sql, int idx) {
        if (multiLineComment && idx - 1 >= 0 && sql.charAt(idx - 1) == '/'
            && idx - 2 >= 0 && sql.charAt(idx - 2) == '*') {
            multiLineComment = false;
        }
    }

    private void updateQuoteState(char ch) {
        if (ch == '\'' && !singleLineComment && !multiLineComment) {
            if (singleQuoteString) {
                singleQuoteString = false;
            } else if (!doubleQuoteString) {
                singleQuoteString = true;
            }
            return;
        }
        if (ch == '"' && !singleLineComment && !multiLineComment) {
            if (doubleQuoteString) {
                doubleQuoteString = false;
            } else if (!singleQuoteString) {
                doubleQuoteString = true;
            }
        }
    }

    private void updateCommentStart(String sql, int idx, int lastIndex) {
        if (singleQuoteString || doubleQuoteString || multiLineComment || singleLineComment
            || idx >= lastIndex) {
            return;
        }
        if (SqlSplitter.isSingleLineComment(sql.charAt(idx), sql.charAt(idx + 1))) {
            singleLineComment = true;
        } else if (sql.charAt(idx) == '/'
            && sql.length() > idx + 2
            && sql.charAt(idx + 1) == '*'
            && sql.charAt(idx + 2) != '+') {
            multiLineComment = true;
        }
    }

    private void handleStatementBoundary(char ch, int idx, int lastIndex) {
        if (ch == ';' && !singleQuoteString && !doubleQuoteString && !multiLineComment
            && !singleLineComment) {
            markLineNumber();
            flushQueryIfNonEmpty();
            return;
        }
        if (idx == lastIndex) {
            markLineNumber();
            if (!singleLineComment && !multiLineComment) {
                query.append(ch);
            }
            flushQueryIfNonEmpty();
            return;
        }
        if (!singleLineComment && !multiLineComment) {
            query.append(ch);
        } else if (ch == '\n') {
            query.append(ch);
        }
    }

    private void flushQueryIfNonEmpty() {
        if (!query.toString().trim().isEmpty()) {
            queries.add(query.toString());
            query = new StringBuilder();
        }
    }

    private void markLineNumber() {
        int line = lineNum + 1;
        if (lineNumMap.isEmpty()) {
            lineNumMap.put(0, new int[]{findStartLine(1), line});
        } else {
            int index = lineNumMap.size();
            int start = lineNumMap.get(lineNumMap.size() - 1)[1] + 1;
            lineNumMap.put(index, new int[]{findStartLine(start), line});
        }
    }

    private int findStartLine(int num) {
        if (num >= lineDescriptor.size() || Boolean.TRUE.equals(lineDescriptor.get(num))) {
            return num;
        }
        return findStartLine(num + 1);
    }
}
