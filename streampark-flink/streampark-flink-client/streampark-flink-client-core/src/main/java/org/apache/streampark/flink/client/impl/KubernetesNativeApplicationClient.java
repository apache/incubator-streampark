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
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.StreamParkLoggerFactory;
import org.apache.streampark.flink.client.bean.CancelRequest;
import org.apache.streampark.flink.client.bean.CancelResponse;
import org.apache.streampark.flink.client.bean.SavepointResponse;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.client.bean.TriggerSavepointRequest;
import org.apache.streampark.flink.client.trait.KubernetesNativeClientTrait;
import org.apache.streampark.flink.client.util.FlinkConfigurationEnhancer;
import org.apache.streampark.flink.packer.pipeline.DockerImageBuildResponse;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.client.deployment.application.ApplicationConfiguration;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions;

import com.google.common.collect.Lists;

/** Kubernetes native application mode submit. */
public final class KubernetesNativeApplicationClient extends KubernetesNativeClientTrait {

    public static final KubernetesNativeApplicationClient INSTANCE =
        new KubernetesNativeApplicationClient();

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory()
            .getLogger(KubernetesNativeApplicationClient.class.getName());

    private KubernetesNativeApplicationClient() {
    }

    @Override
    public SubmitResponse doSubmit(SubmitRequest submitRequest, Configuration flinkConfig) throws Exception {
        AssertUtils.required(
            StringUtils.isNotBlank(submitRequest.getClusterId()),
            "[flink-submit] submit flink job failed, clusterId is null, mode="
                + flinkConfig.get(DeploymentOptions.TARGET));

        submitRequest.checkBuildResult();

        DockerImageBuildResponse buildResult =
            (DockerImageBuildResponse) submitRequest.getBuildResult();

        FlinkConfigurationEnhancer.safeSet(
            flinkConfig,
            PipelineOptions.JARS,
            Lists.newArrayList(buildResult.dockerInnerMainJarPath()));
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, KubernetesConfigOptions.CONTAINER_IMAGE, buildResult.flinkImageTag());

        K8sClusterDescriptorAndSpecification descriptorResult =
            getK8sClusterDescriptorAndSpecification(flinkConfig);
        ApplicationConfiguration applicationConfig =
            ApplicationConfiguration.fromConfiguration(flinkConfig);
        ClusterClient<String> clusterClient =
            descriptorResult.clusterDescriptor
                .deployApplicationCluster(
                    descriptorResult.clusterSpecification, applicationConfig)
                .getClusterClient();

        String clusterId = clusterClient.getClusterId();
        SubmitResponse result =
            SubmitResponse.builder()
                .clusterId(clusterId)
                .flinkConfig(flinkConfig.toMap())
                .jobId(submitRequest.getJobId())
                .jobManagerUrl(clusterClient.getWebInterfaceURL())
                .build();
        LOG.info(
            "[flink-submit] flink job has been submitted. {}",
            flinkConfIdentifierInfo(flinkConfig));

        closeSubmit(submitRequest, descriptorResult.clusterDescriptor, clusterClient);
        return result;
    }

    @Override
    public CancelResponse doCancel(CancelRequest cancelRequest, Configuration flinkConf) throws Exception {
        FlinkConfigurationEnhancer.safeSet(
            flinkConf, DeploymentOptions.TARGET, FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION.getName());
        return executeClientAction(
            cancelRequest,
            flinkConf,
            (jobId, client) -> {
                String resp = cancelJob(cancelRequest, jobId, client);
                client.shutDownCluster();
                return new CancelResponse(resp);
            });
    }

    @Override
    public SavepointResponse doTriggerSavepoint(
                                                TriggerSavepointRequest request,
                                                Configuration flinkConf) throws Exception {
        FlinkConfigurationEnhancer.safeSet(
            flinkConf, DeploymentOptions.TARGET, FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION.getName());
        return super.doTriggerSavepoint(request, flinkConf);
    }
}
