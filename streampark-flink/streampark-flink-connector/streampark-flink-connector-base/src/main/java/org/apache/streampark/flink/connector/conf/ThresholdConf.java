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

import org.apache.streampark.common.util.ConfigUtils;

import java.util.Properties;

/** Threshold configuration holder for sink failover. */
public class ThresholdConf {

    public final int bufferSize;
    public final int queueCapacity;
    public final long delayTime;
    public final int timeout;
    public final int numWriters;
    public final int maxRetries;
    public final FailoverStorageType storageType;
    public final String failoverTable;

    private final Properties parameters;

    public ThresholdConf(String prefixStr, Properties parameters) {
        this.parameters = parameters;
        ThresholdConfigOption option = new ThresholdConfigOption(prefixStr, parameters);
        this.bufferSize = option.bufferSize.get();
        this.queueCapacity = option.queueCapacity.get();
        this.delayTime = option.delayTime.get();
        this.timeout = option.timeout.get();
        this.numWriters = option.numWriters.get();
        this.maxRetries = option.maxRetries.get();
        this.storageType = option.storageType.get();
        this.failoverTable = option.failoverTable.get();
    }

    public Properties getFailoverConfig() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        for (String key : parameters.stringPropertyNames()) {
            map.put(key, parameters.getProperty(key));
        }
        switch (storageType) {
            case Console:
            case NONE:
                return null;
            case Kafka:
                return ConfigUtils.getConf(map, "failover.kafka.");
            case MySQL:
                return ConfigUtils.getConf(map, "failover.mysql.");
            default:
                throw new IllegalArgumentException(
                        "[StreamPark] usage error! failover.storage must not be null!");
        }
    }
}
