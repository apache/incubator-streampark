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

import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.flink.client.impl.AbstractFlinkClient;
import org.apache.streampark.flink.client.impl.KubernetesNativeApplicationClient;
import org.apache.streampark.flink.client.impl.KubernetesNativeSessionClient;
import org.apache.streampark.flink.client.impl.LocalClient;
import org.apache.streampark.flink.client.impl.RemoteClient;
import org.apache.streampark.flink.client.impl.YarnApplicationClient;
import org.apache.streampark.flink.client.impl.YarnPerJobClient;
import org.apache.streampark.flink.client.impl.YarnSessionClient;
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

import org.apache.flink.util.FlinkException;

import java.util.EnumMap;
import java.util.Map;

/** Entry point for Flink client operations routed by deploy mode. */
public final class FlinkClientEntrypoint {

    private static final Map<FlinkDeployMode, AbstractFlinkClient> CLIENTS =
        new EnumMap<>(FlinkDeployMode.class);

    static {
        CLIENTS.put(FlinkDeployMode.LOCAL, LocalClient.INSTANCE);
        CLIENTS.put(FlinkDeployMode.REMOTE, RemoteClient.INSTANCE);
        CLIENTS.put(FlinkDeployMode.YARN_APPLICATION, YarnApplicationClient.INSTANCE);
        CLIENTS.put(FlinkDeployMode.YARN_SESSION, YarnSessionClient.INSTANCE);
        CLIENTS.put(FlinkDeployMode.YARN_PER_JOB, YarnPerJobClient.INSTANCE);
        CLIENTS.put(
            FlinkDeployMode.KUBERNETES_NATIVE_SESSION, KubernetesNativeSessionClient.INSTANCE);
        CLIENTS.put(
            FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION,
            KubernetesNativeApplicationClient.INSTANCE);
    }

    private FlinkClientEntrypoint() {
    }

    @FunctionalInterface
    private interface ClientInvoker<R> {

        R invoke(AbstractFlinkClient client) throws FlinkException;
    }

    private static <R> R invokeClient(
                                      FlinkDeployMode deployMode,
                                      ClientInvoker<R> invoker,
                                      String action) throws FlinkException {
        AbstractFlinkClient client = CLIENTS.get(deployMode);
        if (client != null) {
            return invoker.invoke(client);
        }
        throw new UnsupportedOperationException("Unsupported " + deployMode + " " + action);
    }

    /** Submits an application through the client selected by its deploy mode. */
    public static SubmitResponse submit(SubmitRequest submitRequest) throws FlinkException {
        return invokeClient(
            submitRequest.deployMode(), client -> client.submit(submitRequest), "submit");
    }

    /** Cancels a running job through the client selected by its deploy mode. */
    public static CancelResponse cancel(CancelRequest cancelRequest) throws FlinkException {
        return invokeClient(
            cancelRequest.deployMode(), client -> client.cancel(cancelRequest), "cancel");
    }

    /** Triggers a savepoint through the client selected by its deploy mode. */
    public static SavepointResponse triggerSavepoint(TriggerSavepointRequest savepointRequest) throws FlinkException {
        return invokeClient(
            savepointRequest.deployMode(),
            client -> client.triggerSavepoint(savepointRequest),
            "triggerSavepoint");
    }

    /** Deploys a persistent session cluster. */
    public static DeployResponse deploy(DeployRequest deployRequest) throws Exception {
        if (deployRequest.deployMode() == FlinkDeployMode.YARN_SESSION) {
            return YarnSessionClient.INSTANCE.deploy(deployRequest);
        }
        if (deployRequest.deployMode() == FlinkDeployMode.KUBERNETES_NATIVE_SESSION) {
            return KubernetesNativeSessionClient.INSTANCE.deploy(deployRequest);
        }
        throw new UnsupportedOperationException(
            "Unsupported " + deployRequest.deployMode() + " deploy cluster ");
    }

    /** Shuts down a persistent session cluster. */
    public static ShutdownResponse shutdown(ShutdownRequest shutdownRequest) throws Exception {
        if (shutdownRequest.deployMode() == FlinkDeployMode.YARN_SESSION) {
            return YarnSessionClient.INSTANCE.shutdown(shutdownRequest);
        }
        if (shutdownRequest.deployMode() == FlinkDeployMode.KUBERNETES_NATIVE_SESSION) {
            return KubernetesNativeSessionClient.INSTANCE.shutdown(shutdownRequest);
        }
        throw new UnsupportedOperationException(
            "Unsupported " + shutdownRequest.deployMode() + " shutdown cluster ");
    }
}
