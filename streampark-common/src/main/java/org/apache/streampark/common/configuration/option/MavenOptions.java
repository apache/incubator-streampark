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
 * Maven resolver options used by built-in project compilation.
 *
 * <p>Authentication metadata is declared independently from resolver construction, and passwords
 * are marked sensitive for configuration diagnostics.
 */
public final class MavenOptions {

    /** Absolute path to Maven's settings file. */
    public static final ConfigOption<String> SETTINGS_PATH =
        ConfigOptions.key("streampark.maven.settings")
            .stringType()
            .noDefaultValue()
            .withDescription("Absolute path to Maven settings.xml.")
            .build();

    /** Remote Maven repository used by built-in compilation. */
    public static final ConfigOption<String> REPOSITORY_URL =
        ConfigOptions.key("streampark.maven.central.repository")
            .stringType()
            .defaultValue("https://repo1.maven.org/maven2/")
            .withDescription("Maven repository used by built-in compilation.")
            .build();

    /** User name used to authenticate with the configured repository. */
    public static final ConfigOption<String> USER_NAME =
        ConfigOptions.key("streampark.maven.auth.user")
            .stringType()
            .noDefaultValue()
            .withDescription("Maven repository user name.")
            .build();

    /** Sensitive password used to authenticate with the configured repository. */
    public static final ConfigOption<String> PASSWORD =
        ConfigOptions.key("streampark.maven.auth.password")
            .stringType()
            .noDefaultValue()
            .sensitive()
            .withDescription("Maven repository password.")
            .build();

    private MavenOptions() {
    }
}
