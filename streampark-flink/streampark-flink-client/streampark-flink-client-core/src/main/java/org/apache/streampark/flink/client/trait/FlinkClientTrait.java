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
import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.common.enums.FlinkRestoreMode;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.common.util.FlinkConfigurationUtils;
import org.apache.streampark.common.util.StreamParkLoggerFactory;
import org.apache.streampark.common.util.SystemPropertyUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.client.bean.CancelRequest;
import org.apache.streampark.flink.client.bean.CancelResponse;
import org.apache.streampark.flink.client.bean.SavepointRequestTrait;
import org.apache.streampark.flink.client.bean.SavepointResponse;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.client.bean.TriggerSavepointRequest;
import org.apache.streampark.flink.client.util.FlinkConfigurationEnhancer;
import org.apache.streampark.flink.core.FlinkClusterClient;
import org.apache.streampark.flink.core.conf.FlinkRunOption;

import org.apache.streampark.shaded.org.slf4j.Logger;

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
import org.apache.flink.configuration.ConfigOptions;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Base Flink client operations shared by all deploy-mode implementations. */
public abstract class FlinkClientTrait {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(FlinkClientTrait.class.getName());

    private final String paramKeyFlinkConf = ConfigKeys.KEY_FLINK_CONF(ConfigKeys.PARAM_PREFIX());
    private final String paramKeyFlinkSql = ConfigKeys.KEY_FLINK_SQL(ConfigKeys.PARAM_PREFIX());
    private final String paramKeyAppConf = ConfigKeys.KEY_APP_CONF(ConfigKeys.PARAM_PREFIX());
    private final String paramKeyAppName = ConfigKeys.KEY_APP_NAME(ConfigKeys.PARAM_PREFIX());
    private final String paramKeyFlinkParallelism =
        ConfigKeys.KEY_FLINK_PARALLELISM(ConfigKeys.PARAM_PREFIX());

    public SubmitResponse submit(SubmitRequest submitRequest) throws Exception {
        LOG.info(
            "\n--------------------------------------- flink job start ---------------------------------------\n"
                + "    userFlinkHome    : {}\n"
                + "    flinkVersion     : {}\n"
                + "    appName          : {}\n"
                + "    jobType          : {}\n"
                + "    deployMode       : {}\n"
                + "    k8sNamespace     : {}\n"
                + "    flinkExposedType : {}\n"
                + "    clusterId        : {}\n"
                + "    applicationType  : {}\n"
                + "    savePoint        : {}\n"
                + "    properties       : {}\n"
                + "    args             : {}\n"
                + "    appConf          : {}\n"
                + "    flinkBuildResult : {}\n"
                + "-------------------------------------------------------------------------------------------\n",
            submitRequest.getFlinkVersion().flinkHome,
            submitRequest.getFlinkVersion().version(),
            submitRequest.getEffectiveAppName(),
            submitRequest.getJobType().name(),
            submitRequest.getDeployMode().name(),
            submitRequest.getKubernetesNamespace(),
            submitRequest.getFlinkRestExposedType(),
            submitRequest.getClusterId(),
            submitRequest.getApplicationType().getName(),
            submitRequest.getSavePoint(),
            formatProperties(submitRequest.getProperties()),
            submitRequest.getArgs(),
            submitRequest.getAppConf(),
            submitRequest.getBuildResult());

        Configuration flinkConfig = prepareConfig(submitRequest);
        setConfig(submitRequest, flinkConfig);

        try {
            return doSubmit(submitRequest, flinkConfig);
        } catch (Exception e) {
            LOG.error(
                "flink job {} start failed, deployMode: {}, detail: {}",
                submitRequest.getAppName(),
                submitRequest.getDeployMode().getName(),
                ExceptionUtils.stringifyException(e));
            throw e;
        }
    }

    public abstract void setConfig(SubmitRequest submitRequest, Configuration flinkConf);

