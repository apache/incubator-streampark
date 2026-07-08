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

import org.apache.streampark.common.util.StreamParkLoggerFactory;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.client.bean.CancelRequest;
import org.apache.streampark.flink.client.bean.CancelResponse;
import org.apache.streampark.flink.client.bean.DeployRequest;
import org.apache.streampark.flink.client.bean.DeployResponse;
import org.apache.streampark.flink.client.bean.SavepointResponse;
import org.apache.streampark.flink.client.bean.ShutDownRequest;
import org.apache.streampark.flink.client.bean.ShutDownResponse;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.client.bean.TriggerSavepointRequest;
import org.apache.streampark.flink.client.trait.YarnClientTrait;
import org.apache.streampark.flink.client.util.FlinkConfigurationEnhancer;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.DeploymentOptionsInternal;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.yarn.YarnClusterDescriptor;
import org.apache.flink.yarn.configuration.YarnConfigOptions;
import org.apache.flink.yarn.configuration.YarnDeploymentTarget;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.exceptions.ApplicationNotFoundException;
import org.apache.hadoop.yarn.util.ConverterUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Submit Job to YARN Session Cluster */
public final class YarnSessionClient extends YarnClientTrait {

    public static final YarnSessionClient INSTANCE = new YarnSessionClient();

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(YarnSessionClient.class.getName());

    private YarnSessionClient() {
    }

