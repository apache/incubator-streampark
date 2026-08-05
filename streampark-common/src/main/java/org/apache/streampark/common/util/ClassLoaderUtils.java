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

import org.apache.streampark.common.constants.Constants;

import org.apache.streampark.shaded.org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public final class ClassLoaderUtils {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(ClassLoaderUtils.class.getName());
    private static final ClassLoader ORIGINAL_CLASS_LOADER = Thread.currentThread().getContextClassLoader();
    private ClassLoaderUtils() {
    }

    public static <R> R runAsClassLoader(ClassLoader targetClassLoader, Supplier<R> supplier) {
        try {
            Thread.currentThread().setContextClassLoader(targetClassLoader);
            return supplier.get();
        } finally {
            Thread.currentThread().setContextClassLoader(ORIGINAL_CLASS_LOADER);
        }
    }

    public static ClassLoader cloneClassLoader() throws IOException {
        java.util.Enumeration<URL> urls = ORIGINAL_CLASS_LOADER.getResources(".");
        List<URL> buffer = new ArrayList<>();
        while (urls.hasMoreElements())
            buffer.add(urls.nextElement());
        return new URLClassLoader(buffer.toArray(new URL[0]), ORIGINAL_CLASS_LOADER);
    }

    public static void loadJar(String jarFilePath) {
        File f = new File(jarFilePath);
        if (!f.exists()) {
            throw new IllegalArgumentException(
                "[StreamPark] ClassLoaderUtils.loadJar: jarFilePath " + jarFilePath + " is not exists");
        }
        if (!f.isFile()) {
            throw new IllegalArgumentException(
                "[StreamPark] ClassLoaderUtils.loadJar: jarFilePath " + jarFilePath + " is not file");
        }
        try {
            loadPath(f.getAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load jar: " + jarFilePath, e);
        }
    }

    public static void loadJars(String path) {
        File jarDir = new File(path);
        if (!jarDir.exists()) {
            throw new IllegalArgumentException(
                "[StreamPark] ClassLoaderUtils.loadJars: jarPath " + path + " is not exists");
        }
        if (!jarDir.isDirectory()) {
            throw new IllegalArgumentException(
                "[StreamPark] ClassLoaderUtils.loadJars: jarPath " + path + " is not directory");
        }
        File[] files = jarDir.listFiles();
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException(
                "[StreamPark] ClassLoaderUtils.loadJars: have not jar in path:" + path);
        }
        for (File x : files) {
            try {
                loadPath(x.getAbsolutePath());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load jar: " + x.getAbsolutePath(), e);
            }
        }
    }

    public static void loadResource(String filepath) throws Exception {
        addURL(new File(filepath));
    }
    public static void loadResourceDir(String filepath) throws Exception {
        loopDirs(new File(filepath));
    }

    private static void loadPath(String filepath) throws Exception {
        loopFiles(new File(filepath), Arrays.asList(Constants.JAR_SUFFIX, Constants.ZIP_SUFFIX));
    }

    private static void loopDirs(File file) throws Exception {
        if (file.isDirectory()) {
            addURL(file);
            File[] children = file.listFiles();
            if (children != null)
                for (File c : children)
                    loopDirs(c);
        }
    }

    private static void loopFiles(File file, List<String> ext) throws Exception {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null)
                for (File c : children)
                    loopFiles(c, ext);
        } else if (ext.isEmpty())
            addURL(file);
        else
            for (String e : ext)
                if (file.getName().endsWith(e)) {
                    Utils.requireCheckJarFile(file.toURI().toURL());
                    addURL(file);
                    break;
                }
    }

    private static void addURL(File file) throws Exception {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        if (classLoader instanceof URLClassLoader) {
            java.lang.reflect.Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);
            addURL.invoke(classLoader, file.toURI().toURL());
        } else {
            Class<?> clazz = classLoader.getClass();
            java.lang.reflect.Field ucpField = null;
            while (clazz != null && ucpField == null) {
                try {
                    ucpField = clazz.getDeclaredField("ucp");
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (ucpField == null)
                throw new IllegalArgumentException(
                    "[StreamPark] ClassLoaderUtils.addURL: cannot locate ucp field on classloader chain");
            ucpField.setAccessible(true);
            Object ucp = ucpField.get(classLoader);
            java.lang.reflect.Method addURL = ucp.getClass().getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);
            addURL.invoke(ucp, file.toURI().toURL());
        }
    }
}
