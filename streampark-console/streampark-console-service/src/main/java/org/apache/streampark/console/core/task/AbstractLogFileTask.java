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

package org.apache.streampark.console.core.task;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public abstract class AbstractLogFileTask implements Runnable {

    private final Path logPath;

    /**
     * Whether old log files need to be overwritten? If true, the log file will be clean before write.
     * If false, the log will be appended to new log file.
     */
    private final boolean isOverwrite;

    protected Logger fileLogger;

    private FileAppender<ILoggingEvent> fileAppender;

    private PatternLayoutEncoder ple;

    public AbstractLogFileTask(String logPath, boolean isOverwrite) {
        this.logPath = Paths.get(logPath);
        this.isOverwrite = isOverwrite;
    }

    @Override
    public void run() {
        try {
            Path logDir = logPath.getParent();
            if (!Files.isDirectory(logDir)) {
                log.info("Created log dir {}", logDir);
                Files.createDirectories(logDir);
            }
            if (isOverwrite) {
                Files.deleteIfExists(logPath);
            }
            this.fileLogger = createFileLogger();
            doRun();
        } catch (Throwable t) {
            log.warn("Failed to run task.", t);
            if (fileLogger != null) {
                fileLogger.error("Failed to run task.", t);
            }
            processException(t);
        } finally {
            doFinally();
            if (ple != null) {
                ple.stop();
            }
            if (fileAppender != null) {
                fileAppender.stop();
            }
            if (fileLogger != null) {
                fileLogger.detachAppender(fileAppender);
            }
        }
    }

    private Logger createFileLogger() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        ple = new PatternLayoutEncoder();
        ple.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS,Asia/Singapore} %-5p - %m%n");
        ple.setContext(lc);
        ple.start();

        this.fileAppender = new FileAppender<>();
        fileAppender.setFile(logPath.toString());
        fileAppender.setEncoder(ple);
        fileAppender.setContext(lc);
        fileAppender.start();

        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Thread.currentThread().getName());
        logger.addAppender(fileAppender);
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);
        return logger;
    }

    protected abstract void doRun() throws Throwable;

    protected abstract void processException(Throwable t);

    protected abstract void doFinally();
}
