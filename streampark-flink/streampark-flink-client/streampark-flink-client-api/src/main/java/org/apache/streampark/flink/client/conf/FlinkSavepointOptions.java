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

package org.apache.streampark.flink.client.conf;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;

/**
 * The savepoint restore options, declared here rather than taken from Flink.
 *
 * <p>Flink kept moving the class that declares them — {@code
 * org.apache.flink.runtime.jobgraph.SavepointConfigOptions} through 1.x, {@code
 * org.apache.flink.configuration.StateRecoveryOptions} in 2.x — while keeping the option *keys*
 * byte-identical across every version this project supports. Since this module is compiled once
 * against a single baseline Flink but submits to whichever version the user registered, referring
 * to either class binds the client to one version family and fails against the other with a
 * {@code NoClassDefFoundError} at submission time. Declaring the options from their keys sidesteps
 * that entirely: a {@code Configuration} is keyed by string, so a locally declared option addresses
 * exactly the same setting as Flink's own.
 *
 * <p>The keys are part of Flink's public configuration surface, so they are as stable as the user's
 * own {@code flink-conf.yaml} entries.
 */
public final class FlinkSavepointOptions {

    /** Mirrors Flink's {@code execution.savepoint.path}. */
    public static final ConfigOption<String> SAVEPOINT_PATH =
        ConfigOptions.key("execution.savepoint.path")
            .stringType()
            .noDefaultValue()
            .withDescription("Path to a savepoint to restore the job from.");

    /** Mirrors Flink's {@code execution.savepoint.ignore-unclaimed-state}. */
    public static final ConfigOption<Boolean> SAVEPOINT_IGNORE_UNCLAIMED_STATE =
        ConfigOptions.key("execution.savepoint.ignore-unclaimed-state")
            .booleanType()
            .defaultValue(false)
            .withDescription("Allow to skip savepoint state that cannot be restored.");

    private FlinkSavepointOptions() {
    }
}
