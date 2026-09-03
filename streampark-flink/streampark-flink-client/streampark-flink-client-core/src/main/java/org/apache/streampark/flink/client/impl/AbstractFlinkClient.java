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

import org.apache.streampark.common.configuration.Workspace;
import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.common.util.AssertUtils;
import org.apache.streampark.common.util.ExceptionUtils;
import org.apache.streampark.common.util.LoggerSupport;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.flink.client.bean.FlinkJobGraphBuilder;
import org.apache.streampark.flink.client.bean.SubmissionConfigurationBuilder;
import org.apache.streampark.flink.client.bean.SubmitRequestResolver;
import org.apache.streampark.flink.client.request.CancelRequest;
import org.apache.streampark.flink.client.request.SavepointRequest;
import org.apache.streampark.flink.client.request.SubmitRequest;
import org.apache.streampark.flink.client.request.TriggerSavepointRequest;
import org.apache.streampark.flink.client.response.CancelResponse;
import org.apache.streampark.flink.client.response.SavepointResponse;
import org.apache.streampark.flink.client.response.SubmitResponse;
import org.apache.streampark.flink.core.FlinkClusterClient;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.JobID;
import org.apache.flink.client.cli.CliArgsException;
import org.apache.flink.client.program.ClusterClient;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.util.FlinkException;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Coordinates the common lifecycle of Flink deployment clients. */
public abstract class AbstractFlinkClient extends LoggerSupport {

    /** Submits a job with the deployment-mode-specific client. */
    public final SubmitResponse submit(SubmitRequest request) throws FlinkException {
        logSubmitRequest(request);
        Configuration configuration =
            callAsFlinkException(() -> SubmissionConfigurationBuilder.build(request));
        setConfig(request, configuration);
        return callAsFlinkException(
            () -> doSubmit(request, configuration),
            error -> logSubmitFailure(request, error));
    }

    /** Triggers a savepoint for an existing job. */
    public final SavepointResponse triggerSavepoint(TriggerSavepointRequest request) throws FlinkException {
        logSavepointRequest("trigger savepoint", request);
        return callAsFlinkException(
            () -> doTriggerSavepoint(request, new Configuration()));
    }

    /** Cancels an existing job, optionally creating a savepoint first. */
    public final CancelResponse cancel(CancelRequest request) throws FlinkException {
        logSavepointRequest("cancel", request);
        return callAsFlinkException(() -> doCancel(request, new Configuration()));
    }

    /** Adds configuration required by a deployment mode. */
    protected abstract void setConfig(SubmitRequest request, Configuration configuration);

    /** Performs the deployment-mode-specific submission. */
    protected abstract SubmitResponse doSubmit(
                                               SubmitRequest request,
                                               Configuration configuration) throws FlinkException;

    /** Performs the deployment-mode-specific savepoint operation. */
    protected abstract SavepointResponse doTriggerSavepoint(
                                                            TriggerSavepointRequest request,
                                                            Configuration configuration) throws FlinkException;

    /** Performs the deployment-mode-specific cancellation. */
    protected abstract CancelResponse doCancel(
                                               CancelRequest request,
                                               Configuration configuration) throws FlinkException;

    /** Converts infrastructure exceptions to the stable Flink client exception contract. */
    protected static FlinkException asFlinkException(Throwable throwable) {
        if (throwable instanceof FlinkException) {
            return (FlinkException) throwable;
        }
        if (throwable instanceof Exception) {
            return new FlinkException(throwable);
        }
        return new FlinkException(throwable.getMessage(), throwable);
    }

    protected static <T> T callAsFlinkException(FlinkCallable<T> callable) throws FlinkException {
        return callAsFlinkException(callable, ignored -> {
        });
    }

    protected static <T> T callAsFlinkException(
                                                FlinkCallable<T> callable,
                                                Consumer<Exception> onFailure) throws FlinkException {
        try {
            return callable.call();
        } catch (FlinkException e) {
            onFailure.accept(e);
            throw e;
        } catch (Exception e) {
            onFailure.accept(e);
            throw asFlinkException(e);
        }
    }

    protected static <T> T callAsFlinkExceptionMapping(
                                                       FlinkCallable<T> callable,
                                                       Function<Exception, FlinkException> exceptionMapper) throws FlinkException {
        try {
            return callable.call();
        } catch (FlinkException e) {
            throw e;
        } catch (Exception e) {
            throw exceptionMapper.apply(e);
        }
    }

