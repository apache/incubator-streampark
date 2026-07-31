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

package org.apache.streampark.console.core.managed.service;

import org.apache.streampark.common.enums.FlinkDeployMode;
import org.apache.streampark.console.base.exception.ApiAlertException;

/** Prevents managed applications from entering legacy Flink client and workspace routes. */
public final class ManagedFlinkRoutingGuard {

    private ManagedFlinkRoutingGuard() {
    }

    public static void rejectLegacyRoute(FlinkDeployMode deployMode, String operation) {
        ApiAlertException.throwIfTrue(
            FlinkDeployMode.isManagedMode(deployMode),
            "Managed Flink operation '%s' must be routed through ManagedFlinkProvider; "
                + "the legacy FlinkClient/FsOperator route is prohibited.",
            operation);
    }

    public static void rejectLegacyClusterRoute(
                                                FlinkDeployMode deployMode, String operation) {
        ApiAlertException.throwIfTrue(
            FlinkDeployMode.isManagedMode(deployMode),
            "Managed Flink cluster operation '%s' must be routed through "
                + "ManagedFlinkEnvironmentService; the legacy cluster route is prohibited.",
            operation);
    }
}
