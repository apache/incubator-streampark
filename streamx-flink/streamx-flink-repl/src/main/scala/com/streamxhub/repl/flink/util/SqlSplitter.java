package com.streamxhub.repl.flink.util;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SqlSplitter {

    private Set<String> singleLineCommentPrefixList = new HashSet();

    public SqlSplitter() {
        this.singleLineCommentPrefixList.add("--");
    }

    public SqlSplitter(String... additionalSingleCommentPrefixList) {
        String[] var2 = additionalSingleCommentPrefixList;
        int var3 = additionalSingleCommentPrefixList.length;
        for (int var4 = 0; var4 < var3; ++var4) {
            String singleLineCommentPrefix = var2[var4];
            if (singleLineCommentPrefix.length() > 2) {
                throw new RuntimeException("Invalid singleLineCommentPrefix: " + singleLineCommentPrefix + ", it is at most 2 characters");
            }
            this.singleLineCommentPrefixList.add(singleLineCommentPrefix);
        }

    }

    public List<String> splitSql(String text) {
        text = text.trim();
        List<String> queries = new ArrayList();
        StringBuilder query = new StringBuilder();
        boolean multiLineComment = false;
        boolean singleLineComment = false;
        boolean singleQuoteString = false;
        boolean doubleQuoteString = false;

        for (int index = 0; index < text.length(); ++index) {
            char character = text.charAt(index);
            if (singleLineComment && character == '\n') {
                singleLineComment = false;
                if (query.toString().trim().isEmpty()) {
                    continue;
                }
            }

            if (multiLineComment && index - 1 >= 0 && text.charAt(index - 1) == '/' && index - 2 >= 0 && text.charAt(index - 2) == '*') {
                multiLineComment = false;
            }

            if (character == '\'') {
                if (singleQuoteString) {
                    singleQuoteString = false;
                } else if (!doubleQuoteString) {
                    singleQuoteString = true;
                }
            }

            if (character == '"') {
                if (doubleQuoteString && index > 0) {
                    doubleQuoteString = false;
                } else if (!singleQuoteString) {
                    doubleQuoteString = true;
                }
            }

            if (!singleQuoteString && !doubleQuoteString && !multiLineComment && !singleLineComment && text.length() > index + 1) {
                if (this.isSingleLineComment(text.charAt(index), text.charAt(index + 1))) {
                    singleLineComment = true;
                } else if (text.charAt(index) == '/' && text.length() > index + 2 && text.charAt(index + 1) == '*' && text.charAt(index + 2) != '+') {
                    multiLineComment = true;
                }
            }

            if (character == ';' && !singleQuoteString && !doubleQuoteString && !multiLineComment && !singleLineComment) {
                if (!query.toString().trim().isEmpty()) {
                    queries.add(query.toString().trim());
                    query = new StringBuilder();
                }
            } else if (index == text.length() - 1) {
                if (!singleLineComment && !multiLineComment) {
                    query.append(character);
                }

                if (!query.toString().trim().isEmpty()) {
                    queries.add(query.toString().trim());
                    query = new StringBuilder();
                }
            } else if (!singleLineComment && !multiLineComment) {
                query.append(character);
            }
        }

        return queries;
    }

    private boolean isSingleLineComment(char curChar, char nextChar) {
        Iterator var3 = this.singleLineCommentPrefixList.iterator();

        String singleCommentPrefix;
        do {
            if (!var3.hasNext()) {
                return false;
            }

            singleCommentPrefix = (String) var3.next();
            if (singleCommentPrefix.length() == 1 && curChar == singleCommentPrefix.charAt(0)) {
                return true;
            }
        } while (singleCommentPrefix.length() != 2 || curChar != singleCommentPrefix.charAt(0) || nextChar != singleCommentPrefix.charAt(1));

        return true;
    }
}
