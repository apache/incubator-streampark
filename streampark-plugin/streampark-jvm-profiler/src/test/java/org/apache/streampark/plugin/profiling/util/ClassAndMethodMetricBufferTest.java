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

import java.util.Map;

class ClassAndMethodMetricBufferTest {

    @Test
    void appendValue() {
        ClassAndMethodLongMetricBuffer buffer = new ClassAndMethodLongMetricBuffer();
        buffer.appendValue("class1", "method1", "metric1", 11);
        buffer.appendValue("class1", "method2", "metric1", 22);
        buffer.appendValue("class1", "method2", "metric1", 55);
        buffer.appendValue("class2", "method2", "metric1", 1001);

        Map<ClassAndMethodMetricKey, Histogram> map = buffer.reset();
        Assertions.assertEquals(3, map.size());

        Histogram histogram = map.get(new ClassAndMethodMetricKey("class1", "method1", "metric1"));
        Assertions.assertEquals(1, histogram.getCount());
        Assertions.assertEquals(11, histogram.getSum());
        Assertions.assertEquals(11, histogram.getMin());
        Assertions.assertEquals(11, histogram.getMax());

        histogram = map.get(new ClassAndMethodMetricKey("class1", "method2", "metric1"));
        Assertions.assertEquals(2, histogram.getCount());
        Assertions.assertEquals(77, histogram.getSum());
        Assertions.assertEquals(22, histogram.getMin());
        Assertions.assertEquals(55, histogram.getMax());

        histogram = map.get(new ClassAndMethodMetricKey("class2", "method2", "metric1"));
        Assertions.assertEquals(1, histogram.getCount());
        Assertions.assertEquals(1001, histogram.getSum());
        Assertions.assertEquals(1001, histogram.getMin());
        Assertions.assertEquals(1001, histogram.getMax());

        map = buffer.reset();
        Assertions.assertEquals(0, map.size());

        map = buffer.reset();
        Assertions.assertEquals(0, map.size());
    }

    @Test
    void appendValue_concurrent() throws InterruptedException {
        ClassAndMethodLongMetricBuffer buffer = new ClassAndMethodLongMetricBuffer();

        String[] classNames = new String[] {"class1", "class2", "class1", "class2", "class101"};
        String[] methodNames = new String[] {"method1", "method2", "method1", "method3", "method101"};
        int[] values = new int[] {1, 2, 10, 20, 101};

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
        Assertions.assertEquals(4, result.size());

        Assertions.assertEquals(
            2 * repeatTimes,
            result.get(new ClassAndMethodMetricKey("class1", "method1", "duration")).getCount());
        Assertions.assertEquals(
            11 * repeatTimes,
            result.get(new ClassAndMethodMetricKey("class1", "method1", "duration")).getSum());
        Assertions.assertEquals(
            1, result.get(new ClassAndMethodMetricKey("class1", "method1", "duration")).getMin());
        Assertions.assertEquals(
            10, result.get(new ClassAndMethodMetricKey("class1", "method1", "duration")).getMax());

        Assertions.assertEquals(
            repeatTimes,
            result.get(new ClassAndMethodMetricKey("class2", "method2", "duration")).getCount());
        Assertions.assertEquals(
            2 * repeatTimes,
            result.get(new ClassAndMethodMetricKey("class2", "method2", "duration")).getSum());
        Assertions.assertEquals(
            2, result.get(new ClassAndMethodMetricKey("class2", "method2", "duration")).getMin());
        Assertions.assertEquals(
            2, result.get(new ClassAndMethodMetricKey("class2", "method2", "duration")).getMax());

        Assertions.assertEquals(
            repeatTimes,
            result.get(new ClassAndMethodMetricKey("class2", "method3", "duration")).getCount());
        Assertions.assertEquals(
            20 * repeatTimes,
            result.get(new ClassAndMethodMetricKey("class2", "method3", "duration")).getSum());
        Assertions.assertEquals(
            20, result.get(new ClassAndMethodMetricKey("class2", "method3", "duration")).getMin());
        Assertions.assertEquals(
            20, result.get(new ClassAndMethodMetricKey("class2", "method3", "duration")).getMax());
    }
}
