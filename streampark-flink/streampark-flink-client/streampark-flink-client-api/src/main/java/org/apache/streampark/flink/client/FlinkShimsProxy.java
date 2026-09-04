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

package org.apache.streampark.flink.client;

import org.apache.streampark.common.configuration.Constants;
import org.apache.streampark.common.configuration.option.CoreOptions;
import org.apache.streampark.common.core.FlinkVersion;
import org.apache.streampark.common.util.ChildFirstClassLoader;
import org.apache.streampark.common.util.ClassLoaderObjectInputStream;
import org.apache.streampark.common.util.ClassLoaderUtils;
import org.apache.streampark.common.util.LoggerSupport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Executes version-specific Flink integration code behind an isolated classloader.
 *
 * <p>Both submission and SQL validation load the complete {@code lib} directory of the registered
 * Flink installation. SQL validation additionally loads table artifacts from {@code opt}. Keeping
 * the target runtime intact prevents table APIs from linking against Flink classes supplied by the
 * Console parent classloader.
 */
public final class FlinkShimsProxy extends LoggerSupport {

    private static final FlinkShimsProxy LOG = new FlinkShimsProxy();

    private static final Map<String, ClassLoader> SHIMS_CLASS_LOADER_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, ClassLoader> VERIFY_SQL_CLASS_LOADER_CACHE = new ConcurrentHashMap<>();

