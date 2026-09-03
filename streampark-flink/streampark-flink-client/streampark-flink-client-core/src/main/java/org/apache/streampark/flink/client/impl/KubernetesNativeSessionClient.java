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
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.client.bean.SessionClusterRestClient;
import org.apache.streampark.flink.client.bean.SubmitRequestResolver;
import org.apache.streampark.flink.client.configuration.FlinkConfigurationOps;
import org.apache.streampark.flink.client.request.CancelRequest;
import org.apache.streampark.flink.client.request.ClusterRequest;
import org.apache.streampark.flink.client.request.DeployRequest;
import org.apache.streampark.flink.client.request.ShutdownRequest;
import org.apache.streampark.flink.client.request.SubmitRequest;
import org.apache.streampark.flink.client.request.TriggerSavepointRequest;
import org.apache.streampark.flink.client.response.CancelResponse;
import org.apache.streampark.flink.client.response.DeployResponse;
import org.apache.streampark.flink.client.response.SavepointResponse;
import org.apache.streampark.flink.client.response.ShutdownResponse;
import org.apache.streampark.flink.client.response.SubmitResponse;
import org.apache.streampark.flink.core.FlinkKubernetesClient;
import org.apache.streampark.flink.kubernetes.KubernetesRetriever;
import org.apache.streampark.flink.kubernetes.enums.FlinkKubernetesDeployMode;
import org.apache.streampark.flink.kubernetes.model.ClusterKey;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.DeploymentOptionsInternal;
import org.apache.flink.kubernetes.KubernetesClusterDescriptor;
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions;
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions.ServiceExposedType;
import org.apache.flink.kubernetes.configuration.KubernetesDeploymentTarget;
import org.apache.flink.kubernetes.kubeclient.FlinkKubeClient;
import org.apache.flink.kubernetes.kubeclient.FlinkKubeClientFactory;
import org.apache.flink.util.FlinkException;

import java.io.File;

/** Kubernetes native session mode submit. */
public final class KubernetesNativeSessionClient extends AbstractKubernetesNativeClient {

    public static final KubernetesNativeSessionClient INSTANCE = new KubernetesNativeSessionClient();

    private KubernetesNativeSessionClient() {
    }

    @Override
    protected SubmitResponse doSubmit(
                                      SubmitRequest submitRequest,
                                      Configuration flinkConfig) throws FlinkException {
        if (StringUtils.isBlank(submitRequest.clusterId())) {
            throw new IllegalArgumentException(
                String.format(
                    "[flink-submit] submit flink job failed, clusterId is null, mode=%s",
                    flinkConfig.get(DeploymentOptions.TARGET)));
        }

        return this.restApiSubmit(submitRequest, flinkConfig, SubmitRequestResolver.userJarFile(submitRequest));
    }

    /** Submit flink session job via rest api. */
    private SubmitResponse restApiSubmit(
                                         SubmitRequest submitRequest,
                                         Configuration flinkConfig,
                                         File fatJar) throws FlinkException {
        return callAsFlinkException(
            () -> {
                ClusterKey clusterKey =
                    ClusterKey.builder()
                        .executeMode(FlinkKubernetesDeployMode.SESSION)
                        .namespace(submitRequest.kubernetesNamespace())
                        .clusterId(submitRequest.clusterId())
                        .build();
                String jmRestUrl =
                    KubernetesRetriever.retrieveFlinkRestUrl(clusterKey)
                        .orElseThrow(
                            () -> new FlinkException(
                                "[flink-submit] retrieve flink session rest url failed, clusterKey="
                                    + clusterKey));
                String jobId =
                    SessionClusterRestClient.submit(jmRestUrl, fatJar, flinkConfig);
                return new SubmitResponse(
                    clusterKey.clusterId(), flinkConfig.toMap(), jobId, jmRestUrl);
            });
    }

    @Override
    protected CancelResponse doCancel(
                                      CancelRequest cancelRequest,
                                      Configuration flinkConfig) throws FlinkException {
        setK8sDeployTarget(flinkConfig, FlinkDeployMode.KUBERNETES_NATIVE_SESSION);
        return super.doCancel(cancelRequest, flinkConfig);
    }

