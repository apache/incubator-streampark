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

/** Column information. */
public class Column implements Serializable {

    private final String name;

    private final String type;

    private final @Nullable String comment;

    public Column(String name, String type, String comment) {
        this.name = name;
        this.type = type;
        this.comment = comment;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    @Nullable
    public String getComment() {
        return comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Column column = (Column) o;
        return Objects.equals(name, column.name)
            && Objects.equals(type, column.type)
            && Objects.equals(comment, column.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, comment);
    }

    @Override
    public String toString() {
        return "Column{"
            + "name='"
            + name
            + '\''
            + ", type='"
            + type
            + '\''
            + ", comment='"
            + comment
            + '\''
            + '}';
    }
}