    public SavepointResponse triggerSavepoint(TriggerSavepointRequest savepointRequest) throws Exception {
        LOG.info(
            "\n----------------------------------------- flink job trigger savepoint ---------------------\n"
                + "     userFlinkHome  : {}\n"
                + "     flinkVersion   : {}\n"
                + "     clusterId      : {}\n"
                + "     savePointPath  : {}\n"
                + "     nativeFormat   : {}\n"
                + "     k8sNamespace   : {}\n"
                + "     appId          : {}\n"
                + "     jobId          : {}\n"
                + "-------------------------------------------------------------------------------------------\n",
            savepointRequest.getFlinkVersion().flinkHome,
            savepointRequest.getFlinkVersion().version(),
            savepointRequest.getClusterId(),
            savepointRequest.getSavepointPath(),
            savepointRequest.isNativeFormat(),
            savepointRequest.getKubernetesNamespace(),
            savepointRequest.getClusterId(),
            savepointRequest.getJobId());
        Configuration flinkConf = new Configuration();
        return doTriggerSavepoint(savepointRequest, flinkConf);
    }

    public CancelResponse cancel(CancelRequest cancelRequest) throws Exception {
        LOG.info(
            "\n----------------------------------------- flink job cancel --------------------------------\n"
                + "     userFlinkHome     : {}\n"
                + "     flinkVersion      : {}\n"
                + "     clusterId         : {}\n"
                + "     withSavePoint     : {}\n"
                + "     savePointPath     : {}\n"
                + "     withDrain         : {}\n"
                + "     nativeFormat      : {}\n"
                + "     k8sNamespace      : {}\n"
                + "     appId             : {}\n"
                + "     jobId             : {}\n"
                + "-------------------------------------------------------------------------------------------\n",
            cancelRequest.getFlinkVersion().flinkHome,
            cancelRequest.getFlinkVersion().version(),
            cancelRequest.getClusterId(),
            cancelRequest.isWithSavepoint(),
            cancelRequest.getSavepointPath(),
            cancelRequest.isWithDrain(),
            cancelRequest.isNativeFormat(),
            cancelRequest.getKubernetesNamespace(),
            cancelRequest.getClusterId(),
            cancelRequest.getJobId());
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
                                       SubmitFunction jobGraphFunc,
                                       SubmitFunction restApiFunc) throws Exception {
        try {
            LOG.info("[flink-submit] Submit job with JobGraph Plan.");
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
                        + "\n------------------------------------------------------------------\n\n"
                        + " RestAPI Submit plan failed detail:\n"
                        + " ------------------------------------------------------------------\n"
                        + ExceptionUtils.stringifyException(e1)
                        + "\n------------------------------------------------------------------\n");
            }
        }
    }

    protected JobGraphPackagedProgram getJobGraph(
                                                  Configuration flinkConfig, SubmitRequest submitRequest,
                                                  File jarFile) throws Exception {
        PackagedProgram.Builder builder =
            PackagedProgram.newBuilder()
                .setSavepointRestoreSettings(submitRequest.getSavepointRestoreSettings())
                .setEntryPointClassName(
                    flinkConfig
                        .getOptional(ApplicationConfiguration.APPLICATION_MAIN_CLASS)
                        .get())
                .setArguments(
                    flinkConfig
                        .getOptional(ApplicationConfiguration.APPLICATION_ARGS)
                        .orElse(Lists.newArrayList())
                        .toArray(new String[0]));

        if (submitRequest.getJobType() == FlinkJobType.PYFLINK) {
            if (!submitRequest.getLibs().isEmpty()) {
                // BUG: https://github.com/apache/streampark/issues/3761
            }
        } else {
            builder.setJarFile(jarFile);
        }

        PackagedProgram packageProgram = builder.build();
        JobGraph jobGraph =
            PackagedProgramUtils.createJobGraph(
                packageProgram,
                flinkConfig,
                getParallelism(submitRequest),
                null,
                false);
        return new JobGraphPackagedProgram(packageProgram, jobGraph);
    }

    protected JobID getJobID(String jobId) throws CliArgsException {
        try {
            return JobID.fromHexString(jobId);
        } catch (Exception e) {
            throw new CliArgsException(e.getMessage());
        }
    }

    protected CustomCommandLine validateAndGetActiveCommandLine(
                                                                List<CustomCommandLine> customCommandLines,
                                                                CommandLine commandLine) {
        CommandLine line = Preconditions.checkNotNull(commandLine);
        LOG.info("Custom commandline: {}", customCommandLines);
        for (CustomCommandLine cli : customCommandLines) {
            boolean isActive = cli.isActive(line);
            LOG.info("Checking custom commandline {}, isActive: {}", cli, isActive);
            if (isActive) {
                return cli;
            }
        }
        throw new IllegalStateException("No valid command-line found.");
    }

    protected Configuration getFlinkDefaultConfiguration(String flinkHome) {
        try {
            return GlobalConfiguration.loadConfiguration(flinkHome + "/conf");
        } catch (Exception e) {
            return new Configuration();
        }
    }

    protected <T> T getOptionFromDefaultFlinkConfig(String flinkHome, ConfigOption<T> option) {
        return getFlinkDefaultConfiguration(flinkHome).get(option);
    }

    protected Integer getParallelism(SubmitRequest submitRequest) {
        if (submitRequest.hasProp(ConfigKeys.KEY_FLINK_PARALLELISM())) {
            return Integer.valueOf(submitRequest.getProp(ConfigKeys.KEY_FLINK_PARALLELISM()).toString());
        }
        return getFlinkDefaultConfiguration(submitRequest.getFlinkVersion().flinkHome)
            .getInteger(
                CoreOptions.DEFAULT_PARALLELISM, CoreOptions.DEFAULT_PARALLELISM.defaultValue());
    }

    protected Configuration extractConfiguration(String flinkHome, Map<String, Object> properties) throws Exception {
        Options commandLineOptions = getCommandLineOptions(flinkHome);
        List<String> cliArgs = new ArrayList<>();
        if (MapUtils.isNotEmpty(properties)) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                cliArgs.add("-D" + entry.getKey() + "=" + entry.getValue().toString().trim());
            }
        }
        CommandLine commandLine =
            FlinkRunOption.parse(commandLineOptions, cliArgs.toArray(new String[0]), true);
        CustomCommandLine activeCommandLine =
            validateAndGetActiveCommandLine(getCustomCommandLines(flinkHome), commandLine);
        return applyConfiguration(flinkHome, activeCommandLine, commandLine);
    }

    protected Options getCommandLineOptions(String flinkHome) throws Exception {
        List<CustomCommandLine> customCommandLines = getCustomCommandLines(flinkHome);
        Options customCommandLineOptions = new Options();
        for (CustomCommandLine customCommandLine : customCommandLines) {
            customCommandLine.addGeneralOptions(customCommandLineOptions);
            customCommandLine.addRunOptions(customCommandLineOptions);
        }
        return FlinkRunOption.mergeOptions(
            CliFrontendParser.getRunCommandOptions(), customCommandLineOptions);
    }

    protected String cancelJob(CancelRequest cancelRequest, JobID jobID, ClusterClient<?> client) throws Exception {
        String savePointDir = tryGetSavepointPathIfNeed(cancelRequest);
        FlinkClusterClient<?> clientWrapper = new FlinkClusterClient<>(client);
        if (!cancelRequest.isWithSavepoint() && !cancelRequest.isWithDrain()) {
            client.cancel(jobID).get();
            return null;
        }
        return clientWrapper
            .stopWithSavepoint(
                jobID,
                cancelRequest.isWithDrain(),
                savePointDir,
                cancelRequest.isNativeFormat())
            .get();
    }

    protected String triggerSavepoint(
                                      TriggerSavepointRequest savepointRequest, JobID jobID,
                                      ClusterClient<?> client) throws Exception {
        String savepointPath = tryGetSavepointPathIfNeed(savepointRequest);
        FlinkClusterClient<?> clientWrapper = new FlinkClusterClient<>(client);
        return clientWrapper
            .triggerSavepoint(jobID, savepointPath, savepointRequest.isNativeFormat())
            .get();
    }

    protected void closeSubmit(SubmitRequest submitRequest, AutoCloseable... close) {
        for (AutoCloseable autoCloseable : close) {
            if (autoCloseable instanceof PackagedProgram) {
                if (submitRequest.isSafePackageProgram()) {
                    Utils.close(autoCloseable);
                }
            } else {
                Utils.close(autoCloseable);
            }
        }
    }

    private Configuration prepareConfig(SubmitRequest submitRequest) throws Exception {
        CommandLineAndConfig commandLineAndConfig = getCommandLineAndFlinkConfig(submitRequest);
        CommandLine commandLine = commandLineAndConfig.commandLine;
        Configuration flinkConfig = commandLineAndConfig.flinkConfig;

        if (submitRequest.getJobType() == FlinkJobType.PYFLINK) {
            String pythonVenv = Workspace.local().APP_PYTHON_VENV();
            AssertUtils.required(
                FsOperator.lfs().exists(pythonVenv), pythonVenv + " File does not exist");
            FlinkConfigurationEnhancer.safeSet(flinkConfig, PythonOptions.PYTHON_ARCHIVES, pythonVenv);
            FlinkConfigurationEnhancer.safeSet(
                flinkConfig, PythonOptions.PYTHON_CLIENT_EXECUTABLE,
                org.apache.streampark.common.constants.Constants.PYTHON_EXECUTABLE);
            FlinkConfigurationEnhancer.safeSet(
                flinkConfig, PythonOptions.PYTHON_EXECUTABLE,
                org.apache.streampark.common.constants.Constants.PYTHON_EXECUTABLE);

            String flinkOptPath = System.getenv(ConfigConstants.ENV_FLINK_OPT_DIR);
            if (StringUtils.isBlank(flinkOptPath)) {
                LOG.warn("Get environment variable {} fail", ConfigConstants.ENV_FLINK_OPT_DIR);
                String flinkHome = submitRequest.getFlinkVersion().flinkHome;
                SystemPropertyUtils.setEnv(
                    ConfigConstants.ENV_FLINK_OPT_DIR, flinkHome + "/opt");
                LOG.info(
                    "Set temporary environment variables {} = {}/opt",
                    ConfigConstants.ENV_FLINK_OPT_DIR,
                    flinkHome);
            }
        } else if (submitRequest.getUserJarFile() != null) {
            java.net.URI uri =
                PackagedProgramUtils.resolveURI(submitRequest.getUserJarFile().getAbsolutePath());
            ProgramOptions programOptions = ProgramOptions.create(commandLine);
            ExecutionConfigAccessor executionParameters =
                ExecutionConfigAccessor.fromProgramOptions(
                    programOptions, Collections.singletonList(uri.toString()));
            executionParameters.applyToConfiguration(flinkConfig);
        }

        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, PipelineOptions.NAME, submitRequest.getEffectiveAppName());
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, DeploymentOptions.TARGET, submitRequest.getDeployMode().getName());
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, SavepointConfigOptions.SAVEPOINT_PATH, submitRequest.getSavePoint());
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig,
            ApplicationConfiguration.APPLICATION_MAIN_CLASS,
            submitRequest.getAppMain());
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig,
            ApplicationConfiguration.APPLICATION_ARGS,
            extractProgramArgs(submitRequest));
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, PipelineOptionsInternal.PIPELINE_FIXED_JOB_ID, submitRequest.getJobId());

        if (!submitRequest.hasProp(CheckpointingOptions.MAX_RETAINED_CHECKPOINTS.key())) {
            Configuration flinkDefaultConfiguration =
                getFlinkDefaultConfiguration(submitRequest.getFlinkVersion().flinkHome);
            ConfigOption<Integer> retainedOption = CheckpointingOptions.MAX_RETAINED_CHECKPOINTS;
            FlinkConfigurationEnhancer.safeSet(
                flinkConfig, retainedOption, flinkDefaultConfiguration.get(retainedOption));
        }

        if (StringUtils.isNotBlank(submitRequest.getSavePoint())) {
            FlinkConfigurationEnhancer.safeSet(
                flinkConfig, SavepointConfigOptions.SAVEPOINT_PATH, submitRequest.getSavePoint());
            flinkConfig.setBoolean(
                SavepointConfigOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE,
                submitRequest.isAllowNonRestoredState());
            boolean enableRestoreMode =
                submitRequest.getRestoreMode() != null
                    && submitRequest
                        .getFlinkVersion()
                        .checkVersion(FlinkRestoreMode.SINCE_FLINK_VERSION);
            if (enableRestoreMode) {
                flinkConfig.setString(
                    FlinkRestoreMode.RESTORE_MODE, submitRequest.getRestoreMode().getName());
            }
        }

        if (MapUtils.isNotEmpty(submitRequest.getProperties())) {
            if (submitRequest.hasProp(CoreOptions.FLINK_JVM_OPTIONS.key())) {
                String jvmOpt =
                    submitRequest.getProp(CoreOptions.FLINK_JVM_OPTIONS.key()).toString();
                if (!jvmOpt.contains("-Dfile.encoding=")) {
                    submitRequest
                        .getProperties()
                        .put(CoreOptions.FLINK_JVM_OPTIONS.key(), "-Dfile.encoding=UTF-8 " + jvmOpt);
                }
            }
            for (Map.Entry<String, Object> entry : submitRequest.getProperties().entrySet()) {
                if (entry.getKey().startsWith("env.")) {
                    LOG.info("env opts:  {}: {}", entry.getKey(), entry.getValue());
                    flinkConfig.setString(entry.getKey(), entry.getValue().toString());
                }
            }
        }

        return flinkConfig;
    }

    private List<CustomCommandLine> getCustomCommandLines(String flinkHome) throws Exception {
        Configuration flinkDefaultConfiguration = getFlinkDefaultConfiguration(flinkHome);
        String confDir = flinkHome + "/conf";
        return CliFrontend.loadCustomCommandLines(flinkDefaultConfiguration, confDir);
    }

    private CommandLineAndConfig getCommandLineAndFlinkConfig(SubmitRequest submitRequest) throws Exception {
        Options commandLineOptions =
            getCommandLineOptions(submitRequest.getFlinkVersion().flinkHome);
        Map<String, Object> optionMap = new HashMap<>();

        for (Map.Entry<String, String> opt : submitRequest.getAppOption().entrySet()) {
            if (!commandLineOptions.hasOption(opt.getKey())) {
                LOG.warn("param:{} is error,skip it.", opt.getKey());
                continue;
            }
            String option = commandLineOptions.getOption(opt.getKey().trim()).getOpt();
            String optValue = opt.getValue();
            if ("true".equalsIgnoreCase(optValue) || "false".equalsIgnoreCase(optValue)) {
                if (Boolean.parseBoolean(optValue)) {
                    optionMap.put("-" + option, true);
                }
            } else {
                optionMap.put("-" + option, optValue);
            }
        }

        if (submitRequest.getSavePoint() != null) {
            optionMap.put(
                "-" + FlinkRunOption.SAVEPOINT_PATH_OPTION.getOpt(), submitRequest.getSavePoint());
        }

        for (String key : new String[]{"-e", "--executor", "-t", "--target"}) {
            optionMap.remove(key);
        }
        if (submitRequest.getDeployMode() != null) {
            optionMap.put("-t", submitRequest.getDeployMode().getName());
        }

        List<String> array = new ArrayList<>();
        for (Map.Entry<String, Object> opt : optionMap.entrySet()) {
            array.add(opt.getKey());
            if (opt.getValue() instanceof String) {
                array.add((String) opt.getValue());
            }
        }

        if (MapUtils.isNotEmpty(submitRequest.getProperties())) {
            for (Map.Entry<String, Object> entry : submitRequest.getProperties().entrySet()) {
                if (!entry.getKey().startsWith("env.")) {
                    LOG.info("application dynamicProperties:  {} :{}", entry.getKey(), entry.getValue());
                    array.add("-D" + entry.getKey() + "=" + entry.getValue());
                }
            }
        }

        LOG.info("cliArgs: {}", String.join(" ", array));

        CommandLine commandLine =
            FlinkRunOption.parse(commandLineOptions, array.toArray(new String[0]), true);
        CustomCommandLine activeCommandLine =
            validateAndGetActiveCommandLine(
                getCustomCommandLines(submitRequest.getFlinkVersion().flinkHome), commandLine);
        Configuration configuration =
            applyConfiguration(
                submitRequest.getFlinkVersion().flinkHome, activeCommandLine, commandLine);
        return new CommandLineAndConfig(commandLine, configuration);
    }

    private List<String> extractProgramArgs(SubmitRequest submitRequest) throws Exception {
        List<String> programArgs = new ArrayList<>();
        programArgs.addAll(FlinkConfigurationUtils.extractArguments(submitRequest.getArgs()));

        if (submitRequest.getApplicationType() == ApplicationType.STREAMPARK_FLINK) {
            programArgs.add(paramKeyFlinkConf);
            programArgs.add(submitRequest.getFlinkYaml());
            programArgs.add(paramKeyAppName);
            programArgs.add(DeflaterUtils.zipString(submitRequest.getEffectiveAppName()));
            programArgs.add(paramKeyFlinkParallelism);
            programArgs.add(getParallelism(submitRequest).toString());

            if (submitRequest.getJobType() == FlinkJobType.FLINK_SQL) {
                programArgs.add(paramKeyFlinkSql);
                programArgs.add(submitRequest.getFlinkSQL());
                if (submitRequest.getAppConf() != null) {
                    programArgs.add(paramKeyAppConf);
                    programArgs.add(submitRequest.getAppConf());
                }
            } else if (submitRequest.getAppConf() == null
                || !submitRequest.getAppConf().startsWith("json:")) {
                programArgs.add(paramKeyAppConf);
                programArgs.add(submitRequest.getAppConf());
            }
        }

        if (submitRequest.getProperties().containsKey(ExecutionOptions.RUNTIME_MODE.key())) {
            programArgs.add("--" + ExecutionOptions.RUNTIME_MODE.key());
            programArgs.add(
                submitRequest.getProperties().get(ExecutionOptions.RUNTIME_MODE.key()).toString());
        }

        if (submitRequest.getJobType() == FlinkJobType.PYFLINK
            && submitRequest.getDeployMode() != FlinkDeployMode.YARN_APPLICATION) {
            programArgs.add("-py");
            programArgs.add(submitRequest.getUserJarFile().getAbsolutePath());
        }

        return programArgs;
    }

    private Configuration applyConfiguration(
                                             String flinkHome, CustomCommandLine activeCustomCommandLine,
                                             CommandLine commandLine) throws Exception {
        Preconditions.checkNotNull(activeCustomCommandLine, "activeCustomCommandLine must not be null.");
        Configuration configuration = new Configuration();
        Configuration flinkDefaultConfiguration = getFlinkDefaultConfiguration(flinkHome);
        for (String key : flinkDefaultConfiguration.keySet()) {
            String value = flinkDefaultConfiguration.getString(key, null);
            if (value != null) {
                configuration.setString(key, value);
            }
        }
        configuration.addAll(activeCustomCommandLine.toConfiguration(commandLine));
        return configuration;
    }

    private String tryGetSavepointPathIfNeed(SavepointRequestTrait request) {
        if (!request.isWithSavepoint()) {
            return null;
        }
        if (StringUtils.isNotBlank(request.getSavepointPath())) {
            return request.getSavepointPath();
        }
        String configDir =
            getOptionFromDefaultFlinkConfig(
                request.getFlinkVersion().flinkHome,
                ConfigOptions.key(CheckpointingOptions.SAVEPOINT_DIRECTORY.key())
                    .stringType()
                    .defaultValue(
                        request.getDeployMode() == FlinkDeployMode.YARN_APPLICATION
                            ? Workspace.remote().APP_SAVEPOINTS()
                            : null));
        AssertUtils.required(
            StringUtils.isNotBlank(configDir),
            "[StreamPark] deployMode: "
                + request.getDeployMode().getName()
                + ", savePoint path is null or invalid.");
        return configDir;
    }

    private String formatProperties(Map<String, Object> properties) {
        if (properties == null) {
            return "";
        }
        return properties.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(" "));
    }

    protected static final class JobGraphPackagedProgram {

        public final PackagedProgram packagedProgram;
        public final JobGraph jobGraph;

        JobGraphPackagedProgram(PackagedProgram packagedProgram, JobGraph jobGraph) {
            this.packagedProgram = packagedProgram;
            this.jobGraph = jobGraph;
        }
    }

    private static final class CommandLineAndConfig {

        private final CommandLine commandLine;
        private final Configuration flinkConfig;

        CommandLineAndConfig(CommandLine commandLine, Configuration flinkConfig) {
            this.commandLine = commandLine;
            this.flinkConfig = flinkConfig;
        }
    }

    @FunctionalInterface
    protected interface SubmitFunction {

        SubmitResponse apply(SubmitRequest submitRequest, Configuration flinkConfig, File jarFile) throws Exception;
    }

    @FunctionalInterface
    protected interface ClusterClientAction<O, C> {

        O apply(JobID jobId, ClusterClient<C> client) throws Exception;
    }
}
