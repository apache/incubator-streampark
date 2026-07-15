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

/** Flink SQL validation result. */
public class FlinkSqlValidationResult {

    public final boolean success;
    public final FlinkSqlValidationFailedType failedType;
    public final int lineStart;
    public final int lineEnd;
    public final int errorLine;
    public final int errorColumn;
    public final String sql;
    public final String exception;

    private FlinkSqlValidationResult(Builder builder) {
        this.success = builder.success;
        this.failedType = builder.failedType;
        this.lineStart = builder.lineStart;
        this.lineEnd = builder.lineEnd;
        this.errorLine = builder.errorLine;
        this.errorColumn = builder.errorColumn;
        this.sql = builder.sql;
        this.exception = builder.exception;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Create a successful validation result with default values. */
    public static FlinkSqlValidationResult ok() {
        return builder().build();
    }

    public boolean success() {
        return success;
    }

    public FlinkSqlValidationFailedType failedType() {
        return failedType;
    }

    public int lineStart() {
        return lineStart;
    }

    public int lineEnd() {
        return lineEnd;
    }

    public int errorLine() {
        return errorLine;
    }

    public int errorColumn() {
        return errorColumn;
    }

    public String sql() {
        return sql;
    }

    public String exception() {
        return exception;
    }

    /** Builder for {@link FlinkSqlValidationResult}. */
    public static class Builder {

        private boolean success = true;
        private FlinkSqlValidationFailedType failedType;
        private int lineStart;
        private int lineEnd;
        private int errorLine;
        private int errorColumn;
        private String sql;
        private String exception;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder failedType(FlinkSqlValidationFailedType failedType) {
            this.failedType = failedType;
            return this;
        }

        public Builder lineStart(int lineStart) {
            this.lineStart = lineStart;
            return this;
        }

        public Builder lineEnd(int lineEnd) {
            this.lineEnd = lineEnd;
            return this;
        }

        public Builder errorLine(int errorLine) {
            this.errorLine = errorLine;
            return this;
        }

        public Builder errorColumn(int errorColumn) {
            this.errorColumn = errorColumn;
            return this;
        }

        public Builder sql(String sql) {
            this.sql = sql;
            return this;
        }

        public Builder exception(String exception) {
            this.exception = exception;
            return this;
        }

        public FlinkSqlValidationResult build() {
            return new FlinkSqlValidationResult(this);
        }
    }
}
