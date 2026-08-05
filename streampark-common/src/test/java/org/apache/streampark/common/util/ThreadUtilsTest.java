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

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadUtilsTest {

    @Test
    void threadFactoryShouldCreateDaemonThreadWhenRequested() {
        ThreadFactory factory = ThreadUtils.threadFactory("testThread", true);
        Thread thread = factory.newThread(() -> {
        });
        assertTrue(thread.isDaemon());
        assertTrue(thread.getName().startsWith("testThread"));
    }

    @Test
    void threadFactoryShouldCreateNonDaemonThreadWhenRequested() {
        ThreadFactory factory = ThreadUtils.threadFactory("testThread", false);
        Thread thread = factory.newThread(() -> {
        });
        assertFalse(thread.isDaemon());
        assertTrue(thread.getName().startsWith("testThread"));
    }

    @Test
    void shutdownExecutorServiceShouldGracefullyShutDown() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread.sleep(500);
        ThreadUtils.shutdownExecutorService(executorService);
        assertTrue(executorService.isShutdown());
        assertTrue(executorService.isTerminated());
    }

    @Test
    void shutdownExecutorServiceWithTimeoutShouldForceShutdown() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread.sleep(1000);
        ThreadUtils.shutdownExecutorService(executorService, 1);
        assertTrue(executorService.isShutdown());
        assertTrue(executorService.isTerminated());
    }
}
