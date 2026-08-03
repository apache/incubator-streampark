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
import org.apache.streampark.flink.client.bean.CancelRequest;
import org.apache.streampark.flink.client.bean.CancelResponse;
import org.apache.streampark.flink.client.bean.SavepointResponse;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.client.bean.TriggerSavepointRequest;
import org.apache.streampark.flink.client.trait.KubernetesNativeClientTrait;
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

import scala.Tuple2;

/** Kubernetes native application mode submit. */
public final class KubernetesNativeApplicationClient extends KubernetesNativeClientTrait {

    public static final KubernetesNativeApplicationClient INSTANCE =
        new KubernetesNativeApplicationClient();

    private KubernetesNativeApplicationClient() {
    }

    @Override
    public SubmitResponse doSubmit(SubmitRequest submitRequest, Configuration flinkConfig) throws FlinkException {
        if (StringUtils.isBlank(submitRequest.clusterId())) {
            throw new IllegalArgumentException(
                String.format(
                    "[flink-submit] submit flink job failed, clusterId is null, mode=%s",
                    flinkConfig.get(DeploymentOptions.TARGET)));
        }

        return callAsFlinkException(
            () -> {
                submitRequest.checkBuildResult();

                DockerImageBuildResponse buildResult =
                    (DockerImageBuildResponse) submitRequest.buildResult();

                FlinkConfigurationOps.safeSet(
                    flinkConfig,
                    PipelineOptions.JARS,
                    Lists.newArrayList(buildResult.dockerInnerMainJarPath()));
                FlinkConfigurationOps.safeSet(
                    flinkConfig, KubernetesConfigOptions.CONTAINER_IMAGE, buildResult.flinkImageTag());

                Tuple2<KubernetesClusterDescriptor, ClusterSpecification> descriptorAndSpec =
                    getK8sClusterDescriptorAndSpecification(flinkConfig);
                KubernetesClusterDescriptor clusterDescriptor = descriptorAndSpec._1();
                ClusterSpecification clusterSpecification = descriptorAndSpec._2();

                ApplicationConfiguration applicationConfig =
                    ApplicationConfiguration.fromConfiguration(flinkConfig);
                ClusterClient<String> clusterClient =
                    clusterDescriptor
                        .deployApplicationCluster(clusterSpecification, applicationConfig)
                        .getClusterClient();

                String clusterId = clusterClient.getClusterId();
                SubmitResponse result =
                    new SubmitResponse(
                        clusterId,
                        flinkConfig.toMap(),
                        submitRequest.jobId(),
                        clusterClient.getWebInterfaceURL());
                logInfo(
                    "[flink-submit] flink job has been submitted. "
                        + flinkConfIdentifierInfo(flinkConfig));

                closeSubmit(submitRequest, clusterDescriptor, clusterClient);
                return result;
            });
    }

    @Override
    public CancelResponse doCancel(CancelRequest cancelRequest, Configuration flinkConf) throws FlinkException {
        setK8sDeployTarget(flinkConf, FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION);
        return super.doCancel(cancelRequest, flinkConf);
    }

    @Override
    public SavepointResponse doTriggerSavepoint(
                                                TriggerSavepointRequest request,
                                                Configuration flinkConf) throws FlinkException {
        setK8sDeployTarget(flinkConf, FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION);
        return super.doTriggerSavepoint(request, flinkConf);
    }
}
