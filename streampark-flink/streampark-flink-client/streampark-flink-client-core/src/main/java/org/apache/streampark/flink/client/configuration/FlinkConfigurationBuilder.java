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

package org.apache.streampark.flink.client.configuration;

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
import org.apache.streampark.common.util.FlinkConfigurationLoader;
import org.apache.streampark.common.util.LoggerSupport;
import org.apache.streampark.common.util.SystemPropertyUtils;
import org.apache.streampark.flink.client.bean.ResolvedSubmitRequest;
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

/**
 * Builds the effective Flink configuration for a submission request.
 *
 * <p>Configuration is assembled from one installation snapshot and Flink's own command-line
 * implementations. Installation defaults are applied first, request properties and CLI options
 * override them, and deployment-specific clients add their platform settings afterward. Keeping
 * these stages explicit avoids version-dependent option parsing outside Flink itself.
 */
public final class FlinkConfigurationBuilder extends LoggerSupport {

    private static final FlinkConfigurationBuilder INSTANCE =
        new FlinkConfigurationBuilder();

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

    private FlinkConfigurationBuilder() {
    }

    /**
     * Builds the base configuration used by a deployment client.
     *
     * @param resolved request values resolved from one immutable configuration snapshot
     * @return effective base configuration before platform-specific settings
     */
    public static Configuration build(ResolvedSubmitRequest resolved) throws Exception {
        return INSTANCE.buildConfiguration(resolved);
    }

    /**
     * Extracts a Flink configuration from dynamic cluster properties.
     *
     * @param flinkHome registered Flink installation directory
     * @param properties dynamic properties that override installation defaults
     * @return configuration interpreted by the target Flink command line
     */
    public static Configuration extract(String flinkHome, Map<String, Object> properties) throws Exception {
        return INSTANCE.extractConfiguration(flinkHome, properties);
    }

    /**
     * Loads the registered Flink installation's default configuration.
     *
     * @param flinkHome registered Flink installation directory
     * @return a new mutable Flink configuration populated from the compatible YAML loader
     */
    public static Configuration loadDefault(String flinkHome) {
        return Configuration.fromMap(FlinkConfigurationLoader.loadFlinkHome(flinkHome));
    }

    /**
     * Reads a typed option from the registered Flink installation.
     *
     * @param flinkHome registered Flink installation directory
     * @param option Flink option to read
     * @param <T> option value type
     * @return configured or default option value
     */
    public static <T> T loadDefaultOption(String flinkHome, ConfigOption<T> option) {
        return loadDefault(flinkHome).get(option);
    }

    /** Applies configuration stages in precedence order to one mutable Flink snapshot. */
    private Configuration buildConfiguration(ResolvedSubmitRequest resolved) throws Exception {
        SubmitRequest request = resolved.request();
        CommandLineAndConfiguration cli = parseCommandLine(resolved);
        Configuration configuration = cli.configuration;
        applyJobTypeConfiguration(resolved, cli.commandLine, configuration);
        applyPipelineConfiguration(resolved, configuration);
        applyCheckpointDefaults(request, configuration);
        applySavepointConfiguration(resolved, configuration);
        applyEnvironmentProperties(request, configuration);
        return configuration;
    }

    /** Applies the entry point and artifact metadata required by the submitted job type. */
    private void applyJobTypeConfiguration(
                                           ResolvedSubmitRequest resolved,
                                           CommandLine commandLine,
                                           Configuration configuration) throws Exception {
        SubmitRequest request = resolved.request();
        if (request.jobType() == FlinkJobType.PYFLINK) {
            applyPyFlinkConfiguration(request, configuration);
            return;
        }

        File userJar = resolved.getUserJarFile();
        if (userJar == null) {
            return;
        }
        URI jarUri = PackagedProgramUtils.resolveURI(userJar.getAbsolutePath());
        ProgramOptions programOptions = ProgramOptions.create(commandLine);
        ExecutionConfigAccessor.fromProgramOptions(
            programOptions, Collections.singletonList(jarUri.toString()))
            .applyToConfiguration(configuration);
    }

