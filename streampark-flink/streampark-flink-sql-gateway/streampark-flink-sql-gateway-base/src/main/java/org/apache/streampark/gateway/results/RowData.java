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
import java.util.List;
import java.util.Objects;

/** Row data. */
public class RowData implements Serializable {

    private final String rowKind;

    private final List<Object> fields;

    public RowData(String rowKind, List<Object> fields) {
        this.rowKind = rowKind;
        this.fields = fields;
    }

    public String getRowKind() {
        return rowKind;
    }

    public List<Object> getFields() {
        return fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RowData rowData = (RowData) o;
        return Objects.equals(rowKind, rowData.rowKind) && Objects.equals(fields, rowData.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowKind, fields);
    }

    @Override
    public String toString() {
        return "RowData{" + "rowKind='" + rowKind + '\'' + ", fields=" + fields + '}';
    }
}
