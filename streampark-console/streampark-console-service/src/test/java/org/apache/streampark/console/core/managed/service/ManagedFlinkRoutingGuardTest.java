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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManagedFlinkRoutingGuardTest {

    @Test
    void shouldRejectManagedApplicationFromLegacyRoute() {
        assertThatThrownBy(
            () -> ManagedFlinkRoutingGuard.rejectLegacyRoute(
                FlinkDeployMode.MANAGED_APPLICATION, "start"))
                    .isInstanceOf(ApiAlertException.class)
                    .hasMessageContaining("ManagedFlinkProvider")
                    .hasMessageContaining("FlinkClient/FsOperator")
                    .hasMessageContaining("start");
    }

    @Test
    void shouldAllowAllLegacyDeployModes() {
        for (FlinkDeployMode deployMode : FlinkDeployMode.values()) {
            if (!FlinkDeployMode.isManagedMode(deployMode)) {
                assertThatCode(
                    () -> ManagedFlinkRoutingGuard.rejectLegacyRoute(deployMode, "legacy-test"))
                        .doesNotThrowAnyException();
            }
        }
    }
}
