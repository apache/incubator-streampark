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

import org.apache.streampark.common.conf.ConfigConst;
import org.apache.streampark.common.conf.FlinkVersion;
import org.apache.streampark.common.util.ClassLoaderUtils;
import org.apache.streampark.common.util.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import scala.Function1;

public class FlinkShimsProxy {

  public static final Logger LOG = LoggerFactory.getLogger(FlinkShimsProxy.class);

  private static final String FLINK_SHIMS_PREFIX = "streampark-flink-shims_flink";

  private static final Pattern INCLUDE_PATTERN =
      Pattern.compile(
          "(streampark-shaded-jackson-)(.*).jar", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  private static final Map<String, ClassLoader> SHIMS_CLASS_LOADER_CACHE = new HashMap<>();

  private static final Map<String, ClassLoader> VERIFY_SQL_CLASS_LOADER_CACHE = new HashMap<>();

  private static Pattern getFlinkShimsResourcePattern(String flinkLargeVersion) {
    return Pattern.compile(
        String.format("flink-(.*)-%s(.*).jar", flinkLargeVersion),
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  }

  /**
   * Get shimsClassLoader to execute for scala API
   *
   * @param flinkVersion flinkVersion
   * @param func execute function
   * @tparam T
   * @return
   */
  public static <T> T proxy(FlinkVersion flinkVersion, Function1<ClassLoader, T> func) {
    ClassLoader shimsClassLoader = getFlinkShimsClassLoader(flinkVersion);
    return ClassLoaderUtils.runAsClassLoader(
        shimsClassLoader, (Supplier<T>) () -> func.apply(shimsClassLoader));
  }

  /**
   * Get shimsClassLoader to execute for java API
   *
   * @param flinkVersion flinkVersion
   * @param func execute function
   * @tparam T
   * @return
   */
  public static <T> T proxy(FlinkVersion flinkVersion, Function<ClassLoader, T> func) {
    ClassLoader shimsClassLoader = getFlinkShimsClassLoader(flinkVersion);
    return ClassLoaderUtils.runAsClassLoader(
        shimsClassLoader, (Supplier<T>) () -> func.apply(shimsClassLoader));
  }

  // Need to load all flink-table dependencies compatible with different versions
  public static ClassLoader getVerifySqlLibClassLoader(FlinkVersion flinkVersion) {
    LOG.info("Add verify sql lib,flink version: {}", flinkVersion);
    return VERIFY_SQL_CLASS_LOADER_CACHE.computeIfAbsent(
        flinkVersion.fullVersion(),
        fullVersion -> {
          Predicate<File> getFlinkTable = file -> file.getName().startsWith("flink-table");
          // 1) Flink/lib/flink-table*
          List<URL> libTableURL = getFlinkHomeLib(flinkVersion.flinkHome(), "lib", getFlinkTable);

          // 2) After version 1.15 need add flink/opt/flink-table*
          List<URL> optTableURL = getFlinkHomeLib(flinkVersion.flinkHome(), "opt", getFlinkTable);
          List<URL> shimsUrls =
              new ArrayList<URL>() {
                {
                  addAll(libTableURL);
                  addAll(optTableURL);
                }
              };

          // 3) Add only streampark shims jar
          addShimsUrls(
              flinkVersion,
              file -> {
                if (file != null && file.getName().startsWith("streampark-flink-shims")) {
                  try {
                    shimsUrls.add(file.toURI().toURL());
                  } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                  }
                }
              });
          return new ChildFirstClassLoader(
              shimsUrls.toArray(new URL[0]),
              Thread.currentThread().getContextClassLoader(),
              getFlinkShimsResourcePattern(flinkVersion.majorVersion()));
        });
  }

  public static void addShimsUrls(FlinkVersion flinkVersion, Consumer<File> addShimUrl) {
    String appHome = System.getProperty(ConfigConst.KEY_APP_HOME());
    Utils.notNull(
        appHome, String.format("%s is not found on System env.", ConfigConst.KEY_APP_HOME()));

    File libPath = new File(appHome, "lib");
    Utils.required(libPath.exists());
    final String majorVersion = flinkVersion.majorVersion();
    final String scalaVersion = flinkVersion.scalaVersion();

    Optional.ofNullable(libPath.listFiles())
        .ifPresent(
            files ->
                Arrays.stream(files)
                    .filter(f -> f.getName().endsWith(".jar"))
                    .forEach(
                        jar -> {
                          final String prefixVer =
                              String.format(
                                  "%s-%s_%s", FLINK_SHIMS_PREFIX, majorVersion, scalaVersion);
                          if (jar.getName().startsWith(FLINK_SHIMS_PREFIX)
                              && jar.getName().startsWith(prefixVer)) {
                            addShimUrl.accept(jar);
                            LOG.info("Include flink shims jar lib: {}.", jar.getName());
                          } else {
                            if (INCLUDE_PATTERN.matcher(jar.getName()).matches()) {
                              addShimUrl.accept(jar);
                              LOG.info("Include jar lib: {}.", jar.getName());
                            } else if (jar.getName()
                                .matches(String.format("^streampark-.*_%s.*$$", scalaVersion))) {
                              addShimUrl.accept(jar);
                              LOG.info("Include streampark lib: {}.", jar.getName());
                            }
                          }
                        }));
  }

  /**
   * Get ClassLoader to verify sql
   *
   * @param flinkVersion flinkVersion
   * @param func execute function
   * @tparam T
   * @return
   */
  public static <T> T proxyVerifySql(FlinkVersion flinkVersion, Function<ClassLoader, T> func) {
    ClassLoader shimsClassLoader = getVerifySqlLibClassLoader(flinkVersion);
    return ClassLoaderUtils.runAsClassLoader(
        shimsClassLoader, (Supplier<T>) () -> func.apply(shimsClassLoader));
  }

  private static ClassLoader getFlinkShimsClassLoader(FlinkVersion flinkVersion) {
    LOG.info("Add flink shims urls classloader,flink version: {}", flinkVersion);

    return SHIMS_CLASS_LOADER_CACHE.computeIfAbsent(
        flinkVersion.fullVersion(),
        fullVersion -> {

          // 1) Flink/lib
          final List<URL> libURL =
              getFlinkHomeLib(
                  flinkVersion.flinkHome(), "lib", file -> !file.getName().startsWith("log4j"));
          final List<URL> shimsUrls = new ArrayList<>(libURL);
          // 2) Add all shims jar
          addShimsUrls(
              flinkVersion,
              file -> {
                if (file != null) {
                  try {
                    shimsUrls.add(file.toURI().toURL());
                  } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                  }
                }
              });

          return new ChildFirstClassLoader(
              shimsUrls.toArray(new URL[0]),
              Thread.currentThread().getContextClassLoader(),
              getFlinkShimsResourcePattern(flinkVersion.majorVersion()));
        });
  }

  private static List<URL> getFlinkHomeLib(
      String flinkHome, String childDir, Predicate<File> filterFun) {
    final File file = new File(flinkHome, childDir);
    Utils.required(
        file.isDirectory() && file.exists(),
        String.format(
            "FLINK_HOME '%s' doesn't exist or isn't a directory.", file.getAbsolutePath()));
    File[] fs = file.listFiles();
    return fs == null
        ? new ArrayList<>()
        : Arrays.stream(fs)
            .filter(filterFun)
            .map(
                file1 -> {
                  try {
                    return file1.toURI().toURL();
                  } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                  }
                })
            .collect(Collectors.toList());
  }

  public static <T> T getObject(ClassLoader loader, Object obj) throws Exception {

    final ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
    Object result =
        Utils.usingAdaptive(
            new ObjectOutputStream(arrayOutputStream),
            objectOutputStream -> {
              objectOutputStream.writeObject(obj);
              ByteArrayInputStream byteArrayInputStream =
                  new ByteArrayInputStream(arrayOutputStream.toByteArray());
              return Utils.usingAdaptive(
                  new ClassLoaderObjectInputStream(loader, byteArrayInputStream),
                  ObjectInputStream::readObject,
                  null);
            },
            null);

    return (T) result;
  }
}
