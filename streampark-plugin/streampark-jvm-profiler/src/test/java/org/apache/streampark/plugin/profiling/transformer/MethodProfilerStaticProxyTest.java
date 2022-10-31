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

package org.apache.streampark.plugin.profiling.transformer;

import org.apache.streampark.plugin.profiling.profiler.Constants;
import org.apache.streampark.plugin.profiling.profiler.MethodArgumentCollector;
import org.apache.streampark.plugin.profiling.util.ClassAndMethodMetricKey;
import org.apache.streampark.plugin.profiling.util.ClassMethodArgumentMetricBuffer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

class MethodProfilerStaticProxyTest {
    private ClassMethodArgumentMetricBuffer buffer;

    @BeforeEach
    void before() {
        buffer = new ClassMethodArgumentMetricBuffer();
        MethodArgumentCollector collector = new MethodArgumentCollector(buffer);
        MethodProfilerStaticProxy.setArgumentCollector(collector);
    }

    @AfterEach
    void after() {
        MethodProfilerStaticProxy.setCollector(null);
    }

    @Test
    void collectMethodArgument_nullValue() {
        MethodProfilerStaticProxy.collectMethodArgument("class1", "method1", 1, null);
        MethodProfilerStaticProxy.collectMethodArgument("class1", "method1", 1, null);

        Map<ClassAndMethodMetricKey, AtomicLong> metrics = buffer.reset();
        Assertions.assertEquals(1, metrics.size());
        ClassAndMethodMetricKey key = metrics.keySet().iterator().next();
        Assertions.assertEquals("class1", key.getClassName());
        Assertions.assertEquals("method1", key.getMethodName());
        Assertions.assertEquals("arg.1.null", key.getMetricName());
        Assertions.assertEquals(2, metrics.get(key).intValue());
    }

    @Test
    void collectMethodArgument_veryLongValue() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Constants.MAX_STRING_LENGTH; i++) {
            sb.append('a');
        }
        sb.append('b');
        String veryLongValue = sb.toString();

        MethodProfilerStaticProxy.collectMethodArgument("class1", "method1", 1, veryLongValue);
        MethodProfilerStaticProxy.collectMethodArgument("class1", "method1", 1, veryLongValue);

        Map<ClassAndMethodMetricKey, AtomicLong> metrics = buffer.reset();
        Assertions.assertEquals(1, metrics.size());
        ClassAndMethodMetricKey key = metrics.keySet().iterator().next();
        Assertions.assertEquals("class1", key.getClassName());
        Assertions.assertEquals("method1", key.getMethodName());
        Assertions.assertEquals(Constants.MAX_STRING_LENGTH, key.getMetricName().length());
        Assertions.assertTrue(key.getMetricName().startsWith("arg.1."));
        Assertions.assertEquals(2, metrics.get(key).intValue());
    }
}
