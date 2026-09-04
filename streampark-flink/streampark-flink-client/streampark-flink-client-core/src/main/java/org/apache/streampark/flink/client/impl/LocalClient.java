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
import org.apache.streampark.flink.client.bean.ResolvedSubmitRequest;
import org.apache.streampark.flink.client.request.CancelRequest;
import org.apache.streampark.flink.client.request.SavepointRequest;
import org.apache.streampark.flink.client.request.SubmitRequest;
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

/** Submits and manages Flink jobs in an embedded local cluster. */
public final class LocalClient extends AbstractFlinkClient {

    public static final LocalClient INSTANCE = new LocalClient();

    private LocalClient() {
    }

    /** Adds the resolved job name used by the embedded cluster. */
    @Override
    protected void setConfig(ResolvedSubmitRequest resolved, Configuration flinkConfig) {
        flinkConfig.set(PipelineOptions.NAME, resolved.getJobName());
        logEffectiveSubmitConfiguration(flinkConfig);
    }

    /** Creates an embedded cluster and submits the request's JobGraph. */
    @Override
    protected SubmitResponse doSubmit(
                                      ResolvedSubmitRequest resolved,
                                      Configuration flinkConfig) throws FlinkException {
        SubmitRequest submitRequest = resolved.request();
        return execute(
            () -> {
                FlinkJobGraphBuilder.Result job = buildJobGraph(
                    flinkConfig,
                    resolved,
                    resolved.getUserJarFile());
                MiniClusterClient client = null;
                try {
                    client = createLocalCluster(flinkConfig);
                    String jobId = client.submitJob(job.jobGraph()).get().toString();
                    return new SubmitResponse(
                        jobId, flinkConfig.toMap(), jobId, client.getWebInterfaceURL());
                } finally {
                    closeSubmissionResources(job.program(), client);
                }
            });
    }

    /** Delegates savepoint access to the standalone cluster protocol. */
    @Override
    protected SavepointResponse doTriggerSavepoint(
                                                   SavepointRequest savepointRequest,
                                                   Configuration flinkConfig) throws FlinkException {
        return RemoteClient.INSTANCE.doTriggerSavepoint(savepointRequest, flinkConfig);
    }

    /** Delegates cancellation to the standalone cluster protocol. */
    @Override
    protected CancelResponse doCancel(
                                      CancelRequest cancelRequest,
                                      Configuration flinkConfig) throws FlinkException {
        return RemoteClient.INSTANCE.doCancel(cancelRequest, flinkConfig);
    }

    /** Creates and starts the mini cluster used for one local submission. */
    private MiniClusterClient createLocalCluster(Configuration flinkConfig) throws Exception {
        flinkConfig.set(JobManagerOptions.PORT, 0);

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

        flinkConfig.set(JobManagerOptions.ADDRESS, host);
        flinkConfig.set(JobManagerOptions.PORT, port);
        flinkConfig.set(RestOptions.ADDRESS, host);
        flinkConfig.set(RestOptions.PORT, port);
        flinkConfig.set(DeploymentOptions.TARGET, RemoteExecutor.NAME);

        logInfo(String.format("%nStarting local Flink cluster (host: localhost, port: %d).%n", port));

        return new MiniClusterClient(flinkConfig, cluster);
    }
}
