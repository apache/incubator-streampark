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

package org.apache.streampark.plugin.profiling.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// See http://man7.org/linux/man-pages/man5/proc.5.html for /proc file details
public class ProcFileUtils {
    private static final AgentLogger LOGGER = AgentLogger.getLogger(ProcFileUtils.class.getName());

    private static final String PROC_SELF_STATUS_FILE = "/proc/self/status";
    private static final String PROC_SELF_IO_FILE = "/proc/self/io";
    private static final String PROC_STAT_FILE = "/proc/stat";
    private static final String PROC_SELF_CMDLINE_FILE = "/proc/self/cmdline";
    private static final String VALUE_SEPARATOR = ":";

    public static Map<String, String> getProcStatus() {
        return getProcFileAsMap(PROC_SELF_STATUS_FILE);
    }

    public static Map<String, String> getProcIO() {
        return getProcFileAsMap(PROC_SELF_IO_FILE);
    }

    public static List<Map<String, Object>> getProcStatCpuTime() {
        List<String[]> rows = getProcFileAsRowColumn(PROC_STAT_FILE);
        return getProcStatCpuTime(rows);
    }

    public static Map<String, String> getProcFileAsMap(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || file.isDirectory() || !file.canRead()) {
                return Collections.emptyMap();
            }

            Map<String, String> result = new HashMap<>();
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                int index = line.indexOf(VALUE_SEPARATOR);
                if (index <= 0 || index >= line.length() - 1) {
                    continue;
                }
                String key = line.substring(0, index).trim();
                String value = line.substring(index + 1).trim();
                result.put(key, value);
            }
            return result;
        } catch (Throwable ex) {
            LOGGER.warn("Failed to read file " + filePath, ex);
            return Collections.emptyMap();
        }
    }

    public static List<String[]> getProcFileAsRowColumn(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || file.isDirectory() || !file.canRead()) {
                return Collections.emptyList();
            }

            List<String[]> result = new ArrayList<>();
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                result.add(line.split("\\s+"));
            }
            return result;
        } catch (Throwable ex) {
            LOGGER.warn("Failed to read file " + filePath, ex);
            return Collections.emptyList();
        }
    }

    public static Long getBytesValue(Map<String, String> data, String key) {
        if (data == null) {
            return null;
        }

        if (key == null) {
            return null;
        }

        String value = data.get(key);
        if (value == null) {
            return null;
        }

        return StringUtils.getBytesValueOrNull(value);
    }

    public static List<Map<String, Object>> getProcStatCpuTime(Collection<String[]> rows) {
        if (rows == null) {
            return Collections.emptyList();
        }

        final int minValuesInRow = 6;

        List<Map<String, Object>> result = new ArrayList<>();

        for (String[] row : rows) {
            if (row.length >= minValuesInRow && row[0].toLowerCase().startsWith("cpu")) {
                Map<String, Object> map = new HashMap<>();
                try {
                    map.put("cpu", row[0]);
                    map.put("user", Long.parseLong(row[1].trim()));
                    map.put("nice", Long.parseLong(row[2].trim()));
                    map.put("system", Long.parseLong(row[3].trim()));
                    map.put("idle", Long.parseLong(row[4].trim()));
                    map.put("iowait", Long.parseLong(row[5].trim()));
                    result.add(map);
                } catch (Throwable ex) {
                    continue;
                }
            }
        }

        return result;
    }

    public static String getPid() {
        return getPid(PROC_SELF_STATUS_FILE);
    }

    public static String getPid(String filePath) {
        // See http://man7.org/linux/man-pages/man5/proc.5.html for details about proc status
        Map<String, String> procStatus = getProcFileAsMap(filePath);
        return procStatus.get("Pid");

    }

    public static String getCmdline() {
        try {
            File file = new File(PROC_SELF_CMDLINE_FILE);
            if (!file.exists() || file.isDirectory() || !file.canRead()) {
                return null;
            }

            String cmdline = new String(Files.readAllBytes(Paths.get(file.getPath())));
            cmdline = cmdline.replace((char) 0, ' ');
            return cmdline;
        } catch (Throwable ex) {
            LOGGER.warn("Failed to read file " + PROC_SELF_CMDLINE_FILE, ex);
            return null;
        }
    }
}
