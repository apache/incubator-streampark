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

package org.apache.streampark.common.enums;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** The spark deployment mode enum. */
public enum SparkJobType {

    /** Unknown type replace null */
    UNKNOWN("Unknown", -1),

    /** Spark Jar */
    SPARK_JAR("Spark Jar", 1),

    /** Spark SQL */
    SPARK_SQL("Spark SQL", 2),

    /** Py spark Mode */
    PYSPARK("Python Spark", 3);

    private final String name;

    private final Integer mode;

    SparkJobType(@Nonnull String name, @Nonnull Integer mode) {
        this.name = name;
        this.mode = mode;
    }

    /**
     * Try to resolve the mode value into {@link SparkJobType}.
     *
     * @param value The mode value of potential spark deployment mode.
     * @return The parsed spark deployment mode.
     */
    @Nonnull
    public static SparkJobType valueOf(@Nullable Integer value) {
        for (SparkJobType sparkJobType : values()) {
            if (sparkJobType.mode.equals(value)) {
                return sparkJobType;
            }
        }
        return SparkJobType.UNKNOWN;
    }

    /** Get the mode value of the current {@link SparkJobType} enum. */
    @Nonnull
    public Integer getMode() {
        return mode;
    }
}
