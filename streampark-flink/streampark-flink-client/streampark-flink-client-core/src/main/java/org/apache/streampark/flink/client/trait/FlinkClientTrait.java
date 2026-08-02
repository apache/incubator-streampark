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

package org.apache.streampark.flink.client.trait;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.common.enums.FlinkRestoreMode;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.common.util.FlinkConfigurationUtils;
import org.apache.streampark.common.util.LoggerSupport;
import org.apache.streampark.common.util.SystemPropertyUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.client.bean.CancelRequest;
import org.apache.streampark.flink.client.bean.CancelResponse;
import org.apache.streampark.flink.client.bean.SavepointRequestTrait;
import org.apache.streampark.flink.client.bean.SavepointResponse;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.client.bean.TriggerSavepointRequest;
import org.apache.streampark.flink.core.FlinkClusterClient;
import org.apache.streampark.flink.core.conf.FlinkRunOption;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.JobID;
import org.apache.flink.client.cli.CliArgsException;
import org.apache.flink.client.cli.CliFrontend;
import org.apache.flink.client.cli.CliFrontendParser;
import org.apache.flink.client.cli.CustomCommandLine;
import org.apache.flink.client.cli.ExecutionConfigAccessor;
import org.apache.flink.client.cli.ProgramOptions;
import org.apache.flink.client.deployment.application.ApplicationConfiguration;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.client.program.PackagedProgramUtils;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.ExecutionOptions;
import org.apache.flink.configuration.GlobalConfiguration;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.configuration.PipelineOptionsInternal;
import org.apache.flink.python.PythonOptions;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.SavepointConfigOptions;
import org.apache.flink.util.Preconditions;

import com.google.common.collect.Lists;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import scala.Tuple2;

/** Base trait for Flink client implementations. */
public abstract class FlinkClientTrait extends LoggerSupport {

    private final String paramKeyFlinkConf =
        ConfigKeys.KEY_FLINK_CONF(ConfigKeys.PARAM_PREFIX());
    private final String paramKeyFlinkSql =
        ConfigKeys.KEY_FLINK_SQL(ConfigKeys.PARAM_PREFIX());
    private final String paramKeyAppConf =
        ConfigKeys.KEY_APP_CONF(ConfigKeys.PARAM_PREFIX());
    private final String paramKeyAppName =
        ConfigKeys.KEY_APP_NAME(ConfigKeys.PARAM_PREFIX());
    private final String paramKeyFlinkParallelism =
        ConfigKeys.KEY_FLINK_PARALLELISM(ConfigKeys.PARAM_PREFIX());

    public SubmitResponse submit(SubmitRequest submitRequest) throws Exception {
        logInfo(
            "\n"
                + "--------------------------------------- flink job start ---------------------------------------\n"
                + "    userFlinkHome    : "
                + submitRequest.flinkVersion().getFlinkHome()
                + "\n"
                + "    flinkVersion     : "
                + submitRequest.flinkVersion().version()
                + "\n"
                + "    appName          : "
                + submitRequest.effectiveAppName()
                + "\n"
                + "    jobType          : "
                + submitRequest.jobType().name()
                + "\n"
                + "    deployMode       : "
                + submitRequest.deployMode().name()
                + "\n"
                + "    k8sNamespace     : "
                + submitRequest.kubernetesNamespace()
                + "\n"
                + "    flinkExposedType : "
                + submitRequest.flinkRestExposedType()
                + "\n"
                + "    clusterId        : "
                + submitRequest.clusterId()
                + "\n"
                + "    applicationType  : "
                + submitRequest.applicationType().getName()
                + "\n"
                + "    savePoint        : "
                + submitRequest.savePoint()
                + "\n"
                + "    properties       : "
                + formatProperties(submitRequest.properties())
                + "\n"
                + "    args             : "
                + submitRequest.args()
                + "\n"
                + "    appConf          : "
                + submitRequest.appConf()
                + "\n"
                + "    flinkBuildResult : "
                + submitRequest.buildResult()
                + "\n"
                + "-------------------------------------------------------------------------------------------\n");

        Configuration flinkConfig = prepareConfig(submitRequest);
        setConfig(submitRequest, flinkConfig);

        try {
            return doSubmit(submitRequest, flinkConfig);
        } catch (Exception e) {
            logError(
                "flink job "
                    + submitRequest.appName()
                    + " start failed, "
                    + "deployMode: "
                    + submitRequest.deployMode().getName()
                    + ", "
                    + "detail: "
                    + ExceptionUtils.stringifyException(e));
            throw e;
        }
    }

