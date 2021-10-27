/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.flink.proxy;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * A variant of the URLClassLoader that first loads from the URLs and only after that from the
 * parent.
 *
 * <p>{@link #getResourceAsStream(String)} uses {@link #getResource(String)} internally so we don't
 * override that.
 *
 * @author benjobs
 * @author zzz
 */
public final class ChildFirstClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public static final Consumer<Throwable> NOOP_EXCEPTION_HANDLER = classLoadingException -> {
    };

    private static final String[] ALWAYS_PARENT_FIRST_LOADER_PATTERNS = "java.;scala.;org.apache.flink.;com.esotericsoftware.kryo;org.apache.hadoop.;javax.annotation.;org.slf4j;org.apache.log4j;org.apache.logging;org.apache.commons.logging;ch.qos.logback;org.xml;javax.xml;org.apache.xerces;org.w3c"
        .split(";");


    private Pattern FLINK_PATTERN = Pattern.compile(
        "flink-(.*).jar",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private final Consumer<Throwable> classLoadingExceptionHandler;

    private final Pattern resourcePattern;

    public ChildFirstClassLoader(URL[] urls, Pattern resourcePattern) {
        this(urls, null, resourcePattern);
    }

    public ChildFirstClassLoader(URL[] urls, ClassLoader parent, Pattern resourcePattern) {
        this(urls, parent, resourcePattern, NOOP_EXCEPTION_HANDLER);
    }

    public ChildFirstClassLoader(
        URL[] urls,
        ClassLoader parent,
        Pattern resourcePattern,
        Consumer<Throwable> classLoadingExceptionHandler) {
        super(urls, parent);
        this.resourcePattern = resourcePattern;
        this.classLoadingExceptionHandler = classLoadingExceptionHandler;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            synchronized (getClassLoadingLock(name)) {
                return this.loadClassWithoutExceptionHandling(name, resolve);
            }
        } catch (Throwable classLoadingException) {
            classLoadingExceptionHandler.accept(classLoadingException);
            throw classLoadingException;
        }
    }

    @Override
    public URL getResource(String name) {
        // first, try and find it via the URLClassloader
        URL urlClassLoaderResource = findResource(name);

        if (urlClassLoaderResource != null) {
            return urlClassLoaderResource;
        }
        // delegate to super
        return super.getResource(name);
    }


    private URL filterFlinkShimsResource(URL urlClassLoaderResource) {
        if (urlClassLoaderResource != null && "jar".equals(urlClassLoaderResource.getProtocol())) {
            /**
             * {@link java.net.JarURLConnection#parseSpecs}
             */
            String spec = urlClassLoaderResource.getFile();
            String filename = new File(spec.substring(0, spec.indexOf("!/"))).getName();

            if (FLINK_PATTERN.matcher(filename).matches() && !resourcePattern.matcher(filename).matches()) {
                return null;
            }
        }

        return urlClassLoaderResource;
    }

    private List<URL> addResources(List<URL> result, Enumeration<URL> resources) {
        while (resources.hasMoreElements()) {
            URL urlClassLoaderResource = filterFlinkShimsResource(resources.nextElement());

            if (urlClassLoaderResource != null) {
                result.add(urlClassLoaderResource);
            }
        }

        return result;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        // first get resources from URLClassloader
        final List<URL> result = addResources(new ArrayList<>(), findResources(name));

        ClassLoader parent = getParent();

        if (parent != null) {
            // get parent urls
            addResources(result, parent.getResources(name));
        }

        return new Enumeration<URL>() {
            final Iterator<URL> iter = result.iterator();

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

    private Class<?> loadClassWithoutExceptionHandling(String name, boolean resolve)
        throws ClassNotFoundException {

        // First, check if the class has already been loaded
        Class<?> c = super.findLoadedClass(name);
        if (c == null) {
            if (c == null) {
                // check whether the class should go parent-first
                for (String alwaysParentFirstPattern : ALWAYS_PARENT_FIRST_LOADER_PATTERNS) {
                    if (name.startsWith(alwaysParentFirstPattern)) {
                        return super.loadClass(name, resolve);
                    }
                }

                try {
                    // check the URLs
                    c = findClass(name);
                } catch (ClassNotFoundException e) {
                    // let URLClassLoader do it, which will eventually call the parent
                    c = super.loadClass(name, resolve);
                }
            } else if (resolve) {
                resolveClass(c);
            }
        } else if (resolve) {
            resolveClass(c);
        }
        return c;
    }

}
