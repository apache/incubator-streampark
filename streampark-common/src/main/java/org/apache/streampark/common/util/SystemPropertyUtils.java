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
import org.apache.streampark.shaded.org.slf4j.LoggerFactory;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.regex.Pattern;

public final class SystemPropertyUtils {

    private static final Pattern CLASSES_SUFFIX_PATTERN = Pattern.compile("classes/$");

    private static final Logger LOG =
        LoggerFactory.getLogger(SystemPropertyUtils.class);

    private SystemPropertyUtils() {
    }

    public static String getUserHome() {
        return System.getProperty("user.home");
    }

    public static boolean contains(String key) {
        return get(key) != null;
    }

    public static String get(String key) {
        return get(key, null);
    }

    public static String get(String key, String defaultValue) {
        if (key == null) {
            throw new IllegalArgumentException("[StreamPark] key must not be null.");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty.");
        }
        try {
            String value;
            if (System.getSecurityManager() == null) {
                value = System.getProperty(key);
            } else {
                value =
                    AccessController.doPrivileged(
                        (PrivilegedAction<String>) () -> System.getProperty(key));
            }
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            LOG.warn(
                "[StreamPark] Unable to retrieve a system property '{}'; default values will be used, {}.",
                key,
                e.getMessage());
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        switch (value) {
            case "true":
            case "yes":
            case "1":
                return true;
            case "false":
            case "no":
            case "0":
                return false;
            default:
                if (value.isEmpty()) {
                    return false;
                }
                LOG.warn(
                    "[StreamPark] Unable to parse the boolean system property '{}':{} - using the default value: {}.",
                    key,
                    value,
                    defaultValue);
                return defaultValue;
        }
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static long getLong(String key, long defaultValue) {
        try {
            return Long.parseLong(get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String set(String key, String value) {
        return (String) System.getProperties().setProperty(key, value);
    }

    public static void setEnv(String name, String value) throws Exception {
        Class<?> envClass = Class.forName("java.lang.ProcessEnvironment");
        java.lang.reflect.Method getEnv = envClass.getDeclaredMethod("getenv");
        getEnv.setAccessible(true);
        Object unmodifiableEnvironment = getEnv.invoke(null);
        Class<?> clazz = Class.forName("java.util.Collections$UnmodifiableMap");
        java.lang.reflect.Field field = clazz.getDeclaredField("m");
        field.setAccessible(true);
        Object envMap = field.get(unmodifiableEnvironment);
        if (!(envMap instanceof Map)) {
            throw new IllegalStateException("Process environment is not a map");
        }
        Map.class.getMethod("put", Object.class, Object.class).invoke(envMap, name, value);
    }

    public static String getOrElseUpdate(String key, String defaultValue) {
        String existing = get(key);
        if (existing == null) {
            set(key, defaultValue);
            return defaultValue;
        }
        return existing;
    }

    public static void setAppHome(String key, Class<?> clazz) {
        if (get(key) == null) {
            String jarOrClassPath = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
            File file = new File(jarOrClassPath);
            String appHome;
            if (jarOrClassPath.endsWith("jar")) {
                appHome = file.getParentFile().getParentFile().getPath();
            } else {
                appHome = CLASSES_SUFFIX_PATTERN.matcher(file.getPath()).replaceAll("");
            }
            SystemPropertyUtils.set(key, appHome);
        }
    }

    public static String getTmpdir() {
        return get("java.io.tmpdir", "temp");
    }
}