    protected final CancelResponse toCancelResponse(
                                                    CancelRequest request,
                                                    JobID jobId,
                                                    ClusterClient<?> client) throws FlinkException {
        return callAsFlinkException(() -> new CancelResponse(cancelJob(request, jobId, client)));
    }

    protected final SavepointResponse toSavepointResponse(
                                                          TriggerSavepointRequest request,
                                                          JobID jobId,
                                                          ClusterClient<?> client) throws FlinkException {
        return callAsFlinkException(
            () -> new SavepointResponse(triggerSavepoint(request, jobId, client)));
    }

    protected final SubmitResponse submitJobGraphToCluster(
                                                           SubmitRequest request,
                                                           Configuration configuration,
                                                           File jarFile,
                                                           FlinkCallable<ClusterClient<?>> clientSupplier,
                                                           FlinkCallable<String> clusterIdSupplier,
                                                           AutoCloseable... extraResources) throws FlinkException {
        return callAsFlinkException(
            () -> {
                FlinkJobGraphBuilder.Result result = buildJobGraph(configuration, request, jarFile);
                ClusterClient<?> client = null;
                try {
                    client = clientSupplier.call();
                    String jobId = submitJobGraph(client, result.jobGraph());
                    return new SubmitResponse(
                        clusterIdSupplier.call(),
                        configuration.toMap(),
                        jobId,
                        client.getWebInterfaceURL());
                } finally {
                    AutoCloseable[] resources = new AutoCloseable[extraResources.length + 2];
                    resources[0] = result.program();
                    resources[1] = client;
                    System.arraycopy(extraResources, 0, resources, 2, extraResources.length);
                    closeSubmissionResources(request, resources);
                }
            });
    }

    /** Tries JobGraph submission before falling back to the session REST API. */
    protected final SubmitResponse trySubmit(
                                             SubmitRequest request,
                                             Configuration configuration,
                                             File jarFile,
                                             SubmitFunction jobGraphSubmit,
                                             SubmitFunction restApiSubmit) throws FlinkException {
        try {
            logInfo("[flink-submit] Submitting with the JobGraph protocol");
            return jobGraphSubmit.apply(request, configuration, jarFile);
        } catch (FlinkException jobGraphFailure) {
            logWarn(
                "[flink-submit] JobGraph submission failed; trying the REST API: "
                    + ExceptionUtils.stringifyException(jobGraphFailure));
            try {
                return restApiSubmit.apply(request, configuration, jarFile);
            } catch (FlinkException restFailure) {
                restFailure.addSuppressed(jobGraphFailure);
                throw new FlinkException(
                    "Job submission failed with both the JobGraph protocol and the REST API",
                    restFailure);
            }
        }
    }

    protected final FlinkJobGraphBuilder.Result buildJobGraph(
                                                              Configuration configuration,
                                                              SubmitRequest request,
                                                              File jarFile) throws Exception {
        return FlinkJobGraphBuilder.build(configuration, request, jarFile);
    }

    protected final JobID parseJobId(String jobId) throws CliArgsException {
        try {
            return JobID.fromHexString(jobId);
        } catch (Exception e) {
            throw new CliArgsException(e.getMessage());
        }
    }

    protected final Configuration extractConfiguration(
                                                       String flinkHome,
                                                       Map<String, Object> properties) throws Exception {
        return SubmissionConfigurationBuilder.extract(flinkHome, properties);
    }

    protected final Configuration loadDefaultConfiguration(String flinkHome) {
        return SubmissionConfigurationBuilder.loadDefault(flinkHome);
    }

    protected final String cancelJob(
                                     CancelRequest request, JobID jobId, ClusterClient<?> client) throws Exception {
        String savepointDirectory = resolveSavepointDirectory(request);
        if (!request.withSavepoint() && !request.withDrain()) {
            client.cancel(jobId).get();
            return null;
        }
        return new FlinkClusterClient<>(client)
            .stopWithSavepoint(
                jobId, request.withDrain(), savepointDirectory, request.nativeFormat())
            .get();
    }

    protected final String triggerSavepoint(
                                            TriggerSavepointRequest request,
                                            JobID jobId,
                                            ClusterClient<?> client) throws Exception {
        return new FlinkClusterClient<>(client)
            .triggerSavepoint(
                jobId, resolveSavepointDirectory(request), request.nativeFormat())
            .get();
    }

