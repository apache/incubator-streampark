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

package org.apache.streampark.flink.configuration;

import org.apache.streampark.common.configuration.Configuration;

import java.util.Objects;

/**
 * Complete, immutable configuration assembled for one Flink application initialization.
 *
 * <p>The StreamPark application snapshot remains separate from Flink's native environment and
 * table configurations so namespace extraction cannot silently overwrite application arguments.
 */
public final class FlinkRuntimeConfiguration {

    private final Configuration applicationConfiguration;
    private final FlinkJobParameters jobParameters;
    private final org.apache.flink.configuration.Configuration environmentConfiguration;
    private final org.apache.flink.configuration.Configuration tableConfiguration;

    /**
     * Creates the configuration views for one initialized application.
     *
     * @param applicationConfiguration StreamPark application parameters
     * @param environmentConfiguration native Flink execution configuration
     * @param tableConfiguration native Flink table configuration, or {@code null} for DataStream
     *     applications
     */
    public FlinkRuntimeConfiguration(
                                     Configuration applicationConfiguration,
                                     org.apache.flink.configuration.Configuration environmentConfiguration,
                                     org.apache.flink.configuration.Configuration tableConfiguration) {
        this.applicationConfiguration =
            Objects.requireNonNull(
                applicationConfiguration, "applicationConfiguration must not be null");
        this.jobParameters = FlinkJobParameters.of(applicationConfiguration);
        this.environmentConfiguration =
            Objects.requireNonNull(
                environmentConfiguration, "environmentConfiguration must not be null");
        this.tableConfiguration = tableConfiguration;
    }

    /**
     * Returns StreamPark application parameters before Flink adaptation.
     *
     * @return immutable application configuration
     */
    public Configuration applicationConfiguration() {
        return applicationConfiguration;
    }

    /**
     * Returns application parameters exposed through Flink's global job parameters.
     *
     * @return Flink parameter adapter
     */
    public FlinkJobParameters jobParameters() {
        return jobParameters;
    }

    /**
     * Returns the native Flink execution configuration.
     *
     * @return native execution configuration
     */
    public org.apache.flink.configuration.Configuration environmentConfiguration() {
        return environmentConfiguration;
    }

    /**
     * Returns the native table configuration.
     *
     * @return native table configuration, or {@code null} for DataStream applications
     */
    public org.apache.flink.configuration.Configuration tableConfiguration() {
        return tableConfiguration;
    }

    /**
     * Returns a copy with different application parameters and the same native Flink views.
     *
     * @param configuration replacement application configuration
     * @return new runtime configuration
     */
    public FlinkRuntimeConfiguration withApplicationConfiguration(Configuration configuration) {
        return new FlinkRuntimeConfiguration(
            configuration, environmentConfiguration, tableConfiguration);
    }
}
