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

package com.streamxhub.plugin.profiling.profiler;

import com.streamxhub.streamx.plugin.profiling.profiler.StacktraceCollectorProfiler;
import com.streamxhub.streamx.plugin.profiling.util.ClassAndMethod;
import com.streamxhub.streamx.plugin.profiling.util.Stacktrace;
import com.streamxhub.streamx.plugin.profiling.util.StacktraceMetricBuffer;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

public class StacktraceCollectorProfilerTest {
    @Test
    public void profile() throws InterruptedException {
        StacktraceMetricBuffer buffer = new StacktraceMetricBuffer();
        StacktraceCollectorProfiler profiler = new StacktraceCollectorProfiler(buffer, null);

        profiler.setInterval(123);
        Assert.assertEquals(123L, profiler.getInterval());

        // Use a semaphore to make sure the test thread runs before profile method
        final Semaphore semaphore = new Semaphore(1);
        semaphore.acquire();

        Thread thread =
            new Thread(
                () -> {
                    semaphore.release();
                    try {
                        Thread.sleep(1000 * 60);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

        thread.setName("testDummySleepThread");
        thread.start();

        // Wait until the test thread is running
        semaphore.acquire();
        // Sleep to make sure the test thread runs into Thread.sleep method, so we could test its top
        // method in the stack
        Thread.sleep(100);

        profiler.profile();
        profiler.profile();
        profiler.profile();

        Map<Stacktrace, AtomicLong> map = buffer.reset();
        Assert.assertTrue(map.size() > 0);

        int mainThreadCount = 0;

        List<Stacktrace> testThreadStacktrace = new ArrayList<>();
        List<Long> testThreadStacktraceCount = new ArrayList<>();

        int runnableThreadCount = 0;
        int waitingThreadCount = 0;

        for (Map.Entry<Stacktrace, AtomicLong> entry : map.entrySet()) {
            if (entry.getKey().getThreadName().equalsIgnoreCase("main")) {
                mainThreadCount += entry.getValue().intValue();
            }

            if (entry.getKey().getThreadName().equalsIgnoreCase("testDummySleepThread")) {
                testThreadStacktrace.add(entry.getKey());
                testThreadStacktraceCount.add(entry.getValue().longValue());
            }

            if (entry.getKey().getThreadState().equalsIgnoreCase("RUNNABLE")) {
                runnableThreadCount += entry.getValue().intValue();
            }

            if (entry.getKey().getThreadState().equalsIgnoreCase("WAITING")) {
                waitingThreadCount += entry.getValue().intValue();
            }
        }

        Assert.assertTrue(mainThreadCount >= 3);

        Assert.assertEquals(1, testThreadStacktrace.size());
        Assert.assertEquals("testDummySleepThread", testThreadStacktrace.get(0).getThreadName());
        Assert.assertEquals("TIMED_WAITING", testThreadStacktrace.get(0).getThreadState());

        ClassAndMethod[] stack = testThreadStacktrace.get(0).getStack();
        Assert.assertEquals(4, stack.length);
        Assert.assertEquals(new ClassAndMethod("java.lang.Thread", "sleep"), stack[0]);
        Assert.assertEquals(new ClassAndMethod("java.lang.Thread", "run"), stack[stack.length - 1]);

        Assert.assertEquals(1, testThreadStacktraceCount.size());
        Assert.assertEquals(3L, testThreadStacktraceCount.get(0).longValue());

        Assert.assertTrue(runnableThreadCount >= 3);
        Assert.assertTrue(waitingThreadCount >= 3);

        try {
            thread.interrupt();
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void profile_ignoreThread() throws InterruptedException {
        StacktraceMetricBuffer buffer = new StacktraceMetricBuffer();
        StacktraceCollectorProfiler profiler = new StacktraceCollectorProfiler(buffer, "testDummy");

        profiler.setInterval(123);
        Assert.assertEquals(123L, profiler.getInterval());

        // Use a semaphore to make sure the test thread runs before profile method
        final Semaphore semaphore = new Semaphore(1);
        semaphore.acquire();

        Thread thread =
            new Thread(
                () -> {
                    semaphore.release();
                    try {
                        Thread.sleep(1000 * 60);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

        thread.setName("testDummySleepThread");
        thread.start();

        // Wait until the test thread is running
        semaphore.acquire();
        // Sleep to make sure the test thread runs into Thread.sleep method, so we could test its top
        // method in the stack
        Thread.sleep(100);

        profiler.profile();
        profiler.profile();
        profiler.profile();

        Map<Stacktrace, AtomicLong> map = buffer.reset();
        Assert.assertTrue(map.size() > 0);

        int mainThreadCount = 0;

        List<Stacktrace> testThreadStacktrace = new ArrayList<>();
        List<Long> testThreadStacktraceCount = new ArrayList<>();

        int runnableThreadCount = 0;
        int waitingThreadCount = 0;

        for (Map.Entry<Stacktrace, AtomicLong> entry : map.entrySet()) {
            if (entry.getKey().getThreadName().equalsIgnoreCase("main")) {
                mainThreadCount += entry.getValue().intValue();
            }

            if (entry.getKey().getThreadName().equalsIgnoreCase("testDummySleepThread")) {
                testThreadStacktrace.add(entry.getKey());
                testThreadStacktraceCount.add(entry.getValue().longValue());
            }

            if (entry.getKey().getThreadState().equalsIgnoreCase("RUNNABLE")) {
                runnableThreadCount += entry.getValue().intValue();
            }

            if (entry.getKey().getThreadState().equalsIgnoreCase("WAITING")) {
                waitingThreadCount += entry.getValue().intValue();
            }
        }

        Assert.assertTrue(mainThreadCount >= 3);

        Assert.assertEquals(0, testThreadStacktrace.size());

        Assert.assertTrue(runnableThreadCount >= 3);
        Assert.assertTrue(waitingThreadCount >= 3);

        try {
            thread.interrupt();
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void profile_largeStack() throws InterruptedException {
        StacktraceMetricBuffer buffer = new StacktraceMetricBuffer();
        StacktraceCollectorProfiler profiler = new StacktraceCollectorProfiler(buffer, null, 20000);

        profiler.setInterval(123);
        Assert.assertEquals(123L, profiler.getInterval());

        // Use a semaphore to make sure the test thread runs before profile method
        final Semaphore semaphore = new Semaphore(1);
        semaphore.acquire();

        Thread thread =
            new Thread(
                () -> {
                    simulateLargeStack(0, 1000, semaphore);
                });

        thread.setName("testDummySleepThread");
        thread.start();

        // Wait until the test thread is running
        semaphore.acquire();
        // Sleep to make sure the test thread runs into Thread.sleep method, so we could test its top
        // method in the stack
        Thread.sleep(100);

        profiler.profile();

        Map<Stacktrace, AtomicLong> map = buffer.reset();
        Assert.assertTrue(map.size() > 0);

        List<Stacktrace> testThreadStacktrace = new ArrayList<>();
        List<Long> testThreadStacktraceCount = new ArrayList<>();

        for (Map.Entry<Stacktrace, AtomicLong> entry : map.entrySet()) {
            if (entry.getKey().getThreadName().equalsIgnoreCase("testDummySleepThread")) {
                testThreadStacktrace.add(entry.getKey());
                testThreadStacktraceCount.add(entry.getValue().longValue());
            }
        }

        Assert.assertEquals(1, testThreadStacktrace.size());

        ClassAndMethod[] stack = testThreadStacktrace.get(0).getStack();
        Assert.assertTrue(stack.length > 1);
        Assert.assertEquals(new ClassAndMethod("_stack_", "_trimmed_"), stack[0]);

        Assert.assertEquals(1, testThreadStacktraceCount.size());
        Assert.assertEquals(1L, testThreadStacktraceCount.get(0).longValue());

        try {
            thread.interrupt();
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void simulateLargeStack(int currentIndex, int maxIndex, Semaphore semaphore) {
        if (currentIndex >= maxIndex) {
            semaphore.release();
            try {
                Thread.sleep(1000 * 60);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }

        simulateLargeStack(currentIndex + 1, maxIndex, semaphore);
    }
}
