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

package org.apache.streampark.console.base.config;

import org.apache.streampark.common.configuration.ConfigOption;
import org.apache.streampark.common.configuration.ConfigOptions;
import org.apache.streampark.common.configuration.DataSize;

/** Configuration options owned by the StreamPark console service. */
public final class ConsoleOptions {

    public static final ConfigOption<DataSize> BUILD_LOG_READ_MAX_SIZE =
        ConfigOptions.key("streampark.read-log.max-size")
            .dataSizeType()
            .defaultValue(DataSize.ofMebiBytes(1))
            .check(value -> value.bytes() > 0, "read size must be greater than zero")
            .withDescription("Maximum number of build-log bytes returned by one read operation.")
            .build();

    public static final ConfigOption<String> DATABASE_DIALECT =
        ConfigOptions.key("spring.profiles.active")
            .stringType()
            .defaultValue("h2")
            .withDescription("Active database profile used by the console service.")
            .build();

    private ConsoleOptions() {
    }
}
