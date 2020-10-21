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

import com.uber.profiling.profilers.Constants;

import java.util.List;

public class SparkUtils {
    // Try to get application ID by match regex in class path or system property
    public static String probeAppId(String appIdRegex) {
        String appId = System.getProperty("spark.app.id");

        if (appId == null || appId.isEmpty()) {
            String classPath = ProcessUtils.getJvmClassPath();
            List<String> appIdCandidates = StringUtils.extractByRegex(classPath, appIdRegex);
            if (!appIdCandidates.isEmpty()) {
                appId = appIdCandidates.get(0);
            }
        }

        if (appId == null || appId.isEmpty()) {
            for (String entry : ProcessUtils.getJvmInputArguments()) {
                List<String> appIdCandidates = StringUtils.extractByRegex(entry, appIdRegex);
                if (!appIdCandidates.isEmpty()) {
                    appId = appIdCandidates.get(0);
                    break;
                }
            }
        }

        return appId;
    }
    
    // Get application ID by invoking SparkEnv
    public static String getSparkEnvAppId() {
        // Do not use "org.apache.spark.SparkEnv" directly because the maven shade plugin will convert 
        // the class name to ja_shaded.org.apache.spark.SparkEnv due to relocation.
        String className = org.apache.commons.lang3.StringUtils.joinWith(
                ".", 
                "org",
                "apache",
                "spark",
                "SparkEnv");
        try {
            Object result = ReflectionUtils.executeStaticMethods(
                    className, 
                    "get.conf.getAppId");
            if (result == null) {
                return null;
            }
            return result.toString();
        } catch (Throwable e) {
            return null;
        }
    }

    public static String probeRole(String cmdline) {
        if (ProcessUtils.isSparkExecutor(cmdline)) {
            return Constants.EXECUTOR_ROLE;
        } else if (ProcessUtils.isSparkDriver(cmdline)) {
            return Constants.DRIVER_ROLE;
        } else {
            return null;
        }
    }
    
    public static SparkAppCmdInfo probeCmdInfo() {
        // TODO use /proc file system to get command when the system property is not available
        String cmd = System.getProperty("sun.java.command");
        if (cmd == null || cmd.isEmpty()) {
            return null;
        }

        SparkAppCmdInfo result = new SparkAppCmdInfo();

        result.setAppJar(StringUtils.getArgumentValue(cmd, "--jar"));
        result.setAppClass(StringUtils.getArgumentValue(cmd, "--class"));
        
        return result;
    }
}
