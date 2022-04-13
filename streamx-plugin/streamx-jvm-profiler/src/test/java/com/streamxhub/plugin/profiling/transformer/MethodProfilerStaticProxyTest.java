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

package com.streamxhub.plugin.profiling.transformer;

import com.streamxhub.streamx.plugin.profiling.profiler.Constants;
import com.streamxhub.streamx.plugin.profiling.profiler.MethodArgumentCollector;
import com.streamxhub.streamx.plugin.profiling.transformer.MethodProfilerStaticProxy;
import com.streamxhub.streamx.plugin.profiling.util.ClassAndMethodMetricKey;
import com.streamxhub.streamx.plugin.profiling.util.ClassMethodArgumentMetricBuffer;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MethodProfilerStaticProxyTest {
    private ClassMethodArgumentMetricBuffer buffer;

    @Before
    public void before() {
        buffer = new ClassMethodArgumentMetricBuffer();
        MethodArgumentCollector collector = new MethodArgumentCollector(buffer);
        MethodProfilerStaticProxy.setArgumentCollector(collector);
    }

    @After
    public void after() {
        MethodProfilerStaticProxy.setCollector(null);
    }

    @Test
    public void collectMethodArgument_nullValue() {
        MethodProfilerStaticProxy.collectMethodArgument("class1", "method1", 1, null);
        MethodProfilerStaticProxy.collectMethodArgument("class1", "method1", 1, null);

        Map<ClassAndMethodMetricKey, AtomicLong> metrics = buffer.reset();
        Assert.assertEquals(1, metrics.size());
        ClassAndMethodMetricKey key = metrics.keySet().iterator().next();
        Assert.assertEquals("class1", key.getClassName());
        Assert.assertEquals("method1", key.getMethodName());
        Assert.assertEquals("arg.1.null", key.getMetricName());
        Assert.assertEquals(2, metrics.get(key).intValue());
    }

    @Test
    public void collectMethodArgument_veryLongValue() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Constants.MAX_STRING_LENGTH; i++) {
            sb.append('a');
        }
        sb.append('b');
        String veryLongValue = sb.toString();

        MethodProfilerStaticProxy.collectMethodArgument("class1", "method1", 1, veryLongValue);
        MethodProfilerStaticProxy.collectMethodArgument("class1", "method1", 1, veryLongValue);

        Map<ClassAndMethodMetricKey, AtomicLong> metrics = buffer.reset();
        Assert.assertEquals(1, metrics.size());
        ClassAndMethodMetricKey key = metrics.keySet().iterator().next();
        Assert.assertEquals("class1", key.getClassName());
        Assert.assertEquals("method1", key.getMethodName());
        Assert.assertEquals(Constants.MAX_STRING_LENGTH, key.getMetricName().length());
        Assert.assertTrue(key.getMetricName().startsWith("arg.1."));
        Assert.assertEquals(2, metrics.get(key).intValue());
    }
}
