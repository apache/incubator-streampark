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

import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.common.util.HadoopUtils;
import org.apache.streampark.flink.client.bean.CancelRequest;
import org.apache.streampark.flink.client.bean.CancelResponse;
import org.apache.streampark.flink.client.bean.SavepointRequestTrait;
import org.apache.streampark.flink.client.bean.SavepointResponse;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.TriggerSavepointRequest;
import org.apache.streampark.flink.client.util.FlinkConfigurationEnhancer;

import org.apache.flink.api.common.JobID;
import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.client.program.ClusterClientProvider;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.util.FlinkException;
import org.apache.flink.yarn.YarnClusterClientFactory;
import org.apache.flink.yarn.YarnClusterDescriptor;
import org.apache.flink.yarn.configuration.YarnConfigOptions;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.records.ApplicationId;

import java.lang.reflect.Method;
import java.security.PrivilegedAction;

/** Yarn application mode submit. */
public abstract class YarnClientTrait extends FlinkClientTrait {

    private Method deployInternalMethod;

    @Override
    public void setConfig(SubmitRequest submitRequest, Configuration flinkConfig) {
        FlinkConfigurationEnhancer.safeSet(
                flinkConfig, YarnConfigOptions.APPLICATION_NAME, submitRequest.getEffectiveAppName());
        FlinkConfigurationEnhancer.safeSet(
                flinkConfig,
                YarnConfigOptions.APPLICATION_TYPE,
                submitRequest.getApplicationType().getName());
        FlinkConfigurationEnhancer.safeSet(flinkConfig, YarnConfigOptions.APPLICATION_TAGS, "streampark");
    }

    private <R extends SavepointRequestTrait, O> O executeClientAction(
            R request, Configuration flinkConf, ClusterClientAction<O, ApplicationId> actionFunc)
            throws Exception {
        JobID jobID = getJobID(request.getJobId());
        FlinkConfigurationEnhancer.safeSet(flinkConf, YarnConfigOptions.APPLICATION_ID, request.getClusterId());
        YarnClusterDescriptorResult descriptorResult = getYarnClusterDescriptor(flinkConf);
        ApplicationId applicationId = descriptorResult.applicationId;
        YarnClusterDescriptor clusterDescriptor = descriptorResult.clusterDescriptor;
        ClusterClient<ApplicationId> clusterClient =
                clusterDescriptor.retrieve(applicationId).getClusterClient();

        try {
            return actionFunc.apply(jobID, clusterClient);
        } catch (Exception e) {
            throw new FlinkException(
                    "[StreamPark] Do "
                            + request.getClass().getSimpleName()
                            + " for the job "
                            + request.getJobId()
                            + " failed. detail: "
                            + ExceptionUtils.stringifyException(e));
        }
    }

    @Override
    public SavepointResponse doTriggerSavepoint(
            TriggerSavepointRequest savepointRequest, Configuration flinkConf) throws Exception {
        return executeClientAction(
                savepointRequest,
                flinkConf,
                (jid, client) ->
                        new SavepointResponse(triggerSavepoint(savepointRequest, jid, client)));
    }

    @Override
    public CancelResponse doCancel(CancelRequest cancelRequest, Configuration flinkConf)
            throws Exception {
        return executeClientAction(
                cancelRequest,
                flinkConf,
                (jid, client) -> new CancelResponse(cancelJob(cancelRequest, jid, client)));
    }

    protected ClusterClientProvider<ApplicationId> deployInternal(
            YarnClusterDescriptor clusterDescriptor,
            ClusterSpecification clusterSpecification,
            String applicationName,
            String yarnClusterEntrypoint,
            JobGraph jobGraph,
            Boolean detached)
            throws Exception {
        if (deployInternalMethod == null) {
            Class<?>[] paramClass =
                    new Class<?>[] {
                        ClusterSpecification.class,
                        String.class,
                        String.class,
                        JobGraph.class,
                        boolean.class
                    };
            deployInternalMethod =
                    YarnClusterDescriptor.class.getDeclaredMethod("deployInternal", paramClass);
            deployInternalMethod.setAccessible(true);
        }
        return (ClusterClientProvider<ApplicationId>)
                deployInternalMethod.invoke(
                        clusterDescriptor,
                        clusterSpecification,
                        applicationName,
                        yarnClusterEntrypoint,
                        jobGraph,
                        detached);
    }