    private Configuration prepareConfig(SubmitRequest submitRequest) throws Exception {
        CommandLineAndConfig commandLineAndConfig = getCommandLineAndFlinkConfig(submitRequest);
        CommandLine commandLine = commandLineAndConfig.commandLine;
        Configuration flinkConfig = commandLineAndConfig.flinkConfig;

        if (submitRequest.jobType() == FlinkJobType.PYFLINK) {
            String pythonVenv = Workspace.local().APP_PYTHON_VENV();
            AssertUtils.required(
                FsOperator.lfs().exists(pythonVenv),
                pythonVenv + " File does not exist");

            safeSet(flinkConfig, PythonOptions.PYTHON_ARCHIVES, pythonVenv);
            safeSet(
                flinkConfig,
                PythonOptions.PYTHON_CLIENT_EXECUTABLE,
                Constants.PYTHON_EXECUTABLE);
            safeSet(flinkConfig, PythonOptions.PYTHON_EXECUTABLE, Constants.PYTHON_EXECUTABLE);

            String flinkOptPath = System.getenv(ConfigConstants.ENV_FLINK_OPT_DIR);
            if (StringUtils.isBlank(flinkOptPath)) {
                logWarn(
                    "Get environment variable "
                        + ConfigConstants.ENV_FLINK_OPT_DIR
                        + " fail");
                String flinkHome = submitRequest.flinkVersion().getFlinkHome();
                SystemPropertyUtils.setEnv(
                    ConfigConstants.ENV_FLINK_OPT_DIR, flinkHome + "/opt");
                logInfo(
                    "Set temporary environment variables "
                        + ConfigConstants.ENV_FLINK_OPT_DIR
                        + " = "
                        + flinkHome
                        + "/opt");
            }
        } else if (submitRequest.userJarFile() != null) {
            URI uri =
                PackagedProgramUtils.resolveURI(
                    submitRequest.userJarFile().getAbsolutePath());
            ProgramOptions programOptions = ProgramOptions.create(commandLine);
            ExecutionConfigAccessor executionParameters =
                ExecutionConfigAccessor.fromProgramOptions(
                    programOptions, Collections.singletonList(uri.toString()));
            executionParameters.applyToConfiguration(flinkConfig);
        }

        safeSet(flinkConfig, PipelineOptions.NAME, submitRequest.effectiveAppName());
        safeSet(flinkConfig, DeploymentOptions.TARGET, submitRequest.deployMode().getName());
        safeSet(flinkConfig, SavepointConfigOptions.SAVEPOINT_PATH, submitRequest.savePoint());
        safeSet(
            flinkConfig,
            ApplicationConfiguration.APPLICATION_MAIN_CLASS,
            submitRequest.appMain());
        safeSet(
            flinkConfig,
            ApplicationConfiguration.APPLICATION_ARGS,
            extractProgramArgs(submitRequest));
        safeSet(
            flinkConfig,
            PipelineOptionsInternal.PIPELINE_FIXED_JOB_ID,
            submitRequest.jobId());

        if (!submitRequest.hasProp(CheckpointingOptions.MAX_RETAINED_CHECKPOINTS.key())) {
            Configuration flinkDefaultConfiguration =
                getFlinkDefaultConfiguration(submitRequest.flinkVersion().getFlinkHome());
            ConfigOption<Integer> retainedOption =
                CheckpointingOptions.MAX_RETAINED_CHECKPOINTS;
            flinkConfig.set(
                retainedOption, flinkDefaultConfiguration.get(retainedOption));
        }

        if (StringUtils.isNotBlank(submitRequest.savePoint())) {
            safeSet(
                flinkConfig,
                SavepointConfigOptions.SAVEPOINT_PATH,
                submitRequest.savePoint());
            flinkConfig.setBoolean(
                SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE,
                submitRequest.allowNonRestoredState());
            boolean enableRestoreMode =
                submitRequest.restoreMode() != null
                    && submitRequest
                        .flinkVersion()
                        .checkVersion(FlinkRestoreMode.SINCE_FLINK_VERSION);
            if (enableRestoreMode) {
                flinkConfig.setString(
                    FlinkRestoreMode.RESTORE_MODE, submitRequest.restoreMode().getName());
            }
        }

        if (MapUtils.isNotEmpty(submitRequest.properties())) {
            if (submitRequest.hasProp(CoreOptions.FLINK_JVM_OPTIONS.key())) {
                Object jvmOptObj =
                    submitRequest.getProp(CoreOptions.FLINK_JVM_OPTIONS.key());
                String jvmOpt = jvmOptObj.toString();
                if (!jvmOpt.contains("-Dfile.encoding=")) {
                    String opt = "-Dfile.encoding=UTF-8 " + jvmOpt;
                    submitRequest.properties().put(CoreOptions.FLINK_JVM_OPTIONS.key(), opt);
                }
            }

            for (Map.Entry<String, Object> entry : submitRequest.properties().entrySet()) {
                if (entry.getKey().startsWith("env.")) {
                    logInfo("env opts:  " + entry.getKey() + ": " + entry.getValue());
                    flinkConfig.setString(entry.getKey(), entry.getValue().toString());
                }
            }
        }

        return flinkConfig;
    }

