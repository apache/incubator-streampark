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

package org.apache.streampark.common.configuration.option;

import org.apache.streampark.common.configuration.ConfigOption;
import org.apache.streampark.common.configuration.ConfigOptions;

/**
 * Engine-neutral deployment metadata passed between console and submission clients.
 *
 * <p>Options represent transport contracts only; deployment-mode validation belongs to the
 * submission client that understands the selected engine and cluster manager.
 */
public final class DeploymentOptions {

    /** YARN application identifier returned by the ResourceManager. */
    public static final ConfigOption<String> YARN_APPLICATION_ID = string("yarn.application.id");

    /** User-facing YARN application name. */
    public static final ConfigOption<String> YARN_APPLICATION_NAME = string("yarn.application.name");

    /** YARN scheduler queue selected for submission. */
    public static final ConfigOption<String> YARN_QUEUE = string("yarn.application.queue");

    /** YARN node label expression selected for submission. */
    public static final ConfigOption<String> YARN_NODE_LABEL = string("yarn.application.node-label");

    /** Kubernetes service account assigned to submitted workloads. */
    public static final ConfigOption<String> KUBERNETES_SERVICE_ACCOUNT =
        string("kubernetes.service-account");

    private DeploymentOptions() {
    }

    private static ConfigOption<String> string(String key) {
        return ConfigOptions.key(key)
            .stringType()
            .noDefaultValue()
            .withDescription("Deployment parameter '" + key + "'.")
            .build();
    }
}
