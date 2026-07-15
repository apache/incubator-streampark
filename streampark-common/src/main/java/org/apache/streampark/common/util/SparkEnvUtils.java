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

import org.apache.streampark.shaded.org.slf4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves Spark-side JVM settings from installation layout and spark-env.sh. */
public final class SparkEnvUtils {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(SparkEnvUtils.class.getName());

    private static final Pattern JAVA_HOME_PATTERN =
        Pattern.compile("(?:^|\\n)\\s*(?:export\\s+)?JAVA_HOME\\s*=\\s*(?:[\"']([^\"']+)[\"']|(\\S+))");

    private SparkEnvUtils() {
    }

    /** Minimum Java major version required by the given Spark version string. */
    public static int requiredJavaMajorVersion(String sparkVersion) {
        if (sparkVersion == null || sparkVersion.isEmpty()) {
            return 8;
        }
        String[] parts = sparkVersion.split("\\.");
        if (parts.length == 0) {
            return 8;
        }
        try {
            int major = Integer.parseInt(parts[0].trim());
            if (major >= 4) {
                return 17;
            }
        } catch (NumberFormatException ignored) {
        }
        return 8;
    }

    /**
     * Resolve JAVA_HOME for Spark CLI and SparkLauncher.
     *
     * <p>Resolution order: spark-env.sh, process environment, then system auto-detection.
     */
    public static Optional<String> resolveJavaHome(String sparkHome, String sparkVersion) {
        int minVersion = requiredJavaMajorVersion(sparkVersion);
        return parseJavaHomeFromSparkEnv(sparkHome)
            .filter(SparkEnvUtils::isValidJavaHome)
            .or(() -> Optional.ofNullable(System.getenv("JAVA_HOME")).filter(SparkEnvUtils::isValidJavaHome))
            .or(() -> detectSystemJavaHome(minVersion).filter(SparkEnvUtils::isValidJavaHome));
    }

    public static Optional<String> parseJavaHomeFromSparkEnv(String sparkHome) {
        File sparkEnvFile = new File(sparkHome, "conf/spark-env.sh");
        if (!sparkEnvFile.exists()) {
            return Optional.empty();
        }
        try {
            String content = new String(Files.readAllBytes(sparkEnvFile.toPath()), StandardCharsets.UTF_8);
            return extractJavaHome(content);
        } catch (Exception e) {
            LOG.warn("Failed to read spark-env.sh from {}", sparkEnvFile, e);
            return Optional.empty();
        }
    }

    static Optional<String> extractJavaHome(String content) {
        Matcher matcher = JAVA_HOME_PATTERN.matcher(content);
        while (matcher.find()) {
            String value = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (value != null && !value.isEmpty() && !value.startsWith("#")) {
                return Optional.of(value.trim());
            }
        }
        return Optional.empty();
    }

    private static Optional<String> detectSystemJavaHome(int minMajor) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            try {
                CommandUtils.CommandResult result =
                    CommandUtils.execute("/usr/libexec/java_home -v " + minMajor + " 2>/dev/null");
                if (result.code == 0 && result.output != null && !result.output.trim().isEmpty()) {
                    return Optional.of(result.output.trim());
                }
            } catch (Exception e) {
                LOG.debug("Failed to detect JAVA_HOME on macOS", e);
            }
            return Optional.empty();
        }
        List<String> candidates =
            Arrays.asList(
                System.getenv("JAVA" + minMajor + "_HOME"),
                "/usr/lib/jvm/java-" + minMajor + "-openjdk",
                "/usr/lib/jvm/java-" + minMajor + "-openjdk-amd64",
                "/usr/lib/jvm/java-" + minMajor);
        for (String candidate : candidates) {
            if (candidate != null && isValidJavaHome(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static boolean isValidJavaHome(String javaHome) {
        return javaHome != null && !javaHome.isEmpty() && new File(javaHome, "bin/java").exists();
    }
}
