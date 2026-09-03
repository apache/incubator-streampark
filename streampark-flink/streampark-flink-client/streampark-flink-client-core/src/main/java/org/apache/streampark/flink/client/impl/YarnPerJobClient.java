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
import org.apache.streampark.flink.client.bean.SubmitRequestResolver;
import org.apache.streampark.flink.client.configuration.FlinkConfigurationOps;
import org.apache.streampark.flink.client.request.CancelRequest;
import org.apache.streampark.flink.client.request.SubmitRequest;
import org.apache.streampark.flink.client.response.CancelResponse;
import org.apache.streampark.flink.client.response.SubmitResponse;
import org.apache.streampark.flink.util.FlinkUtils;

import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.util.FlinkException;
import org.apache.flink.yarn.YarnClusterDescriptor;
import org.apache.flink.yarn.configuration.YarnDeploymentTarget;
import org.apache.flink.yarn.entrypoint.YarnJobClusterEntrypoint;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.ApplicationId;

/** Yarn per-job mode submit. */
public final class YarnPerJobClient extends AbstractYarnClient {

    public static final YarnPerJobClient INSTANCE = new YarnPerJobClient();

    private YarnPerJobClient() {
    }

    @Override
    protected void setConfig(SubmitRequest submitRequest, Configuration flinkConfig) {
        super.setConfig(submitRequest, flinkConfig);
        FlinkConfigurationOps.setIfPresent(
            flinkConfig, DeploymentOptions.TARGET, YarnDeploymentTarget.PER_JOB.getName());
        FlinkConfigurationOps.setIfPresent(flinkConfig, DeploymentOptions.ATTACHED, true);
        FlinkConfigurationOps.setIfPresent(flinkConfig, DeploymentOptions.SHUTDOWN_IF_ATTACHED, true);

        logEffectiveSubmitConfiguration(flinkConfig);
    }

    @Override
    protected SubmitResponse doSubmit(
                                      SubmitRequest submitRequest,
                                      Configuration flinkConfig) throws FlinkException {

        return callAsFlinkException(
            () -> {
                String flinkHome = submitRequest.flinkVersion().getFlinkHome();

                Tuple2<ClusterSpecification, YarnClusterDescriptor> deployDescriptor =
                    getYarnClusterDeployDescriptor(flinkConfig, submitRequest.hadoopUser());
                ClusterSpecification clusterSpecification = deployDescriptor._1;
                YarnClusterDescriptor clusterDescriptor = deployDescriptor._2;

                String flinkDistJar = FlinkUtils.getFlinkDistJar(flinkHome);
                clusterDescriptor.setLocalJarPath(new Path(flinkDistJar));
                clusterDescriptor.addShipFiles(java.util.Collections.singletonList(new Path(flinkHome + "/lib")));

                ClusterClient<ApplicationId> clusterClient = null;
                FlinkJobGraphBuilder.Result job = null;
                try {
                    logClusterSpecification(clusterSpecification);
                    job =
                        buildJobGraph(
                            flinkConfig,
                            submitRequest,
                            SubmitRequestResolver.userJarFile(submitRequest));
                    JobGraph jobGraph = job.jobGraph();
                    clusterClient =
                        deployInternal(
                            clusterDescriptor,
                            clusterSpecification,
                            SubmitRequestResolver.effectiveApplicationName(submitRequest),
                            YarnJobClusterEntrypoint.class.getName(),
                            jobGraph,
                            true)
                                .getClusterClient();
                    ApplicationId applicationId = clusterClient.getClusterId();
                    logYarnJobStarted(applicationId);
                    return new SubmitResponse(
                        applicationId.toString(),
                        flinkConfig.toMap(),
                        "",
                        clusterClient.getWebInterfaceURL());
                } finally {
                    closeSubmissionResources(
                        submitRequest,
                        job == null ? null : job.program(),
                        clusterClient,
                        clusterDescriptor);
                }
            });
    }

    @Override
    protected CancelResponse doCancel(
                                      CancelRequest cancelRequest,
                                      Configuration flinkConfig) throws FlinkException {
        return callAsFlinkException(
            () -> {
                CancelResponse response = super.doCancel(cancelRequest, flinkConfig);
                Tuple2<ApplicationId, YarnClusterDescriptor> yarnClusterDescriptor =
                    getYarnClusterDescriptor(flinkConfig);
                try {
                    yarnClusterDescriptor._2
                        .killCluster(ApplicationId.fromString(cancelRequest.clusterId()));
                    return response;
                } finally {
                    Utils.close(yarnClusterDescriptor._2);
                }
            });
    }
}
