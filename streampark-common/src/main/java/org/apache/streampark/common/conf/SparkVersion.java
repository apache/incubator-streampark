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
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @param sparkHome actual spark home that must be a readable local path */
public class SparkVersion implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(SparkVersion.class);

    private static final Pattern SPARK_VER_PATTERN = Pattern.compile("^(\\d+\\.\\d+)(\\.)?.*$");
    private static final Pattern SPARK_VERSION_PATTERN =
        Pattern.compile("\\s{2}version\\s(\\d+\\.\\d+\\.\\d+)");
    private static final Pattern SPARK_SCALA_VERSION_PATTERN =
        Pattern.compile("Using\\sScala\\sversion\\s(\\d+\\.\\d+)");

    private final String sparkHome;
    private final String version;
    private final String scalaVersion;
    private final String majorVersion;
    private final String fullVersion;
    private final File sparkLib;

    public SparkVersion(String sparkHome) {
        this.sparkHome = sparkHome;
        String[] parsed = parseVersionAndScala(sparkHome);
        this.version = parsed[0];
        this.scalaVersion = parsed[1];
        this.majorVersion = parseMajorVersion(this.version);
        this.fullVersion = this.version + "_" + this.scalaVersion;
        this.sparkLib = resolveSparkLib(sparkHome);
    }

    public String sparkHome() {
        return sparkHome;
    }

    public String version() {
        return version;
    }

    public String scalaVersion() {
        return scalaVersion;
    }

    public String majorVersion() {
        return majorVersion;
    }

    public String fullVersion() {
        return fullVersion;
    }

    public File sparkLib() {
        return sparkLib;
    }

    public boolean checkVersion() {
        return checkVersion(true);
    }

    public boolean checkVersion(boolean throwException) {
        String[] parts = version().split("\\.");
        try {
            if (parts.length == 3) {
                int major = Integer.parseInt(parts[0].trim());
                if (major == 2 || major == 3) {
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            // fall through
        }
        if (throwException) {
            throw new UnsupportedOperationException("Unsupported spark version: " + version());
        }
        return false;
    }

    @Override
    public String toString() {
        return "\n"
            + "----------------------------------------- spark version -----------------------------------\n"
            + "     sparkHome    : " + sparkHome() + "\n"
            + "     sparkVersion : " + version() + "\n"
            + "     scalaVersion : " + scalaVersion() + "\n"
            + "-------------------------------------------------------------------------------------------\n";
    }

    private static String[] parseVersionAndScala(String sparkHome) {
        String[] refs = {null, null};
        StringBuilder buffer = new StringBuilder();
        java.util.List<String> cmd =
            Collections.singletonList(
                "export SPARK_HOME=" + sparkHome + "&&" + sparkHome + "/bin/spark-submit --version");

        CommandUtils.execute(
            sparkHome,
            cmd,
            out -> {
                buffer.append(out).append("\n");
                Matcher m = SPARK_VERSION_PATTERN.matcher(out);
                if (m.find()) {
                    refs[0] = m.group(1);
                } else {
                    Matcher m1 = SPARK_SCALA_VERSION_PATTERN.matcher(out);
                    if (m1.find()) {
                        refs[1] = m1.group(1);
                    }
                }
            });

        log.info(buffer.toString());
        if (refs[0] == null || refs[1] == null) {
            throw new IllegalStateException("[StreamPark] parse spark version failed. " + buffer);
        }
        return refs;
    }

    private static String parseMajorVersion(String version) {
        if (version == null)
            return null;
        Matcher matcher = SPARK_VER_PATTERN.matcher(version);
        matcher.matches();
        return matcher.group(1);
    }

    private static File resolveSparkLib(String sparkHome) {
        if (sparkHome == null) {
            throw new IllegalArgumentException("[StreamPark] sparkHome must not be null.");
        }
        if (!new File(sparkHome).exists()) {
            throw new IllegalArgumentException("[StreamPark] sparkHome must be exists.");
        }
        File lib = new File(sparkHome + "/jars");
        if (!lib.exists() || !lib.isDirectory()) {
            throw new IllegalArgumentException(
                "[StreamPark] " + sparkHome + "/lib must be exists and must be directory.");
        }
        return lib;
    }
}
