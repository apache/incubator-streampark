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

import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.common.util.HadoopUtils;
import org.apache.streampark.common.util.Tuple2;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.client.bean.SubmitRequestResolver;
import org.apache.streampark.flink.client.configuration.FlinkConfigurationOps;
import org.apache.streampark.flink.client.request.CancelRequest;
import org.apache.streampark.flink.client.request.SavepointRequest;
import org.apache.streampark.flink.client.request.SubmitRequest;
import org.apache.streampark.flink.client.request.TriggerSavepointRequest;
import org.apache.streampark.flink.client.response.CancelResponse;
import org.apache.streampark.flink.client.response.SavepointResponse;

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

/** Base implementation for YARN deployment clients. */
public abstract class AbstractYarnClient extends AbstractFlinkClient {

    private volatile Method deployInternalMethod;

    @Override
    protected void setConfig(SubmitRequest submitRequest, Configuration flinkConfig) {
        FlinkConfigurationOps.setIfPresent(
            flinkConfig,
            YarnConfigOptions.APPLICATION_NAME,
            SubmitRequestResolver.effectiveApplicationName(submitRequest));
        FlinkConfigurationOps.setIfPresent(
            flinkConfig,
            YarnConfigOptions.APPLICATION_TYPE,
            submitRequest.applicationType().getName());
        FlinkConfigurationOps.setIfPresent(flinkConfig, YarnConfigOptions.APPLICATION_TAGS, "streampark");
    }

    @Override
    protected SavepointResponse doTriggerSavepoint(
                                                   TriggerSavepointRequest savepointRequest,
                                                   Configuration flinkConf) throws FlinkException {
        return executeClientAction(
            savepointRequest,
            flinkConf,
            (jobId, client) -> toSavepointResponse(savepointRequest, jobId, client));
    }

    @Override
    protected CancelResponse doCancel(
                                      CancelRequest cancelRequest,
                                      Configuration flinkConf) throws FlinkException {
        return executeClientAction(
            cancelRequest,
            flinkConf,
            (jobId, client) -> toCancelResponse(cancelRequest, jobId, client));
    }

    @SuppressWarnings("unchecked")
    protected final ClusterClientProvider<ApplicationId> deployInternal(
                                                                        YarnClusterDescriptor clusterDescriptor,
                                                                        ClusterSpecification clusterSpecification,
                                                                        String applicationName,
                                                                        String yarnClusterEntrypoint,
                                                                        JobGraph jobGraph,
                                                                        Boolean detached) throws ReflectiveOperationException {
        Method method = getDeployInternalMethod();
        return (ClusterClientProvider<ApplicationId>) method.invoke(
            clusterDescriptor,
            clusterSpecification,
            applicationName,
            yarnClusterEntrypoint,
            jobGraph,
            detached);
    }

    protected final Tuple2<ApplicationId, YarnClusterDescriptor> getYarnClusterDescriptor(
                                                                                          Configuration flinkConfig,
                                                                                          String user) throws FlinkException {
        return accessYarnClusterDescriptor(
            user, () -> createYarnClusterDescriptor(flinkConfig));
    }

    protected final Tuple2<ApplicationId, YarnClusterDescriptor> getYarnClusterDescriptor(
                                                                                          Configuration flinkConfig) throws FlinkException {
        return getYarnClusterDescriptor(flinkConfig, "");
    }

    protected final Tuple2<ClusterSpecification, YarnClusterDescriptor> getYarnClusterDeployDescriptor(
                                                                                                       Configuration flinkConfig,
                                                                                                       String user) throws FlinkException {
        return accessYarnClusterDescriptor(
            user, () -> createYarnClusterDeployDescriptor(flinkConfig));
    }

    protected final Tuple2<ClusterSpecification, YarnClusterDescriptor> getYarnClusterDeployDescriptor(
                                                                                                       Configuration flinkConfig) throws FlinkException {
        return getYarnClusterDeployDescriptor(flinkConfig, "");
    }

    protected void logClusterSpecification(ClusterSpecification clusterSpecification) {
        logInfo(
            String.format(
                "%n------------------------<<specification>>-------------------------%n"
                    + "%s%n"
                    + "------------------------------------------------------------------%n",
                clusterSpecification));
    }

    protected void logYarnJobStarted(Object applicationId) {
        logInfo(
            String.format(
                "%n-------------------------<<applicationId>>------------------------%n"
                    + "Flink Job Started: applicationId: %s%n"
                    + "__________________________________________________________________%n",
                applicationId));
    }

