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

/** Spark execution mode enum. */
public enum SparkExecutionMode {

    /** Unknown Mode */
    UNKNOWN(-1, "Unknown"),

    /** Local mode */
    LOCAL(0, "local"),

    /** remote */
    REMOTE(1, "remote"),

    /** yarn-cluster mode */
    YARN_CLUSTER(2, "yarn-cluster"),

    /** yarn client */
    YARN_CLIENT(3, "yarn-client");

    private final Integer mode;

    private final String name;

    SparkExecutionMode(@Nonnull Integer mode, @Nonnull String name) {
        this.mode = mode;
        this.name = name;
    }

    /**
     * Try to resolve the mode value into {@link SparkExecutionMode}.
     *
     * @param value The mode value of potential spark execution mode.
     * @return The parsed spark execution mode enum.
     */
    @Nonnull
    public static SparkExecutionMode of(@Nullable Integer value) {
        for (SparkExecutionMode mode : values()) {
            if (mode.mode.equals(value)) {
                return mode;
            }
        }
        return SparkExecutionMode.UNKNOWN;
    }

    /**
     * Try to resolve the mode name into {@link SparkExecutionMode}.
     *
     * @param name The mode name of potential spark execution mode.
     * @return The parsed spark execution mode enum.
     */
    @Nonnull
    public static SparkExecutionMode of(@Nullable String name) {
        for (SparkExecutionMode mode : values()) {
            if (mode.name.equals(name)) {
                return mode;
            }
        }
        return SparkExecutionMode.UNKNOWN;
    }

    public int getMode() {
        return mode;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    /**
     * Judge the given mode whether is yarn mode.
     *
     * @param mode The given mode.
     * @return The judged result.
     */
    public static boolean isYarnMode(@Nullable SparkExecutionMode mode) {
        return YARN_CLUSTER == mode || YARN_CLIENT == mode;
    }

    /**
     * Judge the mode value whether is yarn execution mode.
     *
     * @param value The mode value of potential spark execution mode.
     * @return The judged result.
     */
    public static boolean isYarnMode(@Nullable Integer value) {
        return isYarnMode(of(value));
    }

    /** Judge the given spark execution mode value whether is remote execution mode. */
    public static boolean isRemoteMode(@Nullable Integer value) {
        return isRemoteMode(of(value));
    }

    /** Judge the given spark execution mode whether is remote execution mode. */
    public static boolean isRemoteMode(@Nullable SparkExecutionMode mode) {
        return REMOTE == mode;
    }
}
