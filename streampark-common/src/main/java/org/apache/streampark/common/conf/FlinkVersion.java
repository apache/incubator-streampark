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

import org.apache.streampark.shaded.org.slf4j.Logger;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** @param flinkHome actual flink home that must be a readable local path */
public class FlinkVersion implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG =
        org.apache.streampark.common.util.StreamParkLoggerFactory.loggerFactory()
            .getLogger(FlinkVersion.class.getName());

    private static final Pattern FLINK_VER_PATTERN = Pattern.compile("^(\\d+\\.\\d+)(?:\\..*)?$");
    private static final Pattern FLINK_VERSION_PATTERN =
        Pattern.compile("^Version: ([^,]*), Commit ID: (.*)$");
    private static final Pattern FLINK_DIST_UNDERSCORE_PATTERN =
        Pattern.compile(
            "^flink-dist_(\\d+\\.\\d+)-(\\d+\\.\\d+(?:\\.\\d+)?(?:-SNAPSHOT)?)\\.jar$");
    private static final Pattern FLINK_DIST_DASH_PATTERN =
        Pattern.compile("^flink-dist-(\\d+\\.\\d+(?:\\.\\d+)?(?:-SNAPSHOT)?)\\.jar$");
    private static final Pattern APACHE_FLINK_VERSION_PATTERN =
        Pattern.compile("^(\\d+\\.\\d+\\.\\d+)");
    private static final Pattern OTHER_FLINK_VERSION_PATTERN = Pattern.compile("^(\\d+\\.\\d+)-?$");

    /** Flink installation directory (Scala {@code flinkHome} accessor). */
    public final String flinkHome;
    private transient String scalaVersion;
    private transient String version;
    private transient File flinkLib;
    private transient File flinkDistJar;

    public FlinkVersion(String flinkHome) {
        this.flinkHome = flinkHome;
    }

    public String getFlinkHome() {
        return flinkHome;
    }

    /** Scala API alias for {@link #getFullVersion()}. */
    public String fullVersion() {
        return getFullVersion();
    }

    /** Scala API alias for {@link #getMajorVersion()}. */
    public String majorVersion() {
        return getMajorVersion();
    }

    /** Scala API alias for {@link #getScalaVersion()}. */
    public String scalaVersion() {
        return getScalaVersion();
    }

    /** Scala API alias for {@link #getVersion()}. */
    public String version() {
        return getVersion();
    }

    /** Backward-compatible alias for {@link #getFlinkLibs()}. */
    public List<URL> flinkLibs() throws Exception {
        return getFlinkLibs();
    }

    public String getScalaVersion() {
        if (scalaVersion == null) {
            scalaVersion = parseFromDistJar().map(pair -> pair[1]).orElse("2.12");
        }
        return scalaVersion;
    }

    public String getFullVersion() {
        return getVersion() + "_" + getScalaVersion();
    }

    public File getFlinkLib() {
        if (flinkLib == null) {
            if (flinkHome == null) {
                throw new IllegalArgumentException("[StreamPark] flinkHome must not be null.");
            }
            if (!new File(flinkHome).exists()) {
                throw new IllegalArgumentException("[StreamPark] flinkHome must be exists.");
            }
            File lib = new File(flinkHome + "/lib");
            if (!lib.exists() || !lib.isDirectory()) {
                throw new IllegalArgumentException(
                    "[StreamPark] " + flinkHome + "/lib must be exists and must be directory.");
            }
            flinkLib = lib;
        }
        return flinkLib;
    }

    public List<URL> getFlinkLibs() throws Exception {
        File[] files = getFlinkLib().listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(files).map(f -> {
            try {
                return f.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid Flink lib URL: " + f, e);
            }
        }).collect(Collectors.toList());
    }

    public String getVersion() {
        if (version == null) {
            version =
                parseFromDistJar()
                    .map(pair -> pair[0])
                    .orElseGet(this::parseFromCliFrontend);
        }
        return version;
    }

    public String getMajorVersion() {
        if (getVersion() == null) {
            return null;
        }
        Matcher matcher = FLINK_VER_PATTERN.matcher(getVersion());
        matcher.matches();
        return matcher.group(1);
    }

    public File getFlinkDistJar() {
        if (flinkDistJar == null) {
            File[] distJar =
                getFlinkLib().listFiles(
                    f -> {
                        String name = f.getName();
                        return name.startsWith("flink-dist") && name.endsWith(".jar");
                    });
            if (distJar == null || distJar.length == 0) {
                throw new IllegalArgumentException(
                    "[StreamPark] can no found flink-dist jar in " + getFlinkLib());
            }
            if (distJar.length > 1) {
                throw new IllegalArgumentException(
                    "[StreamPark] found multiple flink-dist jar in " + getFlinkLib());
            }
            flinkDistJar = distJar[0];
        }
        return flinkDistJar;
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
                if (major == 1 && minor >= 17 && minor <= 20) {
                    return true;
                }
                if (major == 2 && minor >= 0 && minor <= 3) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if (throwException) {
            throw new UnsupportedOperationException("Unsupported flink version: " + getVersion());
        }
        return false;
    }

    public boolean checkVersion(int sinceVersion) {
        String[] parts = getVersion().split("\\.");
        if (parts.length >= 2) {
            try {
                int major = Integer.parseInt(parts[0].trim());
                int minor = Integer.parseInt(parts[1].trim());
                if (major == 1 && minor >= sinceVersion) {
                    return true;
                }
                if (major == 2) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    private java.util.Optional<String[]> parseFromDistJar() {
        String jarName = getFlinkDistJar().getName();
        Matcher underscoreMatcher = FLINK_DIST_UNDERSCORE_PATTERN.matcher(jarName);
        if (underscoreMatcher.matches()) {
            String parsedVersion = underscoreMatcher.group(2);
            String parsedScala = underscoreMatcher.group(1);
            LOG.info(
                "Flink version parsed from dist jar name: {}, scala: {}",
                parsedVersion,
                parsedScala);
            return java.util.Optional.of(new String[]{parsedVersion, parsedScala});
        }
        Matcher dashMatcher = FLINK_DIST_DASH_PATTERN.matcher(jarName);
        if (dashMatcher.matches()) {
            String parsedVersion = dashMatcher.group(1);
            LOG.info(
                "Flink version parsed from dist jar name: {}, scala: {}",
                parsedVersion,
                "2.12");
            return java.util.Optional.of(new String[]{parsedVersion, "2.12"});
        }
        return java.util.Optional.empty();
    }

    private String parseFromCliFrontend() {
        final String[] flinkVersion = {null};
        StringBuilder buffer = new StringBuilder();
        List<String> cmd =
            Arrays.asList(
                "java -classpath "
                    + getFlinkDistJar().getName()
                    + " org.apache.flink.client.cli.CliFrontend --version");
        try {
            CommandUtils.execute(
                getFlinkLib().getAbsolutePath(),
                cmd,
                new Consumer<String>() {

                    @Override
                    public void accept(String out) {
                        buffer.append(out).append("\n");
                        Matcher matcher = FLINK_VERSION_PATTERN.matcher(out);
                        if (matcher.find()) {
                            String ver = matcher.group(1);
                            Matcher m1 = APACHE_FLINK_VERSION_PATTERN.matcher(ver);
                            if (m1.find()) {
                                flinkVersion[0] = ver;
                            } else {
                                Matcher m2 = OTHER_FLINK_VERSION_PATTERN.matcher(ver);
                                if (m2.find()) {
                                    flinkVersion[0] = ver;
                                }
                            }
                        }
                    }
                });
        } catch (Exception e) {
            throw new IllegalStateException("[StreamPark] execute flink version command failed", e);
        }
        LOG.info("[StreamPark] {}", buffer);
        if (flinkVersion[0] == null) {
            throw new IllegalStateException("[StreamPark] parse flink version failed. " + buffer);
        }
        return flinkVersion[0];
    }

    @Override
    public String toString() {
        return "\n----------------------------------------- flink version -----------------------------------\n"
            + "     flinkHome    : "
            + flinkHome
            + "\n     distJarName  : "
            + getFlinkDistJar().getName()
            + "\n     flinkVersion : "
            + getVersion()
            + "\n     majorVersion : "
            + getMajorVersion()
            + "\n     scalaVersion : "
            + getScalaVersion()
            + "\n     shimsVersion : streampark-flink-shims_flink-"
            + getMajorVersion()
            + "\n-------------------------------------------------------------------------------------------\n";
    }
}
