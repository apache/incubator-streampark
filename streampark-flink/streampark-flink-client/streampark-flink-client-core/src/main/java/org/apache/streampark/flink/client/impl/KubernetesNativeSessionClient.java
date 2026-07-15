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
import org.apache.streampark.flink.client.util.FlinkConfigurationEnhancer;
import org.apache.streampark.flink.core.FlinkKubernetesClient;
import org.apache.streampark.flink.kubernetes.KubernetesRetriever;
import org.apache.streampark.flink.kubernetes.enums.FlinkK8sDeployMode;
import org.apache.streampark.flink.kubernetes.model.ClusterKey;

import org.apache.streampark.shaded.org.slf4j.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DeploymentOptions;
import org.apache.flink.configuration.DeploymentOptionsInternal;
import org.apache.flink.kubernetes.KubernetesClusterDescriptor;
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions;
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions.ServiceExposedType;
import org.apache.flink.kubernetes.configuration.KubernetesDeploymentTarget;
import org.apache.flink.kubernetes.kubeclient.FlinkKubeClient;
import org.apache.flink.kubernetes.kubeclient.FlinkKubeClientFactory;
import org.apache.flink.runtime.jobgraph.JobGraph;

import java.io.File;
import java.util.Map;
import java.util.Optional;

/** Kubernetes native session mode submit. */
public final class KubernetesNativeSessionClient extends KubernetesNativeClientTrait {

    public static final KubernetesNativeSessionClient INSTANCE = new KubernetesNativeSessionClient();

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory()
            .getLogger(KubernetesNativeSessionClient.class.getName());

    private KubernetesNativeSessionClient() {
    }

    @Override
    public SubmitResponse doSubmit(SubmitRequest submitRequest, Configuration flinkConfig) throws Exception {
        AssertUtils.required(
            StringUtils.isNotBlank(submitRequest.getClusterId()),
            "[flink-submit] submit flink job failed, clusterId is null, mode="
                + flinkConfig.get(DeploymentOptions.TARGET));
        return trySubmit(
            submitRequest,
            flinkConfig,
            submitRequest.getUserJarFile(),
            INSTANCE::jobGraphSubmit,
            INSTANCE::restApiSubmit);
    }

    /** Submit flink session job via rest api. */
    public SubmitResponse restApiSubmit(
                                        SubmitRequest submitRequest, Configuration flinkConfig,
                                        File fatJar) throws Exception {
        ClusterKey clusterKey =
            new ClusterKey(
                FlinkK8sDeployMode.SESSION,
                submitRequest.getKubernetesNamespace(),
                submitRequest.getClusterId());
        Optional<String> restUrlOption = KubernetesRetriever.retrieveFlinkRestUrl(clusterKey);
        if (!restUrlOption.isPresent()) {
            throw new Exception(
                "[flink-submit] retrieve flink session rest url failed, clusterKey=" + clusterKey);
        }
        String jmRestUrl = restUrlOption.get();
        String jobId = FlinkSessionSubmitHelper.submitViaRestApi(jmRestUrl, fatJar, flinkConfig);
        return SubmitResponse.builder()
            .clusterId(clusterKey.clusterId())
            .flinkConfig(flinkConfig.toMap())
            .jobId(jobId)
            .jobManagerUrl(jmRestUrl)
            .build();
    }

    /** Submit flink session job with building JobGraph via ClusterClient api. */
    public SubmitResponse jobGraphSubmit(
                                         SubmitRequest submitRequest, Configuration flinkConfig,
                                         File jarFile) throws Exception {
        KubernetesClusterDescriptor clusterDescriptor = getK8sClusterDescriptor(flinkConfig);
        JobGraphPackagedProgram packageProgramJobGraph = getJobGraph(flinkConfig, submitRequest, jarFile);
        PackagedProgram packageProgram = packageProgramJobGraph.packagedProgram;
        JobGraph jobGraph = packageProgramJobGraph.jobGraph;
        ClusterClient<String> client =
            clusterDescriptor
                .retrieve(flinkConfig.getString(KubernetesConfigOptions.CLUSTER_ID))
                .getClusterClient();
        String jobId = client.submitJob(jobGraph).get().toString();
        SubmitResponse result =
            SubmitResponse.builder()
                .clusterId(client.getClusterId())
                .flinkConfig(flinkConfig.toMap())
                .jobId(jobId)
                .jobManagerUrl(client.getWebInterfaceURL())
                .build();
        LOG.info(
            "[flink-submit] flink job has been submitted. {}, jobId: {}",
            flinkConfIdentifierInfo(flinkConfig),
            jobId);
        closeSubmit(submitRequest, packageProgram, client, client);
        return result;
    }

