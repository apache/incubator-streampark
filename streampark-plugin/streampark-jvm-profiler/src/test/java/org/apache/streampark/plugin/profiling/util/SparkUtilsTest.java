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

package org.apache.streampark.plugin.profiling.util;

import org.apache.streampark.plugin.profiling.Arguments;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SparkUtilsTest {

    @Test
    void probeAppId() {
        Assertions.assertNull(SparkUtils.probeAppId(Arguments.ARG_APP_ID_REGEX));
        Assertions.assertEquals("jar", SparkUtils.probeAppId("jar"));
    }

    @Test
    void getAppId() {
        Assertions.assertNull(SparkUtils.getSparkEnvAppId());
    }

    @Test
    void probeRole() {
        Assertions.assertEquals(
            "executor",
            SparkUtils.probeRole("java org.apache.spark.executor.CoarseGrainedExecutorBackend"));
        Assertions.assertEquals("driver", SparkUtils.probeRole("java org.apache.spark.MockDriver"));
        Assertions.assertNull(SparkUtils.probeRole("java foo"));
    }
}