    /** Configures the managed Python environment used to launch PyFlink applications. */
    private void applyPyFlinkConfiguration(
                                           SubmitRequest request, Configuration configuration) throws Exception {
        String pythonVenv = Workspace.LOCAL.pythonVenv;
        AssertUtils.required(
            FsOperator.lfs().exists(pythonVenv), pythonVenv + " file does not exist");

        configuration.set(PythonOptions.PYTHON_ARCHIVES, pythonVenv);
        configuration.set(PythonOptions.PYTHON_CLIENT_EXECUTABLE, Constants.PYTHON_EXECUTABLE);
        configuration.set(PythonOptions.PYTHON_EXECUTABLE, Constants.PYTHON_EXECUTABLE);

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

    /** Applies job identity, arguments, deployment target, and fixed job ID. */
    private void applyPipelineConfiguration(
                                            ResolvedSubmitRequest resolved,
                                            Configuration configuration) {
        SubmitRequest request = resolved.request();
        configuration.set(PipelineOptions.NAME, resolved.getJobName());
        configuration.set(DeploymentOptions.TARGET, request.deployMode().getName());

        String jobMainClass = resolved.getJobMainClass();
        if (StringUtils.isNotEmpty(jobMainClass)) {
            configuration.set(ApplicationConfiguration.APPLICATION_MAIN_CLASS, jobMainClass);
        }

        configuration.set(ApplicationConfiguration.APPLICATION_ARGS, programArguments(resolved, configuration));
        if (StringUtils.isNotEmpty(request.jobId())) {
            configuration.set(PipelineOptionsInternal.PIPELINE_FIXED_JOB_ID, request.jobId());
        }
    }

    /** Materializes the retained-checkpoint default when no dynamic override was supplied. */
    private void applyCheckpointDefaults(
                                         SubmitRequest request, Configuration configuration) {
        ConfigOption<Integer> retainedCheckpoints = CheckpointingOptions.MAX_RETAINED_CHECKPOINTS;
        if (!request.properties().containsKey(retainedCheckpoints.key())) {
            configuration.set(
                retainedCheckpoints,
                configuration.get(retainedCheckpoints));
        }
    }

    /** Applies savepoint restoration only when the request carries a restore path. */
    private void applySavepointConfiguration(
                                             ResolvedSubmitRequest resolved,
                                             Configuration configuration) {
        SubmitRequest request = resolved.request();
        if (StringUtils.isBlank(request.savePoint())) {
            return;
        }
        configuration.set(FlinkSavepointOptions.SAVEPOINT_PATH, request.savePoint());
        configuration.set(
            FlinkSavepointOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE,
            resolved.isNonRestoredStateAllowed());

        if (request.restoreMode() != null
            && request.flinkVersion().checkVersion(FlinkRestoreMode.SINCE_FLINK_VERSION)) {
            configuration.setString(FlinkRestoreMode.RESTORE_MODE, request.restoreMode().getName());
        }
    }

    /** Applies environment-prefixed options and the default JVM file encoding. */
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
                if (key.startsWith("env.") && value != null) {
                    logInfo("Environment option: " + key + "=" + value);
                    configuration.setString(key, value.toString());
                }
            });
    }

    /**
     * Parses the request through Flink's own CLI using one default-configuration snapshot.
     *
     * <p>Reusing the command-line list and defaults prevents a concurrent file change from
     * producing different option discovery and effective configuration in the same submission.
     */
    private CommandLineAndConfiguration parseCommandLine(ResolvedSubmitRequest resolved) throws Exception {
        SubmitRequest request = resolved.request();
        String flinkHome = request.flinkVersion().getFlinkHome();
        Configuration defaults = loadDefault(flinkHome);
        expandJobPlaceholders(defaults, resolved);
        List<CustomCommandLine> commandLines = customCommandLines(flinkHome, defaults);
        Options options = commandLineOptions(commandLines);
        List<String> arguments = cliArguments(request, buildOptionMap(resolved, options));
        logInfo("Flink CLI arguments: " + String.join(" ", arguments));

        CommandLine commandLine =
            FlinkRunOption.parse(options, arguments.toArray(new String[0]), true);
        CustomCommandLine activeCommandLine =
            activeCommandLine(commandLines, commandLine);
        return new CommandLineAndConfiguration(
            commandLine, applyConfiguration(defaults, activeCommandLine, commandLine));
    }

    /**
     * Replaces StreamPark job variables in deployment-scoped Flink defaults.
     *
     * <p>Session and remote clusters retain their shared installation configuration. Other modes
     * create job-scoped runtimes and may safely resolve job name and ID placeholders.
     */
    static void expandJobPlaceholders(
                                      Configuration configuration,
                                      ResolvedSubmitRequest resolved) {
        FlinkDeployMode deployMode = resolved.request().deployMode();
        if (deployMode == FlinkDeployMode.REMOTE
            || deployMode == FlinkDeployMode.YARN_SESSION
            || deployMode == FlinkDeployMode.KUBERNETES_NATIVE_SESSION) {
            return;
        }

        String jobName = resolved.getJobName();
        String jobId = Long.toString(resolved.request().id());
        for (String key : configuration.keySet()) {
            String value = configuration.getString(key, null);
            if (value != null) {
                configuration.setString(key, replaceJobPlaceholder(value, jobName, jobId));
            }
        }
    }

    /** Replaces the historical braced and unbraced spellings of job variables. */
    private static String replaceJobPlaceholder(String value, String jobName, String jobId) {
        return value
            .replace("${jobName}", jobName)
            .replace("${jobname}", jobName)
            .replace("$jobName", jobName)
            .replace("$jobname", jobName)
            .replace("${jobId}", jobId)
            .replace("${jobid}", jobId)
            .replace("$jobId", jobId)
            .replace("$jobid", jobId);
    }

    /** Filters persisted job options against the options supported by the target Flink. */
    private Map<String, Object> buildOptionMap(
                                               ResolvedSubmitRequest resolved,
                                               Options commandLineOptions) {
        SubmitRequest request = resolved.request();
        Map<String, Object> options = new HashMap<>();
        resolved.jobOptions().forEach(
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

    /** Converts validated options and dynamic properties to Flink CLI arguments. */
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

    /** Builds the argument vector delivered to the submitted job's main method. */
    private List<String> programArguments(
                                          ResolvedSubmitRequest resolved,
                                          Configuration configuration) {
        SubmitRequest request = resolved.request();
        List<String> arguments =
            new ArrayList<>(CommandLineTokenizer.tokenize(request.args()));
        if (request.applicationType() == ApplicationType.STREAMPARK_FLINK
            || request.jobType() == FlinkJobType.FLINK_SQL) {
            addStreamParkArguments(resolved, configuration, arguments);
        }

        Object runtimeMode = request.properties().get(ExecutionOptions.RUNTIME_MODE.key());
        if (runtimeMode != null) {
            arguments.add("--" + ExecutionOptions.RUNTIME_MODE.key());
            arguments.add(runtimeMode.toString());
        }

        if (request.jobType() == FlinkJobType.PYFLINK
            && request.deployMode() != FlinkDeployMode.YARN_APPLICATION) {
            File userJar = resolved.getUserJarFile();
            AssertUtils.notNull(userJar);
            arguments.add("-py");
            arguments.add(userJar.getAbsolutePath());
        }
        return arguments;
    }

    /** Adds StreamPark transport arguments consumed by its Flink runtime entry points. */
    private void addStreamParkArguments(
                                        ResolvedSubmitRequest resolved,
                                        Configuration configuration,
                                        List<String> arguments) {
        SubmitRequest request = resolved.request();
        arguments.add(PARAM_KEY_FLINK_CONF);
        arguments.add(request.flinkYaml());
        // Carry the persisted syntax snapshot so Flink 1.19 never re-resolves its configuration
        // format from installation files that may have changed since registration.
        arguments.add(PARAM_KEY_FLINK_CONF_STANDARD_YAML);
        arguments.add(Boolean.toString(request.standardYaml()));
        arguments.add(PARAM_KEY_APP_NAME);
        arguments.add(DeflaterUtils.zipString(resolved.getJobName()));
        arguments.add(PARAM_KEY_FLINK_PARALLELISM);
        arguments.add(
            Integer.toString(
                configuration.get(
                    CoreOptions.DEFAULT_PARALLELISM,
                    CoreOptions.DEFAULT_PARALLELISM.defaultValue())));

        if (request.jobType() == FlinkJobType.FLINK_SQL) {
            arguments.add(PARAM_KEY_FLINK_SQL);
            arguments.add(resolved.getFlinkSqlContent());
            addJobConfiguration(request.appConf(), arguments);
        } else if (shouldAddJobConfiguration(request.appConf())) {
            addJobConfiguration(request.appConf(), arguments);
        }
    }

    /** Appends a non-null serialized job configuration argument. */
    private void addJobConfiguration(String appConf, List<String> arguments) {
        if (appConf != null) {
            arguments.add(PARAM_KEY_APP_CONF);
            arguments.add(appConf);
        }
    }

    /** Builds a Flink configuration from dynamic properties without a job request. */
    private Configuration extractConfiguration(
                                               String flinkHome, Map<String, Object> properties) throws Exception {
        Configuration defaults = loadDefault(flinkHome);
        List<CustomCommandLine> commandLines = customCommandLines(flinkHome, defaults);
        List<String> arguments = new ArrayList<>();
        if (MapUtils.isNotEmpty(properties)) {
            properties.forEach(
                (key, value) -> {
                    if (value != null) {
                        arguments.add("-D" + key + "=" + value.toString().trim());
                    }
                });
        }
        CommandLine commandLine =
            FlinkRunOption.parse(
                commandLineOptions(commandLines), arguments.toArray(new String[0]), true);
        CustomCommandLine activeCommandLine =
            activeCommandLine(commandLines, commandLine);
        return applyConfiguration(defaults, activeCommandLine, commandLine);
    }

    /** Selects the first target-specific command line that accepts the parsed arguments. */
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

    /** Loads Flink's built-in and plugin command lines against the captured defaults. */
    private List<CustomCommandLine> customCommandLines(
                                                       String flinkHome,
                                                       Configuration defaults) {
        String configurationDirectory = flinkHome + "/conf";
        return ClassLoaderUtils.runAsClassLoader(
            FlinkConfigurationBuilder.class.getClassLoader(),
            () -> CliFrontend.loadCustomCommandLines(
                defaults, configurationDirectory));
    }

    /** Merges target-specific CLI options with Flink's standard run options. */
    private Options commandLineOptions(List<CustomCommandLine> commandLines) {
        Options customOptions = new Options();
        for (CustomCommandLine commandLine : commandLines) {
            commandLine.addGeneralOptions(customOptions);
            commandLine.addRunOptions(customOptions);
        }
        return FlinkRunOption.mergeOptions(
            CliFrontendParser.getRunCommandOptions(), customOptions);
    }

    /** Applies captured defaults first and dynamic CLI configuration last. */
    private Configuration applyConfiguration(
                                             Configuration defaults,
                                             CustomCommandLine activeCommandLine,
                                             CommandLine commandLine) throws Exception {
        Preconditions.checkNotNull(activeCommandLine, "activeCommandLine must not be null");
        Configuration configuration = new Configuration();
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

    /** Preserves boolean flags as booleans while leaving valued options as strings. */
    private static Object parseOptionValue(String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }

    /** Determines whether job configuration must be forwarded to the runtime. */
    private static boolean shouldAddJobConfiguration(String appConf) {
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
