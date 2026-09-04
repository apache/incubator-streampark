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

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Docker HTTP client options used by the application packer.
 *
 * <p>Timeout options retain {@link Duration} values after conversion, preventing individual Docker
 * clients from interpreting bare numeric units independently.
 */
public final class DockerOptions {

    /** Docker daemon endpoint; an empty value delegates discovery to the client. */
    public static final ConfigOption<String> HOST =
        ConfigOptions.key("streampark.docker.http-client.docker-host")
            .stringType()
            .defaultValue("")
            .withDescription("Docker daemon endpoint; an empty value uses the client default.")
            .build();

    /** Maximum number of concurrent Docker HTTP connections. */
    public static final ConfigOption<Integer> MAX_CONNECTIONS =
        ConfigOptions.key("streampark.docker.http-client.max-connections")
            .intType()
            .defaultValue(100)
            .check(value -> value > 0, "maximum connections must be greater than zero")
            .withDescription("Maximum number of Docker HTTP client connections.")
            .build();

    /** Timeout for establishing a Docker daemon connection. */
    public static final ConfigOption<Duration> CONNECTION_TIMEOUT =
        ConfigOptions.key("streampark.docker.http-client.connection-timeout-sec")
            .durationType(ChronoUnit.SECONDS)
            .defaultValue(Duration.ofSeconds(100))
            .check(value -> !value.isZero(), "connection timeout must be greater than zero")
            .withDescription("Docker HTTP client connection timeout.")
            .build();

    /** Timeout for receiving a Docker daemon response. */
    public static final ConfigOption<Duration> RESPONSE_TIMEOUT =
        ConfigOptions.key("streampark.docker.http-client.response-timeout-sec")
            .durationType(ChronoUnit.SECONDS)
            .defaultValue(Duration.ofSeconds(120))
            .check(value -> !value.isZero(), "response timeout must be greater than zero")
            .withDescription("Docker HTTP client response timeout.")
            .build();

    private DockerOptions() {
    }
}
