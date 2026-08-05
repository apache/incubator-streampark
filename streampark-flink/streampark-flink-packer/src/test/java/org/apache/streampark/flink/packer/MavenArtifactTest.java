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

package org.apache.streampark.flink.packer;

import org.apache.streampark.flink.packer.maven.Artifact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MavenArtifactTest {

    static final String[] ILLEGAL_ARTIFACT_COORDS_CASES = {
            ":::",
            "org.apache.flink:flink-table:",
            ":flink-table:1.13.0",
            "org.apache.flink::1.13.0",
            "org.apache.flink:flink-table:",
            "org.apache.flink:"
    };

    @Test
    void createWithLegalCoords() {
        Artifact art = Artifact.of("org.apache.flink:flink-table:1.13.0");
        assertEquals("org.apache.flink", art.groupId());
        assertEquals("flink-table", art.artifactId());
        assertEquals("1.13.0", art.version());
    }

    @Test
    void createWithIllegalCoords() {
        for (String coord : ILLEGAL_ARTIFACT_COORDS_CASES) {
            assertThrows(IllegalArgumentException.class, () -> Artifact.of(coord));
        }
    }
}
