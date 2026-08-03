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

package org.apache.streampark.flink.core;

public class StreamTableEnvConfig {

    private final String[] args;
    private final StreamEnvConfigFunction streamConfig;
    private final TableEnvConfigFunction tableConfig;

    public StreamTableEnvConfig(
                                String[] args, StreamEnvConfigFunction streamConfig,
                                TableEnvConfigFunction tableConfig) {
        this.args = args;
        this.streamConfig = streamConfig;
        this.tableConfig = tableConfig;
    }

    public String[] getArgs() {
        return args;
    }

    public StreamEnvConfigFunction getStreamConfig() {
        return streamConfig;
    }

    public TableEnvConfigFunction getTableConfig() {
        return tableConfig;
    }

    /** Scala API alias for {@link #getArgs()}. */
    public String[] args() {
        return args;
    }

    /** Scala API alias for {@link #getStreamConfig()}. */
    public StreamEnvConfigFunction streamConfig() {
        return streamConfig;
    }

    /** Scala API alias for {@link #getTableConfig()}. */
    public TableEnvConfigFunction tableConfig() {
        return tableConfig;
    }
}
