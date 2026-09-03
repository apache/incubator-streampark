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

package org.apache.streampark.common.configuration;

import org.apache.streampark.common.configuration.option.ApplicationOptions;

/**
 * Flink-specific application options and configuration namespaces.
 *
 * <p>Options in this catalog describe StreamPark's application contract. Native Flink properties
 * remain ordinary Flink configuration and are selected by namespace only when launcher or table
 * configuration views are assembled. Reusing engine-neutral options keeps shared keys identical
 * across Flink, Spark, console, and submission modules.
 */
public final class FlinkOptions {

    /** Engine-neutral application configuration resource. */
    public static final ConfigOption<String> APPLICATION_CONFIG = ApplicationOptions.CONFIG;

    /** Engine-neutral application name. */
    public static final ConfigOption<String> APPLICATION_NAME = ApplicationOptions.NAME;

    /** Engine-neutral SQL text or resource. */
    public static final ConfigOption<String> SQL = ApplicationOptions.SQL;

    /** Compressed native Flink YAML transported to a Table API application. */
    public static final ConfigOption<String> FLINK_CONFIGURATION =
        stringOption("flink.conf", "Compressed Flink configuration for table applications.");

    /** Parsing mode retained when compressed Flink YAML no longer has a source filename. */
    public static final ConfigOption<Boolean> FLINK_CONFIGURATION_STANDARD_YAML =
        ConfigOptions.key("flink.conf.standard-yaml")
            .booleanType()
            .defaultValue(true)
            .withDescription(
                "Whether the compressed Flink configuration uses standard rather than legacy YAML.")
            .build();

    /** Default parallelism applied to a Flink job. */
    public static final ConfigOption<Integer> PARALLELISM =
        ConfigOptions.key("parallelism.default")
            .intType()
            .noDefaultValue()
            .check(value -> value > 0, "parallelism must be greater than zero")
            .withDescription("Default Flink job parallelism.")
            .build();

    /** Pipeline name forwarded to native Flink configuration. */
    public static final ConfigOption<String> PIPELINE_NAME =
        stringOption("pipeline.name", "Flink pipeline name.");

    /** Planner selected while constructing the Table API environment. */
    public static final ConfigOption<String> TABLE_PLANNER =
        stringOption("flink.table.planner", "Planner selected for the Table API environment.");

    /** Runtime mode selected while constructing the Table API environment. */
    public static final ConfigOption<String> TABLE_MODE =
        stringOption("flink.table.mode", "Runtime mode selected for the Table API environment.");

    /** Initial catalog selected for the Table API environment. */
    public static final ConfigOption<String> TABLE_CATALOG =
        stringOption("flink.table.catalog", "Built-in Table API catalog name.");

    /** Initial database selected for the Table API environment. */
    public static final ConfigOption<String> TABLE_DATABASE =
        stringOption("flink.table.database", "Built-in Table API database name.");

    /** Container image pull policy forwarded to Kubernetes deployments. */
    public static final ConfigOption<String> KUBERNETES_IMAGE_PULL_POLICY =
        stringOption(
            "kubernetes.container.image.pull-policy",
            "Kubernetes image pull policy forwarded to Flink deployment.");

    /** Engine-neutral application entry class. */
    public static final ConfigOption<String> APPLICATION_MAIN_CLASS = ApplicationOptions.MAIN_CLASS;

    /** Namespace for Flink launcher options consumed by StreamPark scripts. */
    public static final ConfigPrefix OPTION_PREFIX = ConfigPrefix.of("flink.option.");

    /** Namespace for native Flink dynamic properties. */
    public static final ConfigPrefix PROPERTY_PREFIX = ConfigPrefix.of("flink.property.");

    /** Namespace for StreamPark Table API initialization options. */
    public static final ConfigPrefix TABLE_PREFIX = ConfigPrefix.of("flink.table.");

    private FlinkOptions() {
    }

    private static ConfigOption<String> stringOption(String key, String description) {
        return ConfigOptions.key(key)
            .stringType()
            .noDefaultValue()
            .withDescription(description)
            .build();
    }
}
