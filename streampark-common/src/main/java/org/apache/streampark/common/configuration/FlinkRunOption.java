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

package org.apache.streampark.common.configuration;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Defines the Commons CLI option metadata used by StreamPark's Flink submission flow.
 *
 * <p>The class provides reusable option sets and parsing helpers for Flink launcher arguments. It
 * describes command-line syntax only; runtime configuration keys and values remain the
 * responsibility of {@link FlinkOptions} and the calling submission component.
 */
public final class FlinkRunOption {

    private static final String SAVEPOINT_PATH_ARG = "savepointPath";

    /** Prints launcher help. */
    public static final Option HELP_OPTION = new Option("h", "help", false, null);

    /** Selects the application JAR. */
    public static final Option JAR_OPTION = new Option("j", "jarfile", true, null);

    /** Selects the application entry class. */
    public static final Option CLASS_OPTION = new Option("c", "class", true, null);

    /** Adds a dependency URL to the application classpath. */
    public static final Option CLASSPATH_OPTION = new Option("C", "classpath", true, null);

    /** Sets the application parallelism. */
    public static final Option PARALLELISM_OPTION = new Option("p", "parallelism", true, null);

    /** Submits the application in detached mode. */
    public static final Option DETACHED_OPTION = new Option("d", "detached", false, null);

    /** Shuts down the cluster when an attached client exits. */
    public static final Option SHUTDOWN_IF_ATTACHED_OPTION =
        new Option("sae", "shutdownOnAttachedExit", false, null);

    /** Uses the historical YARN detached-mode flag. */
    public static final Option YARN_DETACHED_OPTION = new Option("yd", "yarndetached", false, null);

    /** Supplies arguments to the application main method. */
    public static final Option ARGS_OPTION = new Option("a", "arguments", true, null);

    /** Selects a JobManager endpoint. */
    public static final Option ADDRESS_OPTION = new Option("m", "jobmanager", true, null);

    /** Selects the savepoint used to restore an application. */
    public static final Option SAVEPOINT_PATH_OPTION = new Option("s", "fromSavepoint", true, null);

    /** Permits restore when savepoint state cannot be mapped to every operator. */
    public static final Option SAVEPOINT_ALLOW_NON_RESTORED_OPTION =
        new Option("n", "allowNonRestoredState", false, null);

    /** Selects a savepoint to dispose. */
    public static final Option SAVEPOINT_DISPOSE_OPTION = new Option("d", "dispose", true, null);

    /** Restricts a query to running jobs. */
    public static final Option RUNNING_OPTION = new Option("r", "running", false, null);

    /** Restricts a query to scheduled jobs. */
    public static final Option SCHEDULED_OPTION = new Option("s", "scheduled", false, null);

    /** Selects all matching jobs. */
    public static final Option ALL_OPTION = new Option("a", "all", false, null);

    /** Sets the high-availability ZooKeeper namespace. */
    public static final Option ZOOKEEPER_NAMESPACE_OPTION =
        new Option("z", "zookeeperNamespace", true, null);

    /** Creates a savepoint while cancelling an application. */
    public static final Option CANCEL_WITH_SAVEPOINT_OPTION =
        new Option("s", "withSavepoint", true, null);

    /** Selects the savepoint directory used while stopping an application. */
    public static final Option STOP_WITH_SAVEPOINT_PATH =
        new Option("p", SAVEPOINT_PATH_ARG, true, null);

    /** Drains event-time input before stopping an application. */
    public static final Option STOP_AND_DRAIN = new Option("d", "drain", false, null);

    /** Selects a Python application file. */
    public static final Option PY_OPTION = new Option("py", "python", true, null);

    /** Adds Python source files or archives. */
    public static final Option PYFILES_OPTION = new Option("pyfs", "pyFiles", true, null);

    /** Selects a Python module as the application entry point. */
    public static final Option PYMODULE_OPTION = new Option("pym", "pyModule", true, null);

    /** Supplies a Python requirements specification. */
    public static final Option PYREQUIREMENTS_OPTION = new Option("pyreq", "pyRequirements", true, null);

    /** Adds archives to the Python worker environment. */
    public static final Option PYARCHIVE_OPTION = new Option("pyarch", "pyArchives", true, null);

    /** Selects the Python worker executable. */
    public static final Option PYEXEC_OPTION = new Option("pyexec", "pyExecutable", true, null);

    /** Selects a Flink executor implementation. */
    public static final Option EXECUTOR_OPTION = new Option("e", "executor", true, null);

    /** Selects the Flink deployment target. */
    public static final Option TARGET_OPTION = new Option("t", "target", true, null);

    /** Supplies repeatable native Flink dynamic properties. */
    public static final Option DYNAMIC_PROPERTIES =
        Option.builder("D")
            .argName("property=value")
            .numberOfArgs(2)
            .valueSeparator('=')
            .build();

