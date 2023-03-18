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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * A variant of the URLClassLoader that first loads from the URLs and only after that from the
 * parent.
 *
 * <p>{@link #getResourceAsStream(String)} uses {@link #getResource(String)} internally so we don't
 * override that.
 */
public class ChildFirstClassLoader extends URLClassLoader {

  private static final Pattern FLINK_PATTERN =
      Pattern.compile("flink-(.*).jar", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  private static final String JAR_PROTOCOL = "jar";

  private static final List<String> PARENT_FIRST_PATTERNS =
      new ArrayList<String>() {
        {
          add("java.");
          add("javax.xml");
          add("org.slf4j");
          add("org.apache.log4j");
          add("org.apache.logging");
          add("org.apache.commons.logging");
          add("ch.qos.logback");
          add("org.xml");
          add("org.w3c");
          add("org.apache.hadoop");
        }
      };

  private final Pattern flinkResourcePattern;
  private final Consumer<Throwable> classLoadingExceptionHandler;

  public ChildFirstClassLoader(URL[] urls, ClassLoader parent, Pattern flinkResourcePattern) {
    this(urls, parent, flinkResourcePattern, throwable -> {});
  }

  public ChildFirstClassLoader(
      URL[] urls,
      ClassLoader parent,
      Pattern flinkResourcePattern,
      Consumer<Throwable> classLoadingExceptionHandler) {
    super(urls, parent);
    this.flinkResourcePattern = flinkResourcePattern;
    this.classLoadingExceptionHandler = classLoadingExceptionHandler;
    ClassLoader.registerAsParallelCapable();
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    try {
      synchronized (this) {
        return this.loadClassWithoutExceptionHandling(name, resolve);
      }
    } catch (Throwable e) {
      Optional.ofNullable(classLoadingExceptionHandler).ifPresent((h) -> h.accept(e));
      throw e;
    }
  }

  @Override
  public URL getResource(String name) {
    // First, try and find it via the URLClassloader
    Optional<URL> urlClassLoaderResourceOptional = Optional.ofNullable(findResource(name));
    // Delegate to super
    return urlClassLoaderResourceOptional.orElseGet(() -> super.getResource(name));
  }

  /**
   * e.g. flinkResourcePattern: flink-1.12
   *
   * <p>flink-1.12.jar/resource flink-1.14.jar/resource other.jar/resource => after
   * filterFlinkShimsResource => flink-1.12.jar/resource other.jar/resource
   *
   * @param urlClassLoaderResource
   * @return
   */
  private URL filterFlinkShimsResource(URL urlClassLoaderResource) {
    if (urlClassLoaderResource == null
        || !JAR_PROTOCOL.equals(urlClassLoaderResource.getProtocol())) {
      return urlClassLoaderResource;
    }

    final String spec = urlClassLoaderResource.getFile();
    final String filename = new File(spec.substring(0, spec.indexOf("!/"))).getName();
    if (FLINK_PATTERN.matcher(filename).matches()
        && !flinkResourcePattern.matcher(filename).matches()) {
      return null;
    }
    return urlClassLoaderResource;
  }

  private List<URL> addResources(List<URL> result, Enumeration<URL> resources) {
    while (resources.hasMoreElements()) {
      Optional.ofNullable(filterFlinkShimsResource(resources.nextElement())).ifPresent(result::add);
    }
    return result;
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    // First get resources from URLClassloader
    final List<URL> result = addResources(new ArrayList<>(), findResources(name));
    if (getParent() != null) {
      // Get parent urls
      addResources(result, getParent().getResources(name));
    }
    return new Enumeration<URL>() {
      private final Iterator<URL> iter = result.iterator();

      @Override
      public boolean hasMoreElements() {
        return iter.hasNext();
      }

      @Override
      public URL nextElement() {
        return iter.next();
      }
    };
  }

  private Class<?> loadClassWithoutExceptionHandling(String name, Boolean resolve)
      throws ClassNotFoundException {
    // First, check if the class has already been loaded
    Class<?> loadedClass = super.findLoadedClass(name);

    if (loadedClass == null) {
      // Check whether the class should go parent-first
      for (String parentFirstPattern : PARENT_FIRST_PATTERNS) {
        if (name.startsWith(parentFirstPattern)) {
          return super.loadClass(name, resolve);
        }
      }
      try {
        return findClass(name);
      } catch (Exception e) {
        return super.loadClass(name, resolve);
      }
    }

    if (resolve) {
      resolveClass(loadedClass);
    }
    return loadedClass;
  }
}
