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

/**
 * YARN ResourceManager connectivity and HTTP authentication options.
 *
 * <p>The authentication option is validated during typed access so unsupported mechanisms fail
 * before an HTTP client is created.
 */
public final class YarnOptions {

    /** Optional ResourceManager proxy endpoint. */
    public static final ConfigOption<String> PROXY_URL =
        ConfigOptions.key("streampark.proxy.yarn-url")
            .stringType()
            .defaultValue("")
            .withDescription("Optional YARN ResourceManager proxy URL.")
            .build();

    /** Authentication mechanism used for ResourceManager HTTP requests. */
    public static final ConfigOption<String> HTTP_AUTHENTICATION =
        ConfigOptions.key("streampark.yarn.http-auth")
            .stringType()
            .defaultValue("simple")
            .check(
                value -> "simple".equalsIgnoreCase(value) || "kerberos".equalsIgnoreCase(value),
                "authentication must be 'simple' or 'kerberos'")
            .withDescription("Authentication mechanism used for YARN HTTP requests.")
            .build();

    private YarnOptions() {
    }
}
