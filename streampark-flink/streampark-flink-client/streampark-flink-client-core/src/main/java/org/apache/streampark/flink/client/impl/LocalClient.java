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

import org.apache.streampark.flink.client.bean.FlinkJobGraphBuilder;
import org.apache.streampark.flink.client.bean.SubmitRequestResolver;
import org.apache.streampark.flink.client.configuration.FlinkConfigurationOps;
import org.apache.streampark.flink.client.request.CancelRequest;
import org.apache.streampark.flink.client.request.SubmitRequest;
import org.apache.streampark.flink.client.request.TriggerSavepointRequest;
import org.apache.streampark.flink.client.response.CancelResponse;
import org.apache.streampark.flink.client.response.SavepointResponse;
import org.apache.streampark.flink.client.response.SubmitResponse;

import org.apache.flink.client.deployment.executors.RemoteExecutor;
import org.apache.flink.client.program.MiniClusterClient;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.configuration.TaskManagerOptions;
import org.apache.flink.runtime.minicluster.MiniCluster;
import org.apache.flink.runtime.minicluster.MiniClusterConfiguration;
import org.apache.flink.util.FlinkException;

/** Submit Flink jobs to a local mini cluster. */
public final class LocalClient extends AbstractFlinkClient {

    public static final LocalClient INSTANCE = new LocalClient();

    private LocalClient() {
    }

    @Override
    protected void setConfig(SubmitRequest submitRequest, Configuration flinkConfig) {
        FlinkConfigurationOps.setIfPresent(
            flinkConfig,
            PipelineOptions.NAME,
            SubmitRequestResolver.effectiveApplicationName(submitRequest));
        logEffectiveSubmitConfiguration(flinkConfig);
    }

    @Override
    protected SubmitResponse doSubmit(
                                      SubmitRequest submitRequest,
                                      Configuration flinkConfig) throws FlinkException {
        return callAsFlinkException(
            () -> {
                FlinkJobGraphBuilder.Result job = buildJobGraph(
                    flinkConfig,
                    submitRequest,
                    SubmitRequestResolver.userJarFile(submitRequest));
                MiniClusterClient client = null;
                try {
                    client = createLocalCluster(flinkConfig);
                    String jobId = client.submitJob(job.jobGraph()).get().toString();
                    return new SubmitResponse(
                        jobId, flinkConfig.toMap(), jobId, client.getWebInterfaceURL());
                } finally {
                    closeSubmissionResources(submitRequest, job.program(), client);
                }
            });
    }

    @Override
    protected SavepointResponse doTriggerSavepoint(
                                                   TriggerSavepointRequest savepointRequest,
                                                   Configuration flinkConfig) throws FlinkException {
        return RemoteClient.INSTANCE.doTriggerSavepoint(savepointRequest, flinkConfig);
    }

    @Override
    protected CancelResponse doCancel(
                                      CancelRequest cancelRequest,
                                      Configuration flinkConfig) throws FlinkException {
        return RemoteClient.INSTANCE.doCancel(cancelRequest, flinkConfig);
    }

    private MiniClusterClient createLocalCluster(Configuration flinkConfig) throws Exception {
        FlinkConfigurationOps.setIfPresent(flinkConfig, JobManagerOptions.PORT, 0);

        int numTaskManagers =
            flinkConfig.getInteger(
                ConfigConstants.LOCAL_NUMBER_TASK_MANAGER,
                ConfigConstants.DEFAULT_LOCAL_NUMBER_TASK_MANAGER);
        int numSlotsPerTaskManager = flinkConfig.getInteger(TaskManagerOptions.NUM_TASK_SLOTS);

        MiniClusterConfiguration miniClusterConfig =
            new MiniClusterConfiguration.Builder()
                .setConfiguration(flinkConfig)
                .setNumSlotsPerTaskManager(numSlotsPerTaskManager)
                .setNumTaskManagers(numTaskManagers)
                .build();

        MiniCluster cluster = new MiniCluster(miniClusterConfig);
        cluster.start();

        String host = "localhost";
        int port = cluster.getRestAddress().get().getPort();

        FlinkConfigurationOps.setIfPresent(flinkConfig, JobManagerOptions.ADDRESS, host);
        FlinkConfigurationOps.setIfPresent(flinkConfig, JobManagerOptions.PORT, port);
        FlinkConfigurationOps.setIfPresent(flinkConfig, RestOptions.ADDRESS, host);
        FlinkConfigurationOps.setIfPresent(flinkConfig, RestOptions.PORT, port);
        FlinkConfigurationOps.setIfPresent(flinkConfig, DeploymentOptions.TARGET, RemoteExecutor.NAME);

        logInfo(String.format("%nStarting local Flink cluster (host: localhost, port: %d).%n", port));

        return new MiniClusterClient(flinkConfig, cluster);
    }
}
