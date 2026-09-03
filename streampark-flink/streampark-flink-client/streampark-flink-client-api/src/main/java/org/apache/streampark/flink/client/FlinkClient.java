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
import org.apache.streampark.flink.client.request.ShutdownRequest;
import org.apache.streampark.flink.client.request.SubmitRequest;
import org.apache.streampark.flink.client.request.TriggerSavepointRequest;
import org.apache.streampark.flink.client.response.CancelResponse;
import org.apache.streampark.flink.client.response.DeployResponse;
import org.apache.streampark.flink.client.response.SavepointResponse;
import org.apache.streampark.flink.client.response.ShutdownResponse;
import org.apache.streampark.flink.client.response.SubmitResponse;
import org.apache.streampark.flink.proxy.FlinkShimsProxy;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Permission;

/** Public facade for version-isolated Flink client operations. */
public final class FlinkClient {

    private static final String FLINK_CLIENT_ENTRYPOINT_CLASS =
        "org.apache.streampark.flink.client.FlinkClientEntrypoint";

    private FlinkClient() {
    }

    /** Submits a Flink job using the version and deployment mode in the request. */
    public static SubmitResponse submit(SubmitRequest submitRequest) {
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

    /** Cancels a running Flink job. */
    public static CancelResponse cancel(CancelRequest stopRequest) {
        return invokeClient(
            stopRequest,
            stopRequest.flinkVersion(),
            CancelRequest.class,
            "cancel",
            CancelResponse.class);
    }

    /** Deploys a Flink session cluster. */
    public static DeployResponse deploy(DeployRequest deployRequest) {
        return invokeClient(
            deployRequest,
            deployRequest.flinkVersion(),
            DeployRequest.class,
            "deploy",
            DeployResponse.class);
    }

    /** Shuts down a Flink session cluster. */
    public static ShutdownResponse shutdown(ShutdownRequest shutdownRequest) {
        return invokeClient(
            shutdownRequest,
            shutdownRequest.flinkVersion(),
            ShutdownRequest.class,
            "shutdown",
            ShutdownResponse.class);
    }

    /** Triggers a savepoint for a running Flink job. */
    public static SavepointResponse triggerSavepoint(TriggerSavepointRequest savepointRequest) {
        return invokeClient(
            savepointRequest,
            savepointRequest.flinkVersion(),
            TriggerSavepointRequest.class,
            "triggerSavepoint",
            SavepointResponse.class);
    }

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
                    Method method = entrypointClass.getDeclaredMethod(methodName, requestClass);
                    method.setAccessible(true);
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

    private static RuntimeException unwrapInvocationTarget(InvocationTargetException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
            return (RuntimeException) cause;
        }
        if (cause instanceof Error) {
            throw (Error) cause;
        }
        return new IllegalStateException("Failed to invoke Flink client method", e);
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
