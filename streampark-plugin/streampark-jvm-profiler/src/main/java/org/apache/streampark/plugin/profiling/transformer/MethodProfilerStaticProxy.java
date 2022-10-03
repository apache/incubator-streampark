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

import org.apache.streampark.plugin.profiling.profiler.MethodArgumentCollector;
import org.apache.streampark.plugin.profiling.profiler.MethodDurationCollector;

public class MethodProfilerStaticProxy {
    private static MethodDurationCollector collectorSingleton;
    private static MethodArgumentCollector argumentCollectorSingleton;

    private MethodProfilerStaticProxy() {
    }

    public static void setCollector(MethodDurationCollector collector) {
        collectorSingleton = collector;
    }

    public static void setArgumentCollector(MethodArgumentCollector collector) {
        argumentCollectorSingleton = collector;
    }

    public static void collectMethodDuration(String className, String methodName, long metricValue) {
        if (collectorSingleton == null) {
            return;
        }

        try {
            collectorSingleton.collectLongMetric(className, methodName, "duration", metricValue);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public static void collectMethodArgument(
        String className, String methodName, int argIndex, Object argValue) {
        if (argumentCollectorSingleton == null) {
            return;
        }

        try {
            String argument = "arg." + argIndex + "." + argValue;
            argumentCollectorSingleton.collectMetric(className, methodName, argument);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
