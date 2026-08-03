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

import org.apache.streampark.common.conf.CommonConfig;
import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.conf.InternalConfigHolder;
import org.apache.streampark.common.fs.LfsOperator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import scala.Option;

/** Hadoop client configuration tools mainly for flink use. */
public final class HadoopConfigUtils {

    private static final String[] HADOOP_CLIENT_CONF_FILES =
        {"core-site.xml", "hdfs-site.xml", "yarn-site.xml"};

    private static final String[] HIVE_CLIENT_CONF_FILES =
        {"core-site.xml", "hdfs-site.xml", "hive-site.xml"};

    private static final Map<String, String> KERBEROS_CONF = loadKerberosConf();

    public static final String HADOOP_USER_NAME =
        InternalConfigHolder.get(CommonConfig.STREAMPARK_HADOOP_USER_NAME());

    public static String hadoopUserName() {
        return HADOOP_USER_NAME;
    }

    public static final String KERBEROS_DEBUG =
        KERBEROS_CONF.getOrDefault(ConfigKeys.KEY_SECURITY_KERBEROS_DEBUG(), "false");

    public static final boolean KERBEROS_ENABLE =
        Boolean.parseBoolean(
            KERBEROS_CONF.getOrDefault(ConfigKeys.KEY_SECURITY_KERBEROS_ENABLE(), "false"));

    public static final String KERBEROS_PRINCIPAL =
        KERBEROS_CONF.getOrDefault(ConfigKeys.KEY_SECURITY_KERBEROS_PRINCIPAL(), "").trim();

    public static final String KERBEROS_KEYTAB =
        KERBEROS_CONF.getOrDefault(ConfigKeys.KEY_SECURITY_KERBEROS_KEYTAB(), "").trim();

    public static final String KERBEROS_KRB5 =
        KERBEROS_CONF.getOrDefault(ConfigKeys.KEY_SECURITY_KERBEROS_KRB5_CONF(), "");

    private HadoopConfigUtils() {
    }

    private static Map<String, String> loadKerberosConf() {
        Properties props = System.getProperties();
        Map<String, String> map = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("security.kerberos")) {
                map.put(key, props.getProperty(key));
            }
        }
        return map;
    }

    public static Option<String> getSystemHadoopConfDir() {
        return Option.apply(getSystemHadoopConfDirOptional().orElse(null));
    }

    private static Optional<String> getSystemHadoopConfDirOptional() {
        try {
            return Optional.of(FileUtils.getPathFromEnv("HADOOP_CONF_DIR"));
        } catch (Exception e) {
            try {
                return Optional.of(
                    FileUtils.resolvePath(FileUtils.getPathFromEnv("HADOOP_HOME"), "/etc/hadoop"));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
    }

    public static Optional<String> getSystemHadoopConfDirAsJava() {
        return getSystemHadoopConfDirOptional();
    }

    public static Option<String> getSystemHiveConfDir() {
        return Option.apply(getSystemHiveConfDirOptional().orElse(null));
    }

    private static Optional<String> getSystemHiveConfDirOptional() {
        try {
            return Optional.of(FileUtils.getPathFromEnv("HIVE_CONF_DIR"));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<String> getSystemHiveConfDirAsJava() {
        return getSystemHiveConfDirOptional();
    }

    public static void replaceHostWithIP(File configFile) {
        if (configFile.exists() && configFile.isFile() && configFile.getName().endsWith(".xml")) {
            Map<String, String> hostsMap = HostsUtils.getSortSystemHosts();
            if (!hostsMap.isEmpty()) {
                rewriteHostIpMapper(configFile, hostsMap);
            }
        }
    }

    public static void batchReplaceHostWithIP(File configDir) {
        batchReplaceHostWithIP(configDir, HADOOP_CLIENT_CONF_FILES);
    }

    public static void batchReplaceHostWithIP(File configDir, String[] filter) {
        if (!configDir.isDirectory()) {
            replaceHostWithIP(configDir);
            return;
        }
        Map<String, String> hostsMap = HostsUtils.getSortSystemHosts();
        if (hostsMap.isEmpty()) {
            return;
        }
        File[] files = configDir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile()) {
                for (String name : filter) {
                    if (name.equals(file.getName())) {
                        rewriteHostIpMapper(file, hostsMap);
                        break;
                    }
                }
            }
        }
    }

    private static void rewriteHostIpMapper(File configFile, Map<String, String> hostsMap) {
        try {
            List<String> rawLines =
                org.apache.commons.io.FileUtils.readLines(configFile, StandardCharsets.UTF_8);
            List<String> lines = new ArrayList<>();
            for (String line : rawLines) {
                if (!line.trim().startsWith("<value>")) {
                    lines.add(line);
                    continue;
                }
                String li = line;
                Map.Entry<String, String> shot = findHostMatch(li, hostsMap);
                while (shot != null) {
                    li = li.replace(shot.getKey(), shot.getValue());
                    shot = findHostMatch(li, hostsMap);
                }
                lines.add(li);
            }
            org.apache.commons.io.FileUtils.writeLines(configFile, lines);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to rewrite Hadoop configuration file: " + configFile, e);
        }
    }

    private static Map.Entry<String, String> findHostMatch(String line, Map<String, String> hostsMap) {
        for (Map.Entry<String, String> entry : hostsMap.entrySet()) {
            if (line.contains(entry.getKey())) {
                return entry;
            }
        }
        return null;
    }

    public static Map<String, String> readSystemHadoopConf() {
        return getSystemHadoopConfDirOptional()
            .map(dir -> readSystemConfFiles(dir, HADOOP_CLIENT_CONF_FILES, "Hadoop"))
            .orElse(Collections.emptyMap());
    }

    public static Map<String, String> readSystemHiveConf() {
        return getSystemHiveConfDirOptional()
            .map(dir -> readSystemConfFiles(dir, HIVE_CLIENT_CONF_FILES, "Hive"))
            .orElse(Collections.emptyMap());
    }

    private static Map<String, String> readSystemConfFiles(
                                                           String confDir,
                                                           String[] confFileNames,
                                                           String confLabel) {
        Map<String, String> map = new LinkedHashMap<>();
        File[] files = LfsOperator.listDir(confDir);
        if (files == null) {
            return map;
        }
        for (File f : files) {
            if (!matchesConfFile(f.getName(), confFileNames)) {
                continue;
            }
            try {
                map.put(
                    f.getName(),
                    org.apache.commons.io.FileUtils.readFileToString(f, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException(
                    "Failed to read " + confLabel + " configuration file: " + f.getAbsolutePath(), e);
            }
        }
        return map;
    }

    private static boolean matchesConfFile(String fileName, String[] confFileNames) {
        for (String name : confFileNames) {
            if (name.equals(fileName)) {
                return true;
            }
        }
        return false;
    }
}
