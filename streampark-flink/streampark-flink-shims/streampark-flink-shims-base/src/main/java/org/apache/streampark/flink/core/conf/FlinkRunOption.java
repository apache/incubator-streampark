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

package org.apache.streampark.flink.core.conf;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/** Applies to all optional parameters under flink run. */
public final class FlinkRunOption {

    private static final String SAVEPOINT_PATH_ARG = "savepointPath";

    public static final Option HELP_OPTION = new Option("h", "help", false, null);
    public static final Option JAR_OPTION = new Option("j", "jarfile", true, null);
    public static final Option CLASS_OPTION = new Option("c", "class", true, null);
    public static final Option CLASSPATH_OPTION = new Option("C", "classpath", true, null);
    public static final Option PARALLELISM_OPTION = new Option("p", "parallelism", true, null);
    public static final Option DETACHED_OPTION = new Option("d", "detached", false, null);
    public static final Option SHUTDOWN_IF_ATTACHED_OPTION =
        new Option("sae", "shutdownOnAttachedExit", false, null);
    public static final Option YARN_DETACHED_OPTION = new Option("yd", "yarndetached", false, null);
    public static final Option ARGS_OPTION = new Option("a", "arguments", true, null);
    public static final Option ADDRESS_OPTION = new Option("m", "jobmanager", true, null);
    public static final Option SAVEPOINT_PATH_OPTION = new Option("s", "fromSavepoint", true, null);
    public static final Option SAVEPOINT_ALLOW_NON_RESTORED_OPTION =
        new Option("n", "allowNonRestoredState", false, null);
    public static final Option SAVEPOINT_DISPOSE_OPTION = new Option("d", "dispose", true, null);
    public static final Option RUNNING_OPTION = new Option("r", "running", false, null);
    public static final Option SCHEDULED_OPTION = new Option("s", "scheduled", false, null);
    public static final Option ALL_OPTION = new Option("a", "all", false, null);
    public static final Option ZOOKEEPER_NAMESPACE_OPTION =
        new Option("z", "zookeeperNamespace", true, null);
    public static final Option CANCEL_WITH_SAVEPOINT_OPTION =
        new Option("s", "withSavepoint", true, null);
    public static final Option STOP_WITH_SAVEPOINT_PATH =
        new Option("p", SAVEPOINT_PATH_ARG, true, null);
    public static final Option STOP_AND_DRAIN = new Option("d", "drain", false, null);
    public static final Option PY_OPTION = new Option("py", "python", true, null);
    public static final Option PYFILES_OPTION = new Option("pyfs", "pyFiles", true, null);
    public static final Option PYMODULE_OPTION = new Option("pym", "pyModule", true, null);
    public static final Option PYREQUIREMENTS_OPTION = new Option("pyreq", "pyRequirements", true, null);
    public static final Option PYARCHIVE_OPTION = new Option("pyarch", "pyArchives", true, null);
    public static final Option PYEXEC_OPTION = new Option("pyexec", "pyExecutable", true, null);
    public static final Option EXECUTOR_OPTION = new Option("e", "executor", true, null);
    public static final Option TARGET_OPTION = new Option("t", "target", true, null);

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

    public static Options getYARNOptions() {
        Options allOptions = new Options();
        allOptions.addOption(DETACHED_OPTION);
        allOptions.addOption(YARN_DETACHED_OPTION);
        return allOptions;
    }

    public static Options buildGeneralOptions(Options options) {
        options.addOption(HELP_OPTION);
        options.addOption(new Option("v", "verbose", false, "This option is deprecated."));
        return options;
    }

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

    public static CommandLine parse(Options options, String[] args, boolean stopAtNonOptions) {
        DefaultParser parser = new DefaultParser();
        try {
            return parser.parse(options, args, stopAtNonOptions);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
