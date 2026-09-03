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
import org.apache.streampark.common.configuration.ConfigurationParser;
import org.apache.streampark.common.configuration.FlinkOptions;
import org.apache.streampark.common.configuration.option.ApplicationOptions;
import org.apache.streampark.common.enums.PlannerType;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.FlinkConfigurationUtils;
import org.apache.streampark.common.util.StreamParkLoggerFactory;
import org.apache.streampark.flink.configuration.FlinkJobParameters;
import org.apache.streampark.flink.configuration.FlinkRuntimeConfiguration;
import org.apache.streampark.flink.core.bean.StreamTableContextSpec;
import org.apache.streampark.flink.core.bean.TableContextSpec;
import org.apache.streampark.flink.util.FlinkParameterUtils;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;

/**
 * Builds batch-table and stream-table environments from namespaced application configuration.
 *
 * <p>Table configuration is retained separately from application parameters. The separation keeps
 * keys such as {@code planner} and {@code catalog} from leaking into user job parameters while
 * still allowing explicit command-line overrides through their full option names.
 */
public class FlinkTableInitializer extends FlinkStreamInitializer {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(FlinkTableInitializer.class.getName());

    private EnvironmentSettings.Builder envSettingsBuilder;
    private TableEnvironment tableEnv;
    private StreamTableEnvironment streamTableEnv;

    FlinkTableInitializer(String[] args) {
        super(args);
    }

    public static TableContextSpec initializeTable(String[] args) {
        FlinkTableInitializer initializer = new FlinkTableInitializer(args);
        return new TableContextSpec(
            initializer.getConfiguration().jobParameters(), initializer.getTableEnv());
    }

    public static StreamTableContextSpec initializeStreamTable(String[] args) {
        FlinkTableInitializer initializer = new FlinkTableInitializer(args);
        return new StreamTableContextSpec(
            initializer.getConfiguration().jobParameters(),
            initializer.getStreamEnv(),
            initializer.getStreamTableEnv());
    }

    TableEnvironment getTableEnv() {
        if (tableEnv == null) {
            LOG.info("job working in batch mode");
            EnvironmentSettings.Builder builder = getEnvSettingsBuilder();
            builder.inBatchMode();
            tableEnv =
                FlinkParameterUtils.setAppName(
                    TableEnvironment.create(builder.build()), getParameter());
        }
        return tableEnv;
    }

    StreamTableEnvironment getStreamTableEnv() {
        if (streamTableEnv == null) {
            LOG.info("components should work in streaming mode");
            EnvironmentSettings.Builder builder = getEnvSettingsBuilder();
            builder.inStreamingMode();
            streamTableEnv =
                FlinkParameterUtils.setAppName(
                    StreamTableEnvironment.create(getStreamEnv(), builder.build()),
                    getParameter());
        }
        return streamTableEnv;
    }

    private EnvironmentSettings.Builder getEnvSettingsBuilder() {
        if (envSettingsBuilder == null) {
            envSettingsBuilder = buildEnvSettings(getConfiguration());
        }
        return envSettingsBuilder;
    }

    private EnvironmentSettings.Builder buildEnvSettings(
                                                         FlinkRuntimeConfiguration runtimeConfiguration) {
        EnvironmentSettings.Builder builder = EnvironmentSettings.newInstance();
        FlinkJobParameters parameters = runtimeConfiguration.jobParameters();
        Configuration tableConfiguration = tableConfiguration(runtimeConfiguration);

        String plannerName =
            parameters
                .getOptional(FlinkOptions.TABLE_PLANNER)
                .orElseGet(() -> tableConfiguration.getOptionalString("planner").orElse(null));
        PlannerType plannerType = plannerType(plannerName);
        switch (plannerType) {
            case BLINK:
                invokePlannerMethod(builder, "useBlinkPlanner", "blinkPlanner will be used.");
                break;
            case OLD:
                invokePlannerMethod(builder, "useOldPlanner", "useOldPlanner will be used.");
                break;
            case ANY:
                invokePlannerMethod(builder, "useAnyPlanner", "useAnyPlanner will be used.");
                break;
            default:
                break;
        }

        String flinkConf =
            parameters
                .getOptional(FlinkOptions.FLINK_CONFIGURATION)
                .orElseThrow(
                    () -> new ConfigException(
                        "Table applications require --"
                            + FlinkOptions.FLINK_CONFIGURATION.key()));
        // Native Flink YAML requires Flink-specific list serialization and compatibility parsing;
        // application YAML continues to use the engine-neutral parser.
        builder.withConfiguration(
            org.apache.flink.configuration.Configuration.fromMap(
                FlinkConfigurationUtils.loadConfigurationFromString(
                    DeflaterUtils.unzipString(flinkConf),
                    parameters.get(FlinkOptions.FLINK_CONFIGURATION_STANDARD_YAML))));

        String catalog =
            parameters
                .getOptional(FlinkOptions.TABLE_CATALOG)
                .orElseGet(() -> tableConfiguration.getOptionalString("catalog").orElse(null));
        String database =
            parameters
                .getOptional(FlinkOptions.TABLE_DATABASE)
                .orElseGet(() -> tableConfiguration.getOptionalString("database").orElse(null));
        if (catalog != null) {
            LOG.info("with built in catalog: {}", catalog);
            builder.withBuiltInCatalogName(catalog);
        }
        if (database != null) {
            LOG.info("with built in database: {}", database);
            builder.withBuiltInDatabaseName(database);
        }
        return builder;
    }

