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

import org.apache.streampark.common.util.Tuple2;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.client.bean.FlinkJobGraphBuilder;
import org.apache.streampark.flink.client.bean.RemoteWorkspace;
import org.apache.streampark.flink.client.bean.SubmitRequestResolver;
import org.apache.streampark.flink.client.configuration.FlinkConfigurationOps;
import org.apache.streampark.flink.client.request.CancelRequest;
import org.apache.streampark.flink.client.request.DeployRequest;
import org.apache.streampark.flink.client.request.ShutdownRequest;
import org.apache.streampark.flink.client.request.SubmitRequest;
import org.apache.streampark.flink.client.request.TriggerSavepointRequest;
import org.apache.streampark.flink.client.response.CancelResponse;
import org.apache.streampark.flink.client.response.DeployResponse;
import org.apache.streampark.flink.client.response.SavepointResponse;
import org.apache.streampark.flink.client.response.ShutdownResponse;
import org.apache.streampark.flink.client.response.SubmitResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.DeploymentOptionsInternal;
import org.apache.flink.util.FlinkException;
import org.apache.flink.yarn.YarnClusterDescriptor;
import org.apache.flink.yarn.configuration.YarnConfigOptions;
import org.apache.flink.yarn.configuration.YarnDeploymentTarget;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.exceptions.ApplicationNotFoundException;
import org.apache.hadoop.yarn.util.ConverterUtils;

import java.util.ArrayList;
import java.util.Map;

/** Submit Flink jobs to a YARN session cluster. */
public final class YarnSessionClient extends AbstractYarnClient {

    public static final YarnSessionClient INSTANCE = new YarnSessionClient();

    private YarnSessionClient() {
    }

    @Override
    protected void setConfig(SubmitRequest submitRequest, Configuration flinkConfig) {
        super.setConfig(submitRequest, flinkConfig);
        FlinkConfigurationOps.setIfPresent(
            flinkConfig, DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName());
        logEffectiveSubmitConfiguration(flinkConfig);
    }

    private void deployClusterConfig(DeployRequest deployRequest, Configuration flinkConfig) {
        ArrayList<String> shipFiles = new ArrayList<>();
        shipFiles.add(deployRequest.flinkVersion().getFlinkHome() + "/lib");
        shipFiles.add(deployRequest.flinkVersion().getFlinkHome() + "/plugins");

        RemoteWorkspace hdfsWorkspace = RemoteWorkspace.resolve(deployRequest.flinkVersion());
        FlinkConfigurationOps.setIfPresent(
            flinkConfig, YarnConfigOptions.FLINK_DIST_JAR, hdfsWorkspace.flinkDistJar());
        FlinkConfigurationOps.setIfPresent(flinkConfig, YarnConfigOptions.SHIP_FILES, shipFiles);
        FlinkConfigurationOps.setIfPresent(
            flinkConfig, DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName());
        FlinkConfigurationOps.setIfPresent(
            flinkConfig,
            DeploymentOptionsInternal.CONF_DIR,
            deployRequest.flinkVersion().getFlinkHome() + "/conf");

        logEffectiveSubmitConfiguration(flinkConfig);
    }

    @Override
    protected SubmitResponse doSubmit(
                                      SubmitRequest submitRequest,
                                      Configuration flinkConfig) throws FlinkException {
        return callAsFlinkException(
            () -> {
                Tuple2<ApplicationId, YarnClusterDescriptor> yarnClusterDescriptor =
                    getYarnClusterDescriptor(flinkConfig);
                ApplicationId yarnClusterId = yarnClusterDescriptor._1;
                YarnClusterDescriptor clusterDescriptor = yarnClusterDescriptor._2;

                FlinkJobGraphBuilder.Result job = null;
                ClusterClient<ApplicationId> client = null;
                try {
                    job =
                        buildJobGraph(
                            flinkConfig,
                            submitRequest,
                            SubmitRequestResolver.userJarFile(submitRequest));

                    client = clusterDescriptor.retrieve(yarnClusterId).getClusterClient();
                    String jobId = client.submitJob(job.jobGraph()).get().toString();
                    logInfo("Flink job started: jobId=" + jobId + ", applicationId=" + yarnClusterId);
                    return new SubmitResponse(
                        yarnClusterId.toString(),
                        flinkConfig.toMap(),
                        jobId,
                        client.getWebInterfaceURL());
                } finally {
                    closeSubmissionResources(
                        submitRequest,
                        job == null ? null : job.program(),
                        client,
                        clusterDescriptor);
                }
            });
    }

    @Override
    protected CancelResponse doCancel(
                                      CancelRequest cancelRequest,
                                      Configuration flinkConfig) throws FlinkException {
        FlinkConfigurationOps.setIfPresent(
            flinkConfig, DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName());
        return super.doCancel(cancelRequest, flinkConfig);
    }

    @Override
    protected SavepointResponse doTriggerSavepoint(
                                                   TriggerSavepointRequest request,
                                                   Configuration flinkConfig) throws FlinkException {
        FlinkConfigurationOps.setIfPresent(
            flinkConfig, DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName());
        return super.doTriggerSavepoint(request, flinkConfig);
    }

