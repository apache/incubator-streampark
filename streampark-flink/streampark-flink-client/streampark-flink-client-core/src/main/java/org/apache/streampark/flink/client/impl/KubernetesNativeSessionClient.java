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
import org.apache.streampark.flink.client.bean.ResolvedSubmitRequest;
import org.apache.streampark.flink.client.bean.SessionClusterRestClient;
import org.apache.streampark.flink.client.request.AbstractClusterRequest;
import org.apache.streampark.flink.client.request.CancelRequest;
import org.apache.streampark.flink.client.request.DeployRequest;
import org.apache.streampark.flink.client.request.SavepointRequest;
import org.apache.streampark.flink.client.request.ShutdownRequest;
import org.apache.streampark.flink.client.request.SubmitRequest;
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

/** Manages Flink jobs and clusters in Kubernetes native session mode. */
public final class KubernetesNativeSessionClient extends AbstractKubernetesNativeClient {

    public static final KubernetesNativeSessionClient INSTANCE = new KubernetesNativeSessionClient();

    private KubernetesNativeSessionClient() {
    }

    /** Deploys or reconnects to the requested Kubernetes session cluster. */
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

        Configuration flinkConfig = getFlinkKubernetesConfig(deployRequest);

        FlinkKubeClient kubeClient = FlinkKubeClientFactory.getInstance().fromConfiguration(flinkConfig, "client");

        KubernetesClusterDescriptor clusterDescriptor = null;
        ClusterClient<String> client = null;

        try {
            Tuple2<KubernetesClusterDescriptor, ClusterSpecification> kubernetesClusterDescriptor =
                createClusterDescriptorAndSpec(flinkConfig);
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

    /** Stops a Kubernetes session cluster when it exists. */
    public ShutdownResponse shutdown(ShutdownRequest shutdownRequest) throws Exception {
        FlinkKubeClient kubeClient = null;
        try {
            Configuration flinkConfig = getFlinkKubernetesConfig(shutdownRequest);
            kubeClient =
                FlinkKubeClientFactory.getInstance().fromConfiguration(flinkConfig, "client");
            FlinkKubernetesClient kubeClientWrapper = new FlinkKubernetesClient(kubeClient);

            boolean clusterExists =
                shutdownRequest.clusterId() != null
                    && kubeClientWrapper.getService(shutdownRequest.clusterId()).isPresent();
            if (clusterExists) {
                kubeClient.stopAndCleanupCluster(shutdownRequest.clusterId());
            }
            // Shutdown is idempotent: an already absent cluster is the desired final state.
            return new ShutdownResponse(shutdownRequest.clusterId());
        } catch (Exception e) {
            logError(
                "shutdown flink session fail in " + shutdownRequest.deployMode() + " mode", e);
            throw e;
        } finally {
            Utils.close(kubeClient);
        }
    }

    /** Submits a job to an existing Kubernetes session cluster through its REST endpoint. */
    @Override
    protected SubmitResponse doSubmit(
                                      ResolvedSubmitRequest resolved,
                                      Configuration flinkConfig) throws FlinkException {
        SubmitRequest submitRequest = resolved.request();
        if (StringUtils.isBlank(submitRequest.clusterId())) {
            throw new IllegalArgumentException(
                String.format(
                    "[flink-submit] submit flink job failed, clusterId is null, mode=%s",
                    flinkConfig.get(DeploymentOptions.TARGET)));
        }

        return restApiSubmit(submitRequest, flinkConfig, resolved.getUserJarFile());
    }

    /** Cancels a job while retaining its Kubernetes session cluster. */
    @Override
    protected CancelResponse doCancel(
                                      CancelRequest cancelRequest,
                                      Configuration flinkConfig) throws FlinkException {
        flinkConfig.set(
            DeploymentOptions.TARGET,
            FlinkDeployMode.KUBERNETES_NATIVE_SESSION.getName());
        return super.doCancel(cancelRequest, flinkConfig);
    }

    /** Triggers a savepoint against an existing Kubernetes session cluster. */
    @Override
    protected SavepointResponse doTriggerSavepoint(
                                                   SavepointRequest triggerSavepointRequest,
                                                   Configuration flinkConfig) throws FlinkException {
        flinkConfig.set(
            DeploymentOptions.TARGET,
            FlinkDeployMode.KUBERNETES_NATIVE_SESSION.getName());
        return super.doTriggerSavepoint(triggerSavepointRequest, flinkConfig);
    }

    /** Submits the uploaded job JAR and returns the session cluster job identity. */
    private SubmitResponse restApiSubmit(
                                         SubmitRequest submitRequest,
                                         Configuration flinkConfig,
                                         File fatJar) throws FlinkException {
        return execute(
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

    /** Builds the Flink configuration shared by session deployment and shutdown operations. */
    private Configuration getFlinkKubernetesConfig(AbstractClusterRequest request) throws Exception {
        Configuration flinkConfig = extractConfiguration(
            request.flinkVersion().getFlinkHome(), request.properties());

        flinkConfig.set(
            DeploymentOptions.TARGET, KubernetesDeploymentTarget.SESSION.getName());

        if (StringUtils.isNotEmpty(
            request.kubernetesDeploySpec().kubernetesNamespace())) {
            flinkConfig.set(
                KubernetesConfigOptions.NAMESPACE,
                request.kubernetesDeploySpec().kubernetesNamespace());
        }

        if (StringUtils.isNotEmpty(request.kubernetesDeploySpec().serviceAccount())) {
            flinkConfig.set(
                KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT,
                request.kubernetesDeploySpec().serviceAccount());
        }

        flinkConfig.set(KubernetesConfigOptions.CLUSTER_ID, request.clusterId());

        if (StringUtils.isNotEmpty(request.kubernetesDeploySpec().flinkImage())) {
            flinkConfig.set(
                KubernetesConfigOptions.CONTAINER_IMAGE,
                request.kubernetesDeploySpec().flinkImage());
        }

        flinkConfig.set(
            KubernetesConfigOptions.REST_SERVICE_EXPOSED_TYPE,
            ServiceExposedType.valueOf(
                request.kubernetesDeploySpec().flinkRestExposedType().getName()));

        flinkConfig.set(
            KubernetesConfigOptions.KUBE_CONFIG_FILE,
            getDefaultKubernetesConf(request.kubernetesDeploySpec().kubeConf()));

        flinkConfig.set(
            DeploymentOptionsInternal.CONF_DIR,
            request.flinkVersion().getFlinkHome() + "/conf");

        return flinkConfig;
    }
}