    private static PlannerType plannerType(String plannerName) {
        if (plannerName == null || plannerName.isEmpty()) {
            return PlannerType.BLINK;
        }
        try {
            return PlannerType.withName(plannerName);
        } catch (IllegalArgumentException ignored) {
            return PlannerType.BLINK;
        }
    }

    private static Configuration tableConfiguration(
                                                    FlinkRuntimeConfiguration runtimeConfiguration) {
        org.apache.flink.configuration.Configuration table =
            runtimeConfiguration.tableConfiguration();
        return table == null
            ? Configuration.empty()
            : Configuration.builder()
                .add("Flink table configuration", org.apache.streampark.common.configuration.ConfigSource.FILE,
                    table.toMap())
                .build();
    }

    private void invokePlannerMethod(
                                     EnvironmentSettings.Builder builder,
                                     String methodName,
                                     String successMessage) {
        try {
            Method method = builder.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(builder);
            LOG.info(successMessage);
        } catch (NoSuchMethodException e) {
            LOG.warn("{} deprecated", methodName);
        } catch (ReflectiveOperationException e) {
            LOG.warn("Failed to invoke {} on EnvironmentSettings.Builder", methodName, e);
        }
    }

    @Override
    FlinkRuntimeConfiguration initParameter() {
        Configuration arguments = CommandLineParser.parse(args);
        Configuration document =
            arguments
                .getOptional(FlinkOptions.APPLICATION_CONFIG)
                .map(this::parseConfig)
                .orElseGet(Configuration::empty);

        Configuration flinkProperties = document.subset(FlinkOptions.PROPERTY_PREFIX.value());
        Configuration tableProperties = document.subset(FlinkOptions.TABLE_PREFIX.value());
        Configuration applicationProperties =
            document.subset(ApplicationOptions.APPLICATION_PREFIX.value());
        Configuration sqlProperties = document.subset(ApplicationOptions.SQL_PREFIX.value());

        Configuration applicationConfiguration =
            Configuration.builder()
                .add(flinkProperties)
                .add(applicationProperties)
                .add(sqlProperties)
                .add(arguments)
                .build();
        FlinkRuntimeConfiguration assembled =
            new FlinkRuntimeConfiguration(
                applicationConfiguration,
                org.apache.flink.configuration.Configuration.fromMap(flinkProperties.toMap()),
                org.apache.flink.configuration.Configuration.fromMap(tableProperties.toMap()));
        return resolveSql(assembled);
    }

    private FlinkRuntimeConfiguration resolveSql(FlinkRuntimeConfiguration configuration) {
        String sql =
            configuration
                .applicationConfiguration()
                .getOptional(ApplicationOptions.SQL)
                .orElse(null);
        if (sql == null) {
            return configuration;
        }

        try {
            String decoded = DeflaterUtils.unzipString(sql);
            Configuration updated =
                Configuration.builder(configuration.applicationConfiguration())
                    .set(ApplicationOptions.SQL, decoded, "decoded SQL argument")
                    .build();
            return configuration.withApplicationConfiguration(updated);
        } catch (RuntimeException ignored) {
            File sqlFile = new File(sql);
            if (!sqlFile.isFile()) {
                throw new ConfigException("SQL argument is neither compressed SQL nor a file: " + sql);
            }
            Configuration sqlConfiguration = ConfigurationParser.parse(Path.of(sql));
            Configuration updated =
                Configuration.builder(configuration.applicationConfiguration())
                    .add(sqlConfiguration)
                    .build();
            return configuration.withApplicationConfiguration(updated);
        }
    }
}
