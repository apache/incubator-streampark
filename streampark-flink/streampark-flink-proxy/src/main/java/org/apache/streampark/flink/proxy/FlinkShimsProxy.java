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

package org.apache.streampark.flink.proxy;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.conf.FlinkVersion;
import org.apache.streampark.common.constants.Constants;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Proxy for loading Flink version-specific shims behind an isolated classloader. */
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
            "org.w3c",
            "org.apache.hadoop"));

    private FlinkShimsProxy() {
    }

    private static Pattern getFlinkShimsResourcePattern(String majorVersion) {
        return Pattern.compile(
            "flink-(.*)-" + majorVersion + "(.*).jar",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    }

    /**
     * Get shimsClassLoader to execute for java/scala API (SAM {@link Function}).
     *
     * @param flinkVersion flinkVersion
     * @param func execute function
     * @param <T> return type
     * @return result of func
     */
    public static <T> T proxy(FlinkVersion flinkVersion, Function<ClassLoader, T> func) {
        ClassLoader shimsClassLoader = getFlinkShimsClassLoader(flinkVersion);
        return ClassLoaderUtils.runAsClassLoader(shimsClassLoader, () -> func.apply(shimsClassLoader));
    }

    /**
     * Get ClassLoader to verify sql.
     *
     * @param flinkVersion flinkVersion
     * @param func execute function
     * @param <T> return type
     * @return result of func
     */
    public static <T> T proxyVerifySql(FlinkVersion flinkVersion, Function<ClassLoader, T> func) {
        ClassLoader shimsClassLoader = getVerifySqlLibClassLoader(flinkVersion);
        return ClassLoaderUtils.runAsClassLoader(shimsClassLoader, () -> func.apply(shimsClassLoader));
    }

    public static <T> T getObject(ClassLoader loader, Object obj,
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

    // need to load all flink-table dependencies compatible with different versions
    private static ClassLoader getVerifySqlLibClassLoader(FlinkVersion flinkVersion) {
        LOG.logInfo("Add verify sql lib,flink version: " + flinkVersion);
        return VERIFY_SQL_CLASS_LOADER_CACHE.computeIfAbsent(
            flinkVersion.fullVersion(),
            key -> {
                Predicate<File> getFlinkTable = f -> f.getName().startsWith("flink-table");
                List<URL> libTableURL = getFlinkHomeLib(flinkVersion.flinkHome, "lib", getFlinkTable);
                List<URL> optTableURL = getFlinkHomeLib(flinkVersion.flinkHome, "opt", getFlinkTable);
                List<URL> shimsUrls = new ArrayList<>(libTableURL);
                shimsUrls.addAll(optTableURL);

                addShimsUrls(
                    flinkVersion,
                    file -> {
                        if (file.getName().startsWith("streampark-flink-shims")) {
                            shimsUrls.add(toUrl(file));
                        }
                    });

                return new ChildFirstClassLoader(
                    shimsUrls.toArray(new URL[0]),
                    Thread.currentThread().getContextClassLoader(),
                    PARENT_FIRST_PATTERNS,
                    jarName -> loadJarFilter(jarName, flinkVersion));
            });
    }

    private static boolean loadJarFilter(String jarName, FlinkVersion flinkVersion) {
        Pattern childFirstPattern = getFlinkShimsResourcePattern(flinkVersion.majorVersion());
        return FLINK_JAR_PATTERN.matcher(jarName).matches()
            && !childFirstPattern.matcher(jarName).matches();
    }

    private static void addShimsUrls(FlinkVersion flinkVersion, Consumer<File> addShimUrl) {
        String appHome = System.getProperty(ConfigKeys.KEY_APP_HOME());
        if (appHome == null) {
            throw new IllegalArgumentException(
                String.format("%s is not found on System env.", ConfigKeys.KEY_APP_HOME()));
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

        for (File jar : jars) {
            String jarName = jar.getName();
            if (!jarName.endsWith(Constants.JAR_SUFFIX)) {
                continue;
            }
            String includeReason = matchShimIncludeReason(jarName, majorVersion, scalaVersion);
            if (includeReason != null) {
                addShimUrl.accept(jar);
                LOG.logInfo(includeReason + jarName);
            }
        }
    }

    private static String matchShimIncludeReason(
                                                 String jarName, String majorVersion, String scalaVersion) {
        if (jarName.startsWith(FLINK_SHIMS_PREFIX)) {
            String prefixVer = FLINK_SHIMS_PREFIX + "-" + majorVersion + "_" + scalaVersion;
            return jarName.startsWith(prefixVer) ? "Include flink shims jar lib: " : null;
        }
        if (jarName.startsWith(FLINK_SHIMS_BASE_PREFIX)) {
            return "Include flink shims base jar lib: ";
        }
        if (INCLUDE_PATTERN.matcher(jarName).matches()) {
            return "Include jar lib: ";
        }
        if (jarName.matches("^streampark-.*_" + scalaVersion + ".*$")) {
            return "Include streampark lib: ";
        }
        return null;
    }

    private static ClassLoader getFlinkShimsClassLoader(FlinkVersion flinkVersion) {
        LOG.logInfo("add flink shims urls classloader,flink version: " + flinkVersion);
        return SHIMS_CLASS_LOADER_CACHE.computeIfAbsent(
            flinkVersion.fullVersion(),
            key -> {
                Predicate<File> filter =
                    file -> !file.getName().startsWith("log4j") && file.getName().endsWith(".jar");
                List<URL> libURL = getFlinkHomeLib(flinkVersion.flinkHome, "lib", filter);
                List<URL> shimsUrls = new ArrayList<>(libURL);

                addShimsUrls(
                    flinkVersion,
                    file -> {
                        if (file != null) {
                            shimsUrls.add(toUrl(file));
                        }
                    });

                return new ChildFirstClassLoader(
                    shimsUrls.toArray(new URL[0]),
                    Thread.currentThread().getContextClassLoader(),
                    PARENT_FIRST_PATTERNS,
                    jarName -> loadJarFilter(jarName, flinkVersion));
            });
    }

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
        List<URL> urls = new ArrayList<>();
        for (File f : files) {
            if (filterFun.test(f)) {
                urls.add(toUrl(f));
            }
        }
        return urls;
    }

    private static URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid file URL: " + file, e);
        }
    }
}