    @Override
    public CancelResponse doCancel(CancelRequest cancelRequest, Configuration flinkConfig) throws Exception {
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, DeploymentOptions.TARGET, FlinkDeployMode.KUBERNETES_NATIVE_SESSION.getName());
        return super.doCancel(cancelRequest, flinkConfig);
    }

    public DeployResponse deploy(DeployRequest deployRequest) throws Exception {
        LOG.info(
            "\n--------------------------------------- kubernetes cluster start ---------------------------------------\n"
                + "    userFlinkHome    : {}\n"
                + "    flinkVersion     : {}\n"
                + "    deployMode       : {}\n"
                + "    clusterId        : {}\n"
                + "    namespace        : {}\n"
                + "    exposedType      : {}\n"
                + "    serviceAccount   : {}\n"
                + "    flinkImage       : {}\n"
                + "    properties       : {}\n"
                + "--------------------------------------------------------------------------------------------------------\n",
            deployRequest.getFlinkVersion().flinkHome,
            deployRequest.getFlinkVersion().version(),
            deployRequest.getDeployMode().name(),
            deployRequest.getClusterId(),
            deployRequest.getK8sParam().getKubernetesNamespace(),
            deployRequest.getK8sParam().getFlinkRestExposedType(),
            deployRequest.getK8sParam().getServiceAccount(),
            deployRequest.getK8sParam().getFlinkImage(),
            formatProperties(deployRequest.getProperties()));

        Configuration flinkConfig = getFlinkK8sConfig(deployRequest);
        FlinkKubeClient kubeClient =
            FlinkKubeClientFactory.getInstance().fromConfiguration(flinkConfig, "client");

        KubernetesClusterDescriptor clusterDescriptor = null;
        ClusterClient<String> client = null;

        try {
            K8sClusterDescriptorAndSpecification kubernetesClusterDescriptor =
                getK8sClusterDescriptorAndSpecification(flinkConfig);
            clusterDescriptor = kubernetesClusterDescriptor.clusterDescriptor;

            FlinkKubernetesClient kubeClientWrapper = new FlinkKubernetesClient(kubeClient);
            if (kubeClientWrapper.getService(deployRequest.getClusterId()).isPresent()) {
                client = clusterDescriptor.retrieve(deployRequest.getClusterId()).getClusterClient();
            } else {
                client =
                    clusterDescriptor
                        .deploySessionCluster(kubernetesClusterDescriptor.clusterSpecification)
                        .getClusterClient();
            }
            return DeployResponse.builder()
                .address(client.getWebInterfaceURL())
                .clusterId(client.getClusterId())
                .build();
        } catch (Exception e) {
            return DeployResponse.builder().error(e).build();
        } finally {
            Utils.close(client, clusterDescriptor, kubeClient);
        }
    }

    public ShutDownResponse shutdown(ShutDownRequest shutDownRequest) throws Exception {
        FlinkKubeClient kubeClient = null;
        try {
            Configuration flinkConfig = getFlinkK8sConfig(shutDownRequest);
            kubeClient = FlinkKubeClientFactory.getInstance().fromConfiguration(flinkConfig, "client");
            FlinkKubernetesClient kubeClientWrapper = new FlinkKubernetesClient(kubeClient);

            boolean stopAndCleanupState =
                shutDownRequest.getClusterId() != null
                    && kubeClientWrapper
                        .getService(shutDownRequest.getClusterId())
                        .isPresent();
            if (stopAndCleanupState) {
                kubeClient.stopAndCleanupCluster(shutDownRequest.getClusterId());
                return new ShutDownResponse(shutDownRequest.getClusterId());
            }
            return null;
        } catch (Exception e) {
            LOG.error("shutdown flink session fail in {} mode", shutDownRequest.getDeployMode(), e);
            throw e;
        } finally {
            Utils.close(kubeClient);
        }
    }

    @Override
    public SavepointResponse doTriggerSavepoint(
                                                TriggerSavepointRequest triggerSavepointRequest,
                                                Configuration flinkConfig) throws Exception {
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, DeploymentOptions.TARGET, FlinkDeployMode.KUBERNETES_NATIVE_SESSION.getName());
        return super.doTriggerSavepoint(triggerSavepointRequest, flinkConfig);
    }

    private Configuration getFlinkK8sConfig(DeployRequestTrait deployRequest) throws Exception {
        Configuration flinkConfig =
            extractConfiguration(
                deployRequest.getFlinkVersion().flinkHome, deployRequest.getProperties());
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, DeploymentOptions.TARGET, KubernetesDeploymentTarget.SESSION.getName());
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig,
            KubernetesConfigOptions.NAMESPACE,
            deployRequest.getK8sParam().getKubernetesNamespace());
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig,
            KubernetesConfigOptions.KUBERNETES_SERVICE_ACCOUNT,
            deployRequest.getK8sParam().getServiceAccount());
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig, KubernetesConfigOptions.CLUSTER_ID, deployRequest.getClusterId());
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig,
            KubernetesConfigOptions.CONTAINER_IMAGE,
            deployRequest.getK8sParam().getFlinkImage());
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig,
            KubernetesConfigOptions.REST_SERVICE_EXPOSED_TYPE,
            ServiceExposedType.valueOf(deployRequest.getK8sParam().getFlinkRestExposedType().getName()));
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig,
            KubernetesConfigOptions.KUBE_CONFIG_FILE,
            getDefaultKubernetesConf(deployRequest.getK8sParam().getKubeConf()));
        FlinkConfigurationEnhancer.safeSet(
            flinkConfig,
            DeploymentOptionsInternal.CONF_DIR,
            deployRequest.getFlinkVersion().flinkHome + "/conf");
        return flinkConfig;
    }

    private String formatProperties(Map<String, Object> properties) {
        if (properties == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }
}
