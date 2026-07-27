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
import org.apache.streampark.common.enums.ApiType;
import org.apache.streampark.common.enums.PlannerType;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.flink.core.conf.FlinkConfiguration;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableConfig;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import scala.Tuple2;
import scala.Tuple3;

/** Initializes Flink Table / StreamTable environments from CLI args and config files. */
public class FlinkTableInitializer extends FlinkStreamingInitializer {

    private EnvironmentSettings.Builder envSettingsBuilder;
    private TableEnvironment tableEnv;
    private StreamTableEnvironment streamTableEnv;

    private FlinkTableInitializer(String[] args, ApiType apiType) {
        super(args, apiType);
    }

    public static Tuple2<ParameterTool, TableEnvironment> initialize(
                                                                     String[] args,
                                                                     scala.Function2<TableConfig, ParameterTool, scala.runtime.BoxedUnit> config) {
        FlinkTableInitializer initializer = new FlinkTableInitializer(args, ApiType.SCALA);
        initializer.tableConfFunc = config;
        return new Tuple2<>(
            initializer.getConfiguration().parameter(),
            initializer.getTableEnv());
    }

    public static Tuple2<ParameterTool, TableEnvironment> initialize(TableEnvConfig args) {
        FlinkTableInitializer initializer = new FlinkTableInitializer(args.args(), ApiType.JAVA);
        initializer.javaTableEnvConfFunc = args.conf();
        return new Tuple2<>(
            initializer.getConfiguration().parameter(),
            initializer.getTableEnv());
    }

    public static Tuple3<ParameterTool, org.apache.flink.streaming.api.scala.StreamExecutionEnvironment, StreamTableEnvironment> initialize(
                                                                                                                                            String[] args,
                                                                                                                                            scala.Function2<org.apache.flink.streaming.api.scala.StreamExecutionEnvironment, ParameterTool, scala.runtime.BoxedUnit> configStream,
                                                                                                                                            scala.Function2<TableConfig, ParameterTool, scala.runtime.BoxedUnit> configTable) {
        FlinkTableInitializer initializer = new FlinkTableInitializer(args, ApiType.SCALA);
        initializer.streamEnvConfFunc = configStream;
        initializer.tableConfFunc = configTable;
        return new Tuple3<>(
            initializer.getConfiguration().parameter(),
            initializer.getStreamEnv(),
            initializer.getStreamTableEnv());
    }

    public static Tuple3<ParameterTool, org.apache.flink.streaming.api.scala.StreamExecutionEnvironment, StreamTableEnvironment> initialize(
                                                                                                                                            StreamTableEnvConfig args) {
        FlinkTableInitializer initializer = new FlinkTableInitializer(args.args(), ApiType.JAVA);
        initializer.javaStreamEnvConfFunc = args.streamConfig();
        initializer.javaTableEnvConfFunc = args.tableConfig();
        return new Tuple3<>(
            initializer.getConfiguration().parameter(),
            initializer.getStreamEnv(),
            initializer.getStreamTableEnv());
    }

    private EnvironmentSettings.Builder getEnvSettingsBuilder() {
        if (envSettingsBuilder == null) {
            envSettingsBuilder = buildEnvSettings();
        }
        return envSettingsBuilder;
    }

    TableEnvironment getTableEnv() {
        if (tableEnv == null) {
            logInfo("job working in batch mode");
            EnvironmentSettings.Builder builder = getEnvSettingsBuilder();
            builder.inBatchMode();
            tableEnv =
                FlinkEnvironmentUtils.setAppName(
                    TableEnvironment.create(builder.build()), parameter());
            applyTableConfig(tableEnv.getConfig());
        }
        return tableEnv;
    }

    StreamTableEnvironment getStreamTableEnv() {
        if (streamTableEnv == null) {
            logInfo("components should work in streaming mode");
            EnvironmentSettings.Builder builder = getEnvSettingsBuilder();
            builder.inStreamingMode();
            EnvironmentSettings setting = builder.build();

            if (streamEnvConfFunc != null) {
                streamEnvConfFunc.apply(getStreamEnv(), parameter());
            }
            if (javaStreamEnvConfFunc != null) {
                javaStreamEnvConfFunc.configuration(getStreamEnv().getJavaEnv(), parameter());
            }
            streamTableEnv =
                FlinkEnvironmentUtils.setAppName(
                    StreamTableEnvironment.create(getStreamEnv(), setting), parameter());
            applyTableConfig(streamTableEnv.getConfig());
        }
        return streamTableEnv;
    }

    private void applyTableConfig(TableConfig config) {
        switch (apiType()) {
            case JAVA:
                if (javaTableEnvConfFunc != null) {
                    javaTableEnvConfFunc.configuration(config, parameter());
                }
                break;
            case SCALA:
                if (tableConfFunc != null) {
                    tableConfFunc.apply(config, parameter());
                }
                break;
            default:
                break;
        }
    }

