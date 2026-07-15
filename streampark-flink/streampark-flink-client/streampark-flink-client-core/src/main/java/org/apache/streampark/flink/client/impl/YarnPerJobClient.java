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
import org.apache.streampark.flink.client.bean.CancelRequest;
import org.apache.streampark.flink.client.bean.CancelResponse;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.client.trait.YarnClientTrait;
import org.apache.streampark.flink.client.util.FlinkConfigurationEnhancer;
import org.apache.streampark.flink.util.FlinkUtils;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.yarn.YarnClusterDescriptor;
import org.apache.flink.yarn.configuration.YarnDeploymentTarget;
import org.apache.flink.yarn.entrypoint.YarnJobClusterEntrypoint;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.ApplicationId;

/** Yarn PerJob mode submit */
public final class YarnPerJobClient extends YarnClientTrait {

    public static final YarnPerJobClient INSTANCE = new YarnPerJobClient();

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(YarnPerJobClient.class.getName());

    private YarnPerJobClient() {
    }

    @Override
    public void setConfig(SubmitRequest submitRequest, Configuration flinkConfig) {
        super.setConfig(submitRequest, flinkConfig);
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, DeploymentOptions.TARGET, YarnDeploymentTarget.PER_JOB.getName());
        FlinkConfigurationEnhancer.safeSet(flinkConfig, DeploymentOptions.ATTACHED, true);
        FlinkConfigurationEnhancer.safeSet(flinkConfig, DeploymentOptions.SHUTDOWN_IF_ATTACHED, true);

        LOG.info(
            "\n------------------------------------------------------------------\n"
                + "Effective submit configuration: {}\n"
                + "------------------------------------------------------------------\n",
            flinkConfig);
    }

    @Override
    public SubmitResponse doSubmit(SubmitRequest submitRequest, Configuration flinkConfig) throws Exception {
        String flinkHome = submitRequest.getFlinkVersion().flinkHome;

        YarnClusterDeployDescriptorResult descriptorResult =
            getYarnClusterDeployDescriptor(flinkConfig, submitRequest.getHadoopUser());
        YarnClusterDescriptor clusterDescriptor = descriptorResult.clusterDescriptor;
        String flinkDistJar = FlinkUtils.getFlinkDistJar(flinkHome);
        clusterDescriptor.setLocalJarPath(new Path(flinkDistJar));
        clusterDescriptor.addShipFiles(java.util.Collections.singletonList(new Path(flinkHome + "/lib")));

        PackagedProgram packagedProgram = null;
        LOG.info(
            "\n------------------------<<specification>>-------------------------\n"
                + "{}\n"
                + "------------------------------------------------------------------\n",
            descriptorResult.clusterSpecification);

        JobGraphPackagedProgram programJobGraph =
            getJobGraph(flinkConfig, submitRequest, submitRequest.getUserJarFile());
        packagedProgram = programJobGraph.packagedProgram;
        JobGraph jobGraph = programJobGraph.jobGraph;

        LOG.info(
            "\n-------------------------<<applicationId>>------------------------\n"
                + "jobGraph getJobID: {}\n"
                + "__________________________________________________________________\n",
            jobGraph.getJobID());

        org.apache.flink.client.program.ClusterClient<ApplicationId> clusterClient =
            deployInternal(
                clusterDescriptor,
                descriptorResult.clusterSpecification,
                submitRequest.getEffectiveAppName(),
                YarnJobClusterEntrypoint.class.getName(),
                jobGraph,
                true)
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
        closeSubmit(submitRequest, packagedProgram, clusterClient, clusterDescriptor);
        return resp;
    }

    @Override
    public CancelResponse doCancel(CancelRequest cancelRequest, Configuration flinkConfig) throws Exception {
        CancelResponse response = super.doCancel(cancelRequest, flinkConfig);
        YarnClusterDescriptorResult descriptorResult = getYarnClusterDescriptor(flinkConfig);
        descriptorResult.clusterDescriptor.killCluster(ApplicationId.fromString(cancelRequest.getClusterId()));
        return response;
    }
}
