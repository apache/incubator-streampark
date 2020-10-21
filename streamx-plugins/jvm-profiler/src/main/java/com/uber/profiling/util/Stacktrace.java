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

import java.util.Arrays;

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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Stacktrace that = (Stacktrace) o;

        if (threadName != null ? !threadName.equals(that.threadName) : that.threadName != null) return false;
        if (threadState != null ? !threadState.equals(that.threadState) : that.threadState != null) return false;
        
        return Arrays.equals(stack, that.stack);
    }

    @Override
    public int hashCode() {
        int result = threadName != null ? threadName.hashCode() : 0;
        result = 31 * result + (threadState != null ? threadState.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(stack);
        return result;
    }
}
