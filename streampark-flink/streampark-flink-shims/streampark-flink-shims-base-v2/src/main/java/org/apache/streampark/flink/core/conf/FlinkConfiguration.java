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

package org.apache.streampark.flink.core.conf;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.ParameterTool;

/** Flink runtime configuration holder. */
public class FlinkConfiguration {

    public final ParameterTool parameter;
    public final Configuration envConfig;
    public final Configuration tableConfig;

    public FlinkConfiguration(
                              ParameterTool parameter, Configuration envConfig, Configuration tableConfig) {
        this.parameter = parameter;
        this.envConfig = envConfig;
        this.tableConfig = tableConfig;
    }

    public FlinkConfiguration withParameter(ParameterTool parameter) {
        return new FlinkConfiguration(parameter, envConfig, tableConfig);
    }
}
