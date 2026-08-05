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

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.enums.PlannerType;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.common.util.StreamParkLoggerFactory;
import org.apache.streampark.flink.core.conf.FlinkConfiguration;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableConfig;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Initializes Flink table and stream-table environments from application arguments. */
public class FlinkTableInitializer extends FlinkStreamingInitializer {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory()
            .getLogger(FlinkTableInitializer.class.getName());

    private TableEnvConfigFunction javaTableEnvConfFunc;

    private EnvironmentSettings.Builder envSettingsBuilder;

    private TableEnvironment tableEnv;

    private StreamTableEnvironment streamTableEnv;

    FlinkTableInitializer(String[] args) {
        super(args);
    }

    public static TableInitResult initialize(TableEnvConfig args) {
        FlinkTableInitializer flinkInitializer = new FlinkTableInitializer(args.args);
        flinkInitializer.javaTableEnvConfFunc = args.conf;
        return new TableInitResult(
            flinkInitializer.getConfiguration().parameter, flinkInitializer.getTableEnv());
    }

    public static StreamTableInitResult initialize(StreamTableEnvConfig args) {
        FlinkTableInitializer flinkInitializer = new FlinkTableInitializer(args.args);
        flinkInitializer.javaStreamEnvConfFunc = args.streamConfig;
        flinkInitializer.javaTableEnvConfFunc = args.tableConfig;
        return new StreamTableInitResult(
            flinkInitializer.getConfiguration().parameter,
            flinkInitializer.getStreamEnv(),
            flinkInitializer.getStreamTableEnv());
    }

    public static StreamTableInitResult initialize(
                                                   String[] args,
                                                   StreamEnvConfigFunction streamConfig,
                                                   TableEnvConfigFunction tableConfig) {
        FlinkTableInitializer flinkInitializer = new FlinkTableInitializer(args);
        flinkInitializer.javaStreamEnvConfFunc = streamConfig;
        flinkInitializer.javaTableEnvConfFunc = tableConfig;
        return new StreamTableInitResult(
            flinkInitializer.getConfiguration().parameter,
            flinkInitializer.getStreamEnv(),
            flinkInitializer.getStreamTableEnv());
    }

    TableEnvironment getTableEnv() {
        if (tableEnv == null) {
            LOG.info("job working in batch mode");
            EnvironmentSettings.Builder builder = getEnvSettingsBuilder();
            builder.inBatchMode();
            tableEnv =
                FlinkParameterUtils.setAppName(
                    TableEnvironment.create(builder.build()), getParameter());
            applyTableEnvConfig(tableEnv.getConfig());
        }
        return tableEnv;
    }

    StreamTableEnvironment getStreamTableEnv() {
        if (streamTableEnv == null) {
            LOG.info("components should work in streaming mode");
            EnvironmentSettings.Builder builder = getEnvSettingsBuilder();
            builder.inStreamingMode();
            EnvironmentSettings setting = builder.build();

            if (javaStreamEnvConfFunc != null) {
                javaStreamEnvConfFunc.configuration(getStreamEnv(), getParameter());
            }
            streamTableEnv =
                FlinkParameterUtils.setAppName(
                    StreamTableEnvironment.create(getStreamEnv(), setting), getParameter());
            applyTableEnvConfig(streamTableEnv.getConfig());
        }
        return streamTableEnv;
    }

    private void applyTableEnvConfig(TableConfig config) {
        if (javaTableEnvConfFunc != null) {
            javaTableEnvConfFunc.configuration(config, getParameter());
        }
    }

    private EnvironmentSettings.Builder getEnvSettingsBuilder() {
        if (envSettingsBuilder == null) {
            envSettingsBuilder = buildEnvSettings(getParameter());
        }
        return envSettingsBuilder;
    }

    private EnvironmentSettings.Builder buildEnvSettings(ParameterTool parameter) {
        EnvironmentSettings.Builder builder = EnvironmentSettings.newInstance();

        PlannerType plannerType = PlannerType.BLINK;
        String plannerName = parameter.get(ConfigKeys.KEY_FLINK_TABLE_PLANNER(), null);
        if (plannerName != null && !plannerName.isEmpty()) {
            try {
                plannerType = PlannerType.withName(plannerName);
            } catch (IllegalArgumentException e) {
                plannerType = PlannerType.BLINK;
            }
        }

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

        String flinkConf = parameter.get(ConfigKeys.KEY_FLINK_CONF(), null);
        if (flinkConf == null || flinkConf.isEmpty()) {
            throw new ExceptionInInitializerError(
                "[StreamPark] Usage:can't find config,please set \"--flink.conf $conf \" in main arguments");
        }
        builder.withConfiguration(
            Configuration.fromMap(
                PropertiesUtils.fromYamlText(DeflaterUtils.unzipString(flinkConf))));

        String catalog = parameter.get(ConfigKeys.KEY_FLINK_TABLE_CATALOG(), null);
        String database = parameter.get(ConfigKeys.KEY_FLINK_TABLE_DATABASE(), null);
        if (catalog != null && database != null) {
            LOG.info("with built in catalog: {}", catalog);
            LOG.info("with built in database: {}", database);
            builder.withBuiltInCatalogName(catalog);
            builder.withBuiltInDatabaseName(database);
        } else if (catalog != null) {
            LOG.info("with built in catalog: {}", catalog);
            builder.withBuiltInCatalogName(catalog);
        } else if (database != null) {
            LOG.info("with built in database: {}", database);
            builder.withBuiltInDatabaseName(database);
        }
        return builder;
    }

