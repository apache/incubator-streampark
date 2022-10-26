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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

class StacktraceMetricBufferTest {

    @Test
    void appendValue() {
        StacktraceMetricBuffer buffer = new StacktraceMetricBuffer();

        Set<Stacktrace> distinctStacktraces = new HashSet<>();

        {
            Stacktrace stacktrace = new Stacktrace();

            buffer.appendValue(stacktrace);

            distinctStacktraces.add(stacktrace);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName(null);
            stacktrace.setThreadState(null);
            stacktrace.setStack(null);

            buffer.appendValue(stacktrace);
            buffer.appendValue(stacktrace);

            distinctStacktraces.add(stacktrace);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName("thread1");
            stacktrace.setThreadState("RUNNING");
            stacktrace.setStack(new ClassAndMethod[] {new ClassAndMethod("class1", "method1")});

            buffer.appendValue(stacktrace);

            distinctStacktraces.add(stacktrace);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName("thread1");
            stacktrace.setThreadState("RUNNING");
            stacktrace.setStack(new ClassAndMethod[] {new ClassAndMethod("class1", "method1")});

            buffer.appendValue(stacktrace);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName("thread1");
            stacktrace.setThreadState("WAITING");
            stacktrace.setStack(new ClassAndMethod[] {new ClassAndMethod("class1", "method1")});

            buffer.appendValue(stacktrace);

            distinctStacktraces.add(stacktrace);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName("thread2");
            stacktrace.setThreadState("RUNNING");
            stacktrace.setStack(new ClassAndMethod[] {new ClassAndMethod("class1", "method1")});

            buffer.appendValue(stacktrace);

            distinctStacktraces.add(stacktrace);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName("thread2");
            stacktrace.setThreadState("RUNNING");
            stacktrace.setStack(new ClassAndMethod[] {new ClassAndMethod("class1", "method1")});

            buffer.appendValue(stacktrace);

            distinctStacktraces.add(stacktrace);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName("thread2");
            stacktrace.setThreadState("RUNNING");
            stacktrace.setStack(new ClassAndMethod[] {new ClassAndMethod("class1", "method2")});

            buffer.appendValue(stacktrace);

            distinctStacktraces.add(stacktrace);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName("thread3");
            stacktrace.setThreadState("WAIRTING");
            stacktrace.setStack(new ClassAndMethod[] {new ClassAndMethod("class2", "method2")});

            buffer.appendValue(stacktrace);

            distinctStacktraces.add(stacktrace);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName("thread3");
            stacktrace.setThreadState("RUNNING");
            stacktrace.setStack(
                new ClassAndMethod[] {
                    new ClassAndMethod("class11", "method11"), new ClassAndMethod("class11", "method12")
                });

            buffer.appendValue(stacktrace);

            distinctStacktraces.add(stacktrace);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName("thread3");
            stacktrace.setThreadState("RUNNING");
            stacktrace.setStack(
                new ClassAndMethod[] {
                    new ClassAndMethod("class11", "method11"), new ClassAndMethod("class11", "method12")
                });

            buffer.appendValue(stacktrace);

            distinctStacktraces.add(stacktrace);
        }

        long lastResetMillis = buffer.getLastResetMillis();
        Assertions.assertTrue(System.currentTimeMillis() - lastResetMillis >= 0);
        Assertions.assertTrue(System.currentTimeMillis() - lastResetMillis <= 1000);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Map<Stacktrace, AtomicLong> map = buffer.reset();

        long lastResetMillis2 = buffer.getLastResetMillis();
        Assertions.assertTrue(lastResetMillis2 - lastResetMillis >= 10);
        Assertions.assertTrue(lastResetMillis2 - lastResetMillis <= 1000);

        Assertions.assertEquals(7, map.size());
        Assertions.assertEquals(7, distinctStacktraces.size());

        {
            Stacktrace stacktrace = new Stacktrace();

            long count = map.get(stacktrace).longValue();
            Assertions.assertEquals(3, count);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName(null);
            stacktrace.setThreadState(null);
            stacktrace.setStack(null);

            long count = map.get(stacktrace).longValue();
            Assertions.assertEquals(3, count);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName("thread1");
            stacktrace.setThreadState("RUNNING");
            stacktrace.setStack(new ClassAndMethod[] {new ClassAndMethod("class1", "method1")});

            long count = map.get(stacktrace).longValue();
            Assertions.assertEquals(2, count);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName("thread1");
            stacktrace.setThreadState("WAITING");
            stacktrace.setStack(new ClassAndMethod[] {new ClassAndMethod("class1", "method1")});

            long count = map.get(stacktrace).longValue();
            Assertions.assertEquals(1, count);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName("thread2");
            stacktrace.setThreadState("RUNNING");
            stacktrace.setStack(new ClassAndMethod[] {new ClassAndMethod("class1", "method1")});

            long count = map.get(stacktrace).longValue();
            Assertions.assertEquals(2, count);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName("thread2");
            stacktrace.setThreadState("RUNNING");
            stacktrace.setStack(new ClassAndMethod[] {new ClassAndMethod("class1", "method2")});

            long count = map.get(stacktrace).longValue();
            Assertions.assertEquals(1, count);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName("thread3");
            stacktrace.setThreadState("WAIRTING");
            stacktrace.setStack(new ClassAndMethod[] {new ClassAndMethod("class2", "method2")});

            long count = map.get(stacktrace).longValue();
            Assertions.assertEquals(1, count);
        }
        {
            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName("thread3");
            stacktrace.setThreadState("RUNNING");
            stacktrace.setStack(
                new ClassAndMethod[] {
                    new ClassAndMethod("class11", "method11"), new ClassAndMethod("class11", "method12")
                });

            long count = map.get(stacktrace).longValue();
            Assertions.assertEquals(2, count);
        }

        for (Stacktrace stacktrace : distinctStacktraces) {
            map.remove(stacktrace);
        }
        Assertions.assertEquals(0, map.size());

        map = buffer.reset();
        Assertions.assertEquals(0, map.size());

        map = buffer.reset();
        Assertions.assertEquals(0, map.size());
    }
}
