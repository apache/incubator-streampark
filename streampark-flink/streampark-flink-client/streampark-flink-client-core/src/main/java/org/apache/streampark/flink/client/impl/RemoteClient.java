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
import org.apache.streampark.flink.client.bean.SavepointRequestTrait;
import org.apache.streampark.flink.client.bean.SavepointResponse;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.client.bean.TriggerSavepointRequest;
import org.apache.streampark.flink.client.tool.FlinkSessionSubmitHelper;
import org.apache.streampark.flink.client.trait.FlinkClientTrait;

import org.apache.flink.api.common.JobID;
import org.apache.flink.client.deployment.ClusterClientFactory;
import org.apache.flink.client.deployment.DefaultClusterClientServiceLoader;
import org.apache.flink.client.deployment.StandaloneClusterDescriptor;
import org.apache.flink.client.deployment.StandaloneClusterId;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.util.FlinkException;

import java.io.File;

import scala.Tuple2;

/** Submit Flink jobs to a remote standalone cluster. */
public final class RemoteClient extends FlinkClientTrait {

    public static final RemoteClient INSTANCE = new RemoteClient();

    private RemoteClient() {
    }

    @Override
    public void setConfig(SubmitRequest submitRequest, Configuration flinkConfig) {
        // no extra configuration
    }

    @Override
    public SubmitResponse doSubmit(SubmitRequest submitRequest, Configuration flinkConfig)
        throws FlinkException {
        try {
            return trySubmit(
                submitRequest,
                flinkConfig,
                submitRequest.userJarFile(),
                this::jobGraphSubmit,
                this::restApiSubmit);
        } catch (FlinkException e) {
            throw e;
        } catch (Exception e) {
            throw asFlinkException(e);
        }
    }

    @Override
    public CancelResponse doCancel(CancelRequest cancelRequest, Configuration flinkConfig)
        throws FlinkException {
        try {
            return executeClientAction(
                cancelRequest,
                flinkConfig,
                (jobId, clusterClient) -> {
                    try {
                        return new CancelResponse(cancelJob(cancelRequest, jobId, clusterClient));
                    } catch (FlinkException e) {
                        throw e;
                    } catch (Exception e) {
                        throw asFlinkException(e);
                    }
                });
        } catch (FlinkException e) {
            throw e;
        } catch (Exception e) {
            throw asFlinkException(e);
        }
    }

    @Override
    public SavepointResponse doTriggerSavepoint(
                                                TriggerSavepointRequest savepointRequest,
                                                Configuration flinkConfig) throws FlinkException {
        try {
            return executeClientAction(
                savepointRequest,
                flinkConfig,
                (jobId, clusterClient) -> {
                    try {
                        return new SavepointResponse(
                            triggerSavepoint(savepointRequest, jobId, clusterClient));
                    } catch (FlinkException e) {
                        throw e;
                    } catch (Exception e) {
                        throw asFlinkException(e);
                    }
                });
        } catch (FlinkException e) {
            throw e;
        } catch (Exception e) {
            throw asFlinkException(e);
        }
    }

    /** Submit flink session job via rest api. */
    public SubmitResponse restApiSubmit(
                                        SubmitRequest submitRequest,
                                        Configuration flinkConfig,
                                        File fatJar) throws FlinkException {
        try {
        Tuple2<StandaloneClusterId, StandaloneClusterDescriptor> standAloneDescriptor =
            getStandAloneClusterDescriptor(flinkConfig);
        StandaloneClusterId yarnClusterId = standAloneDescriptor._1();
        StandaloneClusterDescriptor clusterDescriptor = standAloneDescriptor._2();

        ClusterClient<StandaloneClusterId> client =
            clusterDescriptor.retrieve(yarnClusterId).getClusterClient();
        String jobId =
            FlinkSessionSubmitHelper.submitViaRestApi(
                client.getWebInterfaceURL(), fatJar, flinkConfig);
        logInfo(
            String.format(
                "%s mode submit by restApi, WebInterfaceURL %s, jobId: %s",
                submitRequest.deployMode(), client.getWebInterfaceURL(), jobId));
        SubmitResponse resp =
            new SubmitResponse(null, flinkConfig.toMap(), jobId, client.getWebInterfaceURL());
        closeSubmit(submitRequest, client, clusterDescriptor);
        return resp;
        } catch (FlinkException e) {
            throw e;
        } catch (Exception e) {
            throw asFlinkException(e);
        }
    }

