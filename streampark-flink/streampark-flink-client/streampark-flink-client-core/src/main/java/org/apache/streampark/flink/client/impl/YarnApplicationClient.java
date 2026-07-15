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

package org.apache.streampark.flink.client.impl;

import org.apache.streampark.common.conf.Workspace;
import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.common.fs.FsOperator;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.FileUtils;
import org.apache.streampark.common.util.HdfsUtils;
import org.apache.streampark.common.util.StreamParkLoggerFactory;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.client.trait.YarnClientTrait;
import org.apache.streampark.flink.client.util.FlinkConfigurationEnhancer;
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.flink.client.deployment.application.ApplicationConfiguration;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.python.PythonOptions;
import org.apache.flink.yarn.configuration.YarnConfigOptions;
import org.apache.hadoop.yarn.api.records.ApplicationId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Yarn application mode submit */
public final class YarnApplicationClient extends YarnClientTrait {

    public static final YarnApplicationClient INSTANCE = new YarnApplicationClient();

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(YarnApplicationClient.class.getName());

    private final Workspace workspace = Workspace.remote();

    private YarnApplicationClient() {
    }

    @Override
    public void setConfig(SubmitRequest submitRequest, Configuration flinkConfig) {
        super.setConfig(submitRequest, flinkConfig);
        List<String> providedLibs = new ArrayList<>();
        providedLibs.add(submitRequest.getHdfsWorkspace().getFlinkLib());
        providedLibs.add(submitRequest.getHdfsWorkspace().getFlinkPlugins());
        providedLibs.add(submitRequest.getHdfsWorkspace().getAppJars());
        if (submitRequest.getJobType() == FlinkJobType.FLINK_SQL) {
            providedLibs.add(
                workspace.getAppShims() + "/flink-" + submitRequest.getFlinkVersion().majorVersion());
            String jobLib = workspace.getAppWorkspace() + "/" + submitRequest.getId() + "/lib";
            try {
                if (HdfsUtils.exists(jobLib)) {
                    providedLibs.add(jobLib);
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }

        FlinkConfigurationEnhancer.safeSet(flinkConfig, YarnConfigOptions.PROVIDED_LIB_DIRS, providedLibs);
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, YarnConfigOptions.FLINK_DIST_JAR, submitRequest.getHdfsWorkspace().getFlinkDistJar());
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig,
            PipelineOptions.JARS,
            Collections.singletonList(
                ((ShadedBuildResponse) submitRequest.getBuildResult()).shadedJarPath()));
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, YarnConfigOptions.APPLICATION_NAME, submitRequest.getEffectiveAppName());
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig,
            YarnConfigOptions.APPLICATION_TYPE,
            submitRequest.getApplicationType().getName());

        if (submitRequest.getJobType() == FlinkJobType.PYFLINK) {
            String pyVenv = workspace.getAppPythonVenv();
            AssertUtils.required(FsOperator.hdfs().exists(pyVenv), pyVenv + " File does not exist");

            String localLib = Workspace.local().getAppWorkspace() + "/" + submitRequest.getId() + "/lib";
            if (FileUtils.exists(localLib) && FileUtils.directoryNotBlank(localLib)) {
                FlinkConfigurationEnhancer.safeSet(
                    flinkConfig, PipelineOptions.JARS, java.util.Arrays.asList(localLib));
            }

            List<String> shipFiles = new ArrayList<>();
            shipFiles.add(submitRequest.getUserJarFile().getParentFile().getAbsolutePath());

            FlinkConfigurationEnhancer.safeSet(flinkConfig, YarnConfigOptions.SHIP_FILES, shipFiles);
            FlinkConfigurationEnhancer.safeSet(
                flinkConfig,
                PythonOptions.PYTHON_FILES,
                submitRequest.getUserJarFile().getParentFile().getName());
            FlinkConfigurationEnhancer.safeSet(flinkConfig, PythonOptions.PYTHON_ARCHIVES, pyVenv);
            FlinkConfigurationEnhancer.safeSet(
                flinkConfig, PythonOptions.PYTHON_CLIENT_EXECUTABLE, Constants.PYTHON_EXECUTABLE);
            FlinkConfigurationEnhancer.safeSet(
                flinkConfig, PythonOptions.PYTHON_EXECUTABLE, Constants.PYTHON_EXECUTABLE);

            List<String> args =
                new ArrayList<>(
                    flinkConfig.get(ApplicationConfiguration.APPLICATION_ARGS));
            args.add("-pym");
            args.add(
                submitRequest
                    .getUserJarFile()
                    .getName()
                    .substring(
                        0,
                        submitRequest.getUserJarFile().getName().length()
                            - Constants.PYTHON_SUFFIX.length()));
            FlinkConfigurationEnhancer.safeSet(flinkConfig, ApplicationConfiguration.APPLICATION_ARGS, args);
        }

        LOG.info(
            "\n------------------------------------------------------------------\n"
                + "Effective submit configuration: {}\n"
                + "------------------------------------------------------------------\n",
            flinkConfig);
    }

    @Override
    public SubmitResponse doSubmit(SubmitRequest submitRequest, Configuration flinkConfig) throws Exception {
        YarnClusterDeployDescriptorResult descriptorResult =
            getYarnClusterDeployDescriptor(flinkConfig, submitRequest.getHadoopUser());
        LOG.info(
            "\n------------------------<<specification>>-------------------------\n"
                + "{}\n"
                + "------------------------------------------------------------------\n",
            descriptorResult.clusterSpecification);

        ApplicationConfiguration applicationConfiguration =
            ApplicationConfiguration.fromConfiguration(flinkConfig);
        ClusterClient<ApplicationId> clusterClient =
            descriptorResult.clusterDescriptor
                .deployApplicationCluster(
                    descriptorResult.clusterSpecification, applicationConfiguration)
                .getClusterClient();
        ApplicationId applicationId = clusterClient.getClusterId();
        String jobManagerUrl = clusterClient.getWebInterfaceURL();
        LOG.info(
            "\n-------------------------<<applicationId>>------------------------\n"
                + "Flink Job Started: applicationId: {}\n"
                + "__________________________________________________________________\n",
            applicationId);

        SubmitResponse resp =
            SubmitResponse.builder()
                .clusterId(applicationId.toString())
                .flinkConfig(flinkConfig.toMap())
                .jobManagerUrl(jobManagerUrl)
                .build();
        closeSubmit(submitRequest, clusterClient, descriptorResult.clusterDescriptor);
        return resp;
    }
}
