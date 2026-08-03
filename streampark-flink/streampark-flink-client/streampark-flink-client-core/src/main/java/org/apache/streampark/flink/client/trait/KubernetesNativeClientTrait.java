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

package org.apache.streampark.flink.client.trait;

import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.enums.FlinkK8sRestExposedType;
import org.apache.streampark.flink.client.bean.CancelRequest;
import org.apache.streampark.flink.client.bean.CancelResponse;
import org.apache.streampark.flink.client.bean.SavepointRequestTrait;
import org.apache.streampark.flink.client.bean.SavepointResponse;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.TriggerSavepointRequest;
import org.apache.streampark.flink.kubernetes.PodTemplateTool;
import org.apache.streampark.flink.packer.pipeline.DockerImageBuildResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.JobID;
import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.DeploymentOptionsInternal;
import org.apache.flink.kubernetes.KubernetesClusterClientFactory;
import org.apache.flink.kubernetes.KubernetesClusterDescriptor;
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions;
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions.ServiceExposedType;
import org.apache.flink.util.FlinkException;

import javax.annotation.Nonnull;

import java.util.Map;

import scala.Tuple2;

/** Kubernetes native mode submit support. */
public abstract class KubernetesNativeClientTrait extends FlinkClientTrait {

    @Override
    public void setConfig(SubmitRequest submitRequest, Configuration flinkConfig) {
        FlinkClientTrait.safeSet(
            flinkConfig, KubernetesConfigOptions.CLUSTER_ID, submitRequest.clusterId());
        FlinkClientTrait.safeSet(
            flinkConfig, KubernetesConfigOptions.NAMESPACE, submitRequest.kubernetesNamespace());
        FlinkClientTrait.safeSet(
            flinkConfig,
            KubernetesConfigOptions.REST_SERVICE_EXPOSED_TYPE,
            covertToServiceExposedType(submitRequest.flinkRestExposedType()));

        if (submitRequest.buildResult() != null
            && submitRequest.deployMode() == FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION) {
            DockerImageBuildResponse buildResult =
                (DockerImageBuildResponse) submitRequest.buildResult();
            Map<String, String> podTemplatePaths = buildResult.podTemplatePaths();
            if (podTemplatePaths != null) {
                for (Map.Entry<String, String> entry : podTemplatePaths.entrySet()) {
                    if (PodTemplateTool.KUBERNETES_POD_TEMPLATE.key().equals(entry.getKey())) {
                        FlinkClientTrait.safeSet(
                            flinkConfig,
                            KubernetesConfigOptions.KUBERNETES_POD_TEMPLATE,
                            entry.getValue());
                    } else if (PodTemplateTool.KUBERNETES_JM_POD_TEMPLATE
                        .key()
                        .equals(entry.getKey())) {
                        FlinkClientTrait.safeSet(
                            flinkConfig,
                            KubernetesConfigOptions.JOB_MANAGER_POD_TEMPLATE,
                            entry.getValue());
                    } else if (PodTemplateTool.KUBERNETES_TM_POD_TEMPLATE
                        .key()
                        .equals(entry.getKey())) {
                        FlinkClientTrait.safeSet(
                            flinkConfig,
                            KubernetesConfigOptions.TASK_MANAGER_POD_TEMPLATE,
                            entry.getValue());
                    }
                }
            }
        }

        if (!flinkConfig.contains(DeploymentOptionsInternal.CONF_DIR)) {
            FlinkClientTrait.safeSet(
                flinkConfig,
                DeploymentOptionsInternal.CONF_DIR,
                submitRequest.flinkVersion().getFlinkHome() + "/conf");
        }

        if (flinkConfig.get(KubernetesConfigOptions.NAMESPACE).isEmpty()) {
            flinkConfig.removeConfig(KubernetesConfigOptions.NAMESPACE);
        }

        logEffectiveSubmitConfiguration(flinkConfig);
    }

    protected void setK8sDeployTarget(Configuration flinkConf, FlinkDeployMode deployMode) {
        FlinkClientTrait.safeSet(flinkConf, DeploymentOptions.TARGET, deployMode.getName());
    }

    @Override
    public CancelResponse doCancel(CancelRequest cancelRequest, Configuration flinkConfig) throws FlinkException {
        return executeClientAction(
            cancelRequest,
            flinkConfig,
            (jobId, client) -> {
                String resp = callAsFlinkException(() -> cancelJob(cancelRequest, jobId, client));
                if (cancelRequest.deployMode() == FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION) {
                    client.shutDownCluster();
                }
                return new CancelResponse(resp);
            });
    }

