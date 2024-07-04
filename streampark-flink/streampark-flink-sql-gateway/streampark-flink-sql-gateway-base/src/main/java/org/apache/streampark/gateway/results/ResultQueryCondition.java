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

import java.io.Serializable;
import java.util.Objects;

/** Condition of result query. */
public class ResultQueryCondition implements Serializable {

    public FetchOrientationEnum orientation;

    public long token;
    public int maxRows;

    public ResultQueryCondition() {
    }

    public ResultQueryCondition(FetchOrientationEnum orientation, long token, int maxRows) {
        this.orientation = orientation;
        this.token = token;
        this.maxRows = maxRows;
    }

    public FetchOrientationEnum getOrientation() {
        return orientation;
    }

    public void setOrientation(FetchOrientationEnum orientation) {
        this.orientation = orientation;
    }

    public long getToken() {
        return token;
    }

    public void setToken(long token) {
        this.token = token;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResultQueryCondition that = (ResultQueryCondition) o;
        return token == that.token && maxRows == that.maxRows && orientation == that.orientation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(orientation, token, maxRows);
    }

    @Override
    public String toString() {
        return "ResultQueryCondition{"
                + "orientation="
                + orientation
                + ", token="
                + token
                + ", maxRows="
                + maxRows
                + '}';
    }
}
