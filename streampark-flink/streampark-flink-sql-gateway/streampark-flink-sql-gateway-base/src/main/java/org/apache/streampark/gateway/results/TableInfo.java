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

/** Information of the table or view. */
public class TableInfo implements Serializable {

    private final ObjectIdentifier identifier;
    private final TableKindEnum tableKindEnum;

    public TableInfo(ObjectIdentifier identifier, TableKindEnum tableKindEnum) {
        this.identifier = identifier;
        this.tableKindEnum = tableKindEnum;
    }

    public ObjectIdentifier getIdentifier() {
        return identifier;
    }

    public TableKindEnum getTableKind() {
        return tableKindEnum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TableInfo tableInfo = (TableInfo) o;
        return Objects.equals(identifier, tableInfo.identifier)
            && tableKindEnum == tableInfo.tableKindEnum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, tableKindEnum);
    }

    @Override
    public String toString() {
        return "TableInfo{" + "identifier=" + identifier + ", tableKind=" + tableKindEnum + '}';
    }
}
