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

import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.util.Tuple2;
import org.apache.streampark.flink.client.bean.ResolvedSubmitRequest;
import org.apache.streampark.flink.client.request.CancelRequest;
import org.apache.streampark.flink.client.request.SavepointRequest;
import org.apache.streampark.flink.client.request.SubmitRequest;
import org.apache.streampark.flink.client.response.CancelResponse;
import org.apache.streampark.flink.client.response.SavepointResponse;
import org.apache.streampark.flink.client.response.SubmitResponse;
import org.apache.streampark.flink.packer.pipeline.DockerImageBuildResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.client.deployment.application.ApplicationConfiguration;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.kubernetes.KubernetesClusterDescriptor;
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions;
import org.apache.flink.util.FlinkException;

import com.google.common.collect.Lists;

/** Submits Flink jobs to application-scoped Kubernetes clusters. */
public final class KubernetesNativeApplicationClient extends AbstractKubernetesNativeClient {

    public static final KubernetesNativeApplicationClient INSTANCE =
        new KubernetesNativeApplicationClient();

    private KubernetesNativeApplicationClient() {
    }

    /** Deploys an application cluster from the image produced by the build pipeline. */
    @Override
    protected SubmitResponse doSubmit(ResolvedSubmitRequest resolved,
                                      Configuration flinkConfig) throws FlinkException {
        SubmitRequest submitRequest = resolved.request();

        if (StringUtils.isBlank(submitRequest.clusterId())) {
            throw new IllegalArgumentException(
                String.format(
                    "[flink-submit] submit flink job failed, clusterId is null, mode=%s",
                    flinkConfig.get(DeploymentOptions.TARGET)));
        }

        return execute(
            () -> {
                DockerImageBuildResponse buildResult =
                    (DockerImageBuildResponse) submitRequest.buildResult();

                flinkConfig.set(
                    PipelineOptions.JARS,
                    Lists.newArrayList(buildResult.dockerInnerMainJarPath()));

                flinkConfig.set(
                    KubernetesConfigOptions.CONTAINER_IMAGE, buildResult.flinkImageTag());

                Tuple2<KubernetesClusterDescriptor, ClusterSpecification> descriptorAndSpec =
                    createClusterDescriptorAndSpec(flinkConfig);

                KubernetesClusterDescriptor clusterDescriptor = descriptorAndSpec._1;
                ClusterSpecification clusterSpecification = descriptorAndSpec._2;
                ClusterClient<String> clusterClient = null;
                try {
                    ApplicationConfiguration applicationConfig =
                        ApplicationConfiguration.fromConfiguration(flinkConfig);

                    clusterClient =
                        clusterDescriptor
                            .deployApplicationCluster(clusterSpecification, applicationConfig)
                            .getClusterClient();

                    logInfo("[flink-submit] Flink job submitted. " + flinkConfIdentifierInfo(flinkConfig));
                    return new SubmitResponse(
                        clusterClient.getClusterId(),
                        flinkConfig.toMap(),
                        submitRequest.jobId(),
                        clusterClient.getWebInterfaceURL());
                } finally {
                    closeSubmissionResources(clusterClient, clusterDescriptor);
                }
            });
    }

    /** Cancels the job and removes its application-scoped Kubernetes cluster. */
    @Override
    protected CancelResponse doCancel(CancelRequest cancelRequest,
                                      Configuration flinkConf) throws FlinkException {
        flinkConf.set(
            DeploymentOptions.TARGET,
            FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION.getName());
        return super.doCancel(cancelRequest, flinkConf);
    }

    /** Triggers a savepoint through the application-scoped Kubernetes cluster. */
    @Override
    protected SavepointResponse doTriggerSavepoint(SavepointRequest request,
                                                   Configuration flinkConf) throws FlinkException {
        flinkConf.set(
            DeploymentOptions.TARGET,
            FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION.getName());
        return super.doTriggerSavepoint(request, flinkConf);
    }
}
