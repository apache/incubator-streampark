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

import org.apache.streampark.common.configuration.CommandLineParser;
import org.apache.streampark.common.configuration.ConfigException;
import org.apache.streampark.common.configuration.Configuration;
import org.apache.streampark.common.configuration.ConfigurationFormat;
import org.apache.streampark.common.configuration.ConfigurationParser;
import org.apache.streampark.common.configuration.FlinkOptions;
import org.apache.streampark.common.configuration.option.ApplicationOptions;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.HdfsUtils;
import org.apache.streampark.flink.configuration.FlinkJobParameters;
import org.apache.streampark.flink.configuration.FlinkRuntimeConfiguration;
import org.apache.streampark.flink.core.bean.StreamContextSpec;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Builds the configuration and execution environment for a Flink streaming application.
 *
 * <p>Application files are split into explicit namespaces. {@code flink.property.*} becomes native
 * Flink configuration, {@code app.*} becomes user application parameters, and command-line values
 * override both. This is the only place where those source layers are assembled.
 */
public class FlinkStreamInitializer {

    final String[] args;

    private FlinkRuntimeConfiguration configuration;
    private StreamExecutionEnvironment streamEnv;

    FlinkStreamInitializer(String[] args) {
        this.args = args.clone();
    }

    public static StreamContextSpec initializeStream(String[] args) {
        FlinkStreamInitializer initializer = new FlinkStreamInitializer(args);
        return new StreamContextSpec(
            initializer.getConfiguration().jobParameters(), initializer.getStreamEnv());
    }

    FlinkJobParameters getParameter() {
        return getConfiguration().jobParameters();
    }

    FlinkRuntimeConfiguration getConfiguration() {
        if (configuration == null) {
            configuration = initParameter();
        }
        return configuration;
    }

    StreamExecutionEnvironment getStreamEnv() {
        if (streamEnv == null) {
            streamEnv =
                StreamExecutionEnvironment.getExecutionEnvironment(
                    getConfiguration().environmentConfiguration());
            streamEnv.getConfig().setGlobalJobParameters(getParameter());
        }
        return streamEnv;
    }

    FlinkRuntimeConfiguration initParameter() {
        Configuration arguments = CommandLineParser.parse(args);
        String configResource =
            arguments
                .getOptional(ApplicationOptions.CONFIG)
                .orElseThrow(
                    () -> new ConfigException(
                        "Application configuration is required; use --"
                            + ApplicationOptions.CONFIG.key()
                            + " <resource>"));

        Configuration document = parseConfig(configResource);
        Configuration flinkProperties = document.subset(FlinkOptions.PROPERTY_PREFIX.value());
        Configuration applicationProperties =
            document.subset(ApplicationOptions.APPLICATION_PREFIX.value());

        // Native properties are also visible as job parameters for keys such as pipeline.name.
        Configuration applicationConfiguration =
            Configuration.builder()
                .add(flinkProperties)
                .add(applicationProperties)
                .add(arguments)
                .build();
        org.apache.flink.configuration.Configuration flinkConfiguration =
            org.apache.flink.configuration.Configuration.fromMap(flinkProperties.toMap());
        return new FlinkRuntimeConfiguration(
            applicationConfiguration, flinkConfiguration, null);
    }

    /** Parses an inline, local, or HDFS application configuration resource. */
    Configuration parseConfig(String resource) {
        if (resource.startsWith("yaml://")) {
            return parseCompressed(resource, "yaml://", ConfigurationFormat.YAML);
        }
        if (resource.startsWith("conf://")) {
            return parseCompressed(resource, "conf://", ConfigurationFormat.HOCON);
        }
        if (resource.startsWith("prop://")) {
            return parseCompressed(resource, "prop://", ConfigurationFormat.PROPERTIES);
        }
        if (resource.startsWith("hdfs://")) {
            try {
                return ConfigurationParser.parse(
                    HdfsUtils.read(resource), formatOf(resource), resource);
            } catch (IOException e) {
                throw new ConfigException(
                    "Cannot read application configuration from HDFS: " + resource, e);
            }
        }
        return ConfigurationParser.parse(Path.of(resource));
    }

    private static Configuration parseCompressed(
                                                 String resource,
                                                 String scheme,
                                                 ConfigurationFormat format) {
        try {
            String content = DeflaterUtils.unzipString(resource.substring(scheme.length()));
            return ConfigurationParser.parse(content, format, "inline " + format.name().toLowerCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new ConfigException("Cannot decode inline " + format + " configuration", e);
        }
    }

    private static ConfigurationFormat formatOf(String resource) {
        String normalized = resource.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".yaml") || normalized.endsWith(".yml")) {
            return ConfigurationFormat.YAML;
        }
        if (normalized.endsWith(".conf") || normalized.endsWith(".hocon")) {
            return ConfigurationFormat.HOCON;
        }
        if (normalized.endsWith(".properties")) {
            return ConfigurationFormat.PROPERTIES;
        }
        throw new ConfigException(
            "Application configuration must be YAML, HOCON, or properties: " + resource);
    }
}
