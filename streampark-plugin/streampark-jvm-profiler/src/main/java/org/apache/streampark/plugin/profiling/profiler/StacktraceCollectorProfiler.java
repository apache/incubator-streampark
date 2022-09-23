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

package org.apache.streampark.plugin.profiling.profiler;

import org.apache.streampark.plugin.profiling.Profiler;
import org.apache.streampark.plugin.profiling.Reporter;
import org.apache.streampark.plugin.profiling.util.ClassAndMethod;
import org.apache.streampark.plugin.profiling.util.Stacktrace;
import org.apache.streampark.plugin.profiling.util.StacktraceMetricBuffer;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * This class collects stacktraces by getting thread dump via JMX, and stores the stacktraces into
 * the given buffer.
 *
 */
public class StacktraceCollectorProfiler implements Profiler {
    private long interval;
    private final StacktraceMetricBuffer buffer;
    private String ignoreThreadNamePrefix = "";
    private int maxStringLength = Constants.MAX_STRING_LENGTH;
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    public StacktraceCollectorProfiler(StacktraceMetricBuffer buffer, String ignoreThreadNamePrefix) {
        this(buffer, ignoreThreadNamePrefix, Constants.MAX_STRING_LENGTH);
    }

    public StacktraceCollectorProfiler(
        StacktraceMetricBuffer buffer, String ignoreThreadNamePrefix, int maxStringLength) {
        this.buffer = buffer;
        this.ignoreThreadNamePrefix = ignoreThreadNamePrefix == null ? "" : ignoreThreadNamePrefix;
        this.maxStringLength = maxStringLength;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    @Override
    public long getInterval() {
        return this.interval;
    }

    @Override
    public void setReporter(Reporter reporter) {
    }

    @Override
    public void profile() {
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
        if (threadInfos == null) {
            return;
        }

        for (ThreadInfo threadInfo : threadInfos) {
            String threadName = threadInfo.getThreadName();
            if (threadName == null) {
                threadName = "";
            }

            if (!ignoreThreadNamePrefix.isEmpty() && threadName.startsWith(ignoreThreadNamePrefix)) {
                continue;
            }

            StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();

            Stacktrace stacktrace = new Stacktrace();
            stacktrace.setThreadName(threadName);
            stacktrace.setThreadState(String.valueOf(threadInfo.getThreadState()));

            // Start from bottom of the stacktrace so we could trim top method (most nested method) if the
            // size is too large
            int totalLength = 0;
            List<ClassAndMethod> stack = new ArrayList<>(stackTraceElements.length);
            for (int i = stackTraceElements.length - 1; i >= 0; i--) {
                StackTraceElement stackTraceElement = stackTraceElements[i];
                String className = stackTraceElement.getClassName();
                String methodName = stackTraceElement.getMethodName();
                stack.add(new ClassAndMethod(className, methodName));

                totalLength += className.length() + methodName.length();

                if (totalLength >= maxStringLength) {
                    stack.add(new ClassAndMethod("_stack_", "_trimmed_"));
                    break;
                }
            }

            // Reverse the stack so the top method (most nested method) is the first element of the array
            ClassAndMethod[] classAndMethodArray = new ClassAndMethod[stack.size()];
            for (int i = 0; i < stack.size(); i++) {
                classAndMethodArray[classAndMethodArray.length - 1 - i] = stack.get(i);
            }

            stacktrace.setStack(classAndMethodArray);

            buffer.appendValue(stacktrace);
        }
    }
}