    public abstract void setConfig(SubmitRequest submitRequest, Configuration flinkConf);

    public SavepointResponse triggerSavepoint(TriggerSavepointRequest savepointRequest) throws Exception {
        logInfo(
            "\n"
                + "----------------------------------------- flink job trigger savepoint ---------------------\n"
                + "     userFlinkHome  : "
                + savepointRequest.flinkVersion().getFlinkHome()
                + "\n"
                + "     flinkVersion   : "
                + savepointRequest.flinkVersion().version()
                + "\n"
                + "     clusterId      : "
                + savepointRequest.clusterId()
                + "\n"
                + "     savePointPath  : "
                + savepointRequest.savepointPath()
                + "\n"
                + "     nativeFormat   : "
                + savepointRequest.nativeFormat()
                + "\n"
                + "     k8sNamespace   : "
                + savepointRequest.kubernetesNamespace()
                + "\n"
                + "     appId          : "
                + savepointRequest.clusterId()
                + "\n"
                + "     jobId          : "
                + savepointRequest.jobId()
                + "\n"
                + "-------------------------------------------------------------------------------------------\n");
        Configuration flinkConf = new Configuration();
        return doTriggerSavepoint(savepointRequest, flinkConf);
    }

    public CancelResponse cancel(CancelRequest cancelRequest) throws Exception {
        logInfo(
            "\n"
                + "----------------------------------------- flink job cancel --------------------------------\n"
                + "     userFlinkHome     : "
                + cancelRequest.flinkVersion().getFlinkHome()
                + "\n"
                + "     flinkVersion      : "
                + cancelRequest.flinkVersion().version()
                + "\n"
                + "     clusterId         : "
                + cancelRequest.clusterId()
                + "\n"
                + "     withSavePoint     : "
                + cancelRequest.withSavepoint()
                + "\n"
                + "     savePointPath     : "
                + cancelRequest.savepointPath()
                + "\n"
                + "     withDrain         : "
                + cancelRequest.withDrain()
                + "\n"
                + "     nativeFormat      : "
                + cancelRequest.nativeFormat()
                + "\n"
                + "     k8sNamespace      : "
                + cancelRequest.kubernetesNamespace()
                + "\n"
                + "     appId             : "
                + cancelRequest.clusterId()
                + "\n"
                + "     jobId             : "
                + cancelRequest.jobId()
                + "\n"
                + "-------------------------------------------------------------------------------------------\n");
        Configuration flinkConf = new Configuration();
        return doCancel(cancelRequest, flinkConf);
    }

