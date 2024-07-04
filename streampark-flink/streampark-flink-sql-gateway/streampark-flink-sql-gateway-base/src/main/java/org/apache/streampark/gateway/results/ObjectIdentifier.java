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
import java.util.Objects;

public final class ObjectIdentifier implements Serializable {

    static final String UNKNOWN = "<UNKNOWN>";

    private final @Nullable String catalogName;
    private final @Nullable String databaseName;
    private final String objectName;

    public ObjectIdentifier(
                            @Nullable String catalogName, @Nullable String databaseName, String objectName) {
        this.catalogName = catalogName;
        this.databaseName = databaseName;
        this.objectName = objectName;
    }

    @Nullable
    public String getCatalogName() {
        return catalogName;
    }

    @Nullable
    public String getDatabaseName() {
        return databaseName;
    }

    public String getObjectName() {
        return objectName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ObjectIdentifier that = (ObjectIdentifier) o;
        return Objects.equals(catalogName, that.catalogName)
                && Objects.equals(databaseName, that.databaseName)
                && Objects.equals(objectName, that.objectName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(catalogName, databaseName, objectName);
    }

    @Override
    public String toString() {
        return "ObjectIdentifier{"
                + "catalogName='"
                + catalogName
                + '\''
                + ", databaseName='"
                + databaseName
                + '\''
                + ", objectName='"
                + objectName
                + '\''
                + '}';
    }
}
