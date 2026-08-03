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

package org.apache.streampark.flink.kubernetes.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrackIdTest {

    @Test
    void applicationTrackShouldBeLegalWithNamespaceAndClusterId() {
        TrackId trackId =
            TrackId.onApplication("ns1", "cluster-a", 1L, null, null, null);
        assertThat(trackId.isLegal()).isTrue();
        assertThat(trackId.isActive()).isFalse();
    }

    @Test
    void sessionTrackShouldRequireJobIdToBeActive() {
        TrackId trackId = TrackId.onSession("ns1", "cluster-a", 1L, "job-1", null, null);
        assertThat(trackId.isLegal()).isTrue();
        assertThat(trackId.isActive()).isTrue();
    }

    @Test
    void incompleteSessionTrackShouldBeIllegal() {
        TrackId trackId = TrackId.onSession("ns1", "cluster-a", 1L, null, null, null);
        assertThat(trackId.isLegal()).isFalse();
    }

    @Test
    void equalsShouldCompareIdentityFields() {
        TrackId left = TrackId.onApplication("ns1", "cluster-a", 1L, "job-1", "g1", null);
        TrackId right = TrackId.onApplication("ns1", "cluster-a", 1L, "job-1", "g1", null);
        assertThat(left).isEqualTo(right);
        assertThat(left.hashCode()).isEqualTo(right.hashCode());
    }
}
