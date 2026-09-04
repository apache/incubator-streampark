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

package org.apache.streampark.flink.client;

import org.apache.streampark.common.core.FlinkVersion;
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Permission;

/**
 * Public facade for version-isolated Flink client operations.
 *
 * <p>Each operation selects the classloader for the request's registered Flink installation,
 * serializes the request into that loader, and invokes {@code FlinkClientEntrypoint} reflectively.
 * Responses are serialized back into the caller's classloader. This boundary prevents Flink
 * runtime classes from different supported versions from sharing type identity.
 */
public final class FlinkClient {

    private static final String FLINK_CLIENT_ENTRYPOINT_CLASS =
        "org.apache.streampark.flink.client.FlinkClientEntrypoint";

    // SecurityManager is process-wide, so installation and restoration must be atomic.
    private static final Object EXIT_GUARD_LOCK = new Object();

    private FlinkClient() {
    }

    /**
     * Submits a Flink job using the version and deployment mode in the request.
     *
     * <p>Submission is serialized while the process-wide exit guard is installed. The guard keeps
     * application code that invokes {@link System#exit(int)} from terminating the Console JVM.
     *
     * @param submitRequest complete immutable submission input
     * @return identifiers and effective configuration returned by the deployment client
     */
    public static SubmitResponse submit(SubmitRequest submitRequest) {
        synchronized (EXIT_GUARD_LOCK) {
            return submitWithExitGuard(submitRequest);
        }
    }

    /**
     * Cancels a running Flink job, optionally stopping it with a savepoint.
     *
     * @param stopRequest job identity, deployment context, and cancellation options
     * @return cancellation result containing the savepoint path when one was created
     */
    public static CancelResponse cancel(CancelRequest stopRequest) {
        return invokeClient(
            stopRequest,
            stopRequest.flinkVersion(),
            CancelRequest.class,
            "cancel",
            CancelResponse.class);
    }

    /**
     * Deploys a persistent Flink session cluster.
     *
     * @param deployRequest target version, deployment mode, and platform configuration
     * @return deployed cluster identity and REST address
     */
    public static DeployResponse deploy(DeployRequest deployRequest) {
        return invokeClient(
            deployRequest,
            deployRequest.flinkVersion(),
            DeployRequest.class,
            "deploy",
            DeployResponse.class);
    }

    /**
     * Shuts down a persistent Flink session cluster.
     *
     * @param shutdownRequest target cluster identity and deployment context
     * @return identity of the cluster accepted for shutdown
     */
    public static ShutdownResponse shutdown(ShutdownRequest shutdownRequest) {
        return invokeClient(
            shutdownRequest,
            shutdownRequest.flinkVersion(),
            ShutdownRequest.class,
            "shutdown",
            ShutdownResponse.class);
    }

    /**
     * Triggers a savepoint for a running Flink job without cancelling it.
     *
     * @param savepointRequest job identity, target directory, and format selection
     * @return completed savepoint location
     */
    public static SavepointResponse triggerSavepoint(SavepointRequest savepointRequest) {
        return invokeClient(
            savepointRequest,
            savepointRequest.flinkVersion(),
            SavepointRequest.class,
            "triggerSavepoint",
            SavepointResponse.class);
    }

    /** Installs the process-wide exit guard for exactly one serialized submission. */
    private static SubmitResponse submitWithExitGuard(SubmitRequest submitRequest) {
        SecurityManager previousSecurityManager = System.getSecurityManager();
        boolean exitGuardInstalled = false;
        try {
            try {
                System.setSecurityManager(new ExitSecurityManager());
                exitGuardInstalled = true;
            } catch (UnsupportedOperationException ignored) {
                // JDK 17+ may reject SecurityManager unless -Djava.security.manager=allow is set.
            }
            return invokeClient(
                submitRequest,
                submitRequest.flinkVersion(),
                SubmitRequest.class,
                "submit",
                SubmitResponse.class);
        } finally {
            if (exitGuardInstalled) {
                System.setSecurityManager(previousSecurityManager);
            }
        }
    }

    /**
     * Invokes the client entry point inside the target-version classloader.
     *
     * <p>Requests and responses are serialized across the boundary so target-runtime Flink classes
     * never leak into the Console classloader.
     */
    private static <R> R invokeClient(
                                      Object request,
                                      FlinkVersion flinkVersion,
                                      Class<?> requestType,
                                      String methodName,
                                      Class<R> responseType) {
        flinkVersion.checkVersion();
        return FlinkShimsProxy.proxy(
            flinkVersion,
            classLoader -> {
                try {
                    Class<?> entrypointClass = classLoader.loadClass(FLINK_CLIENT_ENTRYPOINT_CLASS);
                    Class<?> requestClass = classLoader.loadClass(requestType.getName());
                    Method method = entrypointClass.getMethod(methodName, requestClass);
                    Object shimsRequest =
                        FlinkShimsProxy.getObject(classLoader, request, requestClass);
                    Object result = method.invoke(null, shimsRequest);
                    if (result == null) {
                        return null;
                    }
                    return FlinkShimsProxy.getObject(
                        FlinkClient.class.getClassLoader(), result, responseType);
                } catch (InvocationTargetException e) {
                    throw unwrapInvocationTarget(e);
                } catch (ReflectiveOperationException | IOException e) {
                    throw new IllegalStateException(
                        "Failed to invoke Flink client via shims proxy: " + methodName, e);
                }
            });
    }

    /** Preserves unchecked causes raised inside the target classloader invocation. */
    private static RuntimeException unwrapInvocationTarget(InvocationTargetException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
            return (RuntimeException) cause;
        }
        if (cause instanceof Error) {
            throw (Error) cause;
        }
        return new IllegalStateException("Failed to invoke Flink client method", cause);
    }

    /** Prevents submitted user code from terminating the console JVM. */
    static final class ExitSecurityManager extends SecurityManager {

        @Override
        public void checkExit(int status) {
            throw new SecurityException(
                "System.exit("
                    + status
                    + ") was called in your Flink job; the job has been stopped");
        }

        @Override
        public void checkPermission(Permission permission) {
            // Allow all operations except System.exit.
        }

        @Override
        public void checkPermission(Permission permission, Object context) {
            // Allow all operations except System.exit.
        }
    }
}