    /** Submit flink session job with building JobGraph via Standalone ClusterClient api. */
    public SubmitResponse jobGraphSubmit(
                                         SubmitRequest submitRequest,
                                         Configuration flinkConfig,
                                         File jarFile) throws FlinkException {
        try {
        Tuple2<StandaloneClusterId, StandaloneClusterDescriptor> standAloneDescriptor =
            getStandAloneClusterDescriptor(flinkConfig);
        StandaloneClusterDescriptor clusterDescriptor = standAloneDescriptor._2();

        Tuple2<PackagedProgram, JobGraph> packageProgramJobGraph =
            getJobGraph(flinkConfig, submitRequest, jarFile);
        PackagedProgram packageProgram = packageProgramJobGraph._1();
        JobGraph jobGraph = packageProgramJobGraph._2();

        ClusterClient<StandaloneClusterId> client =
            clusterDescriptor.retrieve(standAloneDescriptor._1()).getClusterClient();
        String jobId = client.submitJob(jobGraph).get().toString();
        logInfo(
            String.format(
                "%s mode submit by jobGraph, WebInterfaceURL %s, jobId: %s",
                submitRequest.deployMode(), client.getWebInterfaceURL(), jobId));
        SubmitResponse result =
            new SubmitResponse(null, flinkConfig.toMap(), jobId, client.getWebInterfaceURL());
        closeSubmit(submitRequest, packageProgram, client, clusterDescriptor);
        return result;
        } catch (FlinkException e) {
            throw e;
        } catch (Exception e) {
            throw asFlinkException(e);
        }
    }

    private <O, R extends SavepointRequestTrait> O executeClientAction(
                                                                       R request,
                                                                       Configuration flinkConfig,
                                                                       ClientAction<O> actFunc) throws FlinkException {
        ClusterClient<StandaloneClusterId> client = null;
        Tuple2<StandaloneClusterId, StandaloneClusterDescriptor> standAloneDescriptor = null;
        try {
            FlinkConfigurationOps.safeSet(
                flinkConfig, DeploymentOptions.TARGET, request.deployMode().getName());
            FlinkConfigurationOps.safeSet(
                flinkConfig,
                RestOptions.ADDRESS,
                request.properties().get(RestOptions.ADDRESS.key()).toString());
            FlinkConfigurationOps.safeSet(
                flinkConfig,
                RestOptions.PORT,
                Integer.parseInt(
                    request.properties().get(RestOptions.PORT.key()).toString()));
            logInfo(
                String.format(
                    "%n------------------------------------------------------------------%n"
                        + "Effective submit configuration: %s%n"
                        + "------------------------------------------------------------------%n",
                    flinkConfig));
            standAloneDescriptor = getStandAloneClusterDescriptor(flinkConfig);
            client =
                standAloneDescriptor._2().retrieve(standAloneDescriptor._1()).getClusterClient();
            return actFunc.apply(JobID.fromHexString(request.jobId()), client);
        } catch (FlinkException e) {
            logError("Do " + request.getClass().getSimpleName() + " for flink standalone job fail", e);
            throw e;
        } catch (Exception e) {
            logError("Do " + request.getClass().getSimpleName() + " for flink standalone job fail", e);
            throw asFlinkException(e);
        } finally {
            if (client != null) {
                client.close();
            }
            if (standAloneDescriptor != null) {
                standAloneDescriptor._2().close();
            }
        }
    }

    private Tuple2<StandaloneClusterId, StandaloneClusterDescriptor> getStandAloneClusterDescriptor(
                                                                                                    Configuration flinkConfig) {
        DefaultClusterClientServiceLoader serviceLoader = new DefaultClusterClientServiceLoader();
        ClusterClientFactory<StandaloneClusterId> clientFactory =
            serviceLoader.getClusterClientFactory(flinkConfig);
        StandaloneClusterId standaloneClusterId = clientFactory.getClusterId(flinkConfig);
        StandaloneClusterDescriptor standaloneClusterDescriptor =
            (StandaloneClusterDescriptor) clientFactory.createClusterDescriptor(flinkConfig);
        return new Tuple2<>(standaloneClusterId, standaloneClusterDescriptor);
    }

    @FunctionalInterface
    private interface ClientAction<O> {

        O apply(JobID jobId, ClusterClient<?> clusterClient) throws FlinkException;
    }
}
