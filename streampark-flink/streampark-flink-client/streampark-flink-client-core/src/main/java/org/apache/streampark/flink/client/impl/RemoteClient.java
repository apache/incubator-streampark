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
import org.apache.streampark.flink.client.bean.SavepointRequestTrait;
import org.apache.streampark.flink.client.bean.SavepointResponse;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.client.bean.TriggerSavepointRequest;
import org.apache.streampark.flink.client.tool.FlinkSessionSubmitHelper;
import org.apache.streampark.flink.client.trait.FlinkClientTrait;
import org.apache.streampark.flink.client.util.FlinkConfigurationEnhancer;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.flink.api.common.JobID;
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader;
import org.apache.flink.client.deployment.StandaloneClusterDescriptor;
import org.apache.flink.client.deployment.StandaloneClusterId;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.jobgraph.JobGraph;

import java.io.File;

/** Submit Job to Remote Cluster */
public final class RemoteClient extends FlinkClientTrait {

    public static final RemoteClient INSTANCE = new RemoteClient();

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(RemoteClient.class.getName());

    private RemoteClient() {
    }

    @Override
    public void setConfig(SubmitRequest submitRequest, Configuration flinkConfig) {
    }

    @Override
    public SubmitResponse doSubmit(SubmitRequest submitRequest, Configuration flinkConfig) throws Exception {
        return trySubmit(
            submitRequest,
            flinkConfig,
            submitRequest.getUserJarFile(),
            INSTANCE::jobGraphSubmit,
            INSTANCE::restApiSubmit);
    }

    @Override
    public CancelResponse doCancel(CancelRequest cancelRequest, Configuration flinkConfig) throws Exception {
        return executeClientAction(
            cancelRequest,
            flinkConfig,
            (jobId, clusterClient) -> new CancelResponse(cancelJob(cancelRequest, jobId, clusterClient)));
    }

    @Override
    public SavepointResponse doTriggerSavepoint(
                                                TriggerSavepointRequest savepointRequest,
                                                Configuration flinkConfig) throws Exception {
        return executeClientAction(
            savepointRequest,
            flinkConfig,
            (jobId, clusterClient) -> new SavepointResponse(
                triggerSavepoint(savepointRequest, jobId, clusterClient)));
    }

    private <O, R extends SavepointRequestTrait> O executeClientAction(
                                                                       R request, Configuration flinkConfig,
                                                                       ClusterClientAction<O, StandaloneClusterId> actFunc) throws Exception {
        ClusterClient<StandaloneClusterId> client = null;
        StandaloneClusterDescriptor clusterDescriptor = null;
        try {
            FlinkConfigurationEnhancer.safeSet(flinkConfig, DeploymentOptions.TARGET,
                request.getDeployMode().getName());
            FlinkConfigurationEnhancer.safeSet(
                flinkConfig,
                RestOptions.ADDRESS,
                request.getProperties().get(RestOptions.ADDRESS.key()).toString());
            FlinkConfigurationEnhancer.safeSet(
                flinkConfig,
                RestOptions.PORT,
                Integer.parseInt(request.getProperties().get(RestOptions.PORT.key()).toString()));
            LOG.info(
                "\n------------------------------------------------------------------\n"
                    + "Effective submit configuration: {}\n"
                    + "------------------------------------------------------------------\n",
                flinkConfig);
            StandaloneClusterDescriptorResult descriptorResult = getStandAloneClusterDescriptor(flinkConfig);
            clusterDescriptor = descriptorResult.clusterDescriptor;
            client = clusterDescriptor.retrieve(descriptorResult.clusterId).getClusterClient();
            return actFunc.apply(JobID.fromHexString(request.getJobId()), client);
        } catch (Exception e) {
            LOG.error("Do {} for flink standalone job fail", request.getClass().getSimpleName(), e);
            throw e;
        } finally {
            if (client != null) {
                client.close();
            }
            if (clusterDescriptor != null) {
                clusterDescriptor.close();
            }
        }
    }

