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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @param flinkHome actual flink home that must be a readable local path */
public class FlinkVersion implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(FlinkVersion.class);

    private static final Pattern FLINK_VER_PATTERN = Pattern.compile("^(\\d+\\.\\d+)(\\.)?.*$");
    private static final Pattern FLINK_VERSION_PATTERN =
        Pattern.compile("^Version: (.*), Commit ID: (.*)$");
    private static final Pattern FLINK_SCALA_VERSION_PATTERN =
        Pattern.compile("^flink-dist_(\\d\\.\\d+).*.jar$");
    private static final Pattern APACHE_FLINK_VERSION_PATTERN = Pattern.compile("(^\\d+\\.\\d+\\.\\d+)");
    private static final Pattern OTHER_FLINK_VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+)(-*)");

    private final String flinkHome;
    private final File flinkLib;
    private final List<URL> flinkLibs;
    private final File flinkDistJar;
    private final String scalaVersion;
    private final String version;
    private final String majorVersion;
    private final String fullVersion;

    public FlinkVersion(String flinkHome) {
        this.flinkHome = flinkHome;
        this.flinkLib = resolveFlinkLib(flinkHome);
        this.flinkLibs = resolveFlinkLibs(this.flinkLib);
        this.flinkDistJar = resolveFlinkDistJar(this.flinkLib);
        this.scalaVersion = parseScalaVersion(this.flinkDistJar);
        this.version = parseVersion(this.flinkLib, this.flinkDistJar);
        this.majorVersion = parseMajorVersion(this.version);
        this.fullVersion = this.version + "_" + this.scalaVersion;
    }

    public String flinkHome() {
        return flinkHome;
    }

    public File flinkLib() {
        return flinkLib;
    }

    public List<URL> flinkLibs() {
        return flinkLibs;
    }

    public File flinkDistJar() {
        return flinkDistJar;
    }

    public String scalaVersion() {
        return scalaVersion;
    }

    public String version() {
        return version;
    }

    public String majorVersion() {
        return majorVersion;
    }

    public String fullVersion() {
        return fullVersion;
    }

    public boolean checkVersion() {
        return checkVersion(true);
    }

    public boolean checkVersion(boolean throwException) {
        String[] parts = version().split("\\.");
        try {
            if (parts.length == 3) {
                int major = Integer.parseInt(parts[0].trim());
                int minor = Integer.parseInt(parts[1].trim());
                if (major == 1 && minor >= 12 && minor <= 20) {
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            // fall through
        }
        if (throwException) {
            throw new UnsupportedOperationException("Unsupported flink version: " + version());
        }
        return false;
    }

    public boolean checkVersion(int sinceVersion) {
        String[] parts = version().split("\\.");
        try {
            if (parts.length == 3) {
                int major = Integer.parseInt(parts[0].trim());
                int minor = Integer.parseInt(parts[1].trim());
                return major == 1 && minor >= sinceVersion;
            }
        } catch (NumberFormatException e) {
            // fall through
        }
        return false;
    }

    @Override
    public String toString() {
        String shimsVersion = "streampark-flink-shims_flink-" + majorVersion();
        return "\n"
            + "----------------------------------------- flink version -----------------------------------\n"
            + "     flinkHome    : " + flinkHome() + "\n"
            + "     distJarName  : " + flinkDistJar().getName() + "\n"
            + "     flinkVersion : " + version() + "\n"
            + "     majorVersion : " + majorVersion() + "\n"
            + "     scalaVersion : " + scalaVersion() + "\n"
            + "     shimsVersion : " + shimsVersion + "\n"
            + "-------------------------------------------------------------------------------------------\n";
    }

    private static File resolveFlinkLib(String flinkHome) {
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
        return lib;
    }

    private static List<URL> resolveFlinkLibs(File flinkLib) {
        File[] files = flinkLib.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<URL> urls = new ArrayList<>(files.length);
        for (File f : files) {
            try {
                urls.add(f.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return urls;
    }

    private static File resolveFlinkDistJar(File flinkLib) {
        File[] distJars =
            flinkLib.listFiles(f -> f.getName().matches("flink-dist.*\\.jar"));
        if (distJars == null || distJars.length == 0) {
            throw new IllegalArgumentException(
                "[StreamPark] can no found flink-dist jar in " + flinkLib);
        }
        if (distJars.length > 1) {
            throw new IllegalArgumentException(
                "[StreamPark] found multiple flink-dist jar in " + flinkLib);
        }
        return distJars[0];
    }

    private static String parseScalaVersion(File flinkDistJar) {
        Matcher matcher = FLINK_SCALA_VERSION_PATTERN.matcher(flinkDistJar.getName());
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return "2.12";
    }

    private static String parseVersion(File flinkLib, File flinkDistJar) {
        List<String> cmd =
            Collections.singletonList(
                "java -classpath "
                    + flinkDistJar.getName()
                    + " org.apache.flink.client.cli.CliFrontend --version");
        StringBuilder buffer = new StringBuilder();
        String[] ref = {null};

        CommandUtils.execute(
            flinkLib.getAbsolutePath(),
            cmd,
            out -> {
                buffer.append(out).append("\n");
                Matcher m = FLINK_VERSION_PATTERN.matcher(out);
                if (m.find()) {
                    String v = m.group(1);
                    if (APACHE_FLINK_VERSION_PATTERN.matcher(v).find()) {
                        ref[0] = v;
                    } else if (OTHER_FLINK_VERSION_PATTERN.matcher(v).find()) {
                        ref[0] = v;
                    }
                }
            });

        log.info(buffer.toString());
        if (ref[0] == null) {
            throw new IllegalStateException(
                "[StreamPark] parse flink version failed. " + buffer);
        }
        return ref[0];
    }

    private static String parseMajorVersion(String version) {
        if (version == null)
            return null;
        Matcher matcher = FLINK_VER_PATTERN.matcher(version);
        matcher.matches();
        return matcher.group(1);
    }
}
