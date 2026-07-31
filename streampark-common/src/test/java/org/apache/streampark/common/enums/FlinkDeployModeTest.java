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

package org.apache.streampark.common.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlinkDeployModeTest {

    @Test
    void shouldKeepLegacyNumericMappingsStable() {
        assertEquals(FlinkDeployMode.LOCAL, FlinkDeployMode.of(0));
        assertEquals(FlinkDeployMode.REMOTE, FlinkDeployMode.of(1));
        assertEquals(FlinkDeployMode.YARN_PER_JOB, FlinkDeployMode.of(2));
        assertEquals(FlinkDeployMode.YARN_SESSION, FlinkDeployMode.of(3));
        assertEquals(FlinkDeployMode.YARN_APPLICATION, FlinkDeployMode.of(4));
        assertEquals(FlinkDeployMode.KUBERNETES_NATIVE_SESSION, FlinkDeployMode.of(5));
        assertEquals(FlinkDeployMode.KUBERNETES_NATIVE_APPLICATION, FlinkDeployMode.of(6));
    }

    @Test
    void shouldResolveManagedApplicationByValueAndName() {
        assertEquals(FlinkDeployMode.MANAGED_APPLICATION, FlinkDeployMode.of(7));
        assertEquals(
            FlinkDeployMode.MANAGED_APPLICATION, FlinkDeployMode.of("managed-application"));
        assertTrue(FlinkDeployMode.isManagedMode(7));
        assertTrue(FlinkDeployMode.isManagedMode(FlinkDeployMode.MANAGED_APPLICATION));
    }

    @Test
    void shouldNotClassifyManagedApplicationAsLegacyMode() {
        FlinkDeployMode managed = FlinkDeployMode.MANAGED_APPLICATION;

        assertFalse(FlinkDeployMode.isYarnMode(managed));
        assertFalse(FlinkDeployMode.isKubernetesMode(managed));
        assertFalse(FlinkDeployMode.isSessionMode(managed));
        assertFalse(FlinkDeployMode.isRemoteMode(managed));
    }

    @Test
    void shouldUseExplicitWatcherModeAllowLists() {
        assertEquals(
            Arrays.asList(
                FlinkDeployMode.LOCAL.getMode(),
                FlinkDeployMode.REMOTE.getMode(),
                FlinkDeployMode.YARN_PER_JOB.getMode(),
                FlinkDeployMode.YARN_SESSION.getMode(),
                FlinkDeployMode.YARN_APPLICATION.getMode()),
            FlinkDeployMode.getHttpWatcherModes());
        assertEquals(
            Arrays.asList(
                FlinkDeployMode.REMOTE.getMode(), FlinkDeployMode.YARN_SESSION.getMode()),
            FlinkDeployMode.getClusterWatcherModes());
        assertFalse(
            FlinkDeployMode.getHttpWatcherModes()
                .contains(FlinkDeployMode.MANAGED_APPLICATION.getMode()));
        assertFalse(
            FlinkDeployMode.getClusterWatcherModes()
                .contains(FlinkDeployMode.MANAGED_APPLICATION.getMode()));
        assertFalse(
            FlinkDeployMode.getKubernetesMode()
                .contains(FlinkDeployMode.MANAGED_APPLICATION.getMode()));
    }
}
