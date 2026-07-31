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

import org.apache.streampark.console.core.managed.model.ManagedFlinkApplicationSaveRequest;
import org.apache.streampark.console.core.managed.model.ManagedFlinkReleaseConfig;
import org.apache.streampark.console.core.managed.model.ManagedFlinkRuntimeConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ManagedFlinkDefinitionHasherTest {

    private final ManagedFlinkDefinitionHasher hasher =
        new ManagedFlinkDefinitionHasher(new ObjectMapper());

    @Test
    void shouldProduceStableHashForEquivalentLineEndingsAndMapOrders() {
        ManagedFlinkApplicationSaveRequest first = request("SELECT 1;\r\n", "a", "1", "b", "2");
        ManagedFlinkApplicationSaveRequest second = request("SELECT 1;\n", "b", "2", "a", "1");

        assertThat(hasher.hash(first))
            .isEqualTo(hasher.hash(second))
            .hasSize(64);
    }

    @Test
    void shouldChangeHashWhenCandidateDefinitionChanges() {
        ManagedFlinkApplicationSaveRequest first = request("SELECT 1", "a", "1");
        ManagedFlinkApplicationSaveRequest second = request("SELECT 2", "a", "1");

        assertThat(hasher.hash(first)).isNotEqualTo(hasher.hash(second));
    }

    @Test
    void shouldIncludeProviderRoutingAndJobIdentity() {
        ManagedFlinkApplicationSaveRequest first = request("SELECT 1", "a", "1");
        ManagedFlinkApplicationSaveRequest renamed = request("SELECT 1", "a", "1");
        renamed.setJobName("renamed-job");
        ManagedFlinkApplicationSaveRequest moved = request("SELECT 1", "a", "1");
        moved.setManagedEnvironmentId(200000L);

        assertThat(hasher.hash(first))
            .isNotEqualTo(hasher.hash(renamed))
            .isNotEqualTo(hasher.hash(moved));
    }

    private static ManagedFlinkApplicationSaveRequest request(
                                                              String sql,
                                                              String... properties) {
        ManagedFlinkApplicationSaveRequest request =
            new ManagedFlinkApplicationSaveRequest();
        request.setJobName("managed-job");
        request.setManagedEnvironmentId(100000L);
        request.setJobType("STREAMING_SQL");
        request.setSql(sql);
        ManagedFlinkRuntimeConfig runtime = new ManagedFlinkRuntimeConfig();
        Map<String, String> custom = new LinkedHashMap<>();
        for (int index = 0; index < properties.length; index += 2) {
            custom.put(properties[index], properties[index + 1]);
        }
        runtime.setCustomProperties(custom);
        request.setRuntimeConfig(runtime);
        request.setReleaseConfig(new ManagedFlinkReleaseConfig());
        return request;
    }
}