    public abstract SubmitResponse doSubmit(SubmitRequest submitRequest, Configuration flinkConf) throws Exception;

    public abstract SavepointResponse doTriggerSavepoint(
                                                         TriggerSavepointRequest request,
                                                         Configuration flinkConf) throws Exception;

    public abstract CancelResponse doCancel(CancelRequest cancelRequest, Configuration flinkConf) throws Exception;

    protected SubmitResponse trySubmit(
                                       SubmitRequest submitRequest,
                                       Configuration flinkConfig,
                                       File jarFile,
                                       SubmitFunc jobGraphFunc,
                                       SubmitFunc restApiFunc) throws Exception {
        try {
            logInfo("[flink-submit] Submit job with JobGraph Plan.");
            return jobGraphFunc.apply(submitRequest, flinkConfig, jarFile);
        } catch (Exception e) {
            try {
                return restApiFunc.apply(submitRequest, flinkConfig, jarFile);
            } catch (Exception e1) {
                throw new RuntimeException(
                    "[flink-submit] Both JobGraph submit plan and Rest API submit plan all failed!\n"
                        + "JobGraph Submit plan failed detail:\n"
                        + "------------------------------------------------------------------\n"
                        + ExceptionUtils.stringifyException(e)
                        + "\n"
                        + "------------------------------------------------------------------\n"
                        + "\n"
                        + " RestAPI Submit plan failed detail:\n"
                        + " ------------------------------------------------------------------\n"
                        + ExceptionUtils.stringifyException(e1)
                        + "\n"
                        + "------------------------------------------------------------------\n");
            }
        }
    }

    public Tuple2<PackagedProgram, JobGraph> getJobGraph(
                                                         Configuration flinkConfig, SubmitRequest submitRequest,
                                                         File jarFile) throws Exception {
        PackagedProgram.Builder builder =
            PackagedProgram.newBuilder()
                .setSavepointRestoreSettings(submitRequest.savepointRestoreSettings())
                .setEntryPointClassName(
                    flinkConfig
                        .getOptional(ApplicationConfiguration.APPLICATION_MAIN_CLASS)
                        .orElseThrow(
                            () -> new IllegalStateException(
                                "Application main class is not configured")))
                .setArguments(
                    flinkConfig
                        .getOptional(ApplicationConfiguration.APPLICATION_ARGS)
                        .orElse(Lists.newArrayList())
                        .toArray(new String[0]));

        if (submitRequest.jobType() == FlinkJobType.PYFLINK) {
            if (!submitRequest.libs().isEmpty()) {
                // BUG: https://github.com/apache/streampark/issues/3761
                // builder.setUserClassPaths(Lists.newArrayList(submitRequest.libs()))
            }
        } else {
            builder.setJarFile(jarFile);
            // BUG: https://github.com/apache/streampark/issues/3761
            // .setUserClassPaths(Lists.newArrayList(submitRequest.classPaths()))
        }

        PackagedProgram packageProgram = builder.build();
        JobGraph jobGraph =
            PackagedProgramUtils.createJobGraph(
                packageProgram,
                flinkConfig,
                getParallelism(submitRequest),
                null,
                false);

        return new Tuple2<>(packageProgram, jobGraph);
    }

    public JobID getJobID(String jobId) throws CliArgsException {
        try {
            return JobID.fromHexString(jobId);
        } catch (Exception e) {
            throw new CliArgsException(e.getMessage());
        }
    }

