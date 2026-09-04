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

class FlinkMetricCVTest {

    @Test
    void compareJmAndTmMemory() {
        FlinkMetricCV baseline = metric(512, 1024);

        assertThat(baseline.equalsPayload(metric(512, 1024))).isTrue();
        assertThat(baseline.equalsPayload(metric(256, 1024))).isFalse();
        assertThat(baseline.equalsPayload(metric(512, 2048))).isFalse();
    }

    private static FlinkMetricCV metric(int jmMemory, int tmMemory) {
        return FlinkMetricCV.builder()
            .groupId("default")
            .totalJmMemory(jmMemory)
            .totalTmMemory(tmMemory)
            .pollAckTime(1L)
            .build();
    }
}
