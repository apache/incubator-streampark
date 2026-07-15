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

import java.lang.reflect.Method;
import java.security.Permission;

/** Flink client entry for cross-classloader proxy invocation. */
public final class FlinkClient {

    private static final String FLINK_CLIENT_ENTRYPOINT_CLASS =
        "org.apache.streampark.flink.client.FlinkClientEntrypoint";

    private FlinkClient() {
    }

    public static SubmitResponse submit(SubmitRequest submitRequest) {
        SecurityManager securityManager = System.getSecurityManager();
        try {
            System.setSecurityManager(new ExitSecurityManager());
            return proxy(submitRequest, submitRequest.getFlinkVersion(), SubmitRequest.class.getName(), "submit");
        } finally {
            System.setSecurityManager(securityManager);
        }
    }

    public static CancelResponse cancel(CancelRequest cancelRequest) {
        return proxy(
            cancelRequest,
            cancelRequest.getFlinkVersion(),
            CancelRequest.class.getName(),
            "cancel");
    }

    public static DeployResponse deploy(DeployRequest deployRequest) {
        return proxy(
            deployRequest,
            deployRequest.getFlinkVersion(),
            DeployRequest.class.getName(),
            "deploy");
    }

    public static ShutDownResponse shutdown(ShutDownRequest shutDownRequest) {
        return proxy(
            shutDownRequest,
            shutDownRequest.getFlinkVersion(),
            ShutDownRequest.class.getName(),
            "shutdown");
    }

    public static SavepointResponse triggerSavepoint(TriggerSavepointRequest savepointRequest) {
        return proxy(
            savepointRequest,
            savepointRequest.getFlinkVersion(),
            TriggerSavepointRequest.class.getName(),
            "triggerSavepoint");
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(
                               Object request, FlinkVersion flinkVersion, String requestClassName, String methodName) {
        flinkVersion.checkVersion();
        return FlinkShimsProxy.proxy(
            flinkVersion,
            classLoader -> {
                try {
                    Class<?> submitClass = classLoader.loadClass(FLINK_CLIENT_ENTRYPOINT_CLASS);
                    Class<?> requestClass = classLoader.loadClass(requestClassName);
                    Method method = submitClass.getDeclaredMethod(methodName, requestClass);
                    method.setAccessible(true);
                    Object obj =
                        method.invoke(null, FlinkShimsProxy.getObject(classLoader, request));
                    if (obj == null) {
                        return null;
                    }
                    return (T) FlinkShimsProxy.getObject(FlinkClient.class.getClassLoader(), obj);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            });
    }
}

/** Used to mask JVM requests for external operations */
class ExitSecurityManager extends SecurityManager {

    @Override
    public void checkExit(int status) {
        throw new SecurityException(
            "System.exit("
                + status
                + ") was called in your flink job, The job has been stopped, please check your program...");
    }

    @Override
    public void checkPermission(Permission perm) {
    }
}
