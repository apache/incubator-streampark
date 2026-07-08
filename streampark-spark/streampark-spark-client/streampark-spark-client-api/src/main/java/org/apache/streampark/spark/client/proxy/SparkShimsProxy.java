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

package org.apache.streampark.spark.client.proxy;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.conf.SparkVersion;
import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.common.util.ChildFirstClassLoader;
import org.apache.streampark.common.util.ClassLoaderObjectInputStream;
import org.apache.streampark.common.util.ClassLoaderUtils;
import org.apache.streampark.common.util.StreamParkLoggerFactory;

import org.apache.streampark.shaded.org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Multi-version Spark shims classloader proxy. */
public final class SparkShimsProxy {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(SparkShimsProxy.class.getName());

    private static final ConcurrentHashMap<String, ClassLoader> SHIMS_CLASS_LOADER_CACHE =
        new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, ClassLoader> VERIFY_SQL_CLASS_LOADER_CACHE =
        new ConcurrentHashMap<>();

    private static final Pattern INCLUDE_PATTERN =
        Pattern.compile(
            "(streampark-shaded-jackson-)(.*).jar",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern SPARK_JAR_PATTERN =
        Pattern.compile("spark-(.*).jar", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final String SPARK_SHIMS_PREFIX = "streampark-spark-shims_spark";

    private static final List<String> PARENT_FIRST_PATTERNS =
        Arrays.asList(
            "java.",
            "javax.xml",
            "org.slf4j",
            "org.apache.log4j",
            "org.apache.logging",
            "org.apache.commons.logging",
            "ch.qos.logback",
            "org.xml",
            "org.w3c",
            "org.apache.hadoop",
            "org.apache.spark.launcher");

    private SparkShimsProxy() {
    }

    private static Pattern getSparkShimsResourcePattern(String sparkLargeVersion) {
        return Pattern.compile(
            "spark-(.*)-" + sparkLargeVersion + "(.*).jar",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    }

    public static <T> T proxy(SparkVersion sparkVersion, Function<ClassLoader, T> func) {
        ClassLoader shimsClassLoader = getSparkShimsClassLoader(sparkVersion);
        return ClassLoaderUtils.runAsClassLoader(
            shimsClassLoader, () -> func.apply(shimsClassLoader));
    }

    public static <T> T proxyVerifySql(SparkVersion sparkVersion, Function<ClassLoader, T> func) {
        ClassLoader shimsClassLoader = getVerifySqlLibClassLoader(sparkVersion);
        return ClassLoaderUtils.runAsClassLoader(
            shimsClassLoader, () -> func.apply(shimsClassLoader));
    }

    @SuppressWarnings("unchecked")
    public static <T> T getObject(ClassLoader loader, Object obj) {
        try (
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(arrayOutputStream)) {
            out.writeObject(obj);
            try (
                ClassLoaderObjectInputStream in =
                    new ClassLoaderObjectInputStream(
                        loader, new ByteArrayInputStream(arrayOutputStream.toByteArray()))) {
                return (T) in.readObject();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ClassLoader getVerifySqlLibClassLoader(SparkVersion sparkVersion) {
        LOG.info("Add verify sql lib,spark version: {}", sparkVersion);
        return VERIFY_SQL_CLASS_LOADER_CACHE.computeIfAbsent(
            sparkVersion.fullVersion(),
            key -> {
                List<URL> shimsUrls = new ArrayList<>(getSparkHomeLib(sparkVersion.getSparkHome(), "jars", f -> true));
                addShimsUrls(
                    sparkVersion,
                    file -> {
                        if (file.getName().startsWith("streampark-spark-shims")) {
                            try {
                                shimsUrls.add(file.toURI().toURL());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                return new ChildFirstClassLoader(
                    shimsUrls.toArray(new URL[0]),
                    Thread.currentThread().getContextClassLoader(),
                    PARENT_FIRST_PATTERNS,
                    jarName -> loadJarFilter(jarName, sparkVersion));
            });
    }

    private static boolean loadJarFilter(String jarName, SparkVersion sparkVersion) {
        Pattern childFirstPattern = getSparkShimsResourcePattern(sparkVersion.majorVersion());
        return SPARK_JAR_PATTERN.matcher(jarName).matches()
            && !childFirstPattern.matcher(jarName).matches();
    }

    private static void addShimsUrls(SparkVersion sparkVersion, java.util.function.Consumer<File> addShimUrl) {
        String appHome = System.getProperty(ConfigKeys.KEY_APP_HOME);
        if (appHome == null) {
            throw new IllegalArgumentException(
                String.format("%s is not found on System env.", ConfigKeys.KEY_APP_HOME));
        }
        File libPath = new File(appHome + "/lib");
        if (!libPath.exists()) {
            throw new IllegalArgumentException("lib path does not exist: " + libPath);
        }
        String majorVersion = sparkVersion.majorVersion();
        File[] files = libPath.listFiles();
        if (files == null) {
            return;
        }
        for (File jar : files) {
            String jarName = jar.getName();
            if (!jarName.endsWith(Constants.JAR_SUFFIX)) {
                continue;
            }
            if (jarName.startsWith(SPARK_SHIMS_PREFIX)) {
                String prefixVer = SPARK_SHIMS_PREFIX + "-" + majorVersion + "-";
                if (jarName.startsWith(prefixVer)) {
                    addShimUrl.accept(jar);
                    LOG.info("Include spark shims jar lib: {}", jarName);
                }
            } else if (INCLUDE_PATTERN.matcher(jarName).matches()) {
                addShimUrl.accept(jar);
                LOG.info("Include jar lib: {}", jarName);
            } else if (jarName.matches("^streampark-(?!flink).*\\.jar$")) {
                addShimUrl.accept(jar);
                LOG.info("Include streampark lib: {}", jarName);
            }
        }
    }

    private static ClassLoader getSparkShimsClassLoader(SparkVersion sparkVersion) {
        LOG.info("add spark shims urls classloader,spark version: {}", sparkVersion);
        return SHIMS_CLASS_LOADER_CACHE.computeIfAbsent(
            sparkVersion.fullVersion(),
            key -> {
                List<URL> shimsUrls =
                    new ArrayList<>(
                        getSparkHomeLib(
                            sparkVersion.getSparkHome(),
                            "jars",
                            f -> !f.getName().startsWith("log4j")
                                && !f.getName().startsWith("slf4j")));
                addShimsUrls(
                    sparkVersion,
                    file -> {
                        if (file != null) {
                            try {
                                shimsUrls.add(file.toURI().toURL());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                return new ChildFirstClassLoader(
                    shimsUrls.toArray(new URL[0]),
                    Thread.currentThread().getContextClassLoader(),
                    PARENT_FIRST_PATTERNS,
                    jarName -> loadJarFilter(jarName, sparkVersion));
            });
    }

    private static List<URL> getSparkHomeLib(
                                             String sparkHome, String childDir, Predicate<File> filterFun) {
        File file = new File(sparkHome, childDir);
        if (!file.isDirectory()) {
            throw new IllegalArgumentException("SPARK_HOME " + file + " does not exist");
        }
        File[] files = file.listFiles();
        List<URL> urls = new ArrayList<>();
        if (files == null) {
            return urls;
        }
        for (File f : files) {
            if (filterFun.test(f)) {
                try {
                    urls.add(f.toURI().toURL());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return urls;
    }
}
