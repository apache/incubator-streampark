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

package org.apache.streampark.spark.configuration;

import org.apache.streampark.common.configuration.ConfigOption;
import org.apache.streampark.common.configuration.ConfigOptions;
import org.apache.streampark.common.configuration.ConfigPrefix;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Typed option catalog for Spark applications and Spark submission metadata.
 *
 * <p>Native Spark keys remain unchanged. Submission-only fields are declared here as explicit
 * transport metadata so that callers do not confuse them with Spark's own configuration.
 */
public final class SparkOptions {

    public static final ConfigOption<Integer> DRIVER_CORES = positiveInteger("spark.driver.cores");
    public static final ConfigOption<String> DRIVER_MEMORY = string("spark.driver.memory");
    public static final ConfigOption<Integer> EXECUTOR_INSTANCES =
        positiveInteger("spark.executor.instances");
    public static final ConfigOption<Integer> EXECUTOR_CORES =
        positiveInteger("spark.executor.cores");
    public static final ConfigOption<String> EXECUTOR_MEMORY = string("spark.executor.memory");
    public static final ConfigOption<Boolean> DYNAMIC_ALLOCATION_ENABLED =
        ConfigOptions.key("spark.dynamicAllocation.enabled")
            .booleanType()
            .defaultValue(false)
            .withDescription("Whether Spark dynamic resource allocation is enabled.")
            .build();
    public static final ConfigOption<String> DYNAMIC_ALLOCATION_MAX_EXECUTORS =
        string("spark.dynamicAllocation.maxExecutors");
    public static final ConfigOption<String> YARN_QUEUE = string("spark.yarn.queue");
    public static final ConfigOption<String> YARN_QUEUE_NAME = string("yarnQueueName");
    public static final ConfigOption<String> YARN_QUEUE_LABEL = string("yarnQueueLabel");
    public static final ConfigOption<String> YARN_AM_NODE_LABEL =
        string("spark.yarn.am.nodeLabelExpression");
    public static final ConfigOption<String> YARN_EXECUTOR_NODE_LABEL =
        string("spark.yarn.executor.nodeLabelExpression");

    public static final ConfigPrefix PROPERTY_PREFIX = ConfigPrefix.of("spark.");

    public static final List<ConfigOption<?>> ALL =
        Collections.unmodifiableList(
            Arrays.asList(
                DRIVER_CORES,
                DRIVER_MEMORY,
                EXECUTOR_INSTANCES,
                EXECUTOR_CORES,
                EXECUTOR_MEMORY,
                DYNAMIC_ALLOCATION_ENABLED,
                DYNAMIC_ALLOCATION_MAX_EXECUTORS,
                YARN_QUEUE,
                YARN_QUEUE_NAME,
                YARN_QUEUE_LABEL,
                YARN_AM_NODE_LABEL,
                YARN_EXECUTOR_NODE_LABEL));

    private SparkOptions() {
    }

    private static ConfigOption<String> string(String key) {
        return ConfigOptions.key(key)
            .stringType()
            .noDefaultValue()
            .withDescription("Spark option '" + key + "'.")
            .build();
    }

    private static ConfigOption<Integer> positiveInteger(String key) {
        return ConfigOptions.key(key)
            .intType()
            .noDefaultValue()
            .check(value -> value > 0, key + " must be greater than zero")
            .withDescription("Spark option '" + key + "'.")
            .build();
    }
}
