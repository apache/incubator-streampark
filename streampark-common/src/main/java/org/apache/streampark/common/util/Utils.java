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

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

/** General utility methods. */
public final class Utils {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(Utils.class.getName());

    private static final Pattern UUID_DASH_PATTERN = Pattern.compile("-");

    private static final String OS = System.getProperty("os.name").toLowerCase();

    private Utils() {
    }

    public static boolean isNotEmpty(Object elem) {
        if (elem == null) {
            return false;
        }
        if (elem instanceof Object[]) {
            return ((Object[]) elem).length > 0;
        }
        if (elem instanceof CharSequence) {
            return elem.toString().trim().length() > 0;
        }
        if (elem instanceof Collection) {
            return !((Collection<?>) elem).isEmpty();
        }
        if (elem instanceof Iterable) {
            return ((Iterable<?>) elem).iterator().hasNext();
        }
        if (elem instanceof Map) {
            return !((Map<?, ?>) elem).isEmpty();
        }
        return true;
    }

    public static boolean isEmpty(Object elem) {
        return !isNotEmpty(elem);
    }

    public static String uuid() {
        return UUID_DASH_PATTERN.matcher(UUID.randomUUID().toString()).replaceAll("");
    }

    public static void requireCheckJarFile(URL jar) throws IOException {
        try (
            InputStream in = SafePathUtils.openJarFile(jar);
            JarInputStream ignored = new JarInputStream(new BufferedInputStream(in))) {
            // verify jar is readable
        } catch (IOException e) {
            throw new IOException("Error while opening jar file '" + jar + "'", e);
        }
    }

    public static Manifest getJarManifest(File jarFile) throws IOException {
        try (
            InputStream in = SafePathUtils.openJarFile(jarFile.toURI().toURL());
            JarInputStream jarInputStream = new JarInputStream(new BufferedInputStream(in))) {
            return jarInputStream.getManifest();
        }
    }

    public static String getJarManClass(File jarFile) {
        try {
            Manifest manifest = getJarManifest(jarFile);
            String mainClass = manifest.getMainAttributes().getValue("Main-Class");
            if (mainClass == null) {
                mainClass = manifest.getMainAttributes().getValue("program-class");
            }
            return mainClass;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copyProperties(Properties original, Properties target) {
        for (String key : original.stringPropertyNames()) {
            target.put(key, original.getProperty(key));
        }
    }

    public static Properties toProperties(Map<String, String> map) {
        Properties properties = new Properties();
        if (map != null) {
            map.forEach(properties::setProperty);
        }
        return properties;
    }

    public static boolean isLinux() {
        return OS.indexOf("linux") >= 0;
    }

    public static boolean isWindows() {
        return OS.indexOf("windows") >= 0;
    }

    public static boolean isAnyBank(String... items) {
        if (items == null) {
            return true;
        }
        for (String item : items) {
            if (StringUtils.isBlank(item)) {
                return true;
            }
        }
        return false;
    }

    public static double calPercent(long num1, long num2) {
        if (num1 == 0 || num2 == 0) {
            return 0.0;
        }
        return Double.parseDouble(String.format("%.1f", num1 * 100.0 / num2));
    }

    public static int hashCode(Object... elements) {
        if (elements == null) {
            return 0;
        }
        int result = 1;
        for (Object elem : elements) {
            int hash = elem == null ? 0 : elem.hashCode();
            result = 31 * result + hash;
        }
        return result;
    }

    @SafeVarargs
    public static void close(java.util.function.Consumer<Throwable> func, AutoCloseable... closeable) {
        for (AutoCloseable c : closeable) {
            try {
                if (c != null) {
                    if (c instanceof Flushable) {
                        ((Flushable) c).flush();
                    }
                    c.close();
                }
            } catch (Throwable e) {
                if (func != null) {
                    func.accept(e);
                }
            }
        }
    }

    public static void close(AutoCloseable... closeable) {
        close(null, closeable);
    }

    public static <R> java.util.Optional<R> retry(int retryCount, Duration interval, RetrySupplier<R> supplier) {
        if (retryCount < 0) {
            throw new IllegalArgumentException("retryCount must be >= 0");
        }
        try {
            return java.util.Optional.of(supplier.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return java.util.Optional.empty();
        } catch (Exception e) {
            if (retryCount > 0) {
                LOG.warn("[StreamPark] Retry failed, execution caused by: ", e);
                LOG.warn(
                    "[StreamPark] {} times retry remaining, the next attempt will be in {} ms",
                    retryCount,
                    interval.toMillis());
                LockSupport.parkNanos(interval.toNanos());
                return retry(retryCount - 1, interval, supplier);
            }
            return java.util.Optional.empty();
        }
    }

    public static boolean checkHttpURL(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            return false;
        }
    }

    public static void printLogo(String info) {
        System.out.println("\n");
        System.out.println("        _____ __                                             __       ");
        System.out.println("       / ___// /_________  ____ _____ ___  ____  ____ ______/ /__     ");
        System.out.println("       \\__ \\/ __/ ___/ _ \\/ __ `/ __ `__ \\/ __ \\  __ `/ ___/ //_/");
        System.out.println("      ___/ / /_/ /  /  __/ /_/ / / / / / / /_/ / /_/ / /  / ,<        ");
        System.out.println("     /____/\\__/_/   \\___/\\__,_/_/ /_/ /_/ ____/\\__,_/_/  /_/|_|   ");
        System.out.println("                                       /_/                        \n\n");
        System.out.println("    Version:  3.0.0-SNAPSHOT                                          ");
        System.out.println("    WebSite:  https://streampark.apache.org                           ");
        System.out.println("    GitHub :  https://github.com/apache/streampark                    ");
        System.out.println("    Info   :  " + info + "                                 ");
        System.out.println("    Time   :  " + LocalDateTime.now(ZoneId.systemDefault()) + "              \n\n");
    }

    @FunctionalInterface
    public interface RetrySupplier<R> {

        R get() throws Exception;
    }
}
