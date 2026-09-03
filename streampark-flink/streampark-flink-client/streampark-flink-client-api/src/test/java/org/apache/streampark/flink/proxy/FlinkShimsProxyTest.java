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

package org.apache.streampark.flink.proxy;

import org.apache.streampark.common.configuration.option.CoreOptions;
import org.apache.streampark.common.core.FlinkVersion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class FlinkShimsProxyTest {

    private static final String VERSION = "3.0.0.jar";

    @TempDir
    Path temporaryDirectory;

    @Test
    void includeOnlyTheSharedBaseArtifact() {
        assertThat(includeReason("streampark-flink-shims-base-" + VERSION, "1.20")).isNotNull();
        assertThat(includeReason("streampark-flink-shims-base-" + VERSION, "2.0")).isNotNull();
        assertThat(includeReason("streampark-flink-shims-base-v2-" + VERSION, "1.20")).isNull();
        assertThat(includeReason("streampark-flink-shims-base-v2-" + VERSION, "2.0")).isNull();
    }

    @Test
    void includeOnlyRequestedShim() {
        assertThat(includeReason("streampark-flink-shims_flink-2.0-" + VERSION, "2.0")).isNotNull();
        assertThat(includeReason("streampark-flink-shims_flink-2.1-" + VERSION, "2.0")).isNull();
        assertThat(includeReason("streampark-flink-shims_flink-1.20_2.12-" + VERSION, "1.20"))
            .isNotNull();
    }

    @Test
    void sqlLoaderIncludesTargetDist() throws Exception {
        Path flinkHome = Files.createDirectories(temporaryDirectory.resolve("flink"));
        Path flinkLib = Files.createDirectories(flinkHome.resolve("lib"));
        Path flinkOpt = Files.createDirectories(flinkHome.resolve("opt"));
        Files.createFile(flinkLib.resolve("flink-dist-1.19.99.jar"));
        Files.createFile(flinkLib.resolve("flink-table-api-1.19.99.jar"));
        Files.createFile(flinkOpt.resolve("flink-table-planner-1.19.99.jar"));
        Path appHome = Files.createDirectories(temporaryDirectory.resolve("streampark"));
        Files.createDirectories(appHome.resolve("lib"));

        String propertyKey = CoreOptions.APP_HOME.key();
        String previousAppHome = System.getProperty(propertyKey);
        try {
            System.setProperty(propertyKey, appHome.toString());
            URL[] urls =
                FlinkShimsProxy.proxyVerifySql(
                    new FlinkVersion(flinkHome.toString()),
                    loader -> ((URLClassLoader) loader).getURLs());

            assertThat(Arrays.stream(urls).map(url -> Path.of(url.getPath()).getFileName().toString()))
                .contains(
                    "flink-dist-1.19.99.jar",
                    "flink-table-api-1.19.99.jar",
                    "flink-table-planner-1.19.99.jar");
        } finally {
            if (previousAppHome == null) {
                System.clearProperty(propertyKey);
            } else {
                System.setProperty(propertyKey, previousAppHome);
            }
        }
    }

    private static String includeReason(String jarName, String majorVersion) {
        return FlinkShimsProxy.matchShimIncludeReason(jarName, majorVersion, "2.12");
    }
}
