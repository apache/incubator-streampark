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

/** Spark SQL validation result. */
public class SparkSqlValidationResult {

    private boolean success = true;
    private SparkSqlValidationFailedType failedType;
    private int lineStart;
    private int lineEnd;
    private int errorLine;
    private int errorColumn;
    private String sql;
    private String exception;

    public static SparkSqlValidationResult ok() {
        return new SparkSqlValidationResult();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isSuccess() {
        return success;
    }

    public SparkSqlValidationFailedType getFailedType() {
        return failedType;
    }

    public int getLineStart() {
        return lineStart;
    }

    public int getLineEnd() {
        return lineEnd;
    }

    public int getErrorLine() {
        return errorLine;
    }

    public int getErrorColumn() {
        return errorColumn;
    }

    public String getSql() {
        return sql;
    }

    public String getException() {
        return exception;
    }

    public boolean success() {
        return success;
    }

    public SparkSqlValidationFailedType failedType() {
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

    public String exception() {
        return exception;
    }

    /** Builder for {@link SparkSqlValidationResult}. */
    public static final class Builder {

        private boolean success = true;
        private SparkSqlValidationFailedType failedType;
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

        public Builder failedType(SparkSqlValidationFailedType failedType) {
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

        public SparkSqlValidationResult build() {
            SparkSqlValidationResult result = new SparkSqlValidationResult();
            result.success = success;
            result.failedType = failedType;
            result.lineStart = lineStart;
            result.lineEnd = lineEnd;
            result.errorLine = errorLine;
            result.errorColumn = errorColumn;
            result.sql = sql;
            result.exception = exception;
            return result;
        }
    }
}
