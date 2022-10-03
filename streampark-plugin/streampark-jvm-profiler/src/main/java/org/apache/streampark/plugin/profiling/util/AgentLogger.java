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

import org.apache.commons.lang3.exception.ExceptionUtils;

public class AgentLogger {

    private static boolean debug = false;
    private static ErrorLogReporter errorLogReporter;

    private String prefix;

    public AgentLogger() {
    }

    public static AgentLogger getLogger(String name) {
        return new AgentLogger(name);
    }

    public static void setDebug(boolean enableDebug) {
        debug = enableDebug;
    }

    public static void setErrorLogReporter(ErrorLogReporter reporter) {
        errorLogReporter = reporter;
    }

    public AgentLogger(String name) {
        if (name == null) {
            this.prefix = "";
        } else {
            this.prefix = name + ": ";
        }
    }

    public void log(String msg) {
        info(msg);
    }

    public void info(String msg) {
        System.out.println(System.currentTimeMillis() + " " + prefix + msg);
    }

    public void debug(String msg) {
        if (AgentLogger.debug) {
            info(msg);
        }
    }

    public void warn(String msg) {
        try {
            System.out.println("[WARNING] " + System.currentTimeMillis() + " " + prefix + msg);

            if (AgentLogger.errorLogReporter != null) {
                AgentLogger.errorLogReporter.report(msg, null);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public void warn(String msg, Throwable ex) {
        try {
            System.out.println(
                "[WARNING] "
                    + System.currentTimeMillis()
                    + " "
                    + prefix
                    + msg
                    + " "
                    + ExceptionUtils.getStackTrace(ex));

            if (AgentLogger.errorLogReporter != null) {
                AgentLogger.errorLogReporter.report(msg, ex);
            }
        } catch (Throwable executionException) {
            executionException.printStackTrace();
        }
    }

    // Handle log specially when shutdown, since we should not depend on other kafka to log these
    // messages
    public void logShutdownMessage(String msg) {
        // Sometime spark log in console output seems not fully collected, thus log to error output as
        // well to make sure
        // we capture this shutdown hook execution. This is to help debug some issue when shutdown hook
        // seems not executed.
        String log = System.currentTimeMillis() + " " + prefix + msg;
        System.out.println(log);
        System.err.println(log);
    }
}
