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
import org.apache.streampark.common.util.LoggerSupport;
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

import java.util.function.Function;

public final class FlinkClient extends LoggerSupport {

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
            return proxy(submitRequest, submitRequest.flinkVersion(), SUBMIT_REQUEST, "submit");
        } finally {
            System.setSecurityManager(securityManager);
        }
    }

    public static CancelResponse cancel(CancelRequest stopRequest) {
        return proxy(stopRequest, stopRequest.flinkVersion(), CANCEL_REQUEST, "cancel");
    }

    public static DeployResponse deploy(DeployRequest deployRequest) {
        return proxy(deployRequest, deployRequest.flinkVersion(), DEPLOY_REQUEST, "deploy");
    }

    public static ShutDownResponse shutdown(ShutDownRequest shutDownRequest) {
        return proxy(shutDownRequest, shutDownRequest.flinkVersion(), SHUTDOWN_REQUEST, "shutdown");
    }

    public static SavepointResponse triggerSavepoint(TriggerSavepointRequest savepointRequest) {
        return proxy(
            savepointRequest, savepointRequest.flinkVersion(), SAVEPOINT_REQUEST, "triggerSavepoint");
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(
                               Object request,
                               FlinkVersion flinkVersion,
                               String requestClassName,
                               String methodName) {
        flinkVersion.checkVersion();
        return FlinkShimsProxy.proxy(
            flinkVersion,
            (Function<ClassLoader, T>) classLoader -> {
                try {
                    Class<?> submitClass = classLoader.loadClass(FLINK_CLIENT_ENTRYPOINT_CLASS);
                    Class<?> requestClass = classLoader.loadClass(requestClassName);
                    java.lang.reflect.Method method =
                        submitClass.getDeclaredMethod(methodName, requestClass);
                    method.setAccessible(true);
                    Object obj =
                        method.invoke(
                            null, FlinkShimsProxy.getObject(classLoader, request));
                    if (obj == null) {
                        return null;
                    }
                    return FlinkShimsProxy.getObject(FlinkClient.class.getClassLoader(), obj);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
    }
}
