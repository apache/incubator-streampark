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

/**
 * Java logging support using {@link StreamParkLoggerFactory}.
 */
public abstract class LoggerSupport {

    private static final String PREFIX = "[StreamPark]";

    private org.apache.streampark.shaded.org.slf4j.Logger slf4jLogger;

    protected String logName() {
        String name = getClass().getName();
        if (name.endsWith("$")) {
            return name.substring(0, name.length() - 1);
        }
        return name;
    }

    protected org.apache.streampark.shaded.org.slf4j.Logger logger() {
        if (slf4jLogger == null) {
            slf4jLogger =
                StreamParkLoggerFactory.loggerFactory().getLogger(logName());
        }
        return slf4jLogger;
    }

    protected void logInfo(String msg) {
        if (logger().isInfoEnabled()) {
            logger().info(PREFIX + " " + msg);
        }
    }

    protected void logInfo(String msg, Throwable throwable) {
        if (logger().isInfoEnabled()) {
            logger().info(PREFIX + " " + msg, throwable);
        }
    }

    protected void logDebug(String msg) {
        if (logger().isDebugEnabled()) {
            logger().debug(PREFIX + " " + msg);
        }
    }

    protected void logDebug(String msg, Throwable throwable) {
        if (logger().isDebugEnabled()) {
            logger().debug(PREFIX + " " + msg, throwable);
        }
    }

    public void logTrace(String msg) {
        if (logger().isTraceEnabled()) {
            logger().trace(PREFIX + " " + msg);
        }
    }

    protected void logTrace(String msg, Throwable throwable) {
        if (logger().isTraceEnabled()) {
            logger().trace(PREFIX + " " + msg, throwable);
        }
    }

    public void logWarn(String msg) {
        if (logger().isWarnEnabled()) {
            logger().warn(PREFIX + " " + msg);
        }
    }

    protected void logWarn(String msg, Throwable throwable) {
        if (logger().isWarnEnabled()) {
            logger().warn(PREFIX + " " + msg, throwable);
        }
    }

    protected void logError(String msg) {
        if (logger().isErrorEnabled()) {
            logger().error(PREFIX + " " + msg);
        }
    }

    protected void logError(String msg, Throwable throwable) {
        if (logger().isErrorEnabled()) {
            logger().error(PREFIX + " " + msg, throwable);
        }
    }

    protected boolean isTraceEnabled() {
        return logger().isTraceEnabled();
    }
}
