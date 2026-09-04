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
import org.apache.streampark.common.enums.FlinkKubernetesRestExposedType;
import org.apache.streampark.common.util.Tuple2;
import org.apache.streampark.flink.client.bean.ResolvedSubmitRequest;
import org.apache.streampark.flink.client.request.AbstractSavepointRequest;
import org.apache.streampark.flink.client.request.CancelRequest;
import org.apache.streampark.flink.client.request.SavepointRequest;
import org.apache.streampark.flink.client.request.SubmitRequest;
import org.apache.streampark.flink.client.response.CancelResponse;
import org.apache.streampark.flink.client.response.SavepointResponse;
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

/** Base implementation for Kubernetes native deployment clients. */
public abstract class AbstractKubernetesNativeClient extends AbstractFlinkClient {

    /** Adds Kubernetes identity, exposure, pod-template, and configuration-directory options. */
    @Override
    protected void setConfig(ResolvedSubmitRequest resolved, Configuration flinkConfig) {
        SubmitRequest submitRequest = resolved.request();

        if (StringUtils.isNotEmpty(submitRequest.clusterId())) {
            flinkConfig.set(KubernetesConfigOptions.CLUSTER_ID, submitRequest.clusterId());
        }
        if (StringUtils.isNotEmpty(submitRequest.kubernetesNamespace())) {
            flinkConfig.set(
                KubernetesConfigOptions.NAMESPACE, submitRequest.kubernetesNamespace());
        }
        flinkConfig.set(
            KubernetesConfigOptions.REST_SERVICE_EXPOSED_TYPE,
            convertServiceExposedType(submitRequest.flinkRestExposedType()));

        if (submitRequest.buildResult() != null
            && submitRequest.deployMode() == FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION) {
            DockerImageBuildResponse buildResult =
                (DockerImageBuildResponse) submitRequest.buildResult();
            Map<String, String> podTemplatePaths = buildResult.podTemplatePaths();
            if (podTemplatePaths != null) {
                for (Map.Entry<String, String> entry : podTemplatePaths.entrySet()) {
                    if (StringUtils.isEmpty(entry.getValue())) {
                        continue;
                    }
                    if (PodTemplateTool.KUBERNETES_POD_TEMPLATE.key().equals(entry.getKey())) {
                        flinkConfig.set(
                            KubernetesConfigOptions.KUBERNETES_POD_TEMPLATE, entry.getValue());
                    } else if (PodTemplateTool.KUBERNETES_JM_POD_TEMPLATE
                        .key()
                        .equals(entry.getKey())) {
                        flinkConfig.set(
                            KubernetesConfigOptions.JOB_MANAGER_POD_TEMPLATE, entry.getValue());
                    } else if (PodTemplateTool.KUBERNETES_TM_POD_TEMPLATE
                        .key()
                        .equals(entry.getKey())) {
                        flinkConfig.set(
                            KubernetesConfigOptions.TASK_MANAGER_POD_TEMPLATE, entry.getValue());
                    }
                }
            }
        }

        if (!flinkConfig.contains(DeploymentOptionsInternal.CONF_DIR)) {
            flinkConfig.set(
                DeploymentOptionsInternal.CONF_DIR,
                submitRequest.flinkVersion().getFlinkHome() + "/conf");
        }

        if (flinkConfig.get(KubernetesConfigOptions.NAMESPACE).isEmpty()) {
            flinkConfig.removeConfig(KubernetesConfigOptions.NAMESPACE);
        }

        logEffectiveSubmitConfiguration(flinkConfig);
    }

    /** Cancels a Kubernetes job and removes its application cluster when required. */
    @Override
    protected CancelResponse doCancel(
                                      CancelRequest cancelRequest,
                                      Configuration flinkConfig) throws FlinkException {
        return executeClientAction(
            cancelRequest,
            flinkConfig,
            (jobId, client) -> {
                String resp = execute(() -> cancelJob(cancelRequest, jobId, client));
                if (cancelRequest.deployMode() == FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION) {
                    client.shutDownCluster();
                }
                return new CancelResponse(resp);
            });
    }

