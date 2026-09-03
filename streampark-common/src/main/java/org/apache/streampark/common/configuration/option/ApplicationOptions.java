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

package org.apache.streampark.common.configuration.option;

import org.apache.streampark.common.configuration.ConfigOption;
import org.apache.streampark.common.configuration.ConfigOptions;
import org.apache.streampark.common.configuration.ConfigPrefix;

/**
 * Engine-neutral application arguments and configuration namespaces.
 *
 * <p>These options form the shared submission contract used before an application is adapted to a
 * Flink- or Spark-specific runtime.
 */
public final class ApplicationOptions {

    /** Application configuration resource supplied to an engine runtime. */
    public static final ConfigOption<String> CONFIG =
        ConfigOptions.key("conf")
            .stringType()
            .noDefaultValue()
            .withDescription("Application configuration resource.")
            .build();

    /** Logical application name shared by submission and runtime components. */
    public static final ConfigOption<String> NAME =
        ConfigOptions.key("app.name")
            .stringType()
            .noDefaultValue()
            .withDescription("Application name.")
            .build();

    /** SQL text or resource supplied to a SQL application. */
    public static final ConfigOption<String> SQL =
        ConfigOptions.key("sql")
            .stringType()
            .noDefaultValue()
            .withDescription("SQL text or SQL resource supplied to an application.")
            .build();

    /** Internal application entry class resolved by the submission client. */
    public static final ConfigOption<String> MAIN_CLASS =
        ConfigOptions.key("$internal.application.main")
            .stringType()
            .noDefaultValue()
            .withDescription("Resolved application entry class used by a submission client.")
            .build();

    /** Namespace containing general application properties. */
    public static final ConfigPrefix APPLICATION_PREFIX = ConfigPrefix.of("app.");

    /** Namespace containing SQL application properties. */
    public static final ConfigPrefix SQL_PREFIX = ConfigPrefix.of("sql.");

    private ApplicationOptions() {
    }
}
