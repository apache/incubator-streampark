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

package org.apache.streampark.gateway.results;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** An implementation of {@link ResultSet}. */
public class ResultSet implements Serializable {

    /** The type of the results, which may indicate the result is EOS or has data. */
    private final ResultType resultType;

    /**
     * The token indicates the next batch of the data.
     *
     * <p>When the token is null, it means all the data has been fetched.
     */
    @Nullable
    private final Long nextToken;

    /**
     * The schema of the data.
     *
     * <p>The schema of the DDL, USE, EXPLAIN, SHOW and DESCRIBE align with the schema of the {@link
     * TableResult#getResolvedSchema()}. The only differences is the schema of the `INSERT` statement.
     *
     * <p>The schema of INSERT:
     *
     * <pre>
     * +-------------+-------------+----------+
     * | column name | column type | comments |
     * +-------------+-------------+----------+
     * |   job id    |    string   |          |
     * +- -----------+-------------+----------+
     * </pre>
     */
    private final List<Column> columns;

    /** All the data in the current results. */
    private final List<RowData> data;

    /** Indicates that whether the result is for a query. */
    private final boolean isQueryResult;
    /**
     * If the statement was submitted to a client, returns the JobID which uniquely identifies the
     * job. Otherwise, returns null.
     */
    @Nullable
    private final JobID jobID;

    /** Gets the result kind of the result. */
    private final ResultKindEnum resultKindEnum;

    public ResultSet(
                     ResultType resultType,
                     @Nullable Long nextToken,
                     List<Column> columns,
                     List<RowData> data,
                     boolean isQueryResult,
                     @Nullable JobID jobID,
                     ResultKindEnum resultKindEnum) {
        this.resultType = resultType;
        this.nextToken = nextToken;
        this.columns = columns;
        this.data = data;
        this.isQueryResult = isQueryResult;
        this.jobID = jobID;
        this.resultKindEnum = resultKindEnum;
    }

    public ResultType getResultType() {
        return resultType;
    }

    @Nullable
    public Long getNextToken() {
        return nextToken;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public List<RowData> getData() {
        return data;
    }

    public boolean isQueryResult() {
        return isQueryResult;
    }

    @Nullable
    public JobID getJobID() {
        return jobID;
    }

    public ResultKindEnum getResultKind() {
        return resultKindEnum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResultSet resultSet = (ResultSet) o;
        return isQueryResult == resultSet.isQueryResult
                && resultType == resultSet.resultType
                && Objects.equals(nextToken, resultSet.nextToken)
                && Objects.equals(columns, resultSet.columns)
                && Objects.equals(data, resultSet.data)
                && Objects.equals(jobID, resultSet.jobID)
                && resultKindEnum == resultSet.resultKindEnum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resultType, nextToken, columns, data, isQueryResult, jobID, resultKindEnum);
    }

    @Override
    public String toString() {
        return "ResultSet{"
                + "resultType="
                + resultType
                + ", nextToken="
                + nextToken
                + ", resultSchema="
                + columns
                + ", data="
                + data
                + ", isQueryResult="
                + isQueryResult
                + ", jobID="
                + jobID
                + ", resultKind="
                + resultKindEnum
                + '}';
    }

    /** Describe the kind of the result. */
    public enum ResultType {
        /** Indicate the result is not ready. */
        NOT_READY,

        /** Indicate the result has data. */
        PAYLOAD,

        /** Indicate all results have been fetched. */
        EOS
    }
}
