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

import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.ClassLoaderUtils;
import org.apache.streampark.common.util.Tuple2;
import org.apache.streampark.flink.client.bean.ResolvedSubmitRequest;
import org.apache.streampark.flink.client.bean.SessionClusterRestClient;
import org.apache.streampark.flink.client.request.AbstractSavepointRequest;
import org.apache.streampark.flink.client.request.CancelRequest;
import org.apache.streampark.flink.client.request.SavepointRequest;
import org.apache.streampark.flink.client.request.SubmitRequest;
import org.apache.streampark.flink.client.response.CancelResponse;
import org.apache.streampark.flink.client.response.SavepointResponse;
import org.apache.streampark.flink.client.response.SubmitResponse;

import org.apache.flink.api.common.JobID;
import org.apache.flink.client.deployment.ClusterClientFactory;
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader;
import org.apache.flink.client.deployment.StandaloneClusterDescriptor;
import org.apache.flink.client.deployment.StandaloneClusterId;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.util.FlinkException;

import java.io.File;
import java.util.Map;

/** Submits and manages Flink jobs on a remote standalone cluster. */
public final class RemoteClient extends AbstractFlinkClient {

    public static final RemoteClient INSTANCE = new RemoteClient();

    private RemoteClient() {
    }

    /** Leaves standalone-specific configuration to Flink's standard CLI processing. */
    @Override
    protected void setConfig(ResolvedSubmitRequest resolved, Configuration flinkConfig) {
        // Standalone submission requires no deployment-mode-specific options.
    }

    /** Tries native JobGraph submission before using the standalone REST endpoint. */
    @Override
    protected SubmitResponse doSubmit(
                                      ResolvedSubmitRequest resolved,
                                      Configuration flinkConfig) throws FlinkException {
        return execute(
            () -> trySubmit(
                resolved,
                flinkConfig,
                resolved.getUserJarFile(),
                this::jobGraphSubmit,
                this::restApiSubmit));
    }

    /** Cancels a job through the remote standalone cluster client. */
    @Override
    protected CancelResponse doCancel(
                                      CancelRequest cancelRequest,
                                      Configuration flinkConfig) throws FlinkException {
        return executeClientAction(
            cancelRequest,
            flinkConfig,
            (jobId, clusterClient) -> toCancelResponse(cancelRequest, jobId, clusterClient));
    }

    /** Triggers a savepoint through the remote standalone cluster client. */
    @Override
    protected SavepointResponse doTriggerSavepoint(
                                                   SavepointRequest savepointRequest,
                                                   Configuration flinkConfig) throws FlinkException {
        return executeClientAction(
            savepointRequest,
            flinkConfig,
            (jobId, clusterClient) -> toSavepointResponse(savepointRequest, jobId, clusterClient));
    }

    /** Submits a job through the standalone cluster's REST API. */
    private SubmitResponse restApiSubmit(
                                         ResolvedSubmitRequest resolved,
                                         Configuration flinkConfig,
                                         File fatJar) throws FlinkException {
        SubmitRequest submitRequest = resolved.request();
        return execute(
            () -> {
                Tuple2<StandaloneClusterId, StandaloneClusterDescriptor> standaloneDescriptor =
                    standaloneClusterDescriptor(flinkConfig);
                StandaloneClusterId clusterId = standaloneDescriptor._1;
                StandaloneClusterDescriptor clusterDescriptor = standaloneDescriptor._2;
                ClusterClient<StandaloneClusterId> client = null;
                try {
                    client = clusterDescriptor.retrieve(clusterId).getClusterClient();
                    String jobId =
                        SessionClusterRestClient.submit(
                            client.getWebInterfaceURL(), fatJar, flinkConfig);
                    logInfo(
                        String.format(
                            "%s mode submit by REST API, WebInterfaceURL %s, jobId: %s",
                            submitRequest.deployMode(), client.getWebInterfaceURL(), jobId));
                    return new SubmitResponse(
                        null, flinkConfig.toMap(), jobId, client.getWebInterfaceURL());
                } finally {
                    closeSubmissionResources(client, clusterDescriptor);
                }
            });
    }