    /** Submit flink session job via rest api. */
    public SubmitResponse restApiSubmit(
                                        SubmitRequest submitRequest, Configuration flinkConfig,
                                        File fatJar) throws Exception {
        StandaloneClusterDescriptorResult standAloneDescriptor = getStandAloneClusterDescriptor(flinkConfig);
        StandaloneClusterId clusterId = standAloneDescriptor.clusterId;
        StandaloneClusterDescriptor clusterDescriptor = standAloneDescriptor.clusterDescriptor;

        ClusterClient<StandaloneClusterId> client =
            clusterDescriptor.retrieve(clusterId).getClusterClient();
        String jobId =
            FlinkSessionSubmitHelper.submitViaRestApi(client.getWebInterfaceURL(), fatJar, flinkConfig);
        LOG.info(
            "{} mode submit by restApi, WebInterfaceURL {}, jobId: {}",
            submitRequest.getDeployMode(),
            client.getWebInterfaceURL(),
            jobId);
        SubmitResponse resp =
            SubmitResponse.builder()
                .clusterId(null)
                .flinkConfig(flinkConfig.toMap())
                .jobId(jobId)
                .jobManagerUrl(client.getWebInterfaceURL())
                .build();
        closeSubmit(submitRequest, client, clusterDescriptor);
        return resp;
    }

    /** Submit flink session job with building JobGraph via Standalone ClusterClient api. */
    public SubmitResponse jobGraphSubmit(
                                         SubmitRequest submitRequest, Configuration flinkConfig,
                                         File jarFile) throws Exception {
        StandaloneClusterDescriptorResult standAloneDescriptor = getStandAloneClusterDescriptor(flinkConfig);
        StandaloneClusterDescriptor clusterDescriptor = standAloneDescriptor.clusterDescriptor;
        JobGraphPackagedProgram packageProgramJobGraph = getJobGraph(flinkConfig, submitRequest, jarFile);
        PackagedProgram packageProgram = packageProgramJobGraph.packagedProgram;
        JobGraph jobGraph = packageProgramJobGraph.jobGraph;
        ClusterClient<StandaloneClusterId> client =
            clusterDescriptor.retrieve(standAloneDescriptor.clusterId).getClusterClient();
        String jobId = client.submitJob(jobGraph).get().toString();
        LOG.info(
            "{} mode submit by jobGraph, WebInterfaceURL {}, jobId: {}",
            submitRequest.getDeployMode(),
            client.getWebInterfaceURL(),
            jobId);
        SubmitResponse result =
            SubmitResponse.builder()
                .clusterId(null)
                .flinkConfig(flinkConfig.toMap())
                .jobId(jobId)
                .jobManagerUrl(client.getWebInterfaceURL())
                .build();
        closeSubmit(submitRequest, packageProgram, client, clusterDescriptor);
        return result;
    }

    private StandaloneClusterDescriptorResult getStandAloneClusterDescriptor(Configuration flinkConfig) {
        DefaultClusterClientServiceLoader serviceLoader = new DefaultClusterClientServiceLoader();
        org.apache.flink.client.deployment.ClusterClientFactory<StandaloneClusterId> clientFactory =
            serviceLoader.getClusterClientFactory(flinkConfig);
        StandaloneClusterId standaloneClusterId = clientFactory.getClusterId(flinkConfig);
        StandaloneClusterDescriptor standaloneClusterDescriptor =
            (StandaloneClusterDescriptor) clientFactory.createClusterDescriptor(flinkConfig);
        return new StandaloneClusterDescriptorResult(standaloneClusterId, standaloneClusterDescriptor);
    }

    private static final class StandaloneClusterDescriptorResult {

        private final StandaloneClusterId clusterId;
        private final StandaloneClusterDescriptor clusterDescriptor;

        StandaloneClusterDescriptorResult(
                                          StandaloneClusterId clusterId,
                                          StandaloneClusterDescriptor clusterDescriptor) {
            this.clusterId = clusterId;
            this.clusterDescriptor = clusterDescriptor;
        }
    }
}
