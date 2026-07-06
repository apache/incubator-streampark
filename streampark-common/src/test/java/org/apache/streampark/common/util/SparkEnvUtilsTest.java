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

package org.apache.streampark.common.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SparkEnvUtilsTest {

    @Test
    void requiredJavaMajorVersionForSpark4() {
        assertThat(SparkEnvUtils.requiredJavaMajorVersion("4.0.0")).isEqualTo(17);
        assertThat(SparkEnvUtils.requiredJavaMajorVersion("4.1.2")).isEqualTo(17);
    }

    @Test
    void requiredJavaMajorVersionForSpark35() {
        assertThat(SparkEnvUtils.requiredJavaMajorVersion("3.5.4")).isEqualTo(8);
    }

    @Test
    void extractJavaHomeFromSparkEnvContent() {
        String content =
                "#!/usr/bin/env bash\n"
                        + "export JAVA_HOME=\"/opt/jdk-17\"\n"
                        + "export HADOOP_CONF_DIR=/etc/hadoop\n";
        Optional<String> javaHome = SparkEnvUtils.extractJavaHome(content);
        assertThat(javaHome).contains("/opt/jdk-17");
    }

    @Test
    void extractJavaHomeWithoutExport() {
        String content = "JAVA_HOME=/usr/lib/jvm/java-17-openjdk\n";
        Optional<String> javaHome = SparkEnvUtils.extractJavaHome(content);
        assertThat(javaHome).contains("/usr/lib/jvm/java-17-openjdk");
    }
}
