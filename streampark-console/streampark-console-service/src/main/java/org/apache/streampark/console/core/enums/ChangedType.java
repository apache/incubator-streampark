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

package org.apache.streampark.console.core.enums;

import java.io.Serializable;
import java.util.Arrays;

public enum ChangedType implements Serializable {
    /**
     * none changed
     */
    NONE(0),

    /**
     * dependency has changed
     */
    DEPENDENCY(1),

    /**
     * sql has changed
     */
    SQL(2),

    /**
     * both
     */
    ALL(3);


    private final int value;

    ChangedType(int value) {
        this.value = value;
    }

    public int get() {
        return this.value;
    }

    public static ChangedType of(Integer value) {
        return Arrays.stream(values()).filter((x) -> x.value == value).findFirst().orElse(null);
    }

    public boolean noChanged() {
        return this.equals(NONE);
    }

    public boolean hasChanged() {
        return !noChanged();
    }

    public boolean isDependencyChanged() {
        return this.equals(ALL) || this.equals(DEPENDENCY);
    }

    @Override
    public String toString() {
        switch (this) {
            case NONE:
                return "[NONE], nothing to changed";
            case DEPENDENCY:
                return "[DEPENDENCY], Dependency is changed";
            case SQL:
                return "[SQL], Flink Sql is changed";
            case ALL:
                return "[ALL], Dependency and Flink Sql all changed";
            default:
                return null;
        }
    }
}
