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
 * Local and remote workspace root options.
 *
 * <p>These declarations contain configured roots only. Storage-specific URI resolution and child
 * path derivation are handled by {@link org.apache.streampark.common.configuration.Workspace}.
 */
public final class WorkspaceOptions {

    /** Root directory for artifacts stored on the local filesystem. */
    public static final ConfigOption<String> LOCAL_ROOT =
        ConfigOptions.key("streampark.workspace.local")
            .stringType()
            .defaultValue("/streampark")
            .check(value -> !value.trim().isEmpty(), "workspace path must not be blank")
            .withDescription("Root directory for local StreamPark artifacts.")
            .build();

    /** Root directory or HDFS URI for remotely persisted artifacts. */
    public static final ConfigOption<String> REMOTE_ROOT =
        ConfigOptions.key("streampark.workspace.remote")
            .stringType()
            .defaultValue("/streampark")
            .check(value -> !value.trim().isEmpty(), "workspace path must not be blank")
            .withDescription("Root directory for remote StreamPark artifacts.")
            .build();

    private WorkspaceOptions() {
    }
}