    @Override
    public void setConfig(SubmitRequest submitRequest, Configuration flinkConfig) {
        super.setConfig(submitRequest, flinkConfig);
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName());
        LOG.info(
            "\n------------------------------------------------------------------\n"
                + "Effective submit configuration: {}\n"
                + "------------------------------------------------------------------\n",
            flinkConfig);
    }

    public void deployClusterConfig(DeployRequest deployRequest, Configuration flinkConfig) {
        List<String> shipFiles = new ArrayList<>();
        shipFiles.add(deployRequest.getFlinkVersion().flinkHome + "/lib");
        shipFiles.add(deployRequest.getFlinkVersion().flinkHome + "/plugins");

        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, YarnConfigOptions.FLINK_DIST_JAR, deployRequest.getHdfsWorkspace().getFlinkDistJar());
        FlinkConfigurationEnhancer.safeSet(flinkConfig, YarnConfigOptions.SHIP_FILES, shipFiles);
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName());
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig,
            DeploymentOptionsInternal.CONF_DIR,
            deployRequest.getFlinkVersion().flinkHome + "/conf");

        LOG.info(
            "\n------------------------------------------------------------------\n"
                + "Effective submit configuration: {}\n"
                + "------------------------------------------------------------------\n",
            flinkConfig);
    }

    @Override
    public SubmitResponse doSubmit(SubmitRequest submitRequest, Configuration flinkConfig) throws Exception {
        YarnClusterDescriptorResult descriptorResult = getYarnClusterDescriptor(flinkConfig);
        JobGraphPackagedProgram programJobGraph =
            getJobGraph(flinkConfig, submitRequest, submitRequest.getUserJarFile());
        PackagedProgram packageProgram = programJobGraph.packagedProgram;
        JobGraph jobGraph = programJobGraph.jobGraph;
        ClusterClient<ApplicationId> client =
            descriptorResult.clusterDescriptor.retrieve(descriptorResult.applicationId).getClusterClient();
        String jobId = client.submitJob(jobGraph).get().toString();
        String jobManagerUrl = client.getWebInterfaceURL();

        LOG.info(
            "\n-------------------------<<applicationId>>------------------------\n"
                + "Flink Job Started: jobId: {} , applicationId: {}\n"
                + "__________________________________________________________________\n",
            jobId,
            descriptorResult.applicationId);

        SubmitResponse resp =
            SubmitResponse.builder()
                .clusterId(descriptorResult.applicationId.toString())
                .flinkConfig(flinkConfig.toMap())
                .jobId(jobId)
                .jobManagerUrl(jobManagerUrl)
                .build();
        closeSubmit(submitRequest, packageProgram, client, descriptorResult.clusterDescriptor);
        return resp;
    }

    @Override
    public CancelResponse doCancel(CancelRequest cancelRequest, Configuration flinkConfig) throws Exception {
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName());
        return super.doCancel(cancelRequest, flinkConfig);
    }

    @Override
    public SavepointResponse doTriggerSavepoint(
                                                TriggerSavepointRequest request,
                                                Configuration flinkConfig) throws Exception {
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName());
        return super.doTriggerSavepoint(request, flinkConfig);
    }

    public DeployResponse deploy(DeployRequest deployRequest) throws Exception {
        LOG.info(
            "\n--------------------------------------- flink yarn sesion start ---------------------------------------\n"
                + "    userFlinkHome    : {}\n"
                + "    flinkVersion     : {}\n"
                + "    deployMode       : {}\n"
                + "    clusterId        : {}\n"
                + "    properties       : {}\n"
                + "-------------------------------------------------------------------------------------------------------\n",
            deployRequest.getFlinkVersion().flinkHome,
            deployRequest.getFlinkVersion().version(),
            deployRequest.getDeployMode().name(),
            deployRequest.getClusterId(),
            formatProperties(deployRequest.getProperties()));

        YarnClusterDescriptor clusterDescriptor = null;
        ClusterClient<ApplicationId> client = null;
        try {
            Configuration flinkConfig =
                extractConfiguration(
                    deployRequest.getFlinkVersion().flinkHome, deployRequest.getProperties());
            deployClusterConfig(deployRequest, flinkConfig);
            YarnClusterDeployDescriptorResult yarnClusterDescriptor =
                getYarnClusterDeployDescriptor(flinkConfig);
            clusterDescriptor = yarnClusterDescriptor.clusterDescriptor;
            if (StringUtils.isNotBlank(deployRequest.getClusterId())) {
                try {
                    FinalApplicationStatus applicationStatus =
                        clusterDescriptor
                            .getYarnClient()
                            .getApplicationReport(ApplicationId.fromString(deployRequest.getClusterId()))
                            .getFinalApplicationStatus();
                    if (FinalApplicationStatus.UNDEFINED == applicationStatus) {
                        ClusterClient<ApplicationId> yarnClient =
                            clusterDescriptor
                                .retrieve(ApplicationId.fromString(deployRequest.getClusterId()))
                                .getClusterClient();
                        if (yarnClient.getWebInterfaceURL() != null) {
                            return DeployResponse.builder()
                                .address(yarnClient.getWebInterfaceURL())
                                .clusterId(yarnClient.getClusterId().toString())
                                .build();
                        }
                    }
                } catch (ApplicationNotFoundException e) {
                    LOG.info("this applicationId have not managed by yarn ,need deploy ...");
                }
            }
            client =
                clusterDescriptor
                    .deploySessionCluster(yarnClusterDescriptor.clusterSpecification)
                    .getClusterClient();
            if (client.getWebInterfaceURL() != null) {
                return DeployResponse.builder()
                    .address(client.getWebInterfaceURL())
                    .clusterId(client.getClusterId().toString())
                    .build();
            }
            return DeployResponse.builder()
                .error(new RuntimeException("get the cluster getWebInterfaceURL failed."))
                .build();
        } catch (Exception e) {
            LOG.error("start flink session fail in {} mode", deployRequest.getDeployMode(), e);
            throw e;
        } finally {
            Utils.close(client, clusterDescriptor);
        }
    }

    public ShutDownResponse shutdown(ShutDownRequest shutDownRequest) throws Exception {
        YarnClusterDescriptor clusterDescriptor = null;
        ClusterClient<ApplicationId> client = null;
        try {
            Configuration flinkConfig =
                getFlinkDefaultConfiguration(shutDownRequest.getFlinkVersion().flinkHome);
            if (shutDownRequest.getProperties() != null) {
                for (Map.Entry<String, Object> entry : shutDownRequest.getProperties().entrySet()) {
                    if (entry.getValue() != null) {
                        flinkConfig.setString(entry.getKey(), entry.getValue().toString());
                    }
                }
            }
            FlinkConfigurationEnhancer.safeSet(
                flinkConfig, YarnConfigOptions.APPLICATION_ID, shutDownRequest.getClusterId());
            FlinkConfigurationEnhancer.safeSet(
                flinkConfig, DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName());
            YarnClusterDescriptorResult yarnClusterDescriptor = getYarnClusterDescriptor(flinkConfig);
            clusterDescriptor = yarnClusterDescriptor.clusterDescriptor;
            boolean shutDownState =
                FinalApplicationStatus.UNDEFINED.equals(
                    clusterDescriptor
                        .getYarnClient()
                        .getApplicationReport(
                            ApplicationId.fromString(shutDownRequest.getClusterId()))
                        .getFinalApplicationStatus());
            if (shutDownState) {
                client =
                    clusterDescriptor
                        .retrieve(yarnClusterDescriptor.applicationId)
                        .getClusterClient();
                client.shutDownCluster();
            }
            LOG.info(
                "the {}'s final status is {}",
                shutDownRequest.getClusterId(),
                clusterDescriptor
                    .getYarnClient()
                    .getApplicationReport(
                        ConverterUtils.toApplicationId(shutDownRequest.getClusterId()))
                    .getFinalApplicationStatus());
            return new ShutDownResponse(shutDownRequest.getClusterId());
        } catch (Exception e) {
            LOG.error("shutdown flink session fail in {} mode", shutDownRequest.getDeployMode(), e);
            throw e;
        } finally {
            Utils.close(client, clusterDescriptor);
        }
    }

    private String formatProperties(Map<String, Object> properties) {
        if (properties == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }
}
