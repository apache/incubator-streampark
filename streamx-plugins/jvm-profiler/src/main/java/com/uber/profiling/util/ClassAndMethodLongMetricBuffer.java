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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClassAndMethodLongMetricBuffer is a buffer to store metrics. It is thread safe for appendValue.
 * The reset method will create a new empty internal buffer and return the old one.
 */
public class ClassAndMethodLongMetricBuffer {
    private volatile ConcurrentHashMap<ClassAndMethodMetricKey, Histogram> metrics = new ConcurrentHashMap<>();

    public void appendValue(String className, String methodName, String metricName, long value) {
        ClassAndMethodMetricKey methodMetricKey = new ClassAndMethodMetricKey(className, methodName, metricName);
        Histogram histogram = metrics.computeIfAbsent(methodMetricKey, key -> new Histogram());
        histogram.appendValue(value);
    }

    public Map<ClassAndMethodMetricKey, Histogram> reset() {
        ConcurrentHashMap<ClassAndMethodMetricKey, Histogram> oldCopy = metrics;
        metrics = new ConcurrentHashMap<>();
        return oldCopy;
    }
}