    static {
        HELP_OPTION.setRequired(false);

        JAR_OPTION.setRequired(false);
        JAR_OPTION.setArgName("jarfile");

        CLASS_OPTION.setRequired(false);
        CLASS_OPTION.setArgName("classname");

        CLASSPATH_OPTION.setRequired(false);
        CLASSPATH_OPTION.setArgName("url");

        PARALLELISM_OPTION.setRequired(false);
        PARALLELISM_OPTION.setArgName("parallelism");

        DETACHED_OPTION.setRequired(false);
        SHUTDOWN_IF_ATTACHED_OPTION.setRequired(false);
        YARN_DETACHED_OPTION.setRequired(false);

        ARGS_OPTION.setRequired(false);
        ARGS_OPTION.setArgName("programArgs");
        ARGS_OPTION.setArgs(Option.UNLIMITED_VALUES);

        RUNNING_OPTION.setRequired(false);
        SCHEDULED_OPTION.setRequired(false);

        SAVEPOINT_PATH_OPTION.setRequired(false);
        SAVEPOINT_PATH_OPTION.setArgName(SAVEPOINT_PATH_ARG);

        SAVEPOINT_ALLOW_NON_RESTORED_OPTION.setRequired(false);

        ZOOKEEPER_NAMESPACE_OPTION.setRequired(false);
        ZOOKEEPER_NAMESPACE_OPTION.setArgName("zookeeperNamespace");

        CANCEL_WITH_SAVEPOINT_OPTION.setRequired(false);
        CANCEL_WITH_SAVEPOINT_OPTION.setArgName("targetDirectory");
        CANCEL_WITH_SAVEPOINT_OPTION.setOptionalArg(true);

        STOP_WITH_SAVEPOINT_PATH.setRequired(false);
        STOP_WITH_SAVEPOINT_PATH.setArgName(SAVEPOINT_PATH_ARG);
        STOP_WITH_SAVEPOINT_PATH.setOptionalArg(true);

        STOP_AND_DRAIN.setRequired(false);

        PY_OPTION.setRequired(false);
        PY_OPTION.setArgName("pythonFile");

        PYFILES_OPTION.setRequired(false);
        PYFILES_OPTION.setArgName("pythonFiles");

        PYMODULE_OPTION.setRequired(false);
        PYMODULE_OPTION.setArgName("pythonModule");

        PYREQUIREMENTS_OPTION.setRequired(false);

        PYARCHIVE_OPTION.setRequired(false);

        PYEXEC_OPTION.setRequired(false);
    }

    private FlinkRunOption() {
    }

    /**
     * Returns the union of general run and YARN launcher options.
     *
     * @return new Commons CLI schema containing all supported launcher options
     */
    public static Options allOptions() {
        Options commOptions = getRunCommandOptions();
        Options yarnOptions = getYARNOptions();
        Options resultOptions = new Options();
        for (Option option : commOptions.getOptions()) {
            resultOptions.addOption(option);
        }
        for (Option option : yarnOptions.getOptions()) {
            if (!resultOptions.hasOption(option.getOpt())) {
                resultOptions.addOption(option);
            }
        }
        return resultOptions;
    }

    /**
     * Returns options accepted by the general Flink run command.
     *
     * @return new Commons CLI schema for a Flink run command
     */
    public static Options getRunCommandOptions() {
        Options options = buildGeneralOptions(new Options());
        options = getProgramSpecificOptions(options);
        options.addOption(SAVEPOINT_PATH_OPTION);
        options.addOption(EXECUTOR_OPTION);
        options.addOption(TARGET_OPTION);
        options.addOption(SAVEPOINT_ALLOW_NON_RESTORED_OPTION);
        options.addOption(DYNAMIC_PROPERTIES);
        return options;
    }

    /**
     * Returns YARN-specific launcher options.
     *
     * @return new Commons CLI schema for YARN launcher flags
     */
    public static Options getYARNOptions() {
        Options allOptions = new Options();
        allOptions.addOption(DETACHED_OPTION);
        allOptions.addOption(YARN_DETACHED_OPTION);
        return allOptions;
    }

    /**
     * Adds general launcher options to a supplied schema.
     *
     * @param options target Commons CLI schema
     * @return the same schema after mutation
     */
    public static Options buildGeneralOptions(Options options) {
        options.addOption(HELP_OPTION);
        options.addOption(new Option("v", "verbose", false, "This option is deprecated."));
        return options;
    }

    /**
     * Adds program-specific launcher options to a supplied schema.
     *
     * @param options target Commons CLI schema
     * @return the same schema after mutation
     */
    public static Options getProgramSpecificOptions(Options options) {
        options.addOption(JAR_OPTION);
        options.addOption(CLASS_OPTION);
        options.addOption(ADDRESS_OPTION);
        options.addOption(CLASSPATH_OPTION);
        options.addOption(PARALLELISM_OPTION);
        options.addOption(ARGS_OPTION);
        options.addOption(DETACHED_OPTION);
        options.addOption(SHUTDOWN_IF_ATTACHED_OPTION);
        options.addOption(YARN_DETACHED_OPTION);
        options.addOption(PY_OPTION);
        options.addOption(PYFILES_OPTION);
        options.addOption(PYMODULE_OPTION);
        options.addOption(PYREQUIREMENTS_OPTION);
        options.addOption(PYARCHIVE_OPTION);
        options.addOption(PYEXEC_OPTION);
        return options;
    }

    /**
     * Returns a new schema containing options from both inputs.
     *
     * @param optionsA first source schema
     * @param optionsB second source schema
     * @return new combined schema
     */
    public static Options mergeOptions(Options optionsA, Options optionsB) {
        Options resultOptions = new Options();
        if (optionsA == null || optionsB == null) {
            throw new IllegalArgumentException("options must not be null");
        }
        for (Option option : optionsA.getOptions()) {
            resultOptions.addOption(option);
        }
        for (Option option : optionsB.getOptions()) {
            resultOptions.addOption(option);
        }
        return resultOptions;
    }

    /**
     * Parses launcher arguments and reports syntax errors as configuration failures.
     *
     * @param options Commons CLI schema used for validation
     * @param args tokenized launcher arguments
     * @param stopAtNonOptions whether parsing stops at the first non-option token
     * @return parsed command line
     * @throws ConfigException when Commons CLI rejects the arguments
     */
    public static CommandLine parse(Options options, String[] args, boolean stopAtNonOptions) {
        DefaultParser parser = new DefaultParser();
        try {
            return parser.parse(options, args, stopAtNonOptions);
        } catch (Exception e) {
            throw new ConfigException("Invalid Flink launcher arguments", e);
        }
    }
}
