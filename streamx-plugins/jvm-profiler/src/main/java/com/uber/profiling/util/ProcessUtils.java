/*
 * Copyright (c) 2018 Uber Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uber.profiling.util;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessUtils {
    private static final String SPARK_PROCESS_KEYWORD = "spark.yarn.app.container.log.dir";
    private static final String SPARK_CMDLINE_KEYWORD = "spark.";
    private static final String SPARK_EXECUTOR_CLASS_NAME = "spark.executor.CoarseGrainedExecutorBackend";
    private static final String SPARK_EXECUTOR_KEYWORD = "spark.driver.port";

    private static final Pattern XMX_REGEX = Pattern.compile("-[xX][mM][xX]([a-zA-Z0-9]+)");
    
    public static String getCurrentProcessName() {
        try {
            return ManagementFactory.getRuntimeMXBean().getName();
        } catch (Throwable ex) {
            return ex.getMessage();
        }
    }

    public static String getJvmClassPath() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        return runtimeMXBean.getClassPath();
    }

    public static List<String> getJvmInputArguments() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        List<String> jvmArgs = runtimeMXBean.getInputArguments();
        return jvmArgs == null ? new ArrayList<>() : jvmArgs;
    }

    public static Long getJvmXmxBytes() {
        Long result = null;
        
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        List<String> jvmArgs = runtimeMXBean.getInputArguments();
        if (jvmArgs == null) {
            return null;
        }
        
        for (String entry : jvmArgs) {
            Matcher matcher = XMX_REGEX.matcher(entry);
            if (matcher.matches()) {
                String str = matcher.group(1);
                result = StringUtils.getBytesValueOrNull(str);
            }
        }
        
        return result;
    }
    
    public static boolean isSparkProcess(String cmdline) {
        if (cmdline != null && !cmdline.isEmpty()) {
            if (cmdline.contains(SPARK_CMDLINE_KEYWORD)) {
                return true;
            }
        }
        
        List<String> strList = ProcessUtils.getJvmInputArguments();
        for (String str : strList) {
            if (str.toLowerCase().contains(SPARK_PROCESS_KEYWORD.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSparkExecutor(String cmdline) {
        if (cmdline != null && !cmdline.isEmpty()) {
            if (cmdline.contains(SPARK_EXECUTOR_CLASS_NAME)) {
                return true;
            }
        }
        
        List<String> strList = ProcessUtils.getJvmInputArguments();
        for (String str : strList) {
            if (str.toLowerCase().contains(SPARK_EXECUTOR_KEYWORD.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSparkDriver(String cmdline) {
        return isSparkProcess(cmdline) && !isSparkExecutor(cmdline);
    }

    public static void main(String[] args) {
        System.out.println(getCurrentProcessName());
        System.out.println(isSparkProcess(null));
        System.out.println(isSparkExecutor(null));
        System.out.println(isSparkDriver(null));
    }
}
