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

package org.apache.streampark.spark.client;

import org.apache.streampark.common.enums.SparkDeployMode;
import org.apache.streampark.spark.client.bean.CancelRequest;
import org.apache.streampark.spark.client.bean.CancelResponse;
import org.apache.streampark.spark.client.bean.SubmitRequest;
import org.apache.streampark.spark.client.bean.SubmitResponse;
import org.apache.streampark.spark.client.impl.YarnClient;
import org.apache.streampark.spark.client.trait.SparkClientTrait;

import java.util.HashMap;
import java.util.Map;

/** Spark client endpoint dispatching by deploy mode. */
public final class SparkClientEndpoint {

    private static final Map<SparkDeployMode, SparkClientTrait> CLIENTS = new HashMap<>();

    static {
        CLIENTS.put(SparkDeployMode.YARN_CLUSTER, YarnClient.INSTANCE);
        CLIENTS.put(SparkDeployMode.YARN_CLIENT, YarnClient.INSTANCE);
    }

    private SparkClientEndpoint() {
    }

    public static SubmitResponse submit(SubmitRequest submitRequest) throws Exception {
        SparkClientTrait client = CLIENTS.get(submitRequest.getDeployMode());
        if (client == null) {
            throw new UnsupportedOperationException(
                "Unsupported " + submitRequest.getDeployMode() + " spark submit.");
        }
        return client.submit(submitRequest);
    }

    public static CancelResponse cancel(CancelRequest stopRequest) throws Exception {
        SparkClientTrait client = CLIENTS.get(stopRequest.getDeployMode());
        if (client == null) {
            throw new UnsupportedOperationException(
                "Unsupported " + stopRequest.getDeployMode() + " spark stop.");
        }
        return client.cancel(stopRequest);
    }
}