    private static final Pattern FLINK_JAR_PATTERN =
        Pattern.compile("flink-(.*).jar", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern INCLUDE_PATTERN =
        Pattern.compile("(streampark-shaded-jackson-)(.*).jar", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final String FLINK_SHIMS_PREFIX = "streampark-flink-shims_flink";

    private static final String FLINK_SHIMS_BASE_PREFIX = "streampark-flink-shims-base";

    private static final String STREAMPARK_PREFIX = "streampark-";

    private static final String STREAMPARK_CONSOLE_PREFIX = "streampark-console";

    private static final String STREAMPARK_SHADED_PREFIX = "streampark-shaded-";

    private static final List<String> PARENT_FIRST_PATTERNS = Collections.unmodifiableList(
        Arrays.asList(
            "java.",
            "javax.xml",
            "org.slf4j",
            "org.apache.log4j",
            "org.apache.logging",
            "org.apache.commons.logging",
            "org.apache.commons.cli",
            "ch.qos.logback",
            "org.xml",
            "org.yaml",
            "org.w3c",
            "org.apache.hadoop"));

    private FlinkShimsProxy() {
    }

    /**
     * Executes an operation inside the classloader for the requested Flink version.
     *
     * @param flinkVersion target Flink installation and version
     * @param operation operation that must resolve target-version classes
     * @param <T> operation result type
     * @return result returned by the operation
     */
    public static <T> T proxy(
                              FlinkVersion flinkVersion,
                              Function<ClassLoader, T> operation) {
        ClassLoader shimsClassLoader = getFlinkShimsClassLoader(flinkVersion);
        return ClassLoaderUtils.runAsClassLoader(
            shimsClassLoader, () -> operation.apply(shimsClassLoader));
    }

    /**
     * Executes SQL validation with the target Flink table libraries available.
     *
     * @param flinkVersion target Flink installation and version
     * @param operation validation operation
     * @param <T> operation result type
     * @return result returned by the operation
     */
    public static <T> T proxyVerifySql(
                                       FlinkVersion flinkVersion,
                                       Function<ClassLoader, T> operation) {
        ClassLoader shimsClassLoader = getVerifySqlLibClassLoader(flinkVersion);
        return ClassLoaderUtils.runAsClassLoader(
            shimsClassLoader, () -> operation.apply(shimsClassLoader));
    }

    /**
     * Copies a serializable value into the type space of another classloader.
     *
     * <p>Flink runtime objects must not cross the shims boundary. Stable StreamPark request and
     * response types use this method to transfer values without sharing their defining classes.
     *
     * @param loader classloader that must define the copied value
     * @param obj serializable source value
     * @param type target type loaded in the destination type space
     * @param <T> copied value type
     * @return deserialized value owned by the destination classloader
     * @throws IOException if the value cannot be serialized or read
     * @throws ClassNotFoundException if the destination loader cannot resolve a serialized type
     */
    public static <T> T getObject(
                                  ClassLoader loader,
                                  Object obj,
                                  Class<T> type) throws IOException, ClassNotFoundException {
        try (
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(arrayOutputStream)) {
            out.writeObject(obj);
            try (
                ByteArrayInputStream byteArrayInputStream =
                    new ByteArrayInputStream(arrayOutputStream.toByteArray());
                ClassLoaderObjectInputStream in =
                    new ClassLoaderObjectInputStream(loader, byteArrayInputStream)) {
                return type.cast(in.readObject());
            }
        }
    }

    /** Builds the pattern used to keep the selected concrete shim child-first. */
    private static Pattern getFlinkShimsResourcePattern(String majorVersion) {
        return Pattern.compile(
            "flink-(.*)-" + majorVersion + "(.*).jar",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    }

    /** Returns the cached SQL-validation classloader for a concrete Flink version. */
    private static ClassLoader getVerifySqlLibClassLoader(FlinkVersion flinkVersion) {
        LOG.logInfo("Add verify sql lib,flink version: " + flinkVersion);
        return VERIFY_SQL_CLASS_LOADER_CACHE.computeIfAbsent(
            flinkVersion.fullVersion(),
            key -> {
                List<URL> shimsUrls = getFlinkUrls(flinkVersion, true);

                addShimsUrls(
                    flinkVersion,
                    file -> {
                        if (file.getName().startsWith("streampark-flink-shims")) {
                            shimsUrls.add(toUrl(file));
                        }
                    });

                return createClassLoader(flinkVersion, shimsUrls);
            });
    }

    /** Keeps target Flink jars child-first while excluding the selected shim artifact itself. */
    private static boolean loadJarFilter(String jarName, FlinkVersion flinkVersion) {
        Pattern childFirstPattern = getFlinkShimsResourcePattern(flinkVersion.majorVersion());
        return FLINK_JAR_PATTERN.matcher(jarName).matches()
            && !childFirstPattern.matcher(jarName).matches();
    }

    /** Adds compatible StreamPark integration artifacts in deterministic filename order. */
    private static void addShimsUrls(FlinkVersion flinkVersion, Consumer<File> addShimUrl) {
        String appHome = System.getProperty(CoreOptions.APP_HOME.key());
        if (appHome == null) {
            throw new IllegalArgumentException(
                String.format("%s is not found on System env.", CoreOptions.APP_HOME.key()));
        }

        File libPath = new File(appHome + "/lib");
        if (!libPath.exists()) {
            throw new IllegalArgumentException("lib path does not exist: " + libPath);
        }

        String majorVersion = flinkVersion.majorVersion();
        String scalaVersion = flinkVersion.scalaVersion();
        File[] jars = libPath.listFiles();
        if (jars == null) {
            return;
        }

        List<File> matched = new ArrayList<>();
        for (File jar : jars) {
            String jarName = jar.getName();
            if (!jarName.endsWith(Constants.JAR_SUFFIX)) {
                continue;
            }
            String includeReason = matchShimIncludeReason(jarName, majorVersion, scalaVersion);
            if (includeReason != null) {
                matched.add(jar);
                LOG.logInfo(includeReason + jarName);
            }
        }
        matched.sort(Comparator.comparing(File::getName));
        matched.forEach(addShimUrl);
    }

    /** Returns the inclusion reason for a compatible integration artifact, or {@code null}. */
    static String matchShimIncludeReason(
                                         String jarName, String majorVersion, String scalaVersion) {
        if (jarName.startsWith(FLINK_SHIMS_PREFIX)) {
            // The shims artifacts carried a _${scala.binary.version} suffix until they were renamed
            // without it; both spellings are accepted so the jar is matched either way. Getting this
            // wrong is silent: a non-matching jar is simply left out of the classloader, and the
            // job fails much later with a NoClassDefFoundError for a class the shims needed.
            String prefixVer = FLINK_SHIMS_PREFIX + "-" + majorVersion;
            return jarName.startsWith(prefixVer + "_" + scalaVersion) || jarName.startsWith(prefixVer + "-")
                ? "Include flink shims jar lib: "
                : null;
        }
        if (jarName.startsWith(FLINK_SHIMS_BASE_PREFIX)) {
            return jarName.startsWith(FLINK_SHIMS_BASE_PREFIX + "-")
                && !jarName.startsWith(FLINK_SHIMS_BASE_PREFIX + "-v2-")
                    ? "Include flink shims base jar lib: "
                    : null;
        }
        if (INCLUDE_PATTERN.matcher(jarName).matches()) {
            return "Include jar lib: ";
        }
        // Everything StreamPark builds against Flink belongs in here, so that the whole submission
        // stack resolves org.apache.flink.* from the target version's jars rather than from the
        // console's own bundled baseline. This used to be spelled as "has a _${scala.binary.version}
        // suffix", which stopped selecting anything on the Flink side once those modules were
        // renamed without the suffix. The console's own jars stay out: they are the parent.
        if (jarName.startsWith(STREAMPARK_PREFIX)
            && !jarName.startsWith(STREAMPARK_CONSOLE_PREFIX)
            && !jarName.startsWith(STREAMPARK_SHADED_PREFIX)) {
            return "Include streampark lib: ";
        }
        return null;
    }

    /** Returns the cached submission classloader for a concrete Flink version. */
    private static ClassLoader getFlinkShimsClassLoader(FlinkVersion flinkVersion) {
        LOG.logInfo("add flink shims urls classloader,flink version: " + flinkVersion);
        return SHIMS_CLASS_LOADER_CACHE.computeIfAbsent(
            flinkVersion.fullVersion(),
            key -> {
                List<URL> shimsUrls = getFlinkUrls(flinkVersion, false);

                addShimsUrls(
                    flinkVersion,
                    file -> {
                        if (file != null) {
                            shimsUrls.add(toUrl(file));
                        }
                    });

                return createClassLoader(flinkVersion, shimsUrls);
            });
    }

    /**
     * Collects the target Flink runtime before StreamPark integration artifacts are appended.
     *
     * <p>The complete {@code lib} directory is an indivisible compatibility boundary. Selecting
     * only table jars would allow their transitive Flink classes to fall back to the Console's
     * compile-time version.
     */
    private static List<URL> getFlinkUrls(FlinkVersion flinkVersion, boolean includeTableOpt) {
        Predicate<File> flinkLib =
            file -> !file.getName().startsWith("log4j") && file.getName().endsWith(".jar");
        List<URL> urls = new ArrayList<>(
            getFlinkHomeLib(flinkVersion.flinkHome, "lib", flinkLib));
        if (includeTableOpt) {
            Predicate<File> flinkTable =
                file -> file.getName().startsWith("flink-table") && file.getName().endsWith(".jar");
            urls.addAll(getFlinkHomeLib(flinkVersion.flinkHome, "opt", flinkTable));
        }
        return urls;
    }

    /** Creates a child-first loader whose parent owns stable StreamPark boundary types. */
    private static ClassLoader createClassLoader(
                                                 FlinkVersion flinkVersion,
                                                 List<URL> urls) {
        return new ChildFirstClassLoader(
            urls.toArray(new URL[0]),
            Thread.currentThread().getContextClassLoader(),
            PARENT_FIRST_PATTERNS,
            jarName -> loadJarFilter(jarName, flinkVersion));
    }

    /** Collects matching jars from a Flink installation directory in deterministic order. */
    private static List<URL> getFlinkHomeLib(
                                             String flinkHome,
                                             String childDir,
                                             Predicate<File> filterFun) {
        File file = new File(flinkHome, childDir);
        if (!file.isDirectory()) {
            throw new IllegalArgumentException("FLINK_HOME " + file + " does not exist");
        }
        File[] files = file.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        List<URL> urls = new ArrayList<>();
        for (File f : files) {
            if (filterFun.test(f)) {
                urls.add(toUrl(f));
            }
        }
        return urls;
    }

    /** Converts a local artifact path to a classloader URL with contextual failure reporting. */
    private static URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid file URL: " + file, e);
        }
    }
}