    @Override
    public SavepointResponse doTriggerSavepoint(
                                                TriggerSavepointRequest savepointRequest,
                                                Configuration flinkConfig) throws FlinkException {
        return executeClientAction(
            savepointRequest,
            flinkConfig,
            (jobId, clusterClient) -> toSavepointResponse(savepointRequest, jobId, clusterClient));
    }

    public <O> O executeClientAction(
                                     SavepointRequestTrait request,
                                     Configuration flinkConfig,
                                     ClientAction<O> actFunc) throws FlinkException {
        String hints =
            "[flink-client] execute " + request.getClass().getSimpleName() + " for flink job failed,";
        if (StringUtils.isBlank(request.clusterId())) {
            throw new IllegalArgumentException(
                hints
                    + " clusterId is null, mode="
                    + flinkConfig.get(DeploymentOptions.TARGET));
        }

        FlinkClientTrait.safeSet(
            flinkConfig, KubernetesConfigOptions.CLUSTER_ID, request.clusterId());
        FlinkClientTrait.safeSet(
            flinkConfig, KubernetesConfigOptions.NAMESPACE, request.kubernetesNamespace());

        KubernetesClusterDescriptor clusterDescriptor = null;
        ClusterClient<String> client = null;

        try {
            clusterDescriptor = getK8sClusterDescriptor(flinkConfig);
            client =
                clusterDescriptor
                    .retrieve(
                        flinkConfig
                            .getOptional(KubernetesConfigOptions.CLUSTER_ID)
                            .orElseThrow(
                                () -> new IllegalStateException(
                                    "Kubernetes cluster id is not configured")))
                    .getClusterClient();
            return actFunc.apply(JobID.fromHexString(request.jobId()), client);
        } catch (FlinkException e) {
            logClientActionFailure(hints, flinkConfig, request, e);
            throw e;
        } catch (Exception e) {
            logClientActionFailure(hints, flinkConfig, request, e);
            throw asFlinkException(e);
        } finally {
            if (client != null) {
                client.close();
            }
            if (clusterDescriptor != null) {
                clusterDescriptor.close();
            }
        }
    }

    private void logClientActionFailure(
                                        String hints,
                                        Configuration flinkConfig,
                                        SavepointRequestTrait request,
                                        Exception e) {
        logError(
            hints
                + " mode="
                + flinkConfig.get(DeploymentOptions.TARGET)
                + ", request="
                + request,
            e);
    }

    public Tuple2<KubernetesClusterDescriptor, ClusterSpecification> getK8sClusterDescriptorAndSpecification(Configuration flinkConfig) {
        KubernetesClusterClientFactory clientFactory = new KubernetesClusterClientFactory();
        KubernetesClusterDescriptor clusterDescriptor =
            clientFactory.createClusterDescriptor(flinkConfig);
        ClusterSpecification clusterSpecification =
            clientFactory.getClusterSpecification(flinkConfig);
        return new Tuple2<>(clusterDescriptor, clusterSpecification);
    }

    public KubernetesClusterDescriptor getK8sClusterDescriptor(Configuration flinkConfig) {
        KubernetesClusterClientFactory clientFactory = new KubernetesClusterClientFactory();
        return clientFactory.createClusterDescriptor(flinkConfig);
    }

    protected String flinkConfIdentifierInfo(@Nonnull Configuration conf) {
        return "deployMode="
            + conf.get(DeploymentOptions.TARGET)
            + ", clusterId="
            + conf.get(KubernetesConfigOptions.CLUSTER_ID)
            + ", "
            + "namespace="
            + conf.get(KubernetesConfigOptions.NAMESPACE);
    }

    public String getDefaultKubernetesConf(String k8sConf) {
        String homePath = System.getProperty("user.home");
        if (k8sConf != null) {
            return k8sConf.replace("~", homePath);
        }
        return homePath.concat("/.kube/config");
    }

    private ServiceExposedType covertToServiceExposedType(FlinkK8sRestExposedType exposedType) {
        if (exposedType == FlinkK8sRestExposedType.CLUSTER_IP) {
            return ServiceExposedType.ClusterIP;
        }
        if (exposedType == FlinkK8sRestExposedType.NODE_PORT) {
            return ServiceExposedType.NodePort;
        }
        if (exposedType == FlinkK8sRestExposedType.LOAD_BALANCER) {
            return ServiceExposedType.LoadBalancer;
        }
        return ServiceExposedType.LoadBalancer;
    }

    @FunctionalInterface
    protected interface ClientAction<O> {

        O apply(JobID jobId, ClusterClient<?> clusterClient) throws FlinkException;
    }
}
