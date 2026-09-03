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

import org.apache.streampark.common.configuration.ConfigException;
import org.apache.streampark.common.configuration.SensitiveKeys;
import org.apache.streampark.common.core.FlinkVersion;

import org.apache.streampark.shaded.org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for loading native Flink configuration across legacy and standard YAML formats.
 *
 * <p>Flink versions before 1.19 require the flat {@code flink-conf.yaml} format. Flink 1.19 and
 * 1.20 accept both filenames, prefer {@code flink-conf.yaml} when both exist, and select the parser
 * from the chosen filename. Flink 2.0 removes the legacy filename and requires standard YAML in
 * {@code config.yaml}. Keeping file selection and parser selection together prevents the
 * StreamPark classpath version from changing target-runtime behavior.
 *
 * <p>Directory-based loading always requires the target Flink version. Callers that already know
 * the selected file or transported YAML format can use the corresponding explicit entry point.
 */
public final class FlinkConfigurationUtils {

    /** Filename used by Flink's historical flat configuration syntax. */
    public static final String LEGACY_FLINK_CONF_FILENAME = "flink-conf.yaml";

    /** Filename used by Flink's standard nested YAML syntax. */
    public static final String FLINK_CONF_FILENAME = "config.yaml";

    private static final int DUAL_FORMAT_MAJOR = 1;
    private static final int DUAL_FORMAT_MINOR = 19;
    private static final int STANDARD_ONLY_MAJOR = 2;
    private static final int STANDARD_ONLY_MINOR = 0;
    private static final Pattern FLINK_VERSION_PATTERN =
        Pattern.compile("^(\\d+)\\.(\\d+)(?:\\..*)?$");

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory()
            .getLogger(FlinkConfigurationUtils.class.getName());

    private FlinkConfigurationUtils() {
    }

    /**
     * Loads configuration from a Flink installation using the version discovered from its dist
     * jar.
     *
     * @param flinkHome Flink installation directory
     * @return loaded Flink configuration entries
     * @throws ConfigException when the installation or its required configuration is invalid
     */
    public static Map<String, String> loadFlinkHome(String flinkHome) {
        String home = Objects.requireNonNull(flinkHome, "Flink home must not be null");
        FlinkVersion flinkVersion = new FlinkVersion(home);
        return loadConfiguration(
            Path.of(home, "conf").toString(), flinkVersion.version());
    }

    /**
     * Loads the configuration file required by a concrete Flink version.
     *
     * <p>Flink 1.19 and 1.20 prefer {@code flink-conf.yaml}, with {@code config.yaml} as fallback.
     * Earlier releases require the legacy file, while Flink 2.0 and later require standard YAML.
     *
     * @param configDirectory Flink configuration directory
     * @param flinkVersion target Flink version in major-minor or major-minor-patch form
     * @return loaded Flink configuration entries
     * @throws ConfigException when the required file is absent or parsing fails
     */
    public static Map<String, String> loadConfiguration(
                                                        String configDirectory,
                                                        String flinkVersion) {
        return loadConfigurationFromFile(
            resolveConfigurationFile(configDirectory, flinkVersion));
    }

    /**
     * Loads a single Flink YAML file, selecting legacy behavior by its filename.
     *
     * <p>The exact filename {@code flink-conf.yaml} uses Flink's historical line parser, while
     * {@code config.yaml} uses the standard YAML parser. Other filenames are rejected because the
     * parser cannot be inferred safely from content.
     *
     * @param file existing Flink YAML file
     * @return loaded Flink configuration entries
     * @throws ConfigException when the file is absent or invalid
     */
    public static Map<String, String> loadConfigurationFromFile(File file) {
        if (file == null || !file.isFile()) {
            throw new ConfigException("Flink configuration file does not exist: " + file);
        }
        boolean standardYaml = isStandardYaml(file);
        LOG.info(
            "Loading {} Flink YAML configuration from {}",
            standardYaml ? "standard" : "legacy",
            file);
        Path path = file.toPath();
        Map<String, String> configuration = standardYaml ? loadStandard(path) : loadLegacy(path);
        logConfiguration("Loaded", configuration);
        return configuration;
    }