    /** Triggers a savepoint through the target Kubernetes cluster client. */
    @Override
    protected SavepointResponse doTriggerSavepoint(
                                                   SavepointRequest savepointRequest,
                                                   Configuration flinkConfig) throws FlinkException {
        return executeClientAction(
            savepointRequest,
            flinkConfig,
            (jobId, clusterClient) -> toSavepointResponse(savepointRequest, jobId, clusterClient));
    }

    /** Creates the descriptor and resource specification used for Kubernetes deployment. */
    protected final Tuple2<KubernetesClusterDescriptor, ClusterSpecification> createClusterDescriptorAndSpec(Configuration flinkConfig) {
        KubernetesClusterClientFactory clientFactory = new KubernetesClusterClientFactory();
        KubernetesClusterDescriptor clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig);
        ClusterSpecification clusterSpecification = clientFactory.getClusterSpecification(flinkConfig);
        return new Tuple2<>(clusterDescriptor, clusterSpecification);
    }

    /** Formats the deployment identity included in Kubernetes submission logs. */
    protected String flinkConfIdentifierInfo(@Nonnull Configuration conf) {
        return "deployMode="
            + conf.get(DeploymentOptions.TARGET)
            + ", clusterId="
            + conf.get(KubernetesConfigOptions.CLUSTER_ID)
            + ", "
            + "namespace="
            + conf.get(KubernetesConfigOptions.NAMESPACE);
    }

    /** Resolves an explicit kubeconfig path or the current user's default path. */
    protected final String getDefaultKubernetesConf(String k8sConf) {
        String homePath = System.getProperty("user.home");
        if (k8sConf != null) {
            return k8sConf.replace("~", homePath);
        }
        return homePath.concat("/.kube/config");
    }

    /** Executes a job action against the cluster identified by the serialized request. */
    private <O> O executeClientAction(
                                      AbstractSavepointRequest request,
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

        flinkConfig.set(KubernetesConfigOptions.CLUSTER_ID, request.clusterId());
        flinkConfig.set(KubernetesConfigOptions.NAMESPACE, request.kubernetesNamespace());

        KubernetesClusterDescriptor clusterDescriptor = null;
        ClusterClient<String> client = null;

        try {
            clusterDescriptor = getClusterDescriptor(flinkConfig);
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
            throw mapException(e);
        } finally {
            if (client != null) {
                client.close();
            }
            if (clusterDescriptor != null) {
                clusterDescriptor.close();
            }
        }
    }

    /** Logs the complete cluster identity when a Kubernetes job action fails. */
    private void logClientActionFailure(
                                        String hints,
                                        Configuration flinkConfig,
                                        AbstractSavepointRequest request,
                                        Exception e) {
        logError(
            hints
                + " mode="
                + flinkConfig.get(DeploymentOptions.TARGET)
                + ", request="
                + request,
            e);
    }

    /** Maps StreamPark's stable REST exposure enum to the target Flink enum. */
    private ServiceExposedType convertServiceExposedType(FlinkKubernetesRestExposedType exposedType) {
        if (exposedType == FlinkKubernetesRestExposedType.CLUSTER_IP) {
            return ServiceExposedType.ClusterIP;
        }
        if (exposedType == FlinkKubernetesRestExposedType.NODE_PORT) {
            return ServiceExposedType.NodePort;
        }
        if (exposedType == FlinkKubernetesRestExposedType.LOAD_BALANCER) {
            return ServiceExposedType.LoadBalancer;
        }
        return ServiceExposedType.LoadBalancer;
    }

    /** Creates a descriptor for an existing Kubernetes cluster operation. */
    private final KubernetesClusterDescriptor getClusterDescriptor(
                                                                   Configuration flinkConfig) {
        KubernetesClusterClientFactory clientFactory = new KubernetesClusterClientFactory();
        return clientFactory.createClusterDescriptor(flinkConfig);
    }

    @FunctionalInterface
    private interface ClientAction<O> {

        O apply(JobID jobId, ClusterClient<?> clusterClient) throws FlinkException;
    }
}