    public DeployResponse deploy(DeployRequest deployRequest) throws Exception {
        logInfo(
            String.format(
                "%n--------------------------------------- flink yarn sesion start "
                    + "---------------------------------------%n"
                    + "    userFlinkHome    : %s%n"
                    + "    flinkVersion     : %s%n"
                    + "    deployMode       : %s%n"
                    + "    clusterId        : %s%n"
                    + "    properties       : %s%n"
                    + "-------------------------------------------------------------------------------------------------------%n",
                deployRequest.flinkVersion().getFlinkHome(),
                deployRequest.flinkVersion().version(),
                deployRequest.deployMode().name(),
                deployRequest.clusterId(),
                deployRequest.properties()));

        YarnClusterDescriptor clusterDescriptor = null;
        ClusterClient<ApplicationId> client = null;
        try {
            Configuration flinkConfig =
                extractConfiguration(
                    deployRequest.flinkVersion().getFlinkHome(), deployRequest.properties());
            deployClusterConfig(deployRequest, flinkConfig);
            Tuple2<ClusterSpecification, YarnClusterDescriptor> yarnClusterDescriptor =
                getYarnClusterDeployDescriptor(flinkConfig);
            clusterDescriptor = yarnClusterDescriptor._2;

            if (StringUtils.isNotBlank(deployRequest.clusterId())) {
                DeployResponse existingSession =
                    tryReuseExistingYarnSession(deployRequest, clusterDescriptor);
                if (existingSession != null) {
                    return existingSession;
                }
            }

            client =
                clusterDescriptor
                    .deploySessionCluster(yarnClusterDescriptor._1)
                    .getClusterClient();
            if (client.getWebInterfaceURL() != null) {
                return new DeployResponse(
                    client.getWebInterfaceURL(), client.getClusterId().toString(), null);
            }
            return new DeployResponse(
                null,
                null,
                new IllegalStateException("Failed to get YARN session cluster WebInterfaceURL"));
        } catch (Exception e) {
            logError("start flink session fail in " + deployRequest.deployMode() + " mode");
            throw e;
        } finally {
            Utils.close(client, clusterDescriptor);
        }
    }

    private DeployResponse tryReuseExistingYarnSession(
                                                       DeployRequest deployRequest,
                                                       YarnClusterDescriptor clusterDescriptor) throws Exception {
        try {
            FinalApplicationStatus applicationStatus =
                clusterDescriptor
                    .getYarnClient()
                    .getApplicationReport(
                        ApplicationId.fromString(deployRequest.clusterId()))
                    .getFinalApplicationStatus();
            if (FinalApplicationStatus.UNDEFINED != applicationStatus) {
                return null;
            }
            ClusterClient<ApplicationId> yarnClient =
                clusterDescriptor
                    .retrieve(ApplicationId.fromString(deployRequest.clusterId()))
                    .getClusterClient();
            if (yarnClient.getWebInterfaceURL() == null) {
                return null;
            }
            return new DeployResponse(
                yarnClient.getWebInterfaceURL(),
                yarnClient.getClusterId().toString(),
                null);
        } catch (ApplicationNotFoundException e) {
            logInfo("this applicationId have not managed by yarn ,need deploy ...");
            return null;
        }
    }

    public ShutdownResponse shutdown(ShutdownRequest shutdownRequest) throws Exception {
        YarnClusterDescriptor clusterDescriptor = null;
        ClusterClient<ApplicationId> client = null;
        try {
            Configuration flinkConfig =
                loadDefaultConfiguration(shutdownRequest.flinkVersion().getFlinkHome());
            if (shutdownRequest.properties() != null) {
                for (Map.Entry<String, Object> entry : shutdownRequest.properties().entrySet()) {
                    if (entry.getValue() != null) {
                        flinkConfig.setString(entry.getKey(), entry.getValue().toString());
                    }
                }
            }
            FlinkConfigurationOps.setIfPresent(
                flinkConfig, YarnConfigOptions.APPLICATION_ID, shutdownRequest.clusterId());
            FlinkConfigurationOps.setIfPresent(
                flinkConfig, DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName());

            Tuple2<ApplicationId, YarnClusterDescriptor> yarnClusterDescriptor =
                getYarnClusterDescriptor(flinkConfig);
            clusterDescriptor = yarnClusterDescriptor._2;

            boolean shutdownState =
                FinalApplicationStatus.UNDEFINED.equals(
                    clusterDescriptor
                        .getYarnClient()
                        .getApplicationReport(
                            ApplicationId.fromString(shutdownRequest.clusterId()))
                        .getFinalApplicationStatus());
            if (shutdownState) {
                client =
                    clusterDescriptor
                        .retrieve(yarnClusterDescriptor._1)
                        .getClusterClient();
                client.shutDownCluster();
            }

            logInfo(
                "the "
                    + shutdownRequest.clusterId()
                    + "'s final status is "
                    + clusterDescriptor
                        .getYarnClient()
                        .getApplicationReport(
                            ConverterUtils.toApplicationId(shutdownRequest.clusterId()))
                        .getFinalApplicationStatus());
            return new ShutdownResponse(shutdownRequest.clusterId());
        } catch (Exception e) {
            logError("shutdown flink session fail in " + shutdownRequest.deployMode() + " mode");
            throw e;
        } finally {
            Utils.close(client, clusterDescriptor);
        }
    }
}