    /**
     * Returns the parser mode defined by a supported Flink configuration filename.
     *
     * @param file Flink configuration file
     * @return {@code true} for {@code config.yaml}, {@code false} for {@code flink-conf.yaml}
     * @throws ConfigException when the filename is not supported
     */
    public static boolean isStandardYaml(File file) {
        if (file == null) {
            throw new IllegalArgumentException("Flink configuration file must not be null");
        }
        if (FLINK_CONF_FILENAME.equals(file.getName())) {
            return true;
        }
        if (LEGACY_FLINK_CONF_FILENAME.equals(file.getName())) {
            return false;
        }
        throw new ConfigException("Unsupported Flink configuration filename: " + file.getName());
    }

    /**
     * Resolves the configuration file for a Flink installation without conflating version and YAML
     * syntax compatibility.
     *
     * <p>The transition rule mirrors Flink's rollout of standard YAML:
     *
     * <ul>
     *   <li>Flink before 1.19: {@code flink-conf.yaml}
     *   <li>Flink 1.19 and 1.20: prefer {@code flink-conf.yaml}, otherwise {@code config.yaml}
     *   <li>Flink 2.0 and later: {@code config.yaml}
     * </ul>
     *
     * @param configDirectory Flink configuration directory
     * @param flinkVersion target Flink version
     * @return selected configuration file
     * @throws ConfigException when the version is invalid or the required file is absent
     */
    public static File resolveConfigurationFile(
                                                String configDirectory,
                                                String flinkVersion) {
        if (configDirectory == null) {
            throw new IllegalArgumentException("Flink configuration directory must not be null");
        }
        Path directory = requireDirectory(configDirectory);
        if (compareVersion(flinkVersion, DUAL_FORMAT_MAJOR, DUAL_FORMAT_MINOR) < 0) {
            return requireFile(directory, LEGACY_FLINK_CONF_FILENAME, flinkVersion);
        }
        if (compareVersion(flinkVersion, STANDARD_ONLY_MAJOR, STANDARD_ONLY_MINOR) < 0) {
            Path legacyFile = directory.resolve(LEGACY_FLINK_CONF_FILENAME);
            if (Files.isRegularFile(legacyFile)) {
                return legacyFile.toFile();
            }
        }
        return requireFile(directory, FLINK_CONF_FILENAME, flinkVersion);
    }

    /**
     * Returns whether the selected configuration uses standard YAML.
     *
     * <p>Flink 1.19 and 1.20 select the mode from the preferred existing filename. Other releases
     * have an unambiguous format contract.
     *
     * @param configDirectory Flink configuration directory
     * @param flinkVersion target Flink version
     * @return {@code true} when the selected file must use standard YAML semantics
     * @throws ConfigException when the version is invalid
     */
    public static boolean usesStandardYaml(
                                           String configDirectory,
                                           String flinkVersion) {
        if (configDirectory == null) {
            throw new IllegalArgumentException("Flink configuration directory must not be null");
        }
        return isStandardYaml(resolveConfigurationFile(configDirectory, flinkVersion));
    }

    /**
     * Parses standard, nested Flink YAML text.
     *
     * @param yaml non-blank standard YAML content
     * @return loaded Flink configuration entries
     * @throws ConfigException when the document is empty or invalid
     */
    public static Map<String, String> loadConfigurationFromString(String yaml) {
        return loadConfigurationFromString(yaml, true);
    }

    /**
     * Parses Flink YAML text using explicit standard or legacy semantics.
     *
     * <p>The format flag is required when configuration has been compressed or transported without
     * its filename. Guessing from document content is deliberately avoided because valid scalar
     * values can be interpreted differently by the two parsers.
     *
     * @param yaml non-blank YAML content
     * @param standardYaml whether to use the standard structured YAML parser
     * @return loaded Flink configuration entries
     * @throws ConfigException when the document is empty or invalid
     */
    public static Map<String, String> loadConfigurationFromString(
                                                                  String yaml,
                                                                  boolean standardYaml) {
        if (yaml == null || yaml.trim().isEmpty()) {
            throw new ConfigException("Flink YAML configuration must not be empty");
        }
        return standardYaml ? loadStandard(yaml) : loadLegacy(yaml);
    }

    private static Map<String, String> loadStandard(Path path) {
        return toConfigurationMap(YamlParser.flatten(YamlParser.parse(path)));
    }

