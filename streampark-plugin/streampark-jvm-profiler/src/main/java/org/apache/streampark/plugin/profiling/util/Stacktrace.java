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

import java.util.Arrays;
import java.util.Objects;

public class Stacktrace {
    private String threadName;
    private String threadState;
    private ClassAndMethod[] stack = new ClassAndMethod[0];

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getThreadState() {
        return threadState;
    }

    public void setThreadState(String threadState) {
        this.threadState = threadState;
    }

    public ClassAndMethod[] getStack() {
        return stack;
    }

    public void setStack(ClassAndMethod[] stack) {
        if (stack == null) {
            this.stack = new ClassAndMethod[0];
        } else {
            this.stack = stack;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Stacktrace that = (Stacktrace) o;
        return Objects.equals(threadName, that.threadName) && Objects.equals(threadState, that.threadState) && Arrays.equals(stack, that.stack);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(threadName, threadState);
        result = 31 * result + Arrays.hashCode(stack);
        return result;
    }
}
