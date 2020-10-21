/*
 * Copyright (c) 2018 Uber Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uber.profiling.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ClassMethodArgmentMetricBufferTest {
    @Test
    public void appendValue() {
        ClassMethodArgumentMetricBuffer buffer = new ClassMethodArgumentMetricBuffer();
        buffer.appendValue("class1", "method1", "arg1");
        buffer.appendValue("class1", "method2", "arg1");
        buffer.appendValue("class1", "method2", "arg1");
        buffer.appendValue("class2", "method2", "arg1");

        Map<ClassAndMethodMetricKey, AtomicLong> map = buffer.reset();
        Assert.assertEquals(3, map.size());

        AtomicLong count = map.get(new ClassAndMethodMetricKey("class1", "method1", "arg1"));
        Assert.assertEquals(1, count.get());

        count = map.get(new ClassAndMethodMetricKey("class1", "method2", "arg1"));
        Assert.assertEquals(2, count.get());

        count = map.get(new ClassAndMethodMetricKey("class2", "method2", "arg1"));
        Assert.assertEquals(1, count.get());

        map = buffer.reset();
        Assert.assertEquals(0, map.size());

        map = buffer.reset();
        Assert.assertEquals(0, map.size());
    }

    @Test
    public void appendValue_concurrent() throws InterruptedException {
        ClassMethodArgumentMetricBuffer buffer = new ClassMethodArgumentMetricBuffer();

        String[] classNames = new String[]{"class1", "class2", "class1", "class2", "class101"};
        String[] methodNames = new String[]{"method1", "method2", "method1", "method3", "method101"};

        Thread[] threads = new Thread[classNames.length];

        int repeatTimes = 1000000;

        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            Thread thread = new Thread(() -> {
                for (int repeat = 0; repeat < repeatTimes; repeat++) {
                    buffer.appendValue(classNames[index], methodNames[index], "arg1");
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

        Map<ClassAndMethodMetricKey, AtomicLong> result = buffer.reset();
        Assert.assertEquals(4, result.size());

        Assert.assertEquals(2 * repeatTimes,
                result.get(new ClassAndMethodMetricKey("class1", "method1", "arg1")).get());

        Assert.assertEquals(repeatTimes,
                result.get(new ClassAndMethodMetricKey("class2", "method2", "arg1")).get());

        Assert.assertEquals(repeatTimes,
                result.get(new ClassAndMethodMetricKey("class2", "method3", "arg1")).get());
    }
}