    private static Map<String, String> loadStandard(String yaml) {
        return toConfigurationMap(YamlParser.flatten(YamlParser.parse(yaml)));
    }

    private static Map<String, String> toConfigurationMap(Map<String, Object> values) {
        Map<String, String> configuration = new LinkedHashMap<>();
        // Standard YAML permits nested mappings and sequences. Mappings have already been
        // flattened; lists retain YAML flow syntax because Flink expects values as strings.
        values.forEach(
            (key, value) -> configuration.put(
                key,
                value instanceof List
                    ? YamlParser.toFlowString(value)
                    : Objects.toString(value)));
        return configuration;
    }

    private static Map<String, String> loadLegacy(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return loadLegacy(reader, path.toAbsolutePath().normalize().toString());
        } catch (IOException e) {
            throw new ConfigException("Cannot read legacy Flink configuration from " + path, e);
        }
    }

    private static Map<String, String> loadLegacy(String yaml) {
        try (BufferedReader reader = new BufferedReader(new java.io.StringReader(yaml))) {
            return loadLegacy(reader, "in-memory legacy Flink YAML");
        } catch (IOException e) {
            throw new ConfigException("Cannot read in-memory legacy Flink configuration", e);
        }
    }

    /**
     * Reproduces Flink's historical line-oriented format. Invalid lines are ignored with a warning
     * because older Flink releases use the same compatibility behavior during startup.
     */
    private static Map<String, String> loadLegacy(BufferedReader reader, String resourceName) throws IOException {
        Map<String, String> configuration = new LinkedHashMap<>();
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            int commentStart = line.indexOf('#');
            String content = (commentStart < 0 ? line : line.substring(0, commentStart)).trim();
            if (!content.isEmpty()) {
                parseLegacyLine(configuration, content, resourceName, lineNumber);
            }
        }
        return configuration;
    }

    /** Parses one non-empty legacy Flink configuration line. */
    private static void parseLegacyLine(
                                        Map<String, String> configuration,
                                        String content,
                                        String resourceName,
                                        int lineNumber) {
        int separator = content.indexOf(": ");
        if (separator < 0) {
            LOG.warn(
                "Ignoring invalid Flink configuration line {}:{}; expected 'key: value'",
                resourceName,
                lineNumber);
            return;
        }
        String key = content.substring(0, separator).trim();
        String value = content.substring(separator + 2).trim();
        if (key.isEmpty() || value.isEmpty()) {
            LOG.warn(
                "Ignoring empty Flink configuration entry at {}:{}", resourceName, lineNumber);
            return;
        }
        configuration.put(key, value);
    }

    private static void logConfiguration(String action, Map<String, String> configuration) {
        LOG.info("{} {} Flink configuration properties", action, configuration.size());
        configuration.forEach(
            (key, value) -> LOG.debug(
                "{} Flink configuration property: {}={}",
                action,
                key,
                SensitiveKeys.isSensitive(key) ? SensitiveKeys.MASK : value));
    }

    private static Path requireDirectory(String configDirectory) {
        Path directory = Path.of(configDirectory);
        if (!Files.isDirectory(directory)) {
            throw new ConfigException(
                "Flink configuration directory does not exist: "
                    + directory.toAbsolutePath().normalize());
        }
        return directory;
    }

    private static File requireFile(Path directory, String filename, String flinkVersion) {
        Path selected = directory.resolve(filename);
        if (!Files.isRegularFile(selected)) {
            throw new ConfigException(
                "Flink "
                    + flinkVersion
                    + " configuration file does not exist: "
                    + selected.toAbsolutePath().normalize());
        }
        return selected.toFile();
    }

    private static int compareVersion(String flinkVersion, int targetMajor, int targetMinor) {
        if (flinkVersion == null) {
            throw new IllegalArgumentException("Flink version must not be null");
        }
        Matcher matcher = FLINK_VERSION_PATTERN.matcher(flinkVersion.trim());
        if (!matcher.matches()) {
            throw new ConfigException("Invalid Flink version: " + flinkVersion);
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        // Decimal parsing would incorrectly place version 1.20 before version 1.19.
        int majorComparison = Integer.compare(major, targetMajor);
        return majorComparison != 0
            ? majorComparison
            : Integer.compare(minor, targetMinor);
    }
}
