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

import java.lang.reflect.InvocationTargetException;

class ReflectionUtilsTest {

    @Test
    void executeStaticMethods()
        throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        Object result =
            ReflectionUtils.executeStaticMethods(
                "org.apache.streampark.plugin.profiling.util.ReflectionUtilsTest$ClassB", "getClassA.method1");
        Assertions.assertEquals("hello", result);
    }

    static class ClassA {
        public String method1() {
            return "hello";
        }
    }

    static class ClassB {
        public static ClassA getClassA() {
            return new ClassA();
        }
    }
}