    protected final void closeSubmissionResources(
                                                  SubmitRequest request,
                                                  AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource == null) {
                continue;
            }
            if (!(resource instanceof PackagedProgram)
                || SubmitRequestResolver.canSafelyClosePackagedProgram(request)) {
                Utils.close(resource);
            }
        }
    }

    protected final void logEffectiveSubmitConfiguration(Configuration configuration) {
        logInfo(
            String.format(
                "%n------------------------------------------------------------------%n"
                    + "Effective submit configuration: %s%n"
                    + "------------------------------------------------------------------%n",
                configuration));
    }

    private String resolveSavepointDirectory(SavepointRequest request) {
        if (!request.withSavepoint()) {
            return null;
        }
        if (StringUtils.isNotBlank(request.savepointPath())) {
            return request.savepointPath();
        }

        String defaultDirectory =
            SubmissionConfigurationBuilder.getDefaultOption(
                request.flinkVersion().getFlinkHome(),
                ConfigOptions.key(CheckpointingOptions.SAVEPOINT_DIRECTORY.key())
                    .stringType()
                    .defaultValue(
                        request.deployMode() == FlinkDeployMode.YARN_APPLICATION
                            ? Workspace.REMOTE.savepoints
                            : null));
        AssertUtils.required(
            StringUtils.isNotBlank(defaultDirectory),
            "Savepoint directory is not configured for " + request.deployMode().getName());
        return defaultDirectory;
    }

    /**
     * Invokes {@code ClusterClient.submitJob} across the Flink 1.x and 2.x signature change.
     *
     * <p>Flink 2.x accepts {@code ExecutionPlan}; Flink 1.x accepts {@code JobGraph}. Reflection is
     * limited to this compatibility boundary so the rest of the client remains type-safe.
     */
    private static String submitJobGraph(ClusterClient<?> client, JobGraph jobGraph) throws Exception {
        for (Method method : client.getClass().getMethods()) {
            if ("submitJob".equals(method.getName())
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0].isInstance(jobGraph)) {
                Object future = method.invoke(client, jobGraph);
                return ((CompletableFuture<?>) future).get().toString();
            }
        }
        throw new FlinkException(
            "No ClusterClient.submitJob method accepts JobGraph on "
                + client.getClass().getName());
    }

    private void logSubmitRequest(SubmitRequest request) {
        logInfo(
            String.format(
                "%n--------------------------------------- flink job start ---------------------------------------%n"
                    + "    userFlinkHome    : %s%n"
                    + "    flinkVersion     : %s%n"
                    + "    appName          : %s%n"
                    + "    jobType          : %s%n"
                    + "    deployMode       : %s%n"
                    + "    k8sNamespace     : %s%n"
                    + "    flinkExposedType : %s%n"
                    + "    clusterId        : %s%n"
                    + "    applicationType  : %s%n"
                    + "    savePoint        : %s%n"
                    + "    properties       : %s%n"
                    + "    args             : %s%n"
                    + "    appConf          : %s%n"
                    + "    flinkBuildResult : %s%n"
                    + "-------------------------------------------------------------------------------------------%n",
                request.flinkVersion().getFlinkHome(),
                request.flinkVersion().version(),
                SubmitRequestResolver.effectiveApplicationName(request),
                request.jobType(),
                request.deployMode(),
                request.kubernetesNamespace(),
                request.flinkRestExposedType(),
                request.clusterId(),
                request.applicationType().getName(),
                request.savePoint(),
                formatProperties(request.properties()),
                request.args(),
                request.appConf(),
                request.buildResult()));
    }

    private void logSubmitFailure(SubmitRequest request, Exception error) {
        logError(
            "Flink job "
                + SubmitRequestResolver.effectiveApplicationName(request)
                + " failed to start in "
                + request.deployMode().getName()
                + ": "
                + ExceptionUtils.stringifyException(error));
    }

    private void logSavepointRequest(String operation, SavepointRequest request) {
        logInfo(
            "Flink job "
                + operation
                + ": deployMode="
                + request.deployMode().getName()
                + ", clusterId="
                + request.clusterId()
                + ", jobId="
                + request.jobId()
                + ", savepointPath="
                + request.savepointPath()
                + ", nativeFormat="
                + request.nativeFormat());
    }

    private static String formatProperties(Map<String, Object> properties) {
        if (MapUtils.isEmpty(properties)) {
            return "";
        }
        return properties.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(" "));
    }

    @FunctionalInterface
    protected interface FlinkCallable<T> {

        T call() throws Exception;
    }

    @FunctionalInterface
    protected interface SubmitFunction {

        SubmitResponse apply(SubmitRequest request, Configuration configuration, File jarFile) throws FlinkException;
    }
}
