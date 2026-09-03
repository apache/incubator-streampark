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

package org.apache.streampark.flink.client.bean;

import org.apache.streampark.common.configuration.CommandLineParser;
import org.apache.streampark.common.configuration.CommandLineTokenizer;
import org.apache.streampark.common.configuration.Constants;
import org.apache.streampark.common.configuration.FlinkOptions;
import org.apache.streampark.common.configuration.FlinkRunOption;
import org.apache.streampark.common.configuration.Workspace;
import org.apache.streampark.common.configuration.option.ApplicationOptions;
import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.common.enums.FlinkRestoreMode;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.ClassLoaderUtils;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.FlinkConfigurationUtils;
import org.apache.streampark.common.util.LoggerSupport;
import org.apache.streampark.common.util.SystemPropertyUtils;
import org.apache.streampark.flink.client.configuration.FlinkConfigurationOps;
import org.apache.streampark.flink.client.configuration.FlinkSavepointOptions;
import org.apache.streampark.flink.client.request.SubmitRequest;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.client.cli.CliFrontend;
import org.apache.flink.client.cli.CliFrontendParser;
import org.apache.flink.client.cli.CustomCommandLine;
import org.apache.flink.client.cli.ExecutionConfigAccessor;
import org.apache.flink.client.cli.ProgramOptions;
import org.apache.flink.client.deployment.application.ApplicationConfiguration;
import org.apache.flink.client.program.PackagedProgramUtils;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.ExecutionOptions;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.configuration.PipelineOptionsInternal;
import org.apache.flink.python.PythonOptions;
import org.apache.flink.util.Preconditions;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds the effective Flink configuration for a submission request. */
public final class SubmissionConfigurationBuilder extends LoggerSupport {

    private static final SubmissionConfigurationBuilder INSTANCE =
        new SubmissionConfigurationBuilder();

    private static final String PARAM_KEY_FLINK_CONF =
        CommandLineParser.LONG_OPTION_PREFIX + FlinkOptions.FLINK_CONFIGURATION.key();
    private static final String PARAM_KEY_FLINK_CONF_STANDARD_YAML =
        CommandLineParser.LONG_OPTION_PREFIX
            + FlinkOptions.FLINK_CONFIGURATION_STANDARD_YAML.key();
    private static final String PARAM_KEY_FLINK_SQL =
        CommandLineParser.LONG_OPTION_PREFIX + ApplicationOptions.SQL.key();
    private static final String PARAM_KEY_APP_CONF =
        CommandLineParser.LONG_OPTION_PREFIX + ApplicationOptions.CONFIG.key();
    private static final String PARAM_KEY_APP_NAME =
        CommandLineParser.LONG_OPTION_PREFIX + ApplicationOptions.NAME.key();
    private static final String PARAM_KEY_FLINK_PARALLELISM =
        CommandLineParser.LONG_OPTION_PREFIX + FlinkOptions.PARALLELISM.key();

    private SubmissionConfigurationBuilder() {
    }

    /** Builds the configuration used by a deployment client. */
    public static Configuration build(SubmitRequest request) throws Exception {
        return INSTANCE.buildConfiguration(request);
    }

    /** Extracts a Flink configuration from dynamic cluster properties. */
    public static Configuration extract(String flinkHome, Map<String, Object> properties) throws Exception {
        return INSTANCE.extractConfiguration(flinkHome, properties);
    }

    /** Loads the registered Flink installation's default configuration. */
    public static Configuration loadDefault(String flinkHome) {
        try {
            return Configuration.fromMap(FlinkConfigurationUtils.loadFlinkHome(flinkHome));
        } catch (Exception ignored) {
            return new Configuration();
        }
    }

    /** Resolves the effective parallelism for the request. */
    public static int parallelism(SubmitRequest request) {
        Object configured = request.properties().get(FlinkOptions.PARALLELISM.key());
        if (configured != null) {
            return Integer.parseInt(configured.toString());
        }
        return loadDefault(request.flinkVersion().getFlinkHome())
            .get(CoreOptions.DEFAULT_PARALLELISM, CoreOptions.DEFAULT_PARALLELISM.defaultValue());
    }

    /** Reads an option from the registered Flink installation. */
    public static <T> T getDefaultOption(String flinkHome, ConfigOption<T> option) {
        return loadDefault(flinkHome).get(option);
    }

