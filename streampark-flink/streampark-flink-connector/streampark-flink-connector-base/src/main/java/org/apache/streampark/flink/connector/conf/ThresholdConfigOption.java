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

package org.apache.streampark.flink.connector.conf;

import org.apache.streampark.common.conf.ConfigOption;

import java.util.Properties;

/** Threshold configuration options for sink failover. */
public class ThresholdConfigOption {

    public final ConfigOption<Integer> bufferSize;
    public final ConfigOption<Integer> queueCapacity;
    public final ConfigOption<Long> delayTime;
    public final ConfigOption<Integer> timeout;
    public final ConfigOption<Integer> numWriters;
    public final ConfigOption<Integer> maxRetries;
    public final ConfigOption<FailoverStorageType> storageType;
    public final ConfigOption<String> failoverTable;

    public ThresholdConfigOption(String prefixStr, Properties properties) {
        Properties prop = properties != null ? properties : new Properties();
        String prefix = prefixStr != null ? prefixStr : "";

        this.bufferSize =
                ConfigOption.<Integer>builder("threshold.bufferSize")
                        .defaultValue(1000)
                        .classType(Integer.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.queueCapacity =
                ConfigOption.<Integer>builder("threshold.queueCapacity")
                        .defaultValue(10000)
                        .classType(Integer.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.delayTime =
                ConfigOption.<Long>builder("threshold.delayTime")
                        .defaultValue(1000L)
                        .classType(Long.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.timeout =
                ConfigOption.<Integer>builder("threshold.requestTimeout")
                        .defaultValue(2000)
                        .classType(Integer.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.numWriters =
                ConfigOption.<Integer>builder("threshold.numWriters")
                        .defaultValue(Runtime.getRuntime().availableProcessors())
                        .classType(Integer.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.maxRetries =
                ConfigOption.<Integer>builder("threshold.retries")
                        .defaultValue(3)
                        .classType(Integer.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.storageType =
                ConfigOption.<FailoverStorageType>builder("failover.storage")
                        .defaultValue(FailoverStorageType.NONE)
                        .classType(FailoverStorageType.class)
                        .prefix(prefix)
                        .properties(prop)
                        .handle(
                                k ->
                                        FailoverStorageType.get(
                                                prop.getProperty(k, FailoverStorageType.NONE.name())))
                        .build();

        this.failoverTable =
                ConfigOption.<String>builder("failover.table")
                        .defaultValue("")
                        .classType(String.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();
    }

    public static ThresholdConfigOption of(String prefixStr) {
        return new ThresholdConfigOption(prefixStr, new Properties());
    }

    public static ThresholdConfigOption of(String prefixStr, Properties properties) {
        return new ThresholdConfigOption(prefixStr, properties);
    }
}