    /** Submits a JobGraph through the standalone cluster client. */
    private SubmitResponse jobGraphSubmit(
                                          ResolvedSubmitRequest resolved,
                                          Configuration flinkConfig,
                                          File jarFile) throws FlinkException {
        Tuple2<StandaloneClusterId, StandaloneClusterDescriptor> standaloneDescriptor =
            standaloneClusterDescriptor(flinkConfig);
        return submitJobGraphToCluster(
            resolved,
            flinkConfig,
            jarFile,
            () -> standaloneDescriptor._2.retrieve(standaloneDescriptor._1).getClusterClient(),
            () -> null,
            standaloneDescriptor._2);
    }

    /** Retrieves a standalone cluster, executes a job action, and closes its client resources. */
    private <O, R extends AbstractSavepointRequest> O executeClientAction(
                                                                          R request,
                                                                          Configuration flinkConfig,
                                                                          ClientAction<O> action) throws FlinkException {
        return execute(
            () -> {
                Map<String, Object> properties = request.properties();
                Object address = properties.get(RestOptions.ADDRESS.key());
                Object port = properties.get(RestOptions.PORT.key());
                AssertUtils.required(address != null, "Remote Flink REST address is not configured");
                AssertUtils.required(port != null, "Remote Flink REST port is not configured");

                flinkConfig.set(DeploymentOptions.TARGET, request.deployMode().getName());
                flinkConfig.set(RestOptions.ADDRESS, address.toString());
                flinkConfig.set(RestOptions.PORT, Integer.parseInt(port.toString()));
                logEffectiveSubmitConfiguration(flinkConfig);
                Tuple2<StandaloneClusterId, StandaloneClusterDescriptor> descriptor =
                    standaloneClusterDescriptor(flinkConfig);
                ClusterClient<StandaloneClusterId> clusterClient = null;
                try {
                    clusterClient = descriptor._2.retrieve(descriptor._1).getClusterClient();
                    return action.apply(JobID.fromHexString(request.jobId()), clusterClient);
                } finally {
                    closeSubmissionResources(clusterClient, descriptor._2);
                }
            },
            e -> {
                logError(
                    "Do " + request.getClass().getSimpleName() + " for flink standalone job fail", e);
            });
    }

    /** Creates the standalone descriptor using the client module's own Flink service loader. */
    private Tuple2<StandaloneClusterId, StandaloneClusterDescriptor> standaloneClusterDescriptor(
                                                                                                 Configuration flinkConfig) {
        // DefaultClusterClientServiceLoader is bound to the Flink version bundled with this module
        // (loaded by this class's own classloader), but the calling thread's context classloader may
        // currently be a target-version shims classloader (see FlinkShimsProxy). Its internal
        // ServiceLoader.load(ClusterClientFactory.class) resolves providers via the context
        // classloader, so leaving it as the shims classloader here would load a ClusterClientFactory
        // implementation from a different Flink version than the interface bundled here, throwing
        // ServiceConfigurationError ("not a subtype"). Force it back to this class's own classloader
        // for the duration of this call.
        return ClassLoaderUtils.runAsClassLoader(
            RemoteClient.class.getClassLoader(),
            () -> {
                DefaultClusterClientServiceLoader serviceLoader = new DefaultClusterClientServiceLoader();
                ClusterClientFactory<StandaloneClusterId> clientFactory =
                    serviceLoader.getClusterClientFactory(flinkConfig);
                StandaloneClusterId standaloneClusterId = clientFactory.getClusterId(flinkConfig);
                StandaloneClusterDescriptor standaloneClusterDescriptor =
                    (StandaloneClusterDescriptor) clientFactory.createClusterDescriptor(flinkConfig);
                return new Tuple2<>(standaloneClusterId, standaloneClusterDescriptor);
            });
    }

    @FunctionalInterface
    private interface ClientAction<O> {

        O apply(JobID jobId, ClusterClient<?> clusterClient) throws FlinkException;
    }
}