    private Configuration buildConfiguration(SubmitRequest request) throws Exception {
        CommandLineAndConfiguration cli = parseCommandLine(request);
        Configuration configuration = cli.configuration;
        applyJobTypeConfiguration(request, cli.commandLine, configuration);
        applyPipelineConfiguration(request, configuration);
        applyCheckpointDefaults(request, configuration);
        applySavepointConfiguration(request, configuration);
        applyEnvironmentProperties(request, configuration);
        return configuration;
    }

    private void applyJobTypeConfiguration(
                                           SubmitRequest request,
                                           CommandLine commandLine,
                                           Configuration configuration) throws Exception {
        if (request.jobType() == FlinkJobType.PYFLINK) {
            applyPyFlinkConfiguration(request, configuration);
            return;
        }

        File userJar = SubmitRequestResolver.userJarFile(request);
        if (userJar == null) {
            return;
        }
        URI jarUri = PackagedProgramUtils.resolveURI(userJar.getAbsolutePath());
        ProgramOptions programOptions = ProgramOptions.create(commandLine);
        ExecutionConfigAccessor.fromProgramOptions(
            programOptions, Collections.singletonList(jarUri.toString()))
            .applyToConfiguration(configuration);
    }

    private void applyPyFlinkConfiguration(
                                           SubmitRequest request, Configuration configuration) throws Exception {
        String pythonVenv = Workspace.LOCAL.pythonVenv;
        AssertUtils.required(
            FsOperator.lfs().exists(pythonVenv), pythonVenv + " file does not exist");

        FlinkConfigurationOps.setIfPresent(
            configuration, PythonOptions.PYTHON_ARCHIVES, pythonVenv);
        FlinkConfigurationOps.setIfPresent(
            configuration, PythonOptions.PYTHON_CLIENT_EXECUTABLE, Constants.PYTHON_EXECUTABLE);
        FlinkConfigurationOps.setIfPresent(
            configuration, PythonOptions.PYTHON_EXECUTABLE, Constants.PYTHON_EXECUTABLE);

        if (StringUtils.isBlank(System.getenv(ConfigConstants.ENV_FLINK_OPT_DIR))) {
            String flinkOptPath = request.flinkVersion().getFlinkHome() + "/opt";
            SystemPropertyUtils.setEnv(ConfigConstants.ENV_FLINK_OPT_DIR, flinkOptPath);
            logInfo(
                "Set temporary environment variable "
                    + ConfigConstants.ENV_FLINK_OPT_DIR
                    + "="
                    + flinkOptPath);
        }
    }

    private void applyPipelineConfiguration(
                                            SubmitRequest request, Configuration configuration) {
        FlinkConfigurationOps.setIfPresent(
            configuration, PipelineOptions.NAME, SubmitRequestResolver.effectiveApplicationName(request));
        FlinkConfigurationOps.setIfPresent(
            configuration, DeploymentOptions.TARGET, request.deployMode().getName());
        FlinkConfigurationOps.setIfPresent(
            configuration, FlinkSavepointOptions.SAVEPOINT_PATH, request.savePoint());
        FlinkConfigurationOps.setIfPresent(
            configuration,
            ApplicationConfiguration.APPLICATION_MAIN_CLASS,
            SubmitRequestResolver.applicationMain(request));
        FlinkConfigurationOps.setIfPresent(
            configuration, ApplicationConfiguration.APPLICATION_ARGS, programArguments(request));
        FlinkConfigurationOps.setIfPresent(
            configuration, PipelineOptionsInternal.PIPELINE_FIXED_JOB_ID, request.jobId());
    }

    private void applyCheckpointDefaults(
                                         SubmitRequest request, Configuration configuration) {
        ConfigOption<Integer> retainedCheckpoints = CheckpointingOptions.MAX_RETAINED_CHECKPOINTS;
        if (!request.properties().containsKey(retainedCheckpoints.key())) {
            configuration.set(
                retainedCheckpoints,
                loadDefault(request.flinkVersion().getFlinkHome()).get(retainedCheckpoints));
        }
    }

    private void applySavepointConfiguration(
                                             SubmitRequest request, Configuration configuration) {
        if (StringUtils.isBlank(request.savePoint())) {
            return;
        }
        FlinkConfigurationOps.setIfPresent(
            configuration, FlinkSavepointOptions.SAVEPOINT_PATH, request.savePoint());
        configuration.set(
            FlinkSavepointOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE,
            SubmitRequestResolver.allowNonRestoredState(request));

        if (request.restoreMode() != null
            && request.flinkVersion().checkVersion(FlinkRestoreMode.SINCE_FLINK_VERSION)) {
            configuration.setString(FlinkRestoreMode.RESTORE_MODE, request.restoreMode().getName());
        }
    }

