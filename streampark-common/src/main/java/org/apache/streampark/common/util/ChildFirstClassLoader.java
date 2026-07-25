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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;

public class ChildFirstClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }
    private final List<String> parentFirstClasses;
    private final Predicate<String> loadJarFilter;

    public ChildFirstClassLoader(URL[] urls, ClassLoader parent, List<String> parentFirstClasses,
                                 Predicate<String> loadJarFilter) {
        super(urls, parent);
        this.parentFirstClasses = parentFirstClasses;
        this.loadJarFilter = loadJarFilter;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                boolean parentFirst = false;
                for (String p : parentFirstClasses) {
                    if (name.startsWith(p)) {
                        parentFirst = true;
                        break;
                    }
                }
                if (parentFirst)
                    c = super.loadClass(name, resolve);
                else {
                    try {
                        c = findClass(name);
                    } catch (ClassNotFoundException e) {
                        c = super.loadClass(name, resolve);
                    }
                }
            }
            if (resolve)
                resolveClass(c);
            return c;
        }
    }

    @Override
    public URL getResource(String name) {
        URL urlClassLoaderResource = findResource(name);
        return urlClassLoaderResource != null ? urlClassLoaderResource : super.getResource(name);
    }

    private URL filterResource(URL urlClassLoaderResource) {
        if (urlClassLoaderResource != null && "jar".equals(urlClassLoaderResource.getProtocol())) {
            String spec = urlClassLoaderResource.getFile();
            String jarName = new java.io.File(spec.substring(0, spec.indexOf("!/"))).getName();
            if (loadJarFilter.test(jarName))
                return null;
        }
        return urlClassLoaderResource;
    }

    private void addResources(List<URL> result, Enumeration<URL> resources) {
        while (resources.hasMoreElements()) {
            URL url = filterResource(resources.nextElement());
            if (url != null)
                result.add(url);
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> result = new ArrayList<>();
        addResources(result, findResources(name));
        ClassLoader parent = getParent();
        if (parent != null)
            addResources(result, parent.getResources(name));
        return Collections.enumeration(result);
    }
}
