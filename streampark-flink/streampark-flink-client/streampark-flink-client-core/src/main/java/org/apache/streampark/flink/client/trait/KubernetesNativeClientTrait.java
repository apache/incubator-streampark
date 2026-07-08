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
import org.apache.streampark.common.util.StreamParkLoggerFactory;
import org.apache.streampark.flink.client.bean.CancelRequest;
import org.apache.streampark.flink.client.bean.CancelResponse;
import org.apache.streampark.flink.client.bean.SavepointRequestTrait;
import org.apache.streampark.flink.client.bean.SavepointResponse;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.TriggerSavepointRequest;
import org.apache.streampark.flink.client.util.FlinkConfigurationEnhancer;
import org.apache.streampark.flink.kubernetes.PodTemplateTool;
import org.apache.streampark.flink.packer.pipeline.DockerImageBuildResponse;

import org.apache.streampark.shaded.org.slf4j.Logger;

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

import javax.annotation.Nonnull;

import java.util.Map;

/** Kubernetes native mode submit. */
public abstract class KubernetesNativeClientTrait extends FlinkClientTrait {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory()
            .getLogger(KubernetesNativeClientTrait.class.getName());

    @Override
    public void setConfig(SubmitRequest submitRequest, Configuration flinkConfig) {
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, KubernetesConfigOptions.CLUSTER_ID, submitRequest.getClusterId());
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, KubernetesConfigOptions.NAMESPACE, submitRequest.getKubernetesNamespace());
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig,
            KubernetesConfigOptions.REST_SERVICE_EXPOSED_TYPE,
            covertToServiceExposedType(submitRequest.getFlinkRestExposedType()));

        if (submitRequest.getBuildResult() != null
            && submitRequest.getDeployMode() == FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION) {
            DockerImageBuildResponse buildResult =
                (DockerImageBuildResponse) submitRequest.getBuildResult();
            for (Map.Entry<String, String> entry : buildResult.podTemplatePaths().entrySet()) {
                if (PodTemplateTool.KUBERNETES_POD_TEMPLATE.key().equals(entry.getKey())) {
                    FlinkConfigurationEnhancer.safeSet(
                        flinkConfig, KubernetesConfigOptions.KUBERNETES_POD_TEMPLATE, entry.getValue());
                } else if (PodTemplateTool.KUBERNETES_JM_POD_TEMPLATE.key().equals(entry.getKey())) {
                    FlinkConfigurationEnhancer.safeSet(
                        flinkConfig, KubernetesConfigOptions.JOB_MANAGER_POD_TEMPLATE, entry.getValue());
                } else if (PodTemplateTool.KUBERNETES_TM_POD_TEMPLATE.key().equals(entry.getKey())) {
                    FlinkConfigurationEnhancer.safeSet(
                        flinkConfig, KubernetesConfigOptions.TASK_MANAGER_POD_TEMPLATE, entry.getValue());
                }
            }
        }

        if (!flinkConfig.contains(DeploymentOptionsInternal.CONF_DIR)) {
            FlinkConfigurationEnhancer.safeSet(
                flinkConfig,
                DeploymentOptionsInternal.CONF_DIR,
                submitRequest.getFlinkVersion().flinkHome + "/conf");
        }

        if (flinkConfig.get(KubernetesConfigOptions.NAMESPACE).isEmpty()) {
            flinkConfig.removeConfig(KubernetesConfigOptions.NAMESPACE);
        }

        LOG.info(
            "\n------------------------------------------------------------------\n"
                + "Effective submit configuration: {}\n"
                + "------------------------------------------------------------------\n",
            flinkConfig);
    }

    @Override
    public CancelResponse doCancel(CancelRequest cancelRequest, Configuration flinkConfig) throws Exception {
        return executeClientAction(
            cancelRequest,
            flinkConfig,
            (jobId, client) -> {
                String resp = cancelJob(cancelRequest, jobId, client);
                if (cancelRequest.getDeployMode() == FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION) {
                    client.shutDownCluster();
                }
                return new CancelResponse(resp);
            });
    }

    protected <O, R extends SavepointRequestTrait> O executeClientAction(
                                                                         R request, Configuration flinkConfig,
                                                                         ClusterClientAction<O, String> actFunc) throws Exception {
        String hints =
            "[flink-client] execute " + request.getClass().getSimpleName() + " for flink job failed,";
        if (StringUtils.isBlank(request.getClusterId())) {
            throw new IllegalArgumentException(
                hints
                    + " clusterId is null, mode="
                    + flinkConfig.get(DeploymentOptions.TARGET));
        }

        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, KubernetesConfigOptions.CLUSTER_ID, request.getClusterId());
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, KubernetesConfigOptions.NAMESPACE, request.getKubernetesNamespace());

        KubernetesClusterDescriptor clusterDescriptor = null;
        ClusterClient<String> client = null;
        try {
            clusterDescriptor = getK8sClusterDescriptor(flinkConfig);
            client =
                clusterDescriptor
                    .retrieve(flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID))
                    .getClusterClient();
            return actFunc.apply(JobID.fromHexString(request.getJobId()), client);
        } catch (Exception e) {
            LOG.error(
                "{} mode={}, request={}",
                hints,
                flinkConfig.get(DeploymentOptions.TARGET),
                request);
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

    protected K8sClusterDescriptorAndSpecification getK8sClusterDescriptorAndSpecification(
                                                                                           Configuration flinkConfig) throws Exception {
        KubernetesClusterClientFactory clientFactory = new KubernetesClusterClientFactory();
        KubernetesClusterDescriptor clusterDescriptor = clientFactory.createClusterDescriptor(flinkConfig);
        ClusterSpecification clusterSpecification = clientFactory.getClusterSpecification(flinkConfig);
        return new K8sClusterDescriptorAndSpecification(clusterDescriptor, clusterSpecification);
    }

    protected KubernetesClusterDescriptor getK8sClusterDescriptor(Configuration flinkConfig) throws Exception {
        KubernetesClusterClientFactory clientFactory = new KubernetesClusterClientFactory();
        return clientFactory.createClusterDescriptor(flinkConfig);
    }

    protected String flinkConfIdentifierInfo(@Nonnull Configuration conf) {
        return "deployMode="
            + conf.get(DeploymentOptions.TARGET)
            + ", clusterId="
            + conf.get(KubernetesConfigOptions.CLUSTER_ID)
            + ", namespace="
            + conf.get(KubernetesConfigOptions.NAMESPACE);
    }

    private ServiceExposedType covertToServiceExposedType(FlinkK8sRestExposedType exposedType) {
        if (exposedType == FlinkK8sRestExposedType.CLUSTER_IP) {
            return ServiceExposedType.ClusterIP;
        }
        if (exposedType == FlinkK8sRestExposedType.LOAD_BALANCER) {
            return ServiceExposedType.LoadBalancer;
        }
        if (exposedType == FlinkK8sRestExposedType.NODE_PORT) {
            return ServiceExposedType.NodePort;
        }
        return ServiceExposedType.LoadBalancer;
    }

    protected String getDefaultKubernetesConf(String k8sConf) {
        String homePath = System.getProperty("user.home");
        if (k8sConf != null) {
            return k8sConf.replace("~", homePath);
        }
        return homePath.concat("/.kube/config");
    }

    protected static final class K8sClusterDescriptorAndSpecification {

        public final KubernetesClusterDescriptor clusterDescriptor;
        public final ClusterSpecification clusterSpecification;

        K8sClusterDescriptorAndSpecification(
                                             KubernetesClusterDescriptor clusterDescriptor,
                                             ClusterSpecification clusterSpecification) {
            this.clusterDescriptor = clusterDescriptor;
            this.clusterSpecification = clusterSpecification;
        }
    }
}