    public DeployResponse deploy(DeployRequest deployRequest) throws Exception {
        logInfo(
            String.format(
                "%n--------------------------------------- kubernetes cluster start "
                    + "---------------------------------------%n"
                    + "    userFlinkHome    : %s%n"
                    + "    flinkVersion     : %s%n"
                    + "    deployMode       : %s%n"
                    + "    clusterId        : %s%n"
                    + "    namespace        : %s%n"
                    + "    exposedType      : %s%n"
                    + "    serviceAccount   : %s%n"
                    + "    flinkImage       : %s%n"
                    + "    properties       : %s%n"
                    + "--------------------------------------------------------------------------------------------------------%n",
                deployRequest.flinkVersion().getFlinkHome(),
                deployRequest.flinkVersion().version(),
                deployRequest.deployMode().name(),
                deployRequest.clusterId(),
                deployRequest.kubernetesDeploySpec().kubernetesNamespace(),
                deployRequest.kubernetesDeploySpec().flinkRestExposedType(),
                deployRequest.kubernetesDeploySpec().serviceAccount(),
                deployRequest.kubernetesDeploySpec().flinkImage(),
                deployRequest.properties()));

        Configuration flinkConfig = getFlinkK8sConfig(deployRequest);
        FlinkKubeClient kubeClient =
            FlinkKubeClientFactory.getInstance().fromConfiguration(flinkConfig, "client");

        KubernetesClusterDescriptor clusterDescriptor = null;
        ClusterClient<String> client = null;

        try {
            Tuple2<KubernetesClusterDescriptor, ClusterSpecification> kubernetesClusterDescriptor =
                getK8sClusterDescriptorAndSpecification(flinkConfig);
            clusterDescriptor = kubernetesClusterDescriptor._1;

            FlinkKubernetesClient kubeClientWrapper = new FlinkKubernetesClient(kubeClient);
            if (kubeClientWrapper.getService(deployRequest.clusterId()).isPresent()) {
                client =
                    clusterDescriptor.retrieve(deployRequest.clusterId()).getClusterClient();
            } else {
                client =
                    clusterDescriptor
                        .deploySessionCluster(kubernetesClusterDescriptor._2)
                        .getClusterClient();
            }
            return new DeployResponse(
                client.getWebInterfaceURL(), client.getClusterId(), null);
        } catch (Exception e) {
            return new DeployResponse(null, null, e);
        } finally {
            Utils.close(client, clusterDescriptor, kubeClient);
        }
    }

    public ShutdownResponse shutdown(ShutdownRequest shutdownRequest) throws Exception {
        FlinkKubeClient kubeClient = null;
        try {
            Configuration flinkConfig = getFlinkK8sConfig(shutdownRequest);
            kubeClient =
                FlinkKubeClientFactory.getInstance().fromConfiguration(flinkConfig, "client");
            FlinkKubernetesClient kubeClientWrapper = new FlinkKubernetesClient(kubeClient);

            boolean stopAndCleanupState =
                shutdownRequest.clusterId() != null
                    && kubeClientWrapper.getService(shutdownRequest.clusterId()).isPresent();
            if (stopAndCleanupState) {
                kubeClient.stopAndCleanupCluster(shutdownRequest.clusterId());
                return new ShutdownResponse(shutdownRequest.clusterId());
            }
            return null;
        } catch (Exception e) {
            logError(
                "shutdown flink session fail in " + shutdownRequest.deployMode() + " mode", e);
            throw e;
        } finally {
            Utils.close(kubeClient);
        }
    }

    @Override
    protected SavepointResponse doTriggerSavepoint(
                                                   TriggerSavepointRequest triggerSavepointRequest,
                                                   Configuration flinkConfig) throws FlinkException {
        FlinkConfigurationOps.setIfPresent(
            flinkConfig,
            DeploymentOptions.TARGET,
            FlinkDeployMode.KUBERNETES_NATIVE_SESSION.getName());
        return super.doTriggerSavepoint(triggerSavepointRequest, flinkConfig);
    }

    private Configuration getFlinkK8sConfig(ClusterRequest deployRequest) throws Exception {
        Configuration flinkConfig = extractConfiguration(
            deployRequest.flinkVersion().getFlinkHome(), deployRequest.properties());

        FlinkConfigurationOps.setIfPresent(flinkConfig, DeploymentOptions.TARGET,
            KubernetesDeploymentTarget.SESSION.getName());

        FlinkConfigurationOps.setIfPresent(
            flinkConfig,
            KubernetesConfigOptions.NAMESPACE,
            deployRequest.kubernetesDeploySpec().kubernetesNamespace());

        FlinkConfigurationOps.setIfPresent(
            flinkConfig,
            KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT,
            deployRequest.kubernetesDeploySpec().serviceAccount());

        FlinkConfigurationOps.setIfPresent(
            flinkConfig, KubernetesConfigOptions.CLUSTER_ID, deployRequest.clusterId());

        FlinkConfigurationOps.setIfPresent(
            flinkConfig,
            KubernetesConfigOptions.CONTAINER_IMAGE,
            deployRequest.kubernetesDeploySpec().flinkImage());

        FlinkConfigurationOps.setIfPresent(
            flinkConfig,
            KubernetesConfigOptions.REST_SERVICE_EXPOSED_TYPE,
            ServiceExposedType.valueOf(
                deployRequest.kubernetesDeploySpec().flinkRestExposedType().getName()));

        FlinkConfigurationOps.setIfPresent(
            flinkConfig,
            KubernetesConfigOptions.KUBE_CONFIG_FILE,
            getDefaultKubernetesConf(deployRequest.kubernetesDeploySpec().kubeConf()));

        FlinkConfigurationOps.setIfPresent(
            flinkConfig,
            DeploymentOptionsInternal.CONF_DIR,
            deployRequest.flinkVersion().getFlinkHome() + "/conf");

        return flinkConfig;
    }
}
