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
import org.apache.streampark.flink.client.bean.ResolvedSubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitRequestResolver;
import org.apache.streampark.flink.client.configuration.FlinkConfigurationBuilder;
import org.apache.streampark.flink.client.request.AbstractSavepointRequest;
import org.apache.streampark.flink.client.request.CancelRequest;
import org.apache.streampark.flink.client.request.SavepointRequest;
import org.apache.streampark.flink.client.request.SubmitRequest;
import org.apache.streampark.flink.client.response.CancelResponse;
import org.apache.streampark.flink.client.response.SavepointResponse;
import org.apache.streampark.flink.client.response.SubmitResponse;
import org.apache.streampark.flink.core.FlinkClusterClient;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.JobID;
import org.apache.flink.client.cli.CliArgsException;
import org.apache.flink.client.program.ClusterClient;
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
        ResolvedSubmitRequest resolved = execute(() -> SubmitRequestResolver.resolve(request));
        logSubmitRequest(resolved);
        Configuration configuration = execute(() -> FlinkConfigurationBuilder.build(resolved));
        setConfig(resolved, configuration);
        return execute(
            () -> doSubmit(resolved, configuration),
            error -> logSubmitFailure(resolved, error));
    }

    /** Triggers a savepoint for an existing job. */
    public final SavepointResponse triggerSavepoint(SavepointRequest request) throws FlinkException {
        logSavepointRequest("trigger savepoint", request);
        return execute(
            () -> doTriggerSavepoint(request, new Configuration()));
    }

    /** Cancels an existing job, optionally creating a savepoint first. */
    public final CancelResponse cancel(CancelRequest request) throws FlinkException {
        logSavepointRequest("cancel", request);
        return execute(() -> doCancel(request, new Configuration()));
    }

    /** Adds configuration required by a deployment mode. */
    protected abstract void setConfig(
                                      ResolvedSubmitRequest resolved,
                                      Configuration configuration);

    /** Performs the deployment-mode-specific submission. */
    protected abstract SubmitResponse doSubmit(
                                               ResolvedSubmitRequest resolved,
                                               Configuration configuration) throws FlinkException;

    /** Performs the deployment-mode-specific savepoint operation. */
    protected abstract SavepointResponse doTriggerSavepoint(
                                                            SavepointRequest request,
                                                            Configuration configuration) throws FlinkException;

    /** Performs the deployment-mode-specific cancellation. */
    protected abstract CancelResponse doCancel(
                                               CancelRequest request,
                                               Configuration configuration) throws FlinkException;

    /** Converts infrastructure exceptions to the stable Flink client exception contract. */
    protected static FlinkException mapException(Throwable throwable) {
        if (throwable instanceof FlinkException) {
            return (FlinkException) throwable;
        }
        if (throwable instanceof Exception) {
            return new FlinkException(throwable);
        }
        return new FlinkException(throwable.getMessage(), throwable);
    }

    /** Executes an operation while preserving the public {@link FlinkException} contract. */
    protected static <T> T execute(ClientOperation<T> callable) throws FlinkException {
        return execute(callable, ignored -> {
        });
    }

    /** Executes an operation and invokes the failure callback before exception conversion. */
    protected static <T> T execute(
                                   ClientOperation<T> callable,
                                   Consumer<Exception> onFailure) throws FlinkException {
        try {
            return callable.call();
        } catch (FlinkException e) {
            onFailure.accept(e);
            throw e;
        } catch (Exception e) {
            onFailure.accept(e);
            throw mapException(e);
        }
    }

    /** Executes an operation with caller-defined conversion for non-Flink exceptions. */
    protected static <T> T executeAndMapError(
                                              ClientOperation<T> callable,
                                              Function<Exception, FlinkException> exceptionMapper) throws FlinkException {
        try {
            return callable.call();
        } catch (FlinkException e) {
            throw e;
        } catch (Exception e) {
            throw exceptionMapper.apply(e);
        }
    }

    /** Cancels a job and wraps its optional savepoint path in the stable response type. */
    protected final CancelResponse toCancelResponse(
                                                    CancelRequest request,
                                                    JobID jobId,
                                                    ClusterClient<?> client) throws FlinkException {
        return execute(() -> new CancelResponse(cancelJob(request, jobId, client)));
    }

    /** Triggers a savepoint and wraps its path in the stable response type. */
    protected final SavepointResponse toSavepointResponse(
                                                          SavepointRequest request,
                                                          JobID jobId,
                                                          ClusterClient<?> client) throws FlinkException {
        return execute(
            () -> new SavepointResponse(triggerSavepoint(request, jobId, client)));
    }

    /**
     * Builds and submits a JobGraph while owning every resource opened for the submission.
     *
     * <p>The packaged program, cluster client, and supplied descriptors are closed on both success
     * and failure. Callers must not reuse the supplied resources after this method returns.
     */
    protected final SubmitResponse submitJobGraphToCluster(
                                                           ResolvedSubmitRequest resolved,
                                                           Configuration configuration,
                                                           File jarFile,
                                                           ClientOperation<ClusterClient<?>> clientSupplier,
                                                           ClientOperation<String> clusterIdSupplier,
                                                           AutoCloseable... extraResources) throws FlinkException {
        return execute(
            () -> {
                FlinkJobGraphBuilder.Result result = null;
                ClusterClient<?> client = null;
                try {
                    result = buildJobGraph(configuration, resolved, jarFile);
                    client = clientSupplier.call();
                    String jobId = submitJobGraph(client, result.jobGraph());
                    return new SubmitResponse(
                        clusterIdSupplier.call(),
                        configuration.toMap(),
                        jobId,
                        client.getWebInterfaceURL());
                } finally {
                    AutoCloseable[] resources = new AutoCloseable[extraResources.length + 2];
                    resources[0] = result == null ? null : result.program();
                    resources[1] = client;
                    System.arraycopy(extraResources, 0, resources, 2, extraResources.length);
                    closeSubmissionResources(resources);
                }
            });
    }

    /** Tries JobGraph submission before falling back to the session REST API. */
    protected final SubmitResponse trySubmit(
                                             ResolvedSubmitRequest resolved,
                                             Configuration configuration,
                                             File jarFile,
                                             SubmissionStrategy jobGraphSubmit,
                                             SubmissionStrategy restApiSubmit) throws FlinkException {
        try {
            logInfo("[flink-submit] Submitting with the JobGraph protocol");
            return jobGraphSubmit.apply(resolved, configuration, jarFile);
        } catch (FlinkException jobGraphFailure) {
            logWarn(
                "[flink-submit] JobGraph submission failed; trying the REST API: "
                    + ExceptionUtils.stringifyException(jobGraphFailure));
            try {
                return restApiSubmit.apply(resolved, configuration, jarFile);
            } catch (FlinkException restFailure) {
                restFailure.addSuppressed(jobGraphFailure);
                throw new FlinkException(
                    "Job submission failed with both the JobGraph protocol and the REST API",
                    restFailure);
            }
        }
    }

    /** Delegates JobGraph construction to the version-aware builder. */
    protected final FlinkJobGraphBuilder.Result buildJobGraph(
                                                              Configuration configuration,
                                                              ResolvedSubmitRequest resolved,
                                                              File jarFile) throws Exception {
        return FlinkJobGraphBuilder.build(configuration, resolved, jarFile);
    }

    /** Parses a hexadecimal job ID and reports invalid input as a CLI argument error. */
    protected final JobID parseJobId(String jobId) throws CliArgsException {
        try {
            return JobID.fromHexString(jobId);
        } catch (Exception e) {
            throw new CliArgsException(e.getMessage());
        }
    }

    /** Extracts dynamic cluster properties through Flink's target-specific command line. */
    protected final Configuration extractConfiguration(
                                                       String flinkHome,
                                                       Map<String, Object> properties) throws Exception {
        return FlinkConfigurationBuilder.extract(flinkHome, properties);
    }

    /** Loads the registered Flink installation's default configuration. */
    protected final Configuration loadDefaultConfiguration(String flinkHome) {
        return FlinkConfigurationBuilder.loadDefault(flinkHome);
    }

    /** Cancels a job directly or stops it with a savepoint according to the request. */
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

    /** Triggers a savepoint with the request's format and resolved target directory. */
    protected final String triggerSavepoint(
                                            SavepointRequest request,
                                            JobID jobId,
                                            ClusterClient<?> client) throws Exception {
        return new FlinkClusterClient<>(client)
            .triggerSavepoint(
                jobId, resolveSavepointDirectory(request), request.nativeFormat())
            .get();
    }

    /** Closes submission resources independently so one close failure cannot skip later resources. */
    protected final void closeSubmissionResources(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                Utils.close(resource);
            }
        }
    }

    /** Logs the final Flink configuration after all deployment-specific options are applied. */
    protected final void logEffectiveSubmitConfiguration(Configuration configuration) {
        logInfo(
            String.format(
                "%n------------------------------------------------------------------%n"
                    + "Effective submit configuration: %s%n"
                    + "------------------------------------------------------------------%n",
                configuration));
    }

    /** Resolves the explicit or installation-level savepoint directory for a job operation. */
    private String resolveSavepointDirectory(AbstractSavepointRequest request) {
        if (!request.withSavepoint()) {
            return null;
        }
        if (StringUtils.isNotBlank(request.savepointPath())) {
            return request.savepointPath();
        }

        String defaultDirectory =
            FlinkConfigurationBuilder.loadDefaultOption(
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

    /** Logs a submission request without relying on mutable map iteration at call sites. */
    private void logSubmitRequest(ResolvedSubmitRequest resolved) {
        SubmitRequest request = resolved.request();
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
                resolved.getJobName(),
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

    /** Logs a submission failure with the resolved job and deployment mode. */
    private void logSubmitFailure(ResolvedSubmitRequest resolved, Exception error) {
        SubmitRequest request = resolved.request();
        logError(
            "Flink job "
                + resolved.getJobName()
                + " failed to start in "
                + request.deployMode().getName()
                + ": "
                + ExceptionUtils.stringifyException(error));
    }

    /** Logs the stable identity and savepoint options of a job operation. */
    private void logSavepointRequest(String operation, AbstractSavepointRequest request) {
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

    /** Formats dynamic properties for diagnostic logging. */
    private static String formatProperties(Map<String, Object> properties) {
        if (MapUtils.isEmpty(properties)) {
            return "";
        }
        return properties.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(" "));
    }

    /** Operation that may throw an infrastructure-specific checked exception. */
    @FunctionalInterface
    protected interface ClientOperation<T> {

        T call() throws Exception;
    }

    /** Submission strategy used by the JobGraph-to-REST fallback chain. */
    @FunctionalInterface
    protected interface SubmissionStrategy {

        SubmitResponse apply(
                             ResolvedSubmitRequest resolved,
                             Configuration configuration,
                             File jarFile) throws FlinkException;
    }

}