    CustomCommandLine validateAndGetActiveCommandLine(
                                                      List<CustomCommandLine> customCommandLines,
                                                      CommandLine commandLine) {
        CommandLine line = Preconditions.checkNotNull(commandLine);
        logInfo("Custom commandline: " + customCommandLines);
        for (CustomCommandLine cli : customCommandLines) {
            boolean isActive = cli.isActive(line);
            logInfo("Checking custom commandline " + cli + ", isActive: " + isActive);
            if (isActive) {
                return cli;
            }
        }
        throw new IllegalStateException("No valid command-line found.");
    }

    public Configuration getFlinkDefaultConfiguration(String flinkHome) {
        try {
            return GlobalConfiguration.loadConfiguration(flinkHome + "/conf");
        } catch (Exception e) {
            return new Configuration();
        }
    }

    <T> T getOptionFromDefaultFlinkConfig(String flinkHome, ConfigOption<T> option) {
        return getFlinkDefaultConfiguration(flinkHome).get(option);
    }

    List<CustomCommandLine> getCustomCommandLines(String flinkHome) {
        Configuration flinkDefaultConfiguration = getFlinkDefaultConfiguration(flinkHome);
        String confDir = flinkHome + "/conf";
        return CliFrontend.loadCustomCommandLines(flinkDefaultConfiguration, confDir);
    }

    public Integer getParallelism(SubmitRequest submitRequest) {
        if (submitRequest.hasProp(ConfigKeys.KEY_FLINK_PARALLELISM())) {
            return Integer.valueOf(
                submitRequest.getProp(ConfigKeys.KEY_FLINK_PARALLELISM()).toString());
        }
        return getFlinkDefaultConfiguration(submitRequest.flinkVersion().getFlinkHome())
            .getInteger(
                CoreOptions.DEFAULT_PARALLELISM, CoreOptions.DEFAULT_PARALLELISM.defaultValue());
    }

    Options getCommandLineOptions(String flinkHome) {
        List<CustomCommandLine> customCommandLines = getCustomCommandLines(flinkHome);
        Options customCommandLineOptions = new Options();
        for (CustomCommandLine customCommandLine : customCommandLines) {
            customCommandLine.addGeneralOptions(customCommandLineOptions);
            customCommandLine.addRunOptions(customCommandLineOptions);
        }
        return FlinkRunOption.mergeOptions(
            CliFrontendParser.getRunCommandOptions(), customCommandLineOptions);
    }

