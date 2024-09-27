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

import com.google.common.collect.Lists;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/** Flink execution mode enum. */
public enum FlinkDeployMode {

    /** Unknown Mode */
    UNKNOWN(-1, "Unknown"),

    /** Local mode */
    LOCAL(0, "local"),

    /** remote */
    REMOTE(1, "remote"),

    /** yarn-per-job mode */
    YARN_PER_JOB(2, "yarn-per-job"),

    /** yarn session */
    YARN_SESSION(3, "yarn-session"),

    /** yarn application */
    YARN_APPLICATION(4, "yarn-application"),

    /** kubernetes session */
    KUBERNETES_NATIVE_SESSION(5, "kubernetes-session"),

    /** kubernetes application */
    KUBERNETES_NATIVE_APPLICATION(6, "kubernetes-application");
    private final Integer mode;

    private final String name;

    FlinkDeployMode(@Nonnull Integer mode, @Nonnull String name) {
        this.mode = mode;
        this.name = name;
    }

    /**
     * Try to resolve the mode value into {@link FlinkDeployMode}.
     *
     * @param value The mode value of potential flink execution mode.
     * @return The parsed flink execution mode enum.
     */
    @Nonnull
    public static FlinkDeployMode of(@Nullable Integer value) {
        for (FlinkDeployMode mode : values()) {
            if (mode.mode.equals(value)) {
                return mode;
            }
        }
        return FlinkDeployMode.UNKNOWN;
    }

    /**
     * Try to resolve the mode name into {@link FlinkDeployMode}.
     *
     * @param name The mode name of potential flink execution mode.
     * @return The parsed flink execution mode enum.
     */
    @Nonnull
    public static FlinkDeployMode of(@Nullable String name) {
        for (FlinkDeployMode mode : values()) {
            if (mode.name.equals(name)) {
                return mode;
            }
        }
        return FlinkDeployMode.UNKNOWN;
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
    public static boolean isYarnMode(@Nullable FlinkDeployMode mode) {
        return YARN_PER_JOB == mode || YARN_APPLICATION == mode || YARN_SESSION == mode;
    }

    /**
     * Judge the given mode whether is yarn per-job or application mode.
     *
     * @param mode The given mode.
     * @return The judged result. TODO: We'll inline this method back to the corresponding caller
     *     lines after dropping the yarn perjob mode.
     */
    public static boolean isYarnPerJobOrAppMode(@Nullable FlinkDeployMode mode) {
        return YARN_PER_JOB == mode || YARN_APPLICATION == mode;
    }

    /**
     * Judge the given mode whether is yarn session mode.
     *
     * @param mode The given mode.
     * @return The judged result.
     */
    public static boolean isYarnSessionMode(@Nullable FlinkDeployMode mode) {
        return YARN_SESSION == mode;
    }

    /**
     * Judge the mode value whether is yarn execution mode.
     *
     * @param value The mode value of potential flink execution mode.
     * @return The judged result.
     */
    public static boolean isYarnMode(@Nullable Integer value) {
        return isYarnMode(of(value));
    }

    /**
     * Judge the mode value whether is k8s session execution mode.
     *
     * @param value The mode value of potential flink execution mode.
     * @return The judged result.
     */
    public static boolean isKubernetesSessionMode(@Nullable Integer value) {
        return KUBERNETES_NATIVE_SESSION == of(value);
    }

    /**
     * Judge the mode whether is k8s execution mode.
     *
     * @param mode The given flink execution mode.
     * @return The judged result.
     */
    public static boolean isKubernetesMode(@Nullable FlinkDeployMode mode) {
        return KUBERNETES_NATIVE_SESSION == mode || KUBERNETES_NATIVE_APPLICATION == mode;
    }

    /**
     * Judge the mode value whether is k8s execution mode.
     *
     * @param value The mode value of potential flink execution mode.
     * @return The judged result.
     */
    public static boolean isKubernetesMode(@Nullable Integer value) {
        return isKubernetesMode(of(value));
    }

    /**
     * Judge the mode value whether is k8s application execution mode.
     *
     * @param value The mode value of potential flink execution mode.
     * @return The judged result.
     */
    public static boolean isKubernetesApplicationMode(@Nullable Integer value) {
        return KUBERNETES_NATIVE_APPLICATION == of(value);
    }

    /** Get all k8s mode values into a list. */
    @Nonnull
    public static List<Integer> getKubernetesMode() {
        return Lists.newArrayList(
            KUBERNETES_NATIVE_SESSION.getMode(), KUBERNETES_NATIVE_APPLICATION.getMode());
    }

    /** Judge the given flink execution mode whether is session execution mode. */
    public static boolean isSessionMode(@Nullable FlinkDeployMode mode) {
        return KUBERNETES_NATIVE_SESSION == mode || YARN_SESSION == mode;
    }

    /** Judge the given flink execution mode value whether is remote execution mode. */
    public static boolean isRemoteMode(@Nullable Integer value) {
        return isRemoteMode(of(value));
    }

    /** Judge the given flink execution mode whether is remote execution mode. */
    public static boolean isRemoteMode(@Nullable FlinkDeployMode mode) {
        return REMOTE == mode;
    }
}