    private EnvironmentSettings.Builder buildEnvSettings() {
        EnvironmentSettings.Builder builder = EnvironmentSettings.newInstance();
        PlannerType plannerType;
        try {
            plannerType = PlannerType.withName(parameter().get(ConfigKeys.KEY_FLINK_TABLE_PLANNER()));
        } catch (Exception e) {
            plannerType = PlannerType.BLINK;
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

        String flinkConf = parameter().get(ConfigKeys.KEY_FLINK_CONF(), null);
        if (flinkConf == null || flinkConf.isEmpty()) {
            throw new ExceptionInInitializerError(
                "[StreamPark] Usage:can't find config,please set \"--flink.conf $conf \" in main arguments");
        }
        builder.withConfiguration(
            Configuration.fromMap(
                PropertiesUtils.fromYamlText(DeflaterUtils.unzipString(flinkConf))));

        String catalog = parameter().get(ConfigKeys.KEY_FLINK_TABLE_CATALOG(), null);
        String database = parameter().get(ConfigKeys.KEY_FLINK_TABLE_DATABASE(), null);
        if (catalog != null && database != null) {
            logInfo("with built in catalog: " + catalog);
            logInfo("with built in database: " + database);
            builder.withBuiltInCatalogName(catalog);
            builder.withBuiltInDatabaseName(database);
        } else if (catalog != null) {
            logInfo("with built in catalog: " + catalog);
            builder.withBuiltInCatalogName(catalog);
        } else if (database != null) {
            logInfo("with built in database: " + database);
            builder.withBuiltInDatabaseName(database);
        }
        return builder;
    }

    private boolean invokePlannerMethod(
                                        EnvironmentSettings.Builder builder, String methodName, String successMessage) {
        try {
            Method method = builder.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(builder);
            if (successMessage != null) {
                logInfo(successMessage);
            }
            return true;
        } catch (NoSuchMethodException e) {
            logWarn(methodName + " deprecated");
            return false;
        } catch (ReflectiveOperationException e) {
            logWarn(methodName + " deprecated");
            return false;
        }
    }

    @Override
    FlinkConfiguration initParameter() {
        ParameterTool argsMap = ParameterTool.fromArgs(args());
        String configPath = argsMap.get(ConfigKeys.KEY_APP_CONF(), null);
        FlinkConfiguration configuration;
        if (configPath == null || configPath.isEmpty()) {
            logWarn("Usage:can't find config,you can set \"--conf $path \" in main arguments");
            ParameterTool parameter = ParameterTool.fromSystemProperties().mergeWith(argsMap);
            configuration = new FlinkConfiguration(parameter, new Configuration(), new Configuration());
        } else {
            Map<String, String> configMap = parseConfig(configPath);
            Map<String, String> sqlConf = new HashMap<>();
            configMap.forEach(
                (key, value) -> {
                    if (key.startsWith(ConfigKeys.KEY_SQL_PREFIX())) {
                        sqlConf.put(key.substring(ConfigKeys.KEY_SQL_PREFIX().length()), value);
                    }
                });

            Map<String, String> properConf =
                extractConfigByPrefix(configMap, ConfigKeys.KEY_FLINK_PROPERTY_PREFIX());
            Map<String, String> appConf = extractConfigByPrefix(configMap, ConfigKeys.KEY_APP_PREFIX());
            Map<String, String> tableConf =
                extractConfigByPrefix(configMap, ConfigKeys.KEY_FLINK_TABLE_PREFIX());

            Configuration tableConfiguration = Configuration.fromMap(tableConf);
            Configuration envConfig = Configuration.fromMap(properConf);

            ParameterTool parameter =
                ParameterTool.fromSystemProperties()
                    .mergeWith(ParameterTool.fromMap(properConf))
                    .mergeWith(ParameterTool.fromMap(tableConf))
                    .mergeWith(ParameterTool.fromMap(appConf))
                    .mergeWith(ParameterTool.fromMap(sqlConf))
                    .mergeWith(argsMap);

            configuration = new FlinkConfiguration(parameter, envConfig, tableConfiguration);
        }

        String flinkSql = configuration.parameter().get(ConfigKeys.KEY_FLINK_SQL(), null);
        if (flinkSql == null) {
            return configuration;
        }
        try {
            String value = DeflaterUtils.unzipString(flinkSql);
            Map<String, String> sqlMap = new HashMap<>();
            sqlMap.put(ConfigKeys.KEY_FLINK_SQL(), value);
            return configuration.withParameter(
                configuration.parameter().mergeWith(ParameterTool.fromMap(sqlMap)));
        } catch (Exception ignored) {
            File sqlFile = new File(flinkSql);
            try {
                Map<String, String> value =
                    PropertiesUtils.fromYamlFile(sqlFile.getAbsolutePath());
                return configuration.withParameter(
                    configuration.parameter().mergeWith(ParameterTool.fromMap(value)));
            } catch (Exception e) {
                throw new IllegalArgumentException("[StreamPark] init sql error." + e, e);
            }
        }
    }

    private String[] args() {
        return args;
    }

    private ApiType apiType() {
        return apiType;
    }
}
