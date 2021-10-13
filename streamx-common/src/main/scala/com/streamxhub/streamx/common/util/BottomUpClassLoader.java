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
package com.streamxhub.streamx.common.util;


import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * A variant of the URLClassLoader that first loads from the URLs and only after that from the
 * parent.
 *
 * <p>{@link #getResourceAsStream(String)} uses {@link #getResource(String)} internally so we don't
 * override that.
 * @author benjobs
 */
public final class BottomUpClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public static final Consumer<Throwable> NOOP_EXCEPTION_HANDLER = classLoadingException -> {
    };

    private final Consumer<Throwable> classLoadingExceptionHandler;

    public BottomUpClassLoader(URL[] urls, ClassLoader parent) {
        this(urls, parent, NOOP_EXCEPTION_HANDLER);
    }

    public BottomUpClassLoader(
        URL[] urls,
        ClassLoader parent,
        Consumer<Throwable> classLoadingExceptionHandler) {
        super(urls, parent);
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

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        // first get resources from URLClassloader
        Enumeration<URL> urlClassLoaderResources = findResources(name);

        final List<URL> result = new ArrayList<>();

        while (urlClassLoaderResources.hasMoreElements()) {
            result.add(urlClassLoaderResources.nextElement());
        }

        // get parent urls
        Enumeration<URL> parentResources = getParent().getResources(name);

        while (parentResources.hasMoreElements()) {
            result.add(parentResources.nextElement());
        }

        return new Enumeration<URL>() {
            Iterator<URL> iter = result.iterator();

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
        return c;
    }

}
