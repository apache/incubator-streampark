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

package com.streamxhub.plugin.profiling.util;

import com.streamxhub.streamx.plugin.profiling.util.ClassAndMethodLongMetricBuffer;
import com.streamxhub.streamx.plugin.profiling.util.ClassAndMethodMetricKey;
import com.streamxhub.streamx.plugin.profiling.util.Histogram;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class ClassAndMethodMetricBufferTest {
    @Test
    public void appendValue() {
        ClassAndMethodLongMetricBuffer buffer = new ClassAndMethodLongMetricBuffer();
        buffer.appendValue("class1", "method1", "metric1", 11);
        buffer.appendValue("class1", "method2", "metric1", 22);
        buffer.appendValue("class1", "method2", "metric1", 55);
        buffer.appendValue("class2", "method2", "metric1", 1001);

        Map<ClassAndMethodMetricKey, Histogram> map = buffer.reset();
        Assert.assertEquals(3, map.size());

        Histogram histogram = map.get(new ClassAndMethodMetricKey("class1", "method1", "metric1"));
        Assert.assertEquals(1, histogram.getCount());
        Assert.assertEquals(11, histogram.getSum());
        Assert.assertEquals(11, histogram.getMin());
        Assert.assertEquals(11, histogram.getMax());

        histogram = map.get(new ClassAndMethodMetricKey("class1", "method2", "metric1"));
        Assert.assertEquals(2, histogram.getCount());
        Assert.assertEquals(77, histogram.getSum());
        Assert.assertEquals(22, histogram.getMin());
        Assert.assertEquals(55, histogram.getMax());

        histogram = map.get(new ClassAndMethodMetricKey("class2", "method2", "metric1"));
        Assert.assertEquals(1, histogram.getCount());
        Assert.assertEquals(1001, histogram.getSum());
        Assert.assertEquals(1001, histogram.getMin());
        Assert.assertEquals(1001, histogram.getMax());

        map = buffer.reset();
        Assert.assertEquals(0, map.size());

        map = buffer.reset();
        Assert.assertEquals(0, map.size());
    }

    @Test
    public void appendValue_concurrent() throws InterruptedException {
        ClassAndMethodLongMetricBuffer buffer = new ClassAndMethodLongMetricBuffer();

        String[] classNames = new String[]{"class1", "class2", "class1", "class2", "class101"};
        String[] methodNames = new String[]{"method1", "method2", "method1", "method3", "method101"};
        int[] values = new int[]{1, 2, 10, 20, 101};

        Thread[] threads = new Thread[classNames.length];

        int repeatTimes = 1000000;

        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            Thread thread =
                new Thread(
                    () -> {
                        for (int repeat = 0; repeat < repeatTimes; repeat++) {
                            buffer.appendValue(
                                classNames[index], methodNames[index], "duration", values[index]);
                        }
                    });
            threads[i] = thread;
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }

        Map<ClassAndMethodMetricKey, Histogram> result = buffer.reset();
        Assert.assertEquals(4, result.size());

        Assert.assertEquals(
            2 * repeatTimes,
            result.get(new ClassAndMethodMetricKey("class1", "method1", "duration")).getCount());
        Assert.assertEquals(
            11 * repeatTimes,
            result.get(new ClassAndMethodMetricKey("class1", "method1", "duration")).getSum());
        Assert.assertEquals(
            1, result.get(new ClassAndMethodMetricKey("class1", "method1", "duration")).getMin());
        Assert.assertEquals(
            10, result.get(new ClassAndMethodMetricKey("class1", "method1", "duration")).getMax());

        Assert.assertEquals(
            repeatTimes,
            result.get(new ClassAndMethodMetricKey("class2", "method2", "duration")).getCount());
        Assert.assertEquals(
            2 * repeatTimes,
            result.get(new ClassAndMethodMetricKey("class2", "method2", "duration")).getSum());
        Assert.assertEquals(
            2, result.get(new ClassAndMethodMetricKey("class2", "method2", "duration")).getMin());
        Assert.assertEquals(
            2, result.get(new ClassAndMethodMetricKey("class2", "method2", "duration")).getMax());

        Assert.assertEquals(
            repeatTimes,
            result.get(new ClassAndMethodMetricKey("class2", "method3", "duration")).getCount());
        Assert.assertEquals(
            20 * repeatTimes,
            result.get(new ClassAndMethodMetricKey("class2", "method3", "duration")).getSum());
        Assert.assertEquals(
            20, result.get(new ClassAndMethodMetricKey("class2", "method3", "duration")).getMin());
        Assert.assertEquals(
            20, result.get(new ClassAndMethodMetricKey("class2", "method3", "duration")).getMax());
    }
}