    private void invokePlannerMethod(
                                     EnvironmentSettings.Builder builder, String methodName, String successMessage) {
        try {
            Method method = builder.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(builder);
            if (successMessage != null) {
                LOG.info(successMessage);
            }
        } catch (NoSuchMethodException e) {
            LOG.warn("{} deprecated", methodName);
        } catch (ReflectiveOperationException e) {
            LOG.warn("Failed to invoke {} on EnvironmentSettings.Builder", methodName, e);
        }
    }

    @Override
    FlinkConfiguration initParameter() {
        ParameterTool argsMap = ParameterTool.fromArgs(args);
        String configFile = argsMap.get(ConfigKeys.KEY_APP_CONF(), null);
        FlinkConfiguration configuration;
        if (configFile == null || configFile.isEmpty()) {
            LOG.warn("Usage:can't find config,you can set \"--conf $path \" in main arguments");
            ParameterTool parameter = ParameterTool.fromSystemProperties().mergeWith(argsMap);
            configuration =
                new FlinkConfiguration(parameter, new Configuration(), new Configuration());
        } else {
            Map<String, String> configMap = parseConfig(configFile);
            Map<String, String> sqlConf = new HashMap<>();
            configMap.forEach(
                (key, value) -> {
                    if (key.startsWith(ConfigKeys.KEY_SQL_PREFIX())) {
                        sqlConf.put(key.substring(ConfigKeys.KEY_SQL_PREFIX().length()), value);
                    }
                });

            Map<String, String> properConf =
                extractConfigByPrefix(configMap, ConfigKeys.KEY_FLINK_PROPERTY_PREFIX());
            Map<String, String> appConf =
                extractConfigByPrefix(configMap, ConfigKeys.KEY_APP_PREFIX());
            Map<String, String> tableConf =
                extractConfigByPrefix(configMap, ConfigKeys.KEY_FLINK_TABLE_PREFIX());

            Configuration tableConfig = Configuration.fromMap(tableConf);
            Configuration envConfig = Configuration.fromMap(properConf);

            ParameterTool parameter =
                ParameterTool.fromSystemProperties()
                    .mergeWith(ParameterTool.fromMap(properConf))
                    .mergeWith(ParameterTool.fromMap(tableConf))
                    .mergeWith(ParameterTool.fromMap(appConf))
                    .mergeWith(ParameterTool.fromMap(sqlConf))
                    .mergeWith(argsMap);

            configuration = new FlinkConfiguration(parameter, envConfig, tableConfig);
        }

        String flinkSql = configuration.parameter.get(ConfigKeys.KEY_FLINK_SQL(), null);
        if (flinkSql == null) {
            return configuration;
        }

        try {
            String value = DeflaterUtils.unzipString(flinkSql);
            return configuration.withParameter(
                configuration.parameter.mergeWith(
                    ParameterTool.fromMap(
                        Map.of(ConfigKeys.KEY_FLINK_SQL(), value))));
        } catch (Exception ignored) {
            File sqlFile = new File(flinkSql);
            try {
                Map<String, String> value =
                    PropertiesUtils.fromYamlFile(sqlFile.getAbsolutePath());
                return configuration.withParameter(
                    configuration.parameter.mergeWith(ParameterTool.fromMap(value)));
            } catch (Exception e) {
                throw new IllegalArgumentException("[StreamPark] init sql error." + e);
            }
        }
    }

    /** Table initialization result. */
    public static final class TableInitResult {

        public final ParameterTool parameter;
        public final TableEnvironment tableEnv;

        public TableInitResult(ParameterTool parameter, TableEnvironment tableEnv) {
            this.parameter = parameter;
            this.tableEnv = tableEnv;
        }
    }

    /** Stream-table initialization result. */
    public static final class StreamTableInitResult {

        public final ParameterTool parameter;
        public final StreamExecutionEnvironment streamEnv;
        public final StreamTableEnvironment streamTableEnv;

        public StreamTableInitResult(
                                     ParameterTool parameter,
                                     StreamExecutionEnvironment streamEnv,
                                     StreamTableEnvironment streamTableEnv) {
            this.parameter = parameter;
            this.streamEnv = streamEnv;
            this.streamTableEnv = streamTableEnv;
        }
    }
}
