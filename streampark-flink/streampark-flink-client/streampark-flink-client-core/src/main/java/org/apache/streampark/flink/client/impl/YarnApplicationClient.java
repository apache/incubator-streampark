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
import org.apache.streampark.flink.client.bean.HdfsWorkspace;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.client.trait.YarnClientTrait;
import org.apache.streampark.flink.packer.pipeline.ShadedBuildResponse;

import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.client.deployment.application.ApplicationConfiguration;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.python.PythonOptions;
import org.apache.flink.util.FlinkException;
import org.apache.flink.yarn.YarnClusterDescriptor;
import org.apache.flink.yarn.configuration.YarnConfigOptions;
import org.apache.hadoop.yarn.api.records.ApplicationId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import scala.Tuple2;

/** Yarn application mode submit. */
public final class YarnApplicationClient extends YarnClientTrait {

    public static final YarnApplicationClient INSTANCE = new YarnApplicationClient();

    private static final Workspace WORKSPACE = Workspace.remote;

    private YarnApplicationClient() {
    }

    @Override
    public void setConfig(SubmitRequest submitRequest, Configuration flinkConfig) {
        super.setConfig(submitRequest, flinkConfig);

        List<String> providedLibs = new ArrayList<>();
        HdfsWorkspace hdfsWorkspace = submitRequest.hdfsWorkspace();
        providedLibs.add(hdfsWorkspace.flinkLib());
        providedLibs.add(hdfsWorkspace.flinkPlugins());
        providedLibs.add(hdfsWorkspace.appJars());

        if (submitRequest.jobType() == FlinkJobType.FLINK_SQL) {
            providedLibs.add(
                WORKSPACE.APP_SHIMS() + "/flink-" + submitRequest.flinkVersion().majorVersion());
            String jobLib = WORKSPACE.APP_WORKSPACE() + "/" + submitRequest.id() + "/lib";
            try {
                if (HdfsUtils.exists(jobLib)) {
                    providedLibs.add(jobLib);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        FlinkConfigurationOps.safeSet(flinkConfig, YarnConfigOptions.PROVIDED_LIB_DIRS, providedLibs);
        FlinkConfigurationOps.safeSet(
            flinkConfig, YarnConfigOptions.FLINK_DIST_JAR, hdfsWorkspace.flinkDistJar());
        FlinkConfigurationOps.safeSet(
            flinkConfig,
            PipelineOptions.JARS,
            Collections.singletonList(
                ((ShadedBuildResponse) submitRequest.buildResult()).shadedJarPath()));
        FlinkConfigurationOps.safeSet(
            flinkConfig, YarnConfigOptions.APPLICATION_NAME, submitRequest.effectiveAppName());
        FlinkConfigurationOps.safeSet(
            flinkConfig,
            YarnConfigOptions.APPLICATION_TYPE,
            submitRequest.applicationType().getName());

        if (submitRequest.jobType() == FlinkJobType.PYFLINK) {
            String pyVenv = WORKSPACE.APP_PYTHON_VENV();
            AssertUtils.required(FsOperator.hdfs().exists(pyVenv), pyVenv + " File does not exist");

            String localLib =
                Workspace.local().APP_WORKSPACE() + "/" + submitRequest.id() + "/lib";
            if (FileUtils.exists(localLib) && FileUtils.directoryNotBlank(localLib)) {
                FlinkConfigurationOps.safeSet(flinkConfig, PipelineOptions.JARS, Arrays.asList(localLib));
            }

            ArrayList<String> shipFiles = new ArrayList<>();
            shipFiles.add(submitRequest.userJarFile().getParentFile().getAbsolutePath());

            FlinkConfigurationOps.safeSet(flinkConfig, YarnConfigOptions.SHIP_FILES, shipFiles);
            FlinkConfigurationOps.safeSet(
                flinkConfig,
                PythonOptions.PYTHON_FILES,
                submitRequest.userJarFile().getParentFile().getName());
            FlinkConfigurationOps.safeSet(flinkConfig, PythonOptions.PYTHON_ARCHIVES, pyVenv);
            FlinkConfigurationOps.safeSet(
                flinkConfig, PythonOptions.PYTHON_CLIENT_EXECUTABLE, Constants.PYTHON_EXECUTABLE);
            FlinkConfigurationOps.safeSet(
                flinkConfig, PythonOptions.PYTHON_EXECUTABLE, Constants.PYTHON_EXECUTABLE);

            List<String> args = flinkConfig.get(ApplicationConfiguration.APPLICATION_ARGS);
            ArrayList<String> argsList = new ArrayList<>(args);
            argsList.add("-pym");
            argsList.add(
                submitRequest
                    .userJarFile()
                    .getName()
                    .substring(
                        0,
                        submitRequest.userJarFile().getName().length()
                            - Constants.PYTHON_SUFFIX.length()));
            FlinkConfigurationOps.safeSet(
                flinkConfig, ApplicationConfiguration.APPLICATION_ARGS, argsList);
        }

        logEffectiveSubmitConfiguration(flinkConfig);
    }

    @Override
    public SubmitResponse doSubmit(SubmitRequest submitRequest, Configuration flinkConfig) throws FlinkException {
        return callAsFlinkException(
            () -> {
                Tuple2<ClusterSpecification, YarnClusterDescriptor> deployDescriptor =
                    getYarnClusterDeployDescriptor(flinkConfig, submitRequest.hadoopUser());
                ClusterSpecification clusterSpecification = deployDescriptor._1();
                YarnClusterDescriptor clusterDescriptor = deployDescriptor._2();
                logClusterSpecification(clusterSpecification);

                ApplicationConfiguration applicationConfiguration =
                    ApplicationConfiguration.fromConfiguration(flinkConfig);
                ClusterClient<ApplicationId> clusterClient =
                    clusterDescriptor
                        .deployApplicationCluster(clusterSpecification, applicationConfiguration)
                        .getClusterClient();
                ApplicationId applicationId = clusterClient.getClusterId();
                String jobManagerUrl = clusterClient.getWebInterfaceURL();
                logYarnJobStarted(applicationId);

                SubmitResponse resp =
                    new SubmitResponse(applicationId.toString(), flinkConfig.toMap(), "", jobManagerUrl);
                closeSubmit(submitRequest, clusterClient, clusterDescriptor);
                return resp;
            });
    }
}
