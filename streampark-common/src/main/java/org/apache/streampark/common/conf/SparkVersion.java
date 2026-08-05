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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SparkVersion implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = org.apache.streampark.common.util.StreamParkLoggerFactory.loggerFactory()
        .getLogger(SparkVersion.class.getName());
    private static final Pattern SPARK_VER_PATTERN = Pattern.compile("^(\\d+\\.\\d+)(?:\\..*)?$");
    private static final Pattern SPARK_VERSION_PATTERN = Pattern.compile("\\s{2}version\\s(\\d+\\.\\d+\\.\\d+)");
    private static final Pattern SPARK_SCALA_VERSION_PATTERN =
        Pattern.compile("Using\\sScala\\sversion\\s(\\d+\\.\\d+)");

    /** Spark installation directory (Scala {@code sparkHome} accessor). */
    public final String sparkHome;
    private final String version;
    private final String scalaVersion;

    public SparkVersion(String sparkHome) {
        this.sparkHome = sparkHome;
        String[] parsed = parseVersion(sparkHome);
        this.version = parsed[0];
        this.scalaVersion = parsed[1];
    }

    private static String[] parseVersion(String sparkHome) {
        final String[] sparkVersion = {null, null};
        List<String> cmd =
            Arrays.asList("export SPARK_HOME=" + sparkHome + "&&" + sparkHome + "/bin/spark-submit --version");
        StringBuilder buffer = new StringBuilder();
        try {
            CommandUtils.execute(sparkHome, cmd, out -> {
                buffer.append(out).append("\n");
                Matcher m = SPARK_VERSION_PATTERN.matcher(out);
                if (m.find())
                    sparkVersion[0] = m.group(1);
                Matcher m1 = SPARK_SCALA_VERSION_PATTERN.matcher(out);
                if (m1.find())
                    sparkVersion[1] = m1.group(1);
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Spark version from " + sparkHome, e);
        }
        LOG.info("[StreamPark] {}", buffer);
        if (sparkVersion[0] == null || sparkVersion[1] == null) {
            throw new IllegalStateException("[StreamPark] parse spark version failed. " + buffer);
        }
        return sparkVersion;
    }

    public String getSparkHome() {
        return sparkHome;
    }
    public String getVersion() {
        return version;
    }
    public String getScalaVersion() {
        return scalaVersion;
    }

    public String fullVersion() {
        return getFullVersion();
    }
    public String majorVersion() {
        return getMajorVersion();
    }
    public String scalaVersion() {
        return scalaVersion;
    }
    public String version() {
        return version;
    }

    public String getMajorVersion() {
        if (version == null)
            return null;
        Matcher matcher = SPARK_VER_PATTERN.matcher(version);
        matcher.matches();
        return matcher.group(1);
    }

    public String getFullVersion() {
        return version + "_" + scalaVersion;
    }

    public File getSparkLib() {
        if (sparkHome == null)
            throw new IllegalArgumentException("[StreamPark] sparkHome must not be null.");
        if (!new File(sparkHome).exists())
            throw new IllegalArgumentException("[StreamPark] sparkHome must be exists.");
        File lib = new File(sparkHome + "/jars");
        if (!lib.exists() || !lib.isDirectory())
            throw new IllegalArgumentException(
                "[StreamPark] " + sparkHome + "/lib must be exists and must be directory.");
        return lib;
    }

    public boolean checkVersion() {
        return checkVersion(true);
    }
    public boolean checkVersion(boolean throwException) {
        String[] parts = version.split("\\.");
        if (parts.length >= 1) {
            try {
                int major = Integer.parseInt(parts[0].trim());
                if (major == 2 || major == 3)
                    return true;
            } catch (NumberFormatException ignored) {
            }
        }
        if (throwException)
            throw new UnsupportedOperationException("Unsupported spark version: " + version);
        return false;
    }

    @Override
    public String toString() {
        return "\n----------------------------------------- spark version -----------------------------------\n"
            + "     sparkHome    : " + sparkHome + "\n"
            + "     sparkVersion : " + version + "\n"
            + "     scalaVersion : " + scalaVersion + "\n"
            + "-------------------------------------------------------------------------------------------\n";
    }
}
