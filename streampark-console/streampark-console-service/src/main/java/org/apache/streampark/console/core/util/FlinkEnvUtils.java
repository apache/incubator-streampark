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

package org.apache.streampark.console.core.util;

import org.apache.streampark.common.configuration.ConfigException;
import org.apache.streampark.common.core.FlinkVersion;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.FlinkConfigurationUtils;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.ApiDetailException;
import org.apache.streampark.console.core.entity.FlinkEnv;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Handles version discovery and configuration conversion for persisted Flink environments.
 *
 * <p>{@link FlinkEnv} remains a database entity. Installation inspection, YAML format metadata,
 * compression, and native Flink configuration conversion are kept at this boundary instead of in
 * the persistence model.
 */
public final class FlinkEnvUtils {

    private static final String LEGACY_PREFIX = "legacy:";
    private static final String STANDARD_PREFIX = "standard:";

    private FlinkEnvUtils() {
    }

    /**
     * Refreshes all installation-derived fields after every value has been validated.
     *
     * @param environment target persistence entity
     * @param flinkHome Flink installation directory
     */
    public static void refresh(FlinkEnv environment, String flinkHome) {
        FlinkEnv target = requireEnvironment(environment);
        FlinkVersion detectedVersion = new FlinkVersion(flinkHome);
        String version = detectedVersion.version();
        String scalaVersion = detectedVersion.scalaVersion();
        checkScalaVersion(scalaVersion);
        String configuration = readConfiguration(flinkHome, version);

        // Publish the snapshot only after discovery, compatibility checks, and I/O all succeed.
        target.setFlinkHome(flinkHome);
        target.setVersion(version);
        target.setScalaVersion(scalaVersion);
        target.setFlinkConf(configuration);
    }

    /** Reloads configuration without changing the registered installation metadata. */
    public static void sync(FlinkEnv environment) {
        FlinkEnv target = requireEnvironment(environment);
        target.setFlinkConf(readConfiguration(target.getFlinkHome(), target.getVersion()));
    }

    /** Returns a native Flink version descriptor for the registered installation. */
    public static FlinkVersion version(FlinkEnv environment) {
        FlinkEnv target = requireEnvironment(environment);
        return new FlinkVersion(target.getFlinkHome());
    }

    /** Parses the stored YAML using the format selected when it was synchronized. */
    public static Map<String, String> configuration(FlinkEnv environment) {
        return FlinkConfigurationUtils.loadConfigurationFromString(
            yaml(environment), isStandardYaml(environment));
    }

    /** Returns the parsed environment configuration as Java properties. */
    public static Properties properties(FlinkEnv environment) {
        Properties properties = new Properties();
        properties.putAll(configuration(environment));
        return properties;
    }

    /** Returns the stored configuration as uncompressed YAML text. */
    public static String yaml(FlinkEnv environment) {
        String yaml = DeflaterUtils.unzipString(compressedYaml(environment));
        if (yaml == null) {
            throw new IllegalStateException("Stored Flink configuration is not valid compressed data");
        }
        return yaml;
    }

    /** Returns the compressed payload accepted by the Flink runtime initializer. */
    public static String compressedYaml(FlinkEnv environment) {
        String stored = requireConfiguration(environment);
        if (stored.startsWith(STANDARD_PREFIX)) {
            return stored.substring(STANDARD_PREFIX.length());
        }
        if (stored.startsWith(LEGACY_PREFIX)) {
            return stored.substring(LEGACY_PREFIX.length());
        }
        return stored;
    }

    /** Returns whether the stored document uses standard nested YAML. */
    public static boolean isStandardYaml(FlinkEnv environment) {
        FlinkEnv target = requireEnvironment(environment);
        String stored = requireConfiguration(target);
        if (stored.startsWith(STANDARD_PREFIX)) {
            return true;
        }
        if (stored.startsWith(LEGACY_PREFIX)) {
            return false;
        }
        // Rows written before the marker was introduced use the selection rule of their registered
        // Flink installation. Newly synchronized rows are independent of later filesystem changes.
        return FlinkConfigurationUtils.usesStandardYaml(
            Path.of(target.getFlinkHome(), "conf").toString(), target.getVersion());
    }

    private static String readConfiguration(String flinkHome, String flinkVersion) {
        try {
            File file =
                FlinkConfigurationUtils.resolveConfigurationFile(
                    Path.of(flinkHome, "conf").toString(), flinkVersion);
            String yaml = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            String prefix =
                FlinkConfigurationUtils.isStandardYaml(file) ? STANDARD_PREFIX : LEGACY_PREFIX;
            return prefix + DeflaterUtils.zipString(yaml);
        } catch (ConfigException e) {
            throw new ApiAlertException(e.getMessage(), e);
        } catch (IOException e) {
            throw new ApiDetailException(e);
        }
    }

    private static void checkScalaVersion(String flinkScalaVersion) {
        String streamParkScalaVersion = scala.util.Properties.versionNumberString();
        if (!streamParkScalaVersion.startsWith(flinkScalaVersion)) {
            throw new UnsupportedOperationException(
                String.format(
                    "StreamPark uses Scala %s, but the selected Flink installation uses Scala %s",
                    streamParkScalaVersion,
                    flinkScalaVersion));
        }
    }

    private static FlinkEnv requireEnvironment(FlinkEnv environment) {
        return Objects.requireNonNull(environment, "Flink environment must not be null");
    }

    private static String requireConfiguration(FlinkEnv environment) {
        String configuration = requireEnvironment(environment).getFlinkConf();
        if (configuration == null) {
            throw new IllegalStateException("Flink configuration has not been initialized");
        }
        return configuration;
    }
}
