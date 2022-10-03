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

import java.util.Objects;

public class ClassAndMethodMetricKey {
    private final String className;
    private final String methodName;
    private final String metricName;

    public ClassAndMethodMetricKey(String className, String methodName, String metricName) {
        this.className = className;
        this.methodName = methodName;
        this.metricName = metricName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMetricName() {
        return metricName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClassAndMethodMetricKey that = (ClassAndMethodMetricKey) o;
        return Objects.equals(className, that.className) && Objects.equals(methodName, that.methodName) && Objects.equals(metricName, that.metricName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, metricName);
    }

    @Override
    public String toString() {
        return className + '.' + methodName + " " + metricName;
    }
}
