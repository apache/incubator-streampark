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

package org.apache.streampark.plugin.profiling;

import org.apache.streampark.plugin.profiling.reporter.ConsoleOutputReporter;
import org.apache.streampark.plugin.profiling.util.AgentLogger;
import org.apache.streampark.plugin.profiling.util.ClassAndMethod;
import org.apache.streampark.plugin.profiling.util.ClassMethodArgument;
import org.apache.streampark.plugin.profiling.util.DummyConfigProvider;
import org.apache.streampark.plugin.profiling.util.ReflectionUtils;
import org.apache.streampark.plugin.profiling.util.Utils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Arguments {
    public static final String DEFAULT_APP_ID_REGEX = "application_[\\w_]+";
    public static final long DEFAULT_METRIC_INTERVAL = 60000;
    public static final long DEFAULT_SAMPLE_INTERVAL = 100;

    public static final String ARG_NOOP = "noop";
    public static final String ARG_REPORTER = "reporter";
    public static final String ARG_CONFIG_PROVIDER = "configProvider";
    public static final String ARG_CONFIG_FILE = "configFile";
    public static final String ARG_METRIC_INTERVAL = "metricInterval";
    public static final String ARG_SAMPLE_INTERVAL = "sampleInterval";
    public static final String ARG_TAG = "tag";
    public static final String ARG_CLUSTER = "cluster";
    public static final String ARG_APP_ID_VARIABLE = "appIdVariable";
    public static final String ARG_APP_ID_REGEX = "appIdRegex";
    public static final String ARG_DURATION_PROFILING = "durationProfiling";
    public static final String ARG_ARGUMENT_PROFILING = "argumentProfiling";

    public static final String ARG_IO_PROFILING = "ioProfiling";

    public static final long MIN_INTERVAL_MILLIS = 50;

    private static final AgentLogger LOGGER = AgentLogger.getLogger(Arguments.class.getName());

    private final Map<String, List<String>> rawArgValues = new HashMap<>();

    private boolean noop = false;

    private Constructor<Reporter> reporterConstructor;
    private Constructor<ConfigProvider> configProviderConstructor;
    private String configFile;

    private String appIdVariable;
    private String appIdRegex = DEFAULT_APP_ID_REGEX;
    private long metricInterval = DEFAULT_METRIC_INTERVAL;
    private long sampleInterval = 0L;
    private String tag;
    private String cluster;
    private boolean ioProfiling;

    private final List<ClassAndMethod> durationProfiling = new ArrayList<>();
    private final List<ClassMethodArgument> argumentProfiling = new ArrayList<>();

    private Arguments(Map<String, List<String>> parsedArgs) {
        doArguments(parsedArgs);
    }

    public static Arguments parseArgs(String args) {
        if (args == null) {
            return new Arguments(new HashMap<>());
        }

        args = args.trim();
        if (args.isEmpty()) {
            return new Arguments(new HashMap<>());
        }

        Map<String, List<String>> map = new HashMap<>();
        for (String argPair : args.split(",")) {
            String[] strs = argPair.split("=");
            if (strs.length != 2) {
                throw new IllegalArgumentException(
                    "Arguments for the agent should be like: key1=value1,key2=value2");
            }

            String key = strs[0].trim();
            if (key.isEmpty()) {
                throw new IllegalArgumentException("Argument key should not be empty");
            }

            List<String> list = map.computeIfAbsent(key, k -> new ArrayList<>());
            list.add(strs[1].trim());
        }

        return new Arguments(map);
    }

    public void doArguments(Map<String, List<String>> parsedArgs) {
        rawArgValues.putAll(parsedArgs);

        String argValue = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_NOOP);
        if (ArgumentUtils.needToUpdateArg(argValue)) {
            noop = Boolean.parseBoolean(argValue);
            LOGGER.info("Got argument value for noop: " + noop);
        }

        argValue = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_REPORTER);
        if (ArgumentUtils.needToUpdateArg(argValue)) {
            reporterConstructor = ReflectionUtils.getConstructor(argValue, Reporter.class);
            LOGGER.info("Got argument value for reporter: " + argValue);
        }

        argValue = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_CONFIG_PROVIDER);
        if (ArgumentUtils.needToUpdateArg(argValue)) {
            configProviderConstructor = ReflectionUtils.getConstructor(argValue, ConfigProvider.class);
            LOGGER.info("Got argument value for configProvider: " + argValue);
        }

        argValue = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_CONFIG_FILE);
        if (ArgumentUtils.needToUpdateArg(argValue)) {
            configFile = argValue;
            LOGGER.info("Got argument value for configFile: " + configFile);
        }

        argValue = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_METRIC_INTERVAL);
        if (ArgumentUtils.needToUpdateArg(argValue)) {
            metricInterval = Long.parseLong(argValue);
            LOGGER.info("Got argument value for metricInterval: " + metricInterval);
        }

        if (metricInterval < MIN_INTERVAL_MILLIS) {
            throw new RuntimeException(
                "Metric interval too short, must be at least " + Arguments.MIN_INTERVAL_MILLIS);
        }

        argValue = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_SAMPLE_INTERVAL);
        if (ArgumentUtils.needToUpdateArg(argValue)) {
            sampleInterval = Long.parseLong(argValue);
            LOGGER.info("Got argument value for sampleInterval: " + sampleInterval);
        }

        if (sampleInterval != 0 && sampleInterval < MIN_INTERVAL_MILLIS) {
            throw new RuntimeException(
                "Sample interval too short, must be 0 (disable sampling) or at least "
                    + Arguments.MIN_INTERVAL_MILLIS);
        }

        argValue = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_TAG);
        if (ArgumentUtils.needToUpdateArg(argValue)) {
            tag = argValue;
            LOGGER.info("Got argument value for tag: " + tag);
        }

        argValue = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_CLUSTER);
        if (ArgumentUtils.needToUpdateArg(argValue)) {
            cluster = argValue;
            LOGGER.info("Got argument value for cluster: " + cluster);
        }

        argValue = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_APP_ID_VARIABLE);
        if (ArgumentUtils.needToUpdateArg(argValue)) {
            appIdVariable = argValue;
            LOGGER.info("Got argument value for appIdVariable: " + appIdVariable);
        }

        argValue = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_APP_ID_REGEX);
        if (ArgumentUtils.needToUpdateArg(argValue)) {
            appIdRegex = argValue;
            LOGGER.info("Got argument value for appIdRegex: " + appIdRegex);
        }

        List<String> argValues =
            ArgumentUtils.getArgumentMultiValues(parsedArgs, ARG_DURATION_PROFILING);
        if (!argValues.isEmpty()) {
            durationProfiling.clear();
            for (String str : argValues) {
                int index = str.lastIndexOf(".");
                if (index <= 0 || index + 1 >= str.length()) {
                    throw new IllegalArgumentException("Invalid argument value: " + str);
                }
                String className = str.substring(0, index);
                String methodName = str.substring(index + 1);
                ClassAndMethod classAndMethod = new ClassAndMethod(className, methodName);
                durationProfiling.add(classAndMethod);
                LOGGER.info("Got argument value for durationProfiling: " + classAndMethod);
            }
        }

        argValues = ArgumentUtils.getArgumentMultiValues(parsedArgs, ARG_ARGUMENT_PROFILING);
        if (!argValues.isEmpty()) {
            argumentProfiling.clear();
            for (String str : argValues) {
                int index = str.lastIndexOf(".");
                if (index <= 0 || index + 1 >= str.length()) {
                    throw new IllegalArgumentException("Invalid argument value: " + str);
                }
                String classMethodName = str.substring(0, index);
                int argumentIndex = Integer.parseInt(str.substring(index + 1));

                index = classMethodName.lastIndexOf(".");
                if (index <= 0 || index + 1 >= classMethodName.length()) {
                    throw new IllegalArgumentException("Invalid argument value: " + str);
                }
                String className = classMethodName.substring(0, index);
                String methodName = str.substring(index + 1, classMethodName.length());

                ClassMethodArgument classMethodArgument =
                    new ClassMethodArgument(className, methodName, argumentIndex);
                argumentProfiling.add(classMethodArgument);
                LOGGER.info("Got argument value for argumentProfiling: " + classMethodArgument);
            }
        }

        argValue = ArgumentUtils.getArgumentSingleValue(parsedArgs, ARG_IO_PROFILING);
        if (ArgumentUtils.needToUpdateArg(argValue)) {
            ioProfiling = Boolean.parseBoolean(argValue);
            LOGGER.info("Got argument value for ioProfiling: " + ioProfiling);
        }
    }

    public void runConfigProvider() {
        try {
            ConfigProvider configProvider = getConfigProvider();
            if (configProvider != null) {
                Map<String, Map<String, List<String>>> extraConfig = configProvider.getConfig();

                // Get root level config (use empty string as key in the config map)
                Map<String, List<String>> rootConfig = extraConfig.get("");
                if (rootConfig != null) {
                    doArguments(rootConfig);
                    LOGGER.info("Updated arguments based on config: " + Utils.toJsonString(rootConfig));
                }

                // Get tag level config (use tag value to find config values in the config map)
                if (getTag() != null && !getTag().isEmpty()) {
                    Map<String, List<String>> overrideConfig = extraConfig.get(getTag());
                    if (overrideConfig != null) {
                        doArguments(overrideConfig);
                        LOGGER.info(
                            "Updated arguments based on config override: "
                                + Utils.toJsonString(overrideConfig));
                    }
                }
            }
        } catch (Throwable ex) {
            LOGGER.warn("Failed to update arguments with config provider", ex);
        }
    }

    public Map<String, List<String>> getRawArgValues() {
        return rawArgValues;
    }

    public Reporter getReporter() {
        if (reporterConstructor == null) {
            return new ConsoleOutputReporter();
        } else {
            try {
                Reporter reporter = reporterConstructor.newInstance();
                reporter.doArguments(getRawArgValues());
                return reporter;
            } catch (Throwable e) {
                throw new RuntimeException(
                    String.format(
                        "Failed to create reporter instance %s", reporterConstructor.getDeclaringClass()),
                    e);
            }
        }
    }

    public ConfigProvider getConfigProvider() {
        if (configProviderConstructor == null) {
            return new DummyConfigProvider();
        } else {
            try {
                ConfigProvider configProvider = configProviderConstructor.newInstance();
                if (configProvider instanceof YamlConfigProvider) {
                    if (configFile == null || configFile.isEmpty()) {
                        throw new RuntimeException(
                            "Argument configFile is empty, cannot use " + configProvider.getClass());
                    }
                    ((YamlConfigProvider) configProvider).setFilePath(configFile);
                    return configProvider;
                }
                return configProvider;
            } catch (Throwable e) {
                throw new RuntimeException(
                    String.format(
                        "Failed to create config provider instance %s",
                        configProviderConstructor.getDeclaringClass()),
                    e);
            }
        }
    }

    public boolean isNoop() {
        return noop;
    }

    public void setReporter(String className) {
        reporterConstructor = ReflectionUtils.getConstructor(className, Reporter.class);
    }

    public void setConfigProvider(String className) {
        configProviderConstructor = ReflectionUtils.getConstructor(className, ConfigProvider.class);
    }

    public long getMetricInterval() {
        return metricInterval;
    }

    public long getSampleInterval() {
        return sampleInterval;
    }

    public String getTag() {
        return tag;
    }

    public String getCluster() {
        return cluster;
    }

    public String getAppIdVariable() {
        return appIdVariable;
    }

    public void setAppIdVariable(String appIdVariable) {
        this.appIdVariable = appIdVariable;
    }

    public String getAppIdRegex() {
        return appIdRegex;
    }

    public List<ClassAndMethod> getDurationProfiling() {
        return durationProfiling;
    }

    public List<ClassMethodArgument> getArgumentProfiling() {
        return argumentProfiling;
    }

    public boolean isIoProfiling() {
        return ioProfiling;
    }
}
