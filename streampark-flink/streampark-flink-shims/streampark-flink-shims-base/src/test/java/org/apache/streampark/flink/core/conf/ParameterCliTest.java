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

package org.apache.streampark.flink.core.conf;

import org.apache.streampark.common.conf.ConfigKeys;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterCliTest {

    @TempDir
    Path tempDir;

    @Test
    void readVmOptForNonUrlClassLoader() {
        String result = ParameterCli.read(new String[]{"--vmopt"});
        assertThat(result).contains("--add-opens");
    }

    @Test
    void readAppNameFromPropertiesFile() throws Exception {
        Path conf =
            tempDir.resolve(
                "app.properties");
        Files.writeString(
            conf,
            ConfigKeys.KEY_FLINK_PROPERTY_PREFIX()
                + ConfigKeys.KEY_FLINK_APP_NAME()
                + "=demo_app\n");

        String name = ParameterCli.read(new String[]{"--name", conf.toString()});
        assertThat(name).isEqualTo("demo_app");
    }

    @Test
    void readPropertyFromYamlFile() throws Exception {
        Path conf = tempDir.resolve("app.yml");
        Files.writeString(
            conf,
            ConfigKeys.KEY_FLINK_PROPERTY_PREFIX()
                + ConfigKeys.KEY_FLINK_APP_NAME()
                + ": demo_yaml\n");

        String property = ParameterCli.read(new String[]{"--property", conf.toString()});
        assertThat(property).contains("-D" + ConfigKeys.KEY_FLINK_APP_NAME() + "=demo_yaml");
    }

    @Test
    void emitWritesToProvidedStream() {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        ParameterCli.emit("hello", new java.io.PrintStream(buffer));
        assertThat(buffer.toString()).isEqualTo("hello");
    }
}
