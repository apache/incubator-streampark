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
import org.apache.streampark.flink.client.impl.KubernetesNativeApplicationClient;
import org.apache.streampark.flink.client.impl.KubernetesNativeSessionClient;
import org.apache.streampark.flink.client.impl.LocalClient;
import org.apache.streampark.flink.client.impl.RemoteClient;
import org.apache.streampark.flink.client.impl.YarnApplicationClient;
import org.apache.streampark.flink.client.impl.YarnPerJobClient;
import org.apache.streampark.flink.client.impl.YarnSessionClient;
import org.apache.streampark.flink.client.trait.FlinkClientTrait;

import java.util.EnumMap;
import java.util.Map;

/** Entry point for Flink client operations routed by deploy mode. */
public final class FlinkClientEntrypoint {

    private static final Map<FlinkDeployMode, FlinkClientTrait> CLIENTS = new EnumMap<>(FlinkDeployMode.class);

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

    public static SubmitResponse submit(SubmitRequest submitRequest) throws Exception {
        FlinkClientTrait client = CLIENTS.get(submitRequest.deployMode());
        if (client != null) {
            return client.submit(submitRequest);
        }
        throw new UnsupportedOperationException(
            "Unsupported " + submitRequest.deployMode() + " submit ");
    }

    public static CancelResponse cancel(CancelRequest cancelRequest) throws Exception {
        FlinkClientTrait client = CLIENTS.get(cancelRequest.deployMode());
        if (client != null) {
            return client.cancel(cancelRequest);
        }
        throw new UnsupportedOperationException(
            "Unsupported " + cancelRequest.deployMode() + " cancel ");
    }

    public static SavepointResponse triggerSavepoint(TriggerSavepointRequest savepointRequest) throws Exception {
        FlinkClientTrait client = CLIENTS.get(savepointRequest.deployMode());
        if (client != null) {
            return client.triggerSavepoint(savepointRequest);
        }
        throw new UnsupportedOperationException(
            "Unsupported " + savepointRequest.deployMode() + " triggerSavepoint ");
    }

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

    public static ShutDownResponse shutdown(ShutDownRequest shutDownRequest) throws Exception {
        if (shutDownRequest.deployMode() == FlinkDeployMode.YARN_SESSION) {
            return YarnSessionClient.INSTANCE.shutdown(shutDownRequest);
        }
        if (shutDownRequest.deployMode() == FlinkDeployMode.KUBERNETES_NATIVE_SESSION) {
            return KubernetesNativeSessionClient.INSTANCE.shutdown(shutDownRequest);
        }
        throw new UnsupportedOperationException(
            "Unsupported " + shutDownRequest.deployMode() + " shutdown cluster ");
    }
}
