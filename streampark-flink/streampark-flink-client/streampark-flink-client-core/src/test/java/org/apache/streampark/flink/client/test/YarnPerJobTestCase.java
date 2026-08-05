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

package org.apache.streampark.flink.client.test;

import org.apache.streampark.common.util.StreamParkLoggerFactory;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.core.conf.FlinkRunOption;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.commons.cli.Options;
import org.apache.flink.client.cli.CliFrontend;
import org.apache.flink.client.cli.CliFrontendParser;
import org.apache.flink.client.cli.CustomCommandLine;
import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader;
import org.apache.flink.client.program.ClusterClientProvider;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.client.program.PackagedProgramUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.GlobalConfiguration;
import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.MemorySize.MemoryUnit;
import org.apache.flink.configuration.TaskManagerOptions;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.util.Preconditions;
import org.apache.flink.yarn.YarnClusterDescriptor;
import org.apache.flink.yarn.configuration.YarnDeploymentTarget;
import org.apache.flink.yarn.entrypoint.YarnJobClusterEntrypoint;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.ApplicationId;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/** perJob to submit jobs programmatically */
public final class YarnPerJobTestCase {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(YarnPerJobTestCase.class.getName());

    private static final String PROGRAM_ARGS = "--hostname localhost --port 9999";

    private static final String OPTION = "-e yarn-per-job -p 2 -n";

    private static Configuration flinkDefaultConfiguration;
    private static List<CustomCommandLine> customCommandLines;
    private static Method deployInternalMethod;

    private YarnPerJobTestCase() {
    }

    private static void ensureInitialized() {
        if (deployInternalMethod != null) {
            return;
        }
        String flinkHome = Objects.requireNonNull(System.getenv("FLINK_HOME"), "FLINK_HOME must be set");
        LOG.info("flinkHome: {}", flinkHome);
        flinkDefaultConfiguration = GlobalConfiguration.loadConfiguration(flinkHome + "/conf");
        try {
            customCommandLines =
                CliFrontend.loadCustomCommandLines(flinkDefaultConfiguration, flinkHome + "/conf");
            Class<?>[] paramClass =
                new Class<?>[]{
                        ClusterSpecification.class,
                        String.class,
                        String.class,
                        JobGraph.class,
                        boolean.class
                };
            deployInternalMethod =
                YarnClusterDescriptor.class.getDeclaredMethod("deployInternal", paramClass);
            deployInternalMethod.setAccessible(true);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize YARN integration harness", e);
        }
    }

    @Test
    void verifyYarnPerJobHarnessCompatibility() throws Exception {
        Method deployInternal =
            YarnClusterDescriptor.class.getDeclaredMethod(
                "deployInternal",
                ClusterSpecification.class,
                String.class,
                String.class,
                JobGraph.class,
                boolean.class);
        assertThat(deployInternal).isNotNull();

        SubmitResponse response = new SubmitResponse("application_123", Collections.emptyMap());
        assertThat(response.clusterId()).isEqualTo("application_123");
        assertThat(YarnDeploymentTarget.PER_JOB.getName()).isEqualTo("yarn-per-job");

        Options commandLineOptions =
            FlinkRunOption.mergeOptions(FlinkRunOption.getRunCommandOptions(), new Options());
        org.apache.commons.cli.CommandLine commandLine =
            FlinkRunOption.parse(commandLineOptions, OPTION.split("\\s+"), true);
        assertThat(commandLine.getOptionValue("e")).isEqualTo("yarn-per-job");

        String flinkHome = System.getenv("FLINK_HOME");
        if (flinkHome != null) {
            ensureInitialized();
            assertThat(customCommandLines).isNotEmpty();
            assertThat(new File(flinkHome, "lib").exists()).isTrue();
        }
    }

    @SuppressWarnings("unchecked")
    private static ClusterClientProvider<ApplicationId> deployInternal(
                                                                       YarnClusterDescriptor clusterDescriptor,
                                                                       ClusterSpecification clusterSpecification,
                                                                       String applicationName,
                                                                       String yarnClusterEntrypoint,
                                                                       JobGraph jobGraph,
                                                                       Boolean detached) throws Exception {
        return (ClusterClientProvider<ApplicationId>) deployInternalMethod.invoke(
            clusterDescriptor,
            clusterSpecification,
            applicationName,
            yarnClusterEntrypoint,
            jobGraph,
            detached);
    }

