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

package org.apache.streampark.common.conf;

import org.apache.streampark.common.util.CommandUtils;
import org.apache.streampark.common.util.SparkEnvUtils;

import org.apache.streampark.shaded.org.slf4j.Logger;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @param sparkHome actual spark home that must be a readable local path */
public class SparkVersion implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG =
        org.apache.streampark.common.util.StreamParkLoggerFactory.loggerFactory()
            .getLogger(SparkVersion.class.getName());

    private static final Pattern SPARK_VER_PATTERN = Pattern.compile("^(\\d+\\.\\d+)(\\.)?.*$");
    private static final Pattern SPARK_VERSION_PATTERN =
        Pattern.compile("\\s{2}version\\s(\\d+\\.\\d+\\.\\d+)");
    private static final Pattern SPARK_SCALA_VERSION_PATTERN =
        Pattern.compile("Using\\sScala\\sversion\\s(\\d+\\.\\d+)");
    private static final Pattern SPARK_RELEASE_VERSION_PATTERN =
        Pattern.compile("^Spark\\s+(\\d+\\.\\d+\\.\\d+)");
    private static final Pattern SPARK_CORE_JAR_PATTERN =
        Pattern.compile("^spark-core_(\\d+\\.\\d+)-(\\d+\\.\\d+\\.\\d+)\\.jar$");

    /** Spark installation directory (Scala {@code sparkHome} accessor). */
    public final String sparkHome;
    private transient String version;
    private transient String scalaVersion;

    public SparkVersion(String sparkHome) {
        this.sparkHome = sparkHome;
    }

    public String getSparkHome() {
        return sparkHome;
    }

    public String fullVersion() {
        return getFullVersion();
    }

    public String majorVersion() {
        return getMajorVersion();
    }

    public String scalaVersion() {
        return getScalaVersion();
    }

    public String version() {
        return getVersion();
    }

    public String getVersion() {
        if (version == null) {
            String[] parsed = parseVersion();
            version = parsed[0];
            scalaVersion = parsed[1];
        }
        return version;
    }

    public String getScalaVersion() {
        if (scalaVersion == null) {
            String[] parsed = parseVersion();
            version = parsed[0];
            scalaVersion = parsed[1];
        }
        return scalaVersion;
    }

    public String getMajorVersion() {
        if (getVersion() == null) {
            return null;
        }
        Matcher matcher = SPARK_VER_PATTERN.matcher(getVersion());
        matcher.matches();
        return matcher.group(1);
    }

    public String getFullVersion() {
        return getVersion() + "_" + getScalaVersion();
    }

    public Optional<String> getJavaHome() {
        return SparkEnvUtils.resolveJavaHome(sparkHome, getVersion());
    }

    public File getSparkLib() {
        if (sparkHome == null) {
            throw new IllegalArgumentException("[StreamPark] sparkHome must not be null.");
        }
        if (!new File(sparkHome).exists()) {
            throw new IllegalArgumentException("[StreamPark] sparkHome must be exists.");
        }
        File lib = new File(sparkHome + "/jars");
        if (!lib.exists() || !lib.isDirectory()) {
            throw new IllegalArgumentException(
                "[StreamPark] " + sparkHome + "/jars must be exists and must be directory.");
        }
        return lib;
    }

    public boolean checkVersion() {
        return checkVersion(true);
    }

    public boolean checkVersion(boolean throwException) {
        String[] parts = getVersion().split("\\.");
        if (parts.length >= 2) {
            try {
                int major = Integer.parseInt(parts[0].trim());
                int minor = Integer.parseInt(parts[1].trim());
                if (major == 3 && minor >= 5) {
                    return true;
                }
                if (major == 4) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if (throwException) {
            throw new UnsupportedOperationException("Unsupported spark version: " + getVersion());
        }
        return false;
    }

    private String[] parseVersion() {
        return parseFromSparkCoreJar()
            .or(this::parseFromReleaseFile)
            .or(this::parseFromSparkSubmit)
            .orElseThrow(
                () -> new IllegalStateException(
                    "[StreamPark] parse spark version failed for sparkHome: "
                        + sparkHome
                        + ". Please check whether $SPARK_HOME/jars/spark-core_*.jar or RELEASE exists."));
    }

    private Optional<String[]> parseFromSparkCoreJar() {
        File jarsDir = new File(sparkHome, "jars");
        if (!jarsDir.exists() || !jarsDir.isDirectory()) {
            return Optional.empty();
        }
        File[] files = jarsDir.listFiles();
        if (files == null) {
            return Optional.empty();
        }
        for (File file : files) {
            Matcher matcher = SPARK_CORE_JAR_PATTERN.matcher(file.getName());
            if (matcher.matches()) {
                String parsedScala = matcher.group(1);
                String parsedVersion = matcher.group(2);
                LOG.info(
                    "Spark version parsed from spark-core jar name: {}, scala: {}",
                    parsedVersion,
                    parsedScala);
                return Optional.of(new String[]{parsedVersion, parsedScala});
            }
        }
        return Optional.empty();
    }

    private Optional<String[]> parseFromReleaseFile() {
        File releaseFile = new File(sparkHome, "RELEASE");
        if (!releaseFile.exists()) {
            return Optional.empty();
        }
        try {
            String content = new String(Files.readAllBytes(releaseFile.toPath()), StandardCharsets.UTF_8);
            String firstLine = content.trim().split("\n")[0];
            Matcher matcher = SPARK_RELEASE_VERSION_PATTERN.matcher(firstLine);
            if (!matcher.find()) {
                return Optional.empty();
            }
            return parseFromSparkCoreJar()
                .map(
                    pair -> {
                        String parsedVersion = matcher.group(1);
                        LOG.info(
                            "Spark version parsed from RELEASE file: {}, scala: {}",
                            parsedVersion,
                            pair[1]);
                        return new String[]{parsedVersion, pair[1]};
                    });
        } catch (Exception e) {
            LOG.warn("Failed to parse Spark RELEASE file from {}", releaseFile, e);
            return Optional.empty();
        }
    }

    private Optional<String[]> parseFromSparkSubmit() {
        final String[] sparkVersion = {null, null};
        StringBuilder buffer = new StringBuilder();
        String javaHomeExport =
            SparkEnvUtils.resolveJavaHome(sparkHome, hintSparkVersion())
                .map(javaHome -> "export JAVA_HOME=" + javaHome + "&&")
                .orElse("");
        List<String> cmd =
            Arrays.asList(
                "export SPARK_HOME="
                    + sparkHome
                    + "&&"
                    + javaHomeExport
                    + sparkHome
                    + "/bin/spark-submit --version");
        try {
            CommandUtils.execute(
                sparkHome,
                cmd,
                new Consumer<String>() {

                    @Override
                    public void accept(String out) {
                        buffer.append(out).append("\n");
                        Matcher matcher = SPARK_VERSION_PATTERN.matcher(out);
                        if (matcher.find()) {
                            sparkVersion[0] = matcher.group(1);
                        }
                        Matcher scalaMatcher = SPARK_SCALA_VERSION_PATTERN.matcher(out);
                        if (scalaMatcher.find()) {
                            sparkVersion[1] = scalaMatcher.group(1);
                        }
                    }
                });
        } catch (Exception e) {
            LOG.warn("Failed to parse Spark version from spark-submit", e);
            return Optional.empty();
        }
        LOG.info("[StreamPark] {}", buffer);
        if (sparkVersion[0] != null && sparkVersion[1] != null) {
            LOG.info(
                "Spark version parsed from spark-submit: {}, scala: {}",
                sparkVersion[0],
                sparkVersion[1]);
            return Optional.of(new String[]{sparkVersion[0], sparkVersion[1]});
        }
        return Optional.empty();
    }

    private String hintSparkVersion() {
        Optional<String[]> fromJar = parseFromSparkCoreJar();
        if (fromJar.isPresent()) {
            return fromJar.get()[0];
        }
        File releaseFile = new File(sparkHome, "RELEASE");
        if (releaseFile.exists()) {
            try {
                String content =
                    new String(Files.readAllBytes(releaseFile.toPath()), StandardCharsets.UTF_8);
                String firstLine = content.trim().split("\n")[0];
                Matcher matcher = SPARK_RELEASE_VERSION_PATTERN.matcher(firstLine);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception ignored) {
            }
        }
        return "3.5.0";
    }

    @Override
    public String toString() {
        return "\n----------------------------------------- spark version -----------------------------------\n"
            + "     sparkHome    : "
            + sparkHome
            + "\n     sparkVersion : "
            + getVersion()
            + "\n     scalaVersion : "
            + getScalaVersion()
            + "\n     javaHome     : "
            + getJavaHome().orElse("not resolved")
            + "\n-------------------------------------------------------------------------------------------\n";
    }
}
