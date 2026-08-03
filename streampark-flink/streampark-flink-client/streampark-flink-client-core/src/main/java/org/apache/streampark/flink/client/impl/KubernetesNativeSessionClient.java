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
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.client.bean.CancelRequest;
import org.apache.streampark.flink.client.bean.CancelResponse;
import org.apache.streampark.flink.client.bean.DeployRequest;
import org.apache.streampark.flink.client.bean.DeployRequestTrait;
import org.apache.streampark.flink.client.bean.DeployResponse;
import org.apache.streampark.flink.client.bean.SavepointResponse;
import org.apache.streampark.flink.client.bean.ShutDownRequest;
import org.apache.streampark.flink.client.bean.ShutDownResponse;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.client.bean.TriggerSavepointRequest;
import org.apache.streampark.flink.client.tool.FlinkSessionSubmitHelper;
import org.apache.streampark.flink.client.trait.KubernetesNativeClientTrait;
import org.apache.streampark.flink.core.FlinkKubernetesClient;
import org.apache.streampark.flink.kubernetes.KubernetesRetriever;
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sDeployMode;
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

import scala.Tuple2;

/** Kubernetes native session mode submit. */
public final class KubernetesNativeSessionClient extends KubernetesNativeClientTrait {

    public static final KubernetesNativeSessionClient INSTANCE =
        new KubernetesNativeSessionClient();

    private KubernetesNativeSessionClient() {
    }

    @Override
    public SubmitResponse doSubmit(SubmitRequest submitRequest, Configuration flinkConfig) throws FlinkException {
        if (StringUtils.isBlank(submitRequest.clusterId())) {
            throw new IllegalArgumentException(
                String.format(
                    "[flink-submit] submit flink job failed, clusterId is null, mode=%s",
                    flinkConfig.get(DeploymentOptions.TARGET)));
        }
        return trySubmit(
            submitRequest,
            flinkConfig,
            submitRequest.userJarFile(),
            this::jobGraphSubmit,
            this::restApiSubmit);
    }

    /** Submit flink session job via rest api. */
    public SubmitResponse restApiSubmit(
                                        SubmitRequest submitRequest,
                                        Configuration flinkConfig,
                                        File fatJar) throws FlinkException {
        return callAsFlinkException(
            () -> {
                ClusterKey clusterKey =
                    ClusterKey.builder()
                        .executeMode(FlinkK8sDeployMode.SESSION)
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
                    FlinkSessionSubmitHelper.submitViaRestApi(jmRestUrl, fatJar, flinkConfig);
                return new SubmitResponse(
                    clusterKey.clusterId(), flinkConfig.toMap(), jobId, jmRestUrl);
            });
    }

