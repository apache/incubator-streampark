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

import org.apache.streampark.common.conf.FlinkVersion;
import org.apache.streampark.flink.client.bean.CancelRequest;
import org.apache.streampark.flink.client.bean.CancelResponse;
import org.apache.streampark.flink.client.bean.DeployRequest;
import org.apache.streampark.flink.client.bean.DeployResponse;
import org.apache.streampark.flink.client.bean.SavepointResponse;
import org.apache.streampark.flink.client.bean.ShutDownRequest;
import org.apache.streampark.flink.client.bean.ShutDownResponse;
import org.apache.streampark.flink.client.bean.SubmitRequest;
import org.apache.streampark.flink.client.bean.SubmitResponse;
import org.apache.streampark.flink.client.bean.TriggerSavepointRequest;
import org.apache.streampark.flink.proxy.FlinkShimsProxy;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class FlinkClient {

    private static final String FLINK_CLIENT_ENTRYPOINT_CLASS =
        "org.apache.streampark.flink.client.FlinkClientEntrypoint";

    private static final String SUBMIT_REQUEST =
        "org.apache.streampark.flink.client.bean.SubmitRequest";

    private static final String DEPLOY_REQUEST =
        "org.apache.streampark.flink.client.bean.DeployRequest";

    private static final String CANCEL_REQUEST =
        "org.apache.streampark.flink.client.bean.CancelRequest";

    private static final String SHUTDOWN_REQUEST =
        "org.apache.streampark.flink.client.bean.ShutDownRequest";

    private static final String SAVEPOINT_REQUEST =
        "org.apache.streampark.flink.client.bean.TriggerSavepointRequest";

    private FlinkClient() {
    }

    public static SubmitResponse submit(SubmitRequest submitRequest) {
        SecurityManager securityManager = System.getSecurityManager();
        try {
            System.setSecurityManager(new ExitSecurityManager());
            return invokeClient(
                submitRequest,
                submitRequest.flinkVersion(),
                SUBMIT_REQUEST,
                "submit",
                SubmitResponse.class);
        } finally {
            System.setSecurityManager(securityManager);
        }
    }

    public static CancelResponse cancel(CancelRequest stopRequest) {
        return invokeClient(
            stopRequest, stopRequest.flinkVersion(), CANCEL_REQUEST, "cancel", CancelResponse.class);
    }

    public static DeployResponse deploy(DeployRequest deployRequest) {
        return invokeClient(
            deployRequest, deployRequest.flinkVersion(), DEPLOY_REQUEST, "deploy", DeployResponse.class);
    }

    public static ShutDownResponse shutdown(ShutDownRequest shutDownRequest) {
        return invokeClient(
            shutDownRequest,
            shutDownRequest.flinkVersion(),
            SHUTDOWN_REQUEST,
            "shutdown",
            ShutDownResponse.class);
    }

    public static SavepointResponse triggerSavepoint(TriggerSavepointRequest savepointRequest) {
        return invokeClient(
            savepointRequest,
            savepointRequest.flinkVersion(),
            SAVEPOINT_REQUEST,
            "triggerSavepoint",
            SavepointResponse.class);
    }

    private static <R> R invokeClient(
                                      Object request,
                                      FlinkVersion flinkVersion,
                                      String requestClassName,
                                      String methodName,
                                      Class<R> responseType) {
        flinkVersion.checkVersion();
        return FlinkShimsProxy.proxy(
            flinkVersion,
            classLoader -> {
                try {
                    Class<?> entrypointClass = classLoader.loadClass(FLINK_CLIENT_ENTRYPOINT_CLASS);
                    Class<?> requestClass = classLoader.loadClass(requestClassName);
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
}
