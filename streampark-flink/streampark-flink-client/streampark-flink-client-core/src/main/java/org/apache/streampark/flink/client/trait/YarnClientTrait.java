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

import scala.Tuple2;

/** Yarn application mode submit support. */
public abstract class YarnClientTrait extends FlinkClientTrait {

    private volatile Method deployInternalMethod;

    @Override
    public void setConfig(SubmitRequest submitRequest, Configuration flinkConfig) {
        FlinkClientTrait.safeSet(
            flinkConfig, YarnConfigOptions.APPLICATION_NAME, submitRequest.effectiveAppName());
        FlinkClientTrait.safeSet(
            flinkConfig,
            YarnConfigOptions.APPLICATION_TYPE,
            submitRequest.applicationType().getName());
        FlinkClientTrait.safeSet(flinkConfig, YarnConfigOptions.APPLICATION_TAGS, "streampark");
    }

    @Override
    public SavepointResponse doTriggerSavepoint(
                                                TriggerSavepointRequest savepointRequest,
                                                Configuration flinkConf) throws Exception {
        return executeClientAction(
            savepointRequest,
            flinkConf,
            (jobId, client) -> new SavepointResponse(triggerSavepoint(savepointRequest, jobId, client)));
    }

    @Override
    public CancelResponse doCancel(CancelRequest cancelRequest, Configuration flinkConf) throws Exception {
        return executeClientAction(
            cancelRequest,
            flinkConf,
            (jobId, client) -> new CancelResponse(cancelJob(cancelRequest, jobId, client)));
    }

    public ClusterClientProvider<ApplicationId> deployInternal(
                                                               YarnClusterDescriptor clusterDescriptor,
                                                               ClusterSpecification clusterSpecification,
                                                               String applicationName,
                                                               String yarnClusterEntrypoint,
                                                               JobGraph jobGraph,
                                                               Boolean detached) throws Exception {
        Method method = getDeployInternalMethod();
        return (ClusterClientProvider<ApplicationId>) method.invoke(
            clusterDescriptor,
            clusterSpecification,
            applicationName,
            yarnClusterEntrypoint,
            jobGraph,
            detached);
    }

    public Tuple2<ApplicationId, YarnClusterDescriptor> getYarnClusterDescriptor(
                                                                                 Configuration flinkConfig,
                                                                                 String user) throws Exception {
        try {
            return doAsYarnClusterDescriptor(
                user,
                () -> {
                    YarnClusterClientFactory clientFactory = new YarnClusterClientFactory();
                    ApplicationId yarnClusterId = clientFactory.getClusterId(flinkConfig);
                    if (yarnClusterId == null) {
                        throw new IllegalArgumentException("Yarn cluster id is null");
                    }
                    YarnClusterDescriptor clusterDescriptor =
                        clientFactory.createClusterDescriptor(flinkConfig);
                    return new Tuple2<>(yarnClusterId, clusterDescriptor);
                });
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "[StreamPark] access ClusterDescriptor error: " + e, e);
        }
    }

    public Tuple2<ApplicationId, YarnClusterDescriptor> getYarnClusterDescriptor(
                                                                                 Configuration flinkConfig) throws Exception {
        return getYarnClusterDescriptor(flinkConfig, "");
    }

    public Tuple2<ClusterSpecification, YarnClusterDescriptor> getYarnClusterDeployDescriptor(
                                                                                              Configuration flinkConfig,
                                                                                              String user) throws Exception {
        try {
            return doAsYarnClusterDescriptor(
                user,
                () -> {
                    YarnClusterClientFactory clientFactory = new YarnClusterClientFactory();
                    ClusterSpecification clusterSpecification =
                        clientFactory.getClusterSpecification(flinkConfig);
                    YarnClusterDescriptor clusterDescriptor =
                        clientFactory.createClusterDescriptor(flinkConfig);
                    return new Tuple2<>(clusterSpecification, clusterDescriptor);
                });
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "[StreamPark] access ClusterDescriptor error: " + e, e);
        }
    }

    public Tuple2<ClusterSpecification, YarnClusterDescriptor> getYarnClusterDeployDescriptor(
                                                                                              Configuration flinkConfig) throws Exception {
        return getYarnClusterDeployDescriptor(flinkConfig, "");
    }

    private <O> O executeClientAction(
                                      SavepointRequestTrait request, Configuration flinkConf,
                                      ClientAction<O> actionFunc) throws Exception {
        JobID jobID = getJobID(request.jobId());
        FlinkClientTrait.safeSet(flinkConf, YarnConfigOptions.APPLICATION_ID, request.clusterId());
        Tuple2<ApplicationId, YarnClusterDescriptor> descriptor =
            getYarnClusterDescriptor(flinkConf);
        ClusterClient<?> clusterClient =
            descriptor._2().retrieve(descriptor._1()).getClusterClient();
        try {
            return actionFunc.apply(jobID, clusterClient);
        } catch (Exception e) {
            throw new FlinkException(
                "[StreamPark] Do "
                    + request.getClass().getSimpleName()
                    + " for the job "
                    + request.jobId()
                    + " failed. "
                    + "detail: "
                    + ExceptionUtils.stringifyException(e),
                e);
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

    private <T> T doAsYarnClusterDescriptor(String user, YarnDescriptorSupplier<T> func) throws Exception {
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
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        } catch (RuntimeException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException(
                "[StreamPark] Error executing YarnClusterDescriptor operation as user " + user,
                cause);
        }
    }

    @FunctionalInterface
    private interface ClientAction<O> {

        O apply(JobID jobId, ClusterClient<?> client) throws Exception;
    }

    @FunctionalInterface
    private interface YarnDescriptorSupplier<T> {

        T get() throws Exception;
    }
}