    /** Submit flink session job with building JobGraph via ClusterClient api. */
    public SubmitResponse jobGraphSubmit(
                                         SubmitRequest submitRequest,
                                         Configuration flinkConfig,
                                         File jarFile) throws FlinkException {
        return callAsFlinkException(
            () -> {
                SubmitResponse result =
                    submitJobGraphToCluster(
                        submitRequest,
                        flinkConfig,
                        jarFile,
                        () -> getK8sClusterDescriptor(flinkConfig)
                            .retrieve(flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID))
                            .getClusterClient(),
                        () -> flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID));
                logInfo(
                    "[flink-submit] flink job has been submitted. "
                        + flinkConfIdentifierInfo(flinkConfig)
                        + ", jobId: "
                        + result.jobId());
                return result;
            });
    }

    @Override
    public CancelResponse doCancel(CancelRequest cancelRequest, Configuration flinkConfig) throws FlinkException {
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
                deployRequest.k8sParam().kubernetesNamespace(),
                deployRequest.k8sParam().flinkRestExposedType(),
                deployRequest.k8sParam().serviceAccount(),
                deployRequest.k8sParam().flinkImage(),
                deployRequest.properties()));

        Configuration flinkConfig = getFlinkK8sConfig(deployRequest);
        FlinkKubeClient kubeClient =
            FlinkKubeClientFactory.getInstance().fromConfiguration(flinkConfig, "client");

        KubernetesClusterDescriptor clusterDescriptor = null;
        ClusterClient<String> client = null;

        try {
            Tuple2<KubernetesClusterDescriptor, ClusterSpecification> kubernetesClusterDescriptor =
                getK8sClusterDescriptorAndSpecification(flinkConfig);
            clusterDescriptor = kubernetesClusterDescriptor._1();

            FlinkKubernetesClient kubeClientWrapper = new FlinkKubernetesClient(kubeClient);
            if (kubeClientWrapper.getService(deployRequest.clusterId()).isPresent()) {
                client =
                    clusterDescriptor.retrieve(deployRequest.clusterId()).getClusterClient();
            } else {
                client =
                    clusterDescriptor
                        .deploySessionCluster(kubernetesClusterDescriptor._2())
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

    public ShutDownResponse shutdown(ShutDownRequest shutDownRequest) throws Exception {
        FlinkKubeClient kubeClient = null;
        try {
            Configuration flinkConfig = getFlinkK8sConfig(shutDownRequest);
            kubeClient =
                FlinkKubeClientFactory.getInstance().fromConfiguration(flinkConfig, "client");
            FlinkKubernetesClient kubeClientWrapper = new FlinkKubernetesClient(kubeClient);

            boolean stopAndCleanupState =
                shutDownRequest.clusterId() != null
                    && kubeClientWrapper.getService(shutDownRequest.clusterId()).isPresent();
            if (stopAndCleanupState) {
                kubeClient.stopAndCleanupCluster(shutDownRequest.clusterId());
                return new ShutDownResponse(shutDownRequest.clusterId());
            }
            return null;
        } catch (Exception e) {
            logError(
                "shutdown flink session fail in " + shutDownRequest.deployMode() + " mode", e);
            throw e;
        } finally {
            Utils.close(kubeClient);
        }
    }

    @Override
    public SavepointResponse doTriggerSavepoint(
                                                TriggerSavepointRequest triggerSavepointRequest,
                                                Configuration flinkConfig) throws FlinkException {
        FlinkConfigurationOps.safeSet(
            flinkConfig,
            DeploymentOptions.TARGET,
            FlinkDeployMode.KUBERNETES_NATIVE_SESSION.getName());
        return super.doTriggerSavepoint(triggerSavepointRequest, flinkConfig);
    }

    private Configuration getFlinkK8sConfig(DeployRequestTrait deployRequest) throws Exception {
        Configuration flinkConfig =
            extractConfiguration(
                deployRequest.flinkVersion().getFlinkHome(), deployRequest.properties());
        FlinkConfigurationOps.safeSet(
            flinkConfig, DeploymentOptions.TARGET, KubernetesDeploymentTarget.SESSION.getName());
        FlinkConfigurationOps.safeSet(
            flinkConfig,
            KubernetesConfigOptions.NAMESPACE,
            deployRequest.k8sParam().kubernetesNamespace());
        FlinkConfigurationOps.safeSet(
            flinkConfig,
            KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT,
            deployRequest.k8sParam().serviceAccount());
        FlinkConfigurationOps.safeSet(
            flinkConfig, KubernetesConfigOptions.CLUSTER_ID, deployRequest.clusterId());
        FlinkConfigurationOps.safeSet(
            flinkConfig,
            KubernetesConfigOptions.CONTAINER_IMAGE,
            deployRequest.k8sParam().flinkImage());
        FlinkConfigurationOps.safeSet(
            flinkConfig,
            KubernetesConfigOptions.REST_SERVICE_EXPOSED_TYPE,
            ServiceExposedType.valueOf(
                deployRequest.k8sParam().flinkRestExposedType().getName()));
        FlinkConfigurationOps.safeSet(
            flinkConfig,
            KubernetesConfigOptions.KUBE_CONFIG_FILE,
            getDefaultKubernetesConf(deployRequest.k8sParam().kubeConf()));
        FlinkConfigurationOps.safeSet(
            flinkConfig,
            DeploymentOptionsInternal.CONF_DIR,
            deployRequest.flinkVersion().getFlinkHome() + "/conf");
        return flinkConfig;
    }
}
