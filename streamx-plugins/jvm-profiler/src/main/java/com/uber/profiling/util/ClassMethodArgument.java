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

public class ClassMethodArgument {
    private final String className;
    private final String methodName;
    private final int argumentIndex;

    public ClassMethodArgument(String className, String methodName, int argumentIndex) {
        if (className == null) {
            throw new NullPointerException("className");
        }

        if (methodName == null) {
            throw new NullPointerException("methodName");
        }

        if (argumentIndex < 0) {
            throw new IllegalArgumentException("argumentIndex (must equal or greater than 0: 0 means not collecting argument value, 1 means collecting first argument value)");
        }

        this.className = className;
        this.methodName = methodName;
        this.argumentIndex = argumentIndex;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getArgumentIndex() {
        return argumentIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassMethodArgument that = (ClassMethodArgument) o;

        if (argumentIndex != that.argumentIndex) return false;
        if (className != null ? !className.equals(that.className) : that.className != null) return false;
        return methodName != null ? methodName.equals(that.methodName) : that.methodName == null;
    }

    @Override
    public int hashCode() {
        int result = className != null ? className.hashCode() : 0;
        result = 31 * result + (methodName != null ? methodName.hashCode() : 0);
        result = 31 * result + argumentIndex;
        return result;
    }

    @Override
    public String toString() {
        return "{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", argumentIndex='" + argumentIndex + '\'' +
                '}';
    }
}
