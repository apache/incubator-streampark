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

/** Result of Spark SQL validation (Scala case-class compatible). */
public class SparkSqlValidationResult {

    private boolean success = true;
    private SparkSqlValidationFailedType failedType;
    private int lineStart;
    private int lineEnd;
    private int errorLine;
    private int errorColumn;
    private String sql;
    private String exception;

    public SparkSqlValidationResult() {
    }

    public SparkSqlValidationResult(
                                    boolean success,
                                    SparkSqlValidationFailedType failedType,
                                    int lineStart,
                                    int lineEnd,
                                    int errorLine,
                                    int errorColumn,
                                    String sql,
                                    String exception) {
        this.success = success;
        this.failedType = failedType;
        this.lineStart = lineStart;
        this.lineEnd = lineEnd;
        this.errorLine = errorLine;
        this.errorColumn = errorColumn;
        this.sql = sql;
        this.exception = exception;
    }

    public boolean success() {
        return success;
    }

    public boolean isSuccess() {
        return success;
    }

    public SparkSqlValidationFailedType failedType() {
        return failedType;
    }

    public SparkSqlValidationFailedType getFailedType() {
        return failedType;
    }

    public int lineStart() {
        return lineStart;
    }

    public int getLineStart() {
        return lineStart;
    }

    public int lineEnd() {
        return lineEnd;
    }

    public int getLineEnd() {
        return lineEnd;
    }

    public int errorLine() {
        return errorLine;
    }

    public int getErrorLine() {
        return errorLine;
    }

    public int errorColumn() {
        return errorColumn;
    }

    public int getErrorColumn() {
        return errorColumn;
    }

    public String sql() {
        return sql;
    }

    public String getSql() {
        return sql;
    }

    public String exception() {
        return exception;
    }

    public String getException() {
        return exception;
    }

    public SparkSqlValidationResult withSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public SparkSqlValidationResult withFailedType(SparkSqlValidationFailedType failedType) {
        this.failedType = failedType;
        return this;
    }

    public SparkSqlValidationResult withLineStart(int lineStart) {
        this.lineStart = lineStart;
        return this;
    }

    public SparkSqlValidationResult withLineEnd(int lineEnd) {
        this.lineEnd = lineEnd;
        return this;
    }

    public SparkSqlValidationResult withErrorLine(int errorLine) {
        this.errorLine = errorLine;
        return this;
    }

    public SparkSqlValidationResult withErrorColumn(int errorColumn) {
        this.errorColumn = errorColumn;
        return this;
    }

    public SparkSqlValidationResult withSql(String sql) {
        this.sql = sql;
        return this;
    }

    public SparkSqlValidationResult withException(String exception) {
        this.exception = exception;
        return this;
    }
}