    protected YarnClusterDescriptorResult getYarnClusterDescriptor(Configuration flinkConfig)
            throws Exception {
        return getYarnClusterDescriptor(flinkConfig, "");
    }

    protected YarnClusterDescriptorResult getYarnClusterDescriptor(
            Configuration flinkConfig, String user) throws Exception {
        try {
            return doAsYarnClusterDescriptor(
                    user,
                    () -> {
                        YarnClusterClientFactory clientFactory = new YarnClusterClientFactory();
                        ApplicationId yarnClusterId = clientFactory.getClusterId(flinkConfig);
                        if (yarnClusterId == null) {
                            throw new IllegalArgumentException("yarnClusterId is null");
                        }
                        YarnClusterDescriptor clusterDescriptor =
                                clientFactory.createClusterDescriptor(flinkConfig);
                        return new YarnClusterDescriptorResult(yarnClusterId, clusterDescriptor);
                    });
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "[StreamPark] access ClusterDescriptor error: " + e, e);
        }
    }

    protected YarnClusterDeployDescriptorResult getYarnClusterDeployDescriptor(
            Configuration flinkConfig) throws Exception {
        return getYarnClusterDeployDescriptor(flinkConfig, "");
    }

    protected YarnClusterDeployDescriptorResult getYarnClusterDeployDescriptor(
            Configuration flinkConfig, String user) throws Exception {
        try {
            return doAsYarnClusterDescriptorDeploy(user, flinkConfig);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "[StreamPark] access ClusterDescriptor error: " + e, e);
        }
    }

    private YarnClusterDeployDescriptorResult doAsYarnClusterDescriptorDeploy(
            String user, Configuration flinkConfig) throws Exception {
        UserGroupInformation ugi = HadoopUtils.getUgi();
        UserGroupInformation finalUgi =
                user != null
                                && !user.isEmpty()
                                && !ugi.getShortUserName().equals(user)
                        ? UserGroupInformation.createProxyUser(user, ugi)
                        : ugi;
        try {
            return finalUgi.doAs(
                    (PrivilegedAction<YarnClusterDeployDescriptorResult>) () -> {
                        try {
                            YarnClusterClientFactory clientFactory = new YarnClusterClientFactory();
                            ClusterSpecification clusterSpecification =
                                    clientFactory.getClusterSpecification(flinkConfig);
                            YarnClusterDescriptor clusterDescriptor =
                                    clientFactory.createClusterDescriptor(flinkConfig);
                            return new YarnClusterDeployDescriptorResult(
                                    clusterSpecification, clusterDescriptor);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException(
                    "[StreamPark] Error executing YarnClusterDescriptor operation as user " + user, e);
        }
    }

    private <T> T doAsYarnClusterDescriptor(String user, YarnClusterSupplier<T> func)
            throws Exception {
        UserGroupInformation ugi = HadoopUtils.getUgi();
        UserGroupInformation finalUgi =
                user != null
                                && !user.isEmpty()
                                && !ugi.getShortUserName().equals(user)
                        ? UserGroupInformation.createProxyUser(user, ugi)
                        : ugi;
        try {
            return finalUgi.doAs(
                    (PrivilegedAction<T>)
                            () -> {
                                try {
                                    return func.get();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
        } catch (Exception e) {
            throw new RuntimeException(
                    "[StreamPark] Error executing YarnClusterDescriptor operation as user " + user, e);
        }
    }

    protected static final class YarnClusterDescriptorResult {
        public final ApplicationId applicationId;
        public final YarnClusterDescriptor clusterDescriptor;

        YarnClusterDescriptorResult(
                ApplicationId applicationId, YarnClusterDescriptor clusterDescriptor) {
            this.applicationId = applicationId;
            this.clusterDescriptor = clusterDescriptor;
        }
    }

    protected static final class YarnClusterDeployDescriptorResult {
        public final ClusterSpecification clusterSpecification;
        public final YarnClusterDescriptor clusterDescriptor;

        YarnClusterDeployDescriptorResult(
                ClusterSpecification clusterSpecification,
                YarnClusterDescriptor clusterDescriptor) {
            this.clusterSpecification = clusterSpecification;
            this.clusterDescriptor = clusterDescriptor;
        }
    }

    @FunctionalInterface
    private interface YarnClusterSupplier<T> {
        T get() throws Exception;
    }
}
