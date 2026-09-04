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
import org.apache.streampark.flink.client.bean.ResolvedSubmitRequest;
import org.apache.streampark.flink.client.request.CancelRequest;
import org.apache.streampark.flink.client.request.DeployRequest;
import org.apache.streampark.flink.client.request.SavepointRequest;
import org.apache.streampark.flink.client.request.ShutdownRequest;
import org.apache.streampark.flink.client.request.SubmitRequest;
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

import java.util.ArrayList;
import java.util.Map;

/** Manages Flink jobs and clusters in YARN session mode. */
public final class YarnSessionClient extends AbstractYarnClient {

    public static final YarnSessionClient INSTANCE = new YarnSessionClient();

    private YarnSessionClient() {
    }

    /** Deploys a new YARN session cluster or reconnects to a live requested cluster. */
    public DeployResponse deploy(DeployRequest deployRequest) throws Exception {
        logInfo(
            String.format(
                "%n--------------------------------------- Flink YARN session start "
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

    /** Stops the requested YARN session cluster; an already absent cluster is treated as stopped. */
    public ShutdownResponse shutdown(ShutdownRequest shutdownRequest) throws Exception {
        YarnClusterDescriptor clusterDescriptor = null;
        ClusterClient<ApplicationId> client = null;
        try {
            Configuration flinkConfig =
                loadDefaultConfiguration(shutdownRequest.flinkVersion().getFlinkHome());
            for (Map.Entry<String, Object> entry : shutdownRequest.properties().entrySet()) {
                if (entry.getValue() != null) {
                    flinkConfig.setString(entry.getKey(), entry.getValue().toString());
                }
            }
            flinkConfig.set(YarnConfigOptions.APPLICATION_ID, shutdownRequest.clusterId());
            flinkConfig.set(DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName());

            Tuple2<ApplicationId, YarnClusterDescriptor> yarnClusterDescriptor =
                getYarnClusterDescriptor(flinkConfig);
            clusterDescriptor = yarnClusterDescriptor._2;

            FinalApplicationStatus finalStatus =
                clusterDescriptor
                    .getYarnClient()
                    .getApplicationReport(ApplicationId.fromString(shutdownRequest.clusterId()))
                    .getFinalApplicationStatus();
            boolean clusterRunning =
                FinalApplicationStatus.UNDEFINED.equals(
                    finalStatus);
            if (clusterRunning) {
                client =
                    clusterDescriptor
                        .retrieve(yarnClusterDescriptor._1)
                        .getClusterClient();
                client.shutDownCluster();
            }

            logInfo(
                "YARN application "
                    + shutdownRequest.clusterId()
                    + " had final status "
                    + finalStatus);
            return new ShutdownResponse(shutdownRequest.clusterId());
        } catch (ApplicationNotFoundException e) {
            // Shutdown is idempotent: a YARN application that no longer exists is already stopped.
            return new ShutdownResponse(shutdownRequest.clusterId());
        } catch (Exception e) {
            logError("shutdown flink session fail in " + shutdownRequest.deployMode() + " mode");
            throw e;
        } finally {
            Utils.close(client, clusterDescriptor);
        }
    }

    /** Adds the YARN session deployment target to a job submission configuration. */
    @Override
    protected void setConfig(ResolvedSubmitRequest resolved, Configuration flinkConfig) {
        super.setConfig(resolved, flinkConfig);
        flinkConfig.set(DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName());
        logEffectiveSubmitConfiguration(flinkConfig);
    }

    /** Builds and submits a JobGraph to an existing YARN session cluster. */
    @Override
    protected SubmitResponse doSubmit(
                                      ResolvedSubmitRequest resolved,
                                      Configuration flinkConfig) throws FlinkException {
        SubmitRequest submitRequest = resolved.request();
        return execute(
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
                            resolved,
                            resolved.getUserJarFile());

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
                        job == null ? null : job.program(),
                        client,
                        clusterDescriptor);
                }
            });
    }

    /** Cancels a job while retaining its YARN session cluster. */
    @Override
    protected CancelResponse doCancel(
                                      CancelRequest cancelRequest,
                                      Configuration flinkConfig) throws FlinkException {
        flinkConfig.set(DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName());
        return super.doCancel(cancelRequest, flinkConfig);
    }

    /** Triggers a savepoint against an existing YARN session cluster. */
    @Override
    protected SavepointResponse doTriggerSavepoint(
                                                   SavepointRequest request,
                                                   Configuration flinkConfig) throws FlinkException {
        flinkConfig.set(DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName());
        return super.doTriggerSavepoint(request, flinkConfig);
    }

    /** Adds the remote distribution and local ship files required to deploy a session cluster. */
    private void deployClusterConfig(DeployRequest deployRequest, Configuration flinkConfig) {
        ArrayList<String> shipFiles = new ArrayList<>();
        shipFiles.add(deployRequest.flinkVersion().getFlinkHome() + "/lib");
        shipFiles.add(deployRequest.flinkVersion().getFlinkHome() + "/plugins");

        RemoteWorkspace hdfsWorkspace = RemoteWorkspace.resolve(deployRequest.flinkVersion());
        flinkConfig.set(YarnConfigOptions.FLINK_DIST_JAR, hdfsWorkspace.flinkDistJar());
        flinkConfig.set(YarnConfigOptions.SHIP_FILES, shipFiles);
        flinkConfig.set(DeploymentOptions.TARGET, YarnDeploymentTarget.SESSION.getName());
        flinkConfig.set(
            DeploymentOptionsInternal.CONF_DIR,
            deployRequest.flinkVersion().getFlinkHome() + "/conf");

        logEffectiveSubmitConfiguration(flinkConfig);
    }

    /** Returns a live existing session response, or {@code null} when redeployment is required. */
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
            try (
                ClusterClient<ApplicationId> yarnClient =
                    clusterDescriptor
                        .retrieve(ApplicationId.fromString(deployRequest.clusterId()))
                        .getClusterClient()) {
                if (yarnClient.getWebInterfaceURL() == null) {
                    return null;
                }
                return new DeployResponse(
                    yarnClient.getWebInterfaceURL(),
                    yarnClient.getClusterId().toString(),
                    null);
            }
        } catch (ApplicationNotFoundException e) {
            logInfo(
                "YARN application "
                    + deployRequest.clusterId()
                    + " was not found; deploying a new session cluster");
            return null;
        }
    }
}
