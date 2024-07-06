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

/** The flink deployment mode enum. */
public enum FlinkDevelopmentMode {

    /** Unknown type replace null */
    UNKNOWN("Unknown", -1),

    /** custom code */
    CUSTOM_CODE("Custom Code", 1),

    /** Flink SQL */
    FLINK_SQL("Flink SQL", 2),

    /** Py flink Mode */
    PYFLINK("Python Flink", 3);

    private final String name;

    private final Integer mode;

    FlinkDevelopmentMode(@Nonnull String name, @Nonnull Integer mode) {
        this.name = name;
        this.mode = mode;
    }

    /**
     * Try to resolve the mode value into {@link FlinkDevelopmentMode}.
     *
     * @param value The mode value of potential flink deployment mode.
     * @return The parsed flink deployment mode.
     */
    @Nonnull
    public static FlinkDevelopmentMode of(@Nullable Integer value) {
        for (FlinkDevelopmentMode flinkDevelopmentMode : values()) {
            if (flinkDevelopmentMode.mode.equals(value)) {
                return flinkDevelopmentMode;
            }
        }
        return FlinkDevelopmentMode.UNKNOWN;
    }

    /** Get the mode value of the current {@link FlinkDevelopmentMode} enum. */
    @Nonnull
    public Integer getMode() {
        return mode;
    }
}