    public static void main(String[] args) throws Exception {
        ensureInitialized();
        String flinkHome = System.getenv("FLINK_HOME");
        String userJar = flinkHome + "/examples/streaming/SocketWindowWordCount.jar";
        Options customCommandLineOptions = new Options();
        for (CustomCommandLine customCommandLine : customCommandLines) {
            customCommandLine.addGeneralOptions(customCommandLineOptions);
            customCommandLine.addRunOptions(customCommandLineOptions);
        }
        Options commandLineOptions =
            FlinkRunOption.mergeOptions(
                CliFrontendParser.getRunCommandOptions(), customCommandLineOptions);
        org.apache.commons.cli.CommandLine commandLine =
            FlinkRunOption.parse(commandLineOptions, OPTION.split("\\s+"), true);

        CustomCommandLine activeCommandLine = null;
        LOG.info("Custom commandlines: {}", customCommandLines);
        for (CustomCommandLine cli : customCommandLines) {
            if (cli.isActive(Preconditions.checkNotNull(commandLine))) {
                activeCommandLine = cli;
                break;
            }
        }
        if (activeCommandLine == null) {
            throw new IllegalStateException("No valid command-line found.");
        }

        Configuration executorConfig = activeCommandLine.toConfiguration(commandLine);
        Configuration flinkConfig = new Configuration(executorConfig);
        flinkConfig.set(DeploymentOptions.TARGET, YarnDeploymentTarget.PER_JOB.getName());
        flinkConfig.set(
            org.apache.flink.client.deployment.application.ApplicationConfiguration.APPLICATION_ARGS,
            Arrays.asList(PROGRAM_ARGS.split("\\s+")));
        flinkConfig.set(
            JobManagerOptions.TOTAL_FLINK_MEMORY, MemorySize.parse("1024", MemoryUnit.MEGA_BYTES));
        flinkConfig.set(
            TaskManagerOptions.TOTAL_FLINK_MEMORY, MemorySize.parse("1024", MemoryUnit.MEGA_BYTES));

        DefaultClusterClientServiceLoader clusterClientServiceLoader =
            new DefaultClusterClientServiceLoader();
        org.apache.flink.client.deployment.ClusterClientFactory<ApplicationId> clientFactory =
            clusterClientServiceLoader.getClusterClientFactory(flinkConfig);

        YarnClusterDescriptor clusterDescriptor =
            (YarnClusterDescriptor) clientFactory.createClusterDescriptor(flinkConfig);
        String[] distJars =
            new File(flinkHome + "/lib")
                .list((dir, name) -> name.matches("flink-dist.*\\.jar"));
        if (distJars == null || distJars.length == 0) {
            throw new IllegalArgumentException(
                "[StreamPark] can no found flink-dist jar in " + flinkHome + "/lib");
        }
        if (distJars.length > 1) {
            throw new IllegalArgumentException(
                "[StreamPark] found multiple flink-dist jar in "
                    + flinkHome
                    + "/lib,["
                    + String.join(",", distJars)
                    + "]");
        }
        clusterDescriptor.setLocalJarPath(new Path(flinkHome + "/lib/" + distJars[0]));

        try {
            ClusterSpecification clusterSpecification =
                clientFactory.getClusterSpecification(flinkConfig);
            LOG.info("------------------<<specification>>------------------");
            LOG.info("{}", clusterSpecification);
            LOG.info("------------------------------------");

            PackagedProgram packagedProgram =
                PackagedProgram.newBuilder()
                    .setJarFile(new File(userJar))
                    .setArguments(PROGRAM_ARGS.split("\\s+"))
                    .build();
            JobGraph jobGraph =
                PackagedProgramUtils.createJobGraph(packagedProgram, flinkConfig, 1, false);
            LOG.info("------------------<<jobId>>------------------");
            LOG.info("{}", jobGraph.getJobID());
            LOG.info("------------------------------------");

            org.apache.flink.client.program.ClusterClient<ApplicationId> clusterClient =
                deployInternal(
                    clusterDescriptor,
                    clusterSpecification,
                    "MyJob",
                    YarnJobClusterEntrypoint.class.getName(),
                    jobGraph,
                    false)
                        .getClusterClient();
            ApplicationId applicationId = clusterClient.getClusterId();
            LOG.info("------------------<<applicationId>>-------------------");
            LOG.info("Flink Job Started: applicationId: {} ", applicationId);
            LOG.info("-------------------------------------");
            new SubmitResponse(applicationId.toString(), flinkConfig.toMap());
        } finally {
            clusterDescriptor.close();
        }
    }
}