    public Configuration extractConfiguration(String flinkHome, Map<String, Object> properties) throws Exception {
        Options commandLineOptions = getCommandLineOptions(flinkHome);
        List<String> cliArgs = new ArrayList<>();
        if (MapUtils.isNotEmpty(properties)) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                cliArgs.add("-D" + entry.getKey() + "=" + entry.getValue().toString().trim());
            }
        }
        CommandLine commandLine =
            FlinkRunOption.parse(
                commandLineOptions, cliArgs.toArray(new String[0]), true);
        CustomCommandLine activeCommandLine =
            validateAndGetActiveCommandLine(getCustomCommandLines(flinkHome), commandLine);
        return applyConfiguration(flinkHome, activeCommandLine, commandLine);
    }

    public String cancelJob(CancelRequest cancelRequest, JobID jobID, ClusterClient<?> client) throws Exception {
        String savePointDir = tryGetSavepointPathIfNeed(cancelRequest);

        FlinkClusterClient<?> clientWrapper = new FlinkClusterClient<>(client);
        boolean withSavepoint = cancelRequest.withSavepoint();
        boolean withDrain = cancelRequest.withDrain();

        if (!withSavepoint && !withDrain) {
            client.cancel(jobID).get();
            return null;
        }
        return clientWrapper
            .stopWithSavepoint(
                jobID, cancelRequest.withDrain(), savePointDir, cancelRequest.nativeFormat())
            .get();
    }

    public String triggerSavepoint(
                                   TriggerSavepointRequest savepointRequest, JobID jobID,
                                   ClusterClient<?> client) throws Exception {
        String savepointPath = tryGetSavepointPathIfNeed(savepointRequest);
        FlinkClusterClient<?> clientWrapper = new FlinkClusterClient<>(client);
        return clientWrapper
            .triggerSavepoint(jobID, savepointPath, savepointRequest.nativeFormat())
            .get();
    }

    public void closeSubmit(SubmitRequest submitRequest, AutoCloseable... close) {
        for (AutoCloseable resource : close) {
            if (resource instanceof PackagedProgram) {
                if (submitRequest.safePackageProgram()) {
                    Utils.close(resource);
                }
            } else {
                Utils.close(resource);
            }
        }
    }

    static <T> Configuration safeSet(Configuration flinkConfig, ConfigOption<T> option, T value) {
        if (value != null && !value.toString().isEmpty()) {
            flinkConfig.set(option, value);
        }
        return flinkConfig;
    }

    private String tryGetSavepointPathIfNeed(SavepointRequestTrait request) {
        if (!request.withSavepoint()) {
            return null;
        }
        if (StringUtils.isNotBlank(request.savepointPath())) {
            return request.savepointPath();
        }
        String configDir =
            getOptionFromDefaultFlinkConfig(
                request.flinkVersion().getFlinkHome(),
                org.apache.flink.configuration.ConfigOptions.key(
                    CheckpointingOptions.SAVEPOINT_DIRECTORY.key())
                    .stringType()
                    .defaultValue(
                        request.deployMode() == FlinkDeployMode.YARN_APPLICATION
                            ? Workspace.remote().APP_SAVEPOINTS()
                            : null));

        AssertUtils.required(
            StringUtils.isNotBlank(configDir),
            "[StreamPark] deployMode: "
                + request.deployMode().getName()
                + ", savePoint path is null or invalid.");
        return configDir;
    }

    private CommandLineAndConfig getCommandLineAndFlinkConfig(SubmitRequest submitRequest) throws Exception {
        Options commandLineOptions =
            getCommandLineOptions(submitRequest.flinkVersion().getFlinkHome());

        Map<String, Object> optionMap = new HashMap<>();
        Map<String, String> appOption = submitRequest.appOption();
        for (Map.Entry<String, String> opt : appOption.entrySet()) {
            boolean verify = commandLineOptions.hasOption(opt.getKey());
            if (!verify) {
                logWarn("param:" + opt.getKey() + " is error,skip it.");
            } else {
                String option = commandLineOptions.getOption(opt.getKey().trim()).getOpt();
                Object parsedValue = parseOptionValue(opt.getValue());
                if (parsedValue instanceof Boolean) {
                    if ((Boolean) parsedValue) {
                        optionMap.put("-" + option, true);
                    }
                } else {
                    optionMap.put("-" + option, parsedValue);
                }
            }
        }

        if (submitRequest.savePoint() != null) {
            optionMap.put(
                "-" + FlinkRunOption.SAVEPOINT_PATH_OPTION.getOpt(),
                submitRequest.savePoint());
        }

        optionMap.remove("-e");
        optionMap.remove("--executor");
        optionMap.remove("-t");
        optionMap.remove("--target");
        if (submitRequest.deployMode() != null) {
            optionMap.put("-t", submitRequest.deployMode().getName());
        }

        List<String> array = new ArrayList<>();
        for (Map.Entry<String, Object> opt : optionMap.entrySet()) {
            array.add(opt.getKey());
            if (opt.getValue() instanceof String) {
                array.add((String) opt.getValue());
            }
        }

        if (MapUtils.isNotEmpty(submitRequest.properties())) {
            for (Map.Entry<String, Object> key : submitRequest.properties().entrySet()) {
                if (!key.getKey().startsWith("env.")) {
                    logInfo(
                        "application dynamicProperties:  "
                            + key.getKey()
                            + " :"
                            + key.getValue());
                    array.add("-D" + key.getKey() + "=" + key.getValue());
                }
            }
        }

        String[] cliArgs = array.toArray(new String[0]);
        logger().info("cliArgs: " + String.join(" ", cliArgs));

        CommandLine commandLine = FlinkRunOption.parse(commandLineOptions, cliArgs, true);

        CustomCommandLine activeCommandLine =
            validateAndGetActiveCommandLine(
                getCustomCommandLines(submitRequest.flinkVersion().getFlinkHome()), commandLine);

        Configuration configuration =
            applyConfiguration(
                submitRequest.flinkVersion().getFlinkHome(), activeCommandLine, commandLine);

        return new CommandLineAndConfig(commandLine, configuration);
    }

    private List<String> extractProgramArgs(SubmitRequest submitRequest) {
        List<String> programArgs = new ArrayList<>();
        programArgs.addAll(FlinkConfigurationUtils.extractArguments(submitRequest.args()));

        if (submitRequest.applicationType() == ApplicationType.STREAMPARK_FLINK) {
            programArgs.add(paramKeyFlinkConf);
            programArgs.add(submitRequest.flinkYaml());
            programArgs.add(paramKeyAppName);
            programArgs.add(DeflaterUtils.zipString(submitRequest.effectiveAppName()));
            programArgs.add(paramKeyFlinkParallelism);
            programArgs.add(getParallelism(submitRequest).toString());

            if (submitRequest.jobType() == FlinkJobType.FLINK_SQL) {
                programArgs.add(paramKeyFlinkSql);
                programArgs.add(submitRequest.flinkSQL());
                if (submitRequest.appConf() != null) {
                    programArgs.add(paramKeyAppConf);
                    programArgs.add(submitRequest.appConf());
                }
            } else if (shouldAddAppConf(submitRequest.appConf())) {
                programArgs.add(paramKeyAppConf);
                programArgs.add(submitRequest.appConf());
            }
        }

        if (MapUtils.isNotEmpty(submitRequest.properties())
            && submitRequest.properties().containsKey(ExecutionOptions.RUNTIME_MODE.key())) {
            String runtimeMode =
                submitRequest.properties().get(ExecutionOptions.RUNTIME_MODE.key()).toString();
            programArgs.add("--" + ExecutionOptions.RUNTIME_MODE.key());
            programArgs.add(runtimeMode);
        }

        if (submitRequest.jobType() == FlinkJobType.PYFLINK) {
            if (submitRequest.deployMode() != FlinkDeployMode.YARN_APPLICATION) {
                programArgs.add("-py");
                programArgs.add(submitRequest.userJarFile().getAbsolutePath());
            }
        }

        return Lists.newArrayList(programArgs);
    }

    private Configuration applyConfiguration(
                                             String flinkHome, CustomCommandLine activeCustomCommandLine,
                                             CommandLine commandLine) throws Exception {
        Preconditions.checkNotNull(
            activeCustomCommandLine, "activeCustomCommandLine must not be null.");
        Configuration configuration = new Configuration();
        Configuration flinkDefaultConfiguration = getFlinkDefaultConfiguration(flinkHome);
        Set<String> keys = flinkDefaultConfiguration.keySet();
        for (String key : keys) {
            String value = flinkDefaultConfiguration.getString(key, null);
            if (value != null) {
                configuration.setString(key, value);
            }
        }
        configuration.addAll(activeCustomCommandLine.toConfiguration(commandLine));
        return configuration;
    }

    private static Object parseOptionValue(String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }

    private static boolean shouldAddAppConf(String appConf) {
        if (appConf == null) {
            return true;
        }
        try {
            return !appConf.startsWith("json:");
        } catch (Exception e) {
            return true;
        }
    }

    private static String formatProperties(Map<String, Object> properties) {
        if (MapUtils.isEmpty(properties)) {
            return "";
        }
        return properties.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(" "));
    }

    @FunctionalInterface
    protected interface SubmitFunc {

        SubmitResponse apply(SubmitRequest request, Configuration config, File jarFile) throws Exception;
    }

    private static final class CommandLineAndConfig {

        final CommandLine commandLine;
        final Configuration flinkConfig;

        CommandLineAndConfig(CommandLine commandLine, Configuration flinkConfig) {
            this.commandLine = commandLine;
            this.flinkConfig = flinkConfig;
        }
    }
}
