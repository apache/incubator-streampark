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

import org.apache.streampark.flink.client.bean.CancelRequest;
import org.apache.streampark.flink.client.bean.CancelResponse;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.client.trait.YarnClientTrait;
import org.apache.streampark.flink.util.FlinkUtils;

import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.util.FlinkException;
import org.apache.flink.yarn.YarnClusterDescriptor;
import org.apache.flink.yarn.configuration.YarnDeploymentTarget;
import org.apache.flink.yarn.entrypoint.YarnJobClusterEntrypoint;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.ApplicationId;

import scala.Tuple2;

/** Yarn per-job mode submit. */
public final class YarnPerJobClient extends YarnClientTrait {

    public static final YarnPerJobClient INSTANCE = new YarnPerJobClient();

    private YarnPerJobClient() {
    }

    @Override
    public void setConfig(SubmitRequest submitRequest, Configuration flinkConfig) {
        super.setConfig(submitRequest, flinkConfig);
        FlinkConfigurationOps.safeSet(
            flinkConfig, DeploymentOptions.TARGET, YarnDeploymentTarget.PER_JOB.getName());
        FlinkConfigurationOps.safeSet(flinkConfig, DeploymentOptions.ATTACHED, true);
        FlinkConfigurationOps.safeSet(flinkConfig, DeploymentOptions.SHUTDOWN_IF_ATTACHED, true);

        logEffectiveSubmitConfiguration(flinkConfig);
    }

    @Override
    public SubmitResponse doSubmit(SubmitRequest submitRequest, Configuration flinkConfig) throws FlinkException {
        return callAsFlinkException(
            () -> {
                String flinkHome = submitRequest.flinkVersion().getFlinkHome();

                Tuple2<ClusterSpecification, YarnClusterDescriptor> deployDescriptor =
                    getYarnClusterDeployDescriptor(flinkConfig, submitRequest.hadoopUser());
                ClusterSpecification clusterSpecification = deployDescriptor._1();
                YarnClusterDescriptor clusterDescriptor = deployDescriptor._2();

                String flinkDistJar = FlinkUtils.getFlinkDistJar(flinkHome);
                clusterDescriptor.setLocalJarPath(new Path(flinkDistJar));
                clusterDescriptor.addShipFiles(java.util.Collections.singletonList(new Path(flinkHome + "/lib")));

                PackagedProgram packagedProgram = null;
                ClusterClient<ApplicationId> clusterClient;
                logClusterSpecification(clusterSpecification);

                Tuple2<PackagedProgram, JobGraph> programJobGraph =
                    getJobGraph(flinkConfig, submitRequest, submitRequest.userJarFile());
                packagedProgram = programJobGraph._1();
                JobGraph jobGraph = programJobGraph._2();

                logInfo(
                    String.format(
                        "%n-------------------------<<applicationId>>------------------------%n"
                            + "jobGraph getJobID: %s%n"
                            + "__________________________________________________________________%n",
                        jobGraph.getJobID()));

                clusterClient =
                    deployInternal(
                        clusterDescriptor,
                        clusterSpecification,
                        submitRequest.effectiveAppName(),
                        YarnJobClusterEntrypoint.class.getName(),
                        jobGraph,
                        true)
                            .getClusterClient();

                ApplicationId applicationId = clusterClient.getClusterId();
                String jobManagerUrl = clusterClient.getWebInterfaceURL();
                logYarnJobStarted(applicationId);

                SubmitResponse resp =
                    new SubmitResponse(applicationId.toString(), flinkConfig.toMap(), "", jobManagerUrl);
                closeSubmit(submitRequest, packagedProgram, clusterClient, clusterDescriptor);
                return resp;
            });
    }

    @Override
    public CancelResponse doCancel(CancelRequest cancelRequest, Configuration flinkConfig) throws FlinkException {
        return callAsFlinkException(
            () -> {
                CancelResponse response = super.doCancel(cancelRequest, flinkConfig);
                Tuple2<ApplicationId, YarnClusterDescriptor> yarnClusterDescriptor =
                    getYarnClusterDescriptor(flinkConfig);
                yarnClusterDescriptor
                    ._2()
                    .killCluster(ApplicationId.fromString(cancelRequest.clusterId()));
                return response;
            });
    }
}