    private void applyEnvironmentProperties(
                                            SubmitRequest request, Configuration configuration) {
        if (MapUtils.isEmpty(request.properties())) {
            return;
        }

        Object jvmOptions = request.properties().get(CoreOptions.FLINK_JVM_OPTIONS.key());
        if (jvmOptions != null && !jvmOptions.toString().contains("-Dfile.encoding=")) {
            configuration.setString(
                CoreOptions.FLINK_JVM_OPTIONS.key(),
                "-Dfile.encoding=UTF-8 " + jvmOptions);
        }

        request.properties().forEach(
            (key, value) -> {
                if (key.startsWith("env.")) {
                    logInfo("Environment option: " + key + "=" + value);
                    configuration.setString(key, value.toString());
                }
            });
    }

    private CommandLineAndConfiguration parseCommandLine(SubmitRequest request) throws Exception {
        String flinkHome = request.flinkVersion().getFlinkHome();
        Options options = commandLineOptions(flinkHome);
        List<String> arguments = cliArguments(request, buildOptionMap(request, options));
        logInfo("Flink CLI arguments: " + String.join(" ", arguments));

        CommandLine commandLine =
            FlinkRunOption.parse(options, arguments.toArray(new String[0]), true);
        CustomCommandLine activeCommandLine =
            activeCommandLine(customCommandLines(flinkHome), commandLine);
        return new CommandLineAndConfiguration(
            commandLine, applyConfiguration(flinkHome, activeCommandLine, commandLine));
    }

    private Map<String, Object> buildOptionMap(SubmitRequest request, Options commandLineOptions) {
        Map<String, Object> options = new HashMap<>();
        SubmitRequestResolver.applicationOptions(request).forEach(
            (key, value) -> {
                if (!commandLineOptions.hasOption(key)) {
                    logWarn("Ignoring unsupported Flink CLI option: " + key);
                    return;
                }
                String option = commandLineOptions.getOption(key.trim()).getOpt();
                Object parsedValue = parseOptionValue(value);
                if (Boolean.TRUE.equals(parsedValue)) {
                    options.put("-" + option, true);
                } else if (!(parsedValue instanceof Boolean)) {
                    options.put("-" + option, parsedValue);
                }
            });

        if (request.savePoint() != null) {
            options.put("-" + FlinkRunOption.SAVEPOINT_PATH_OPTION.getOpt(), request.savePoint());
        }
        options.remove("-e");
        options.remove("--executor");
        options.remove("-t");
        options.remove("--target");
        if (request.deployMode() != null) {
            options.put("-t", request.deployMode().getName());
        }
        return options;
    }

    private List<String> cliArguments(SubmitRequest request, Map<String, Object> options) {
        List<String> arguments = new ArrayList<>();
        options.forEach(
            (key, value) -> {
                arguments.add(key);
                if (value instanceof String) {
                    arguments.add((String) value);
                }
            });
        request.properties().forEach(
            (key, value) -> {
                if (!key.startsWith("env.")) {
                    arguments.add("-D" + key + "=" + value);
                }
            });
        return arguments;
    }

    private List<String> programArguments(SubmitRequest request) {
        List<String> arguments =
            new ArrayList<>(CommandLineTokenizer.tokenize(request.args()));
        if (request.applicationType() == ApplicationType.STREAMPARK_FLINK
            || request.jobType() == FlinkJobType.FLINK_SQL) {
            addStreamParkArguments(request, arguments);
        }

        Object runtimeMode = request.properties().get(ExecutionOptions.RUNTIME_MODE.key());
        if (runtimeMode != null) {
            arguments.add("--" + ExecutionOptions.RUNTIME_MODE.key());
            arguments.add(runtimeMode.toString());
        }

        if (request.jobType() == FlinkJobType.PYFLINK
            && request.deployMode() != FlinkDeployMode.YARN_APPLICATION) {
            File userJar = SubmitRequestResolver.userJarFile(request);
            AssertUtils.notNull(userJar);
            arguments.add("-py");
            arguments.add(userJar.getAbsolutePath());
        }
        return arguments;
    }

