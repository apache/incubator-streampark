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
 * Process bootstrap options required before other StreamPark services are initialized.
 *
 * <p>Declarations in this class must remain free of environment reads and filesystem access.
 */
public final class CoreOptions {

    /** StreamPark installation directory used to resolve distribution resources. */
    public static final ConfigOption<String> APP_HOME =
        ConfigOptions.key("app.home")
            .stringType()
            .noDefaultValue()
            .withDescription("StreamPark installation directory.")
            .build();

    private CoreOptions() {
    }
}
