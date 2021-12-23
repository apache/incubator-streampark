/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.plugin.profiling;

import com.streamxhub.streamx.plugin.profiling.util.AgentLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author benjobs
 */
public class ShutdownHookRunner implements Runnable {
    private static final AgentLogger LOGGER = AgentLogger.getLogger(ShutdownHookRunner.class.getName());

    private List<Profiler> profilers;
    private List<Reporter> reporters;
    private List<AutoCloseable> closeables;

    public ShutdownHookRunner(
        Collection<Profiler> profilers,
        Collection<Reporter> reporters,
        Collection<AutoCloseable> objectsToCloseOnShutdown) {
        this.profilers = profilers == null ? new ArrayList<>() : new ArrayList<>(profilers);
        this.reporters = reporters == null ? new ArrayList<>() : new ArrayList<>(reporters);
        this.closeables =
            objectsToCloseOnShutdown == null ? new ArrayList<>() : new ArrayList<>(objectsToCloseOnShutdown);
    }

    @Override
    public void run() {
        logShutdownMessage("Running java agent shutdown");

        for (Profiler profiler : profilers) {
            try {
                logShutdownMessage("Running periodic profiler (last run): " + profiler);
                profiler.profile();
                logShutdownMessage("Ran periodic profiler (last run): " + profiler);
            } catch (Throwable ex) {
                LOGGER.warn("Failed to run periodic profiler (last run): " + profiler, ex);
            }
        }

        for (Reporter r : reporters) {
            try {
                logShutdownMessage("Closing reporter " + r);
                r.close();
                logShutdownMessage("Closed reporter " + r);
            } catch (Throwable ex) {
                LOGGER.warn(
                    "Failed to close reporter " + r + ", " + new Date() + ", " + System.currentTimeMillis(),
                    ex);
            }
        }

        for (AutoCloseable closeable : closeables) {
            // Do not use logger.warn here because the logger may depend on error log reporter which will
            // be already closed here.
            // So we use logShutdownMessage (System.out.println) to print out logs.
            try {
                logShutdownMessage("Closing object " + closeable);
                closeable.close();
                logShutdownMessage("Closed object " + closeable);
            } catch (Throwable ex) {
                logShutdownMessage("Failed to close object " + closeable);
                ex.printStackTrace();
            }
        }
    }

    private void logShutdownMessage(String msg) {
        // Sometime spark log in console output seems not fully collected, thus log to error output as
        // well to make sure
        // we capture this shutdown hook execution. This is to help debug some issue when shutdown hook
        // seems not executed.
        String log = System.currentTimeMillis() + " " + msg;
        System.out.println(log);
        System.err.println(log);
    }
}