    private void addStreamParkArguments(SubmitRequest request, List<String> arguments) {
        arguments.add(PARAM_KEY_FLINK_CONF);
        arguments.add(request.flinkYaml());
        // Carry the persisted syntax snapshot so Flink 1.19 never re-resolves its configuration
        // format from installation files that may have changed since registration.
        arguments.add(PARAM_KEY_FLINK_CONF_STANDARD_YAML);
        arguments.add(Boolean.toString(request.standardYaml()));
        arguments.add(PARAM_KEY_APP_NAME);
        arguments.add(
            DeflaterUtils.zipString(SubmitRequestResolver.effectiveApplicationName(request)));
        arguments.add(PARAM_KEY_FLINK_PARALLELISM);
        arguments.add(Integer.toString(parallelism(request)));

        if (request.jobType() == FlinkJobType.FLINK_SQL) {
            arguments.add(PARAM_KEY_FLINK_SQL);
            arguments.add(SubmitRequestResolver.flinkSql(request));
            addApplicationConfiguration(request.appConf(), arguments);
        } else if (shouldAddApplicationConfiguration(request.appConf())) {
            addApplicationConfiguration(request.appConf(), arguments);
        }
    }

    private void addApplicationConfiguration(String appConf, List<String> arguments) {
        if (appConf != null) {
            arguments.add(PARAM_KEY_APP_CONF);
            arguments.add(appConf);
        }
    }

    private Configuration extractConfiguration(
                                               String flinkHome, Map<String, Object> properties) throws Exception {
        List<String> arguments = new ArrayList<>();
        if (MapUtils.isNotEmpty(properties)) {
            properties.forEach(
                (key, value) -> arguments.add("-D" + key + "=" + value.toString().trim()));
        }
        CommandLine commandLine =
            FlinkRunOption.parse(
                commandLineOptions(flinkHome), arguments.toArray(new String[0]), true);
        CustomCommandLine activeCommandLine =
            activeCommandLine(customCommandLines(flinkHome), commandLine);
        return applyConfiguration(flinkHome, activeCommandLine, commandLine);
    }

    private CustomCommandLine activeCommandLine(
                                                List<CustomCommandLine> commandLines,
                                                CommandLine commandLine) {
        Preconditions.checkNotNull(commandLine);
        for (CustomCommandLine candidate : commandLines) {
            if (candidate.isActive(commandLine)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No active Flink command line found");
    }

    private List<CustomCommandLine> customCommandLines(String flinkHome) {
        Configuration defaultConfiguration = loadDefault(flinkHome);
        String configurationDirectory = flinkHome + "/conf";
        return ClassLoaderUtils.runAsClassLoader(
            SubmissionConfigurationBuilder.class.getClassLoader(),
            () -> CliFrontend.loadCustomCommandLines(
                defaultConfiguration, configurationDirectory));
    }

    private Options commandLineOptions(String flinkHome) {
        Options customOptions = new Options();
        for (CustomCommandLine commandLine : customCommandLines(flinkHome)) {
            commandLine.addGeneralOptions(customOptions);
            commandLine.addRunOptions(customOptions);
        }
        return FlinkRunOption.mergeOptions(
            CliFrontendParser.getRunCommandOptions(), customOptions);
    }

    private Configuration applyConfiguration(
                                             String flinkHome,
                                             CustomCommandLine activeCommandLine,
                                             CommandLine commandLine) throws Exception {
        Preconditions.checkNotNull(activeCommandLine, "activeCommandLine must not be null");
        Configuration configuration = new Configuration();
        Configuration defaults = loadDefault(flinkHome);
        Set<String> keys = defaults.keySet();
        for (String key : keys) {
            String value = defaults.getString(key, null);
            if (value != null) {
                configuration.setString(key, value);
            }
        }
        configuration.addAll(activeCommandLine.toConfiguration(commandLine));
        return configuration;
    }

    private static Object parseOptionValue(String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }

    private static boolean shouldAddApplicationConfiguration(String appConf) {
        return StringUtils.isNotBlank(appConf) && !appConf.startsWith("json:");
    }

    private static final class CommandLineAndConfiguration {

        private final CommandLine commandLine;
        private final Configuration configuration;

        private CommandLineAndConfiguration(
                                            CommandLine commandLine, Configuration configuration) {
            this.commandLine = commandLine;
            this.configuration = configuration;
        }
    }
}
