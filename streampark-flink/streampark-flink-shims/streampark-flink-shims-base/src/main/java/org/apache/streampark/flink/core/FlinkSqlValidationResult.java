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

/** Result of Flink SQL validation (Scala case-class compatible). */
public class FlinkSqlValidationResult {

    private boolean success = true;
    private FlinkSqlValidationFailedType failedType;
    private int lineStart;
    private int lineEnd;
    private int errorLine;
    private int errorColumn;
    private String sql;
    private String exception;

    /** Default success result; mirrors the Scala case-class no-arg constructor. */
    public FlinkSqlValidationResult() {
        // Intentionally empty: fields default to a successful validation result.
    }

    /** Builds a failed validation result without exposing an oversized public constructor. */
    public static FlinkSqlValidationResult failure(
                                                   FlinkSqlValidationFailedType failedType,
                                                   int lineStart,
                                                   int lineEnd,
                                                   int errorLine,
                                                   int errorColumn,
                                                   String sql,
                                                   String exception) {
        FlinkSqlValidationResult result = new FlinkSqlValidationResult();
        result.success = false;
        result.failedType = failedType;
        result.lineStart = lineStart;
        result.lineEnd = lineEnd;
        result.errorLine = errorLine;
        result.errorColumn = errorColumn;
        result.sql = sql;
        result.exception = exception;
        return result;
    }

    /** Scala field-style access. */
    public boolean success() {
        return success;
    }

    public boolean isSuccess() {
        return success;
    }

    public FlinkSqlValidationFailedType failedType() {
        return failedType;
    }

    public FlinkSqlValidationFailedType getFailedType() {
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

    public FlinkSqlValidationResult withSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public FlinkSqlValidationResult withFailedType(FlinkSqlValidationFailedType failedType) {
        this.failedType = failedType;
        return this;
    }

    public FlinkSqlValidationResult withLineStart(int lineStart) {
        this.lineStart = lineStart;
        return this;
    }

    public FlinkSqlValidationResult withLineEnd(int lineEnd) {
        this.lineEnd = lineEnd;
        return this;
    }

    public FlinkSqlValidationResult withErrorLine(int errorLine) {
        this.errorLine = errorLine;
        return this;
    }

    public FlinkSqlValidationResult withErrorColumn(int errorColumn) {
        this.errorColumn = errorColumn;
        return this;
    }

    public FlinkSqlValidationResult withSql(String sql) {
        this.sql = sql;
        return this;
    }

    public FlinkSqlValidationResult withException(String exception) {
        this.exception = exception;
        return this;
    }
}