    private Tuple2<ApplicationId, YarnClusterDescriptor> createYarnClusterDescriptor(
                                                                                     Configuration flinkConfig) throws FlinkException {
        return callAsFlinkException(
            () -> {
                YarnClusterClientFactory clientFactory = new YarnClusterClientFactory();
                ApplicationId yarnClusterId = clientFactory.getClusterId(flinkConfig);
                if (yarnClusterId == null) {
                    throw new FlinkException("Yarn cluster id is null");
                }
                YarnClusterDescriptor clusterDescriptor =
                    clientFactory.createClusterDescriptor(flinkConfig);
                return new Tuple2<>(yarnClusterId, clusterDescriptor);
            });
    }

    private Tuple2<ClusterSpecification, YarnClusterDescriptor> createYarnClusterDeployDescriptor(
                                                                                                  Configuration flinkConfig) throws FlinkException {
        return callAsFlinkException(
            () -> {
                YarnClusterClientFactory clientFactory = new YarnClusterClientFactory();
                ClusterSpecification clusterSpecification =
                    clientFactory.getClusterSpecification(flinkConfig);
                YarnClusterDescriptor clusterDescriptor =
                    clientFactory.createClusterDescriptor(flinkConfig);
                return new Tuple2<>(clusterSpecification, clusterDescriptor);
            });
    }

    private <O> O executeClientAction(
                                      SavepointRequest request, Configuration flinkConf,
                                      ClientAction<O> actionFunc) throws FlinkException {
        return callAsFlinkException(
            () -> {
                JobID jobID = parseJobId(request.jobId());
                FlinkConfigurationOps.setIfPresent(flinkConf, YarnConfigOptions.APPLICATION_ID, request.clusterId());
                Tuple2<ApplicationId, YarnClusterDescriptor> descriptor =
                    getYarnClusterDescriptor(flinkConf);
                ClusterClient<?> clusterClient = null;
                try {
                    clusterClient =
                        descriptor._2.retrieve(descriptor._1).getClusterClient();
                    return applyClientAction(request, actionFunc, jobID, clusterClient);
                } finally {
                    Utils.close(clusterClient, descriptor._2);
                }
            });
    }

    private <O> O applyClientAction(
                                    SavepointRequest request,
                                    ClientAction<O> actionFunc,
                                    JobID jobID,
                                    ClusterClient<?> clusterClient) throws FlinkException {
        return callAsFlinkExceptionMapping(
            () -> actionFunc.apply(jobID, clusterClient),
            e -> new FlinkException(
                "[StreamPark] Do "
                    + request.getClass().getSimpleName()
                    + " for the job "
                    + request.jobId()
                    + " failed. "
                    + "detail: "
                    + ExceptionUtils.stringifyException(e),
                e));
    }

    private <T> T accessYarnClusterDescriptor(String user, YarnDescriptorSupplier<T> func) throws FlinkException {
        try {
            return doAsYarnClusterDescriptor(user, func);
        } catch (FlinkException e) {
            throw new FlinkException("[StreamPark] access ClusterDescriptor error: " + e.getMessage(), e);
        }
    }

    private Method getDeployInternalMethod() throws NoSuchMethodException {
        if (deployInternalMethod == null) {
            synchronized (this) {
                if (deployInternalMethod == null) {
                    Class<?>[] paramClass =
                        new Class<?>[]{
                                ClusterSpecification.class,
                                String.class,
                                String.class,
                                JobGraph.class,
                                boolean.class
                        };
                    Method deployInternal =
                        YarnClusterDescriptor.class.getDeclaredMethod(
                            "deployInternal", paramClass);
                    deployInternal.setAccessible(true);
                    deployInternalMethod = deployInternal;
                }
            }
        }
        return deployInternalMethod;
    }

    private <T> T doAsYarnClusterDescriptor(String user, YarnDescriptorSupplier<T> func) throws FlinkException {
        UserGroupInformation ugi = HadoopUtils.getUgi();
        UserGroupInformation finalUgi =
            user != null
                && !user.isEmpty()
                && !ugi.getShortUserName().equals(user)
                    ? UserGroupInformation.createProxyUser(user, ugi)
                    : ugi;

        try {
            return finalUgi.doAs(
                (PrivilegedAction<T>) () -> {
                    try {
                        return func.get();
                    } catch (FlinkException e) {
                        throw new IllegalStateException(e);
                    } catch (Exception e) {
                        throw new IllegalStateException("Yarn cluster descriptor operation failed", e);
                    }
                });
        } catch (IllegalStateException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new FlinkException(
                "[StreamPark] Error executing YarnClusterDescriptor operation as user " + user,
                cause);
        }
    }

    @FunctionalInterface
    private interface ClientAction<O> {

        O apply(JobID jobId, ClusterClient<?> client) throws FlinkException;
    }

    @FunctionalInterface
    private interface YarnDescriptorSupplier<T> {

        T get() throws FlinkException;
    }
}
