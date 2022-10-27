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

import java.util.Arrays;
import java.util.Collections;

class ClassAndMethodFilterTest {

    @Test
    void matchClass() {
        ClassAndMethodFilter filter = new ClassAndMethodFilter(null);
        Assertions.assertFalse(filter.matchMethod("class1", "method1"));

        filter = new ClassAndMethodFilter(Collections.singletonList(new ClassAndMethod("class1", "")));
        Assertions.assertTrue(filter.matchClass("class1"));

        filter = new ClassAndMethodFilter(Collections.singletonList(new ClassAndMethod("", "method1")));
        Assertions.assertTrue(filter.matchClass("class1"));

        filter = new ClassAndMethodFilter(Collections.singletonList(new ClassAndMethod("class2", "method1")));
        Assertions.assertFalse(filter.matchClass("class1"));
        Assertions.assertTrue(filter.matchClass("class2"));

        filter =
            new ClassAndMethodFilter(
                Arrays.asList(
                    new ClassAndMethod("class2", "method1"), new ClassAndMethod("class1", "method1")));
        Assertions.assertTrue(filter.matchClass("class1"));
        Assertions.assertTrue(filter.matchClass("class2"));

        filter =
            new ClassAndMethodFilter(
                Arrays.asList(
                    new ClassAndMethod("class2", "method1"),
                    new ClassAndMethod("class1", "method1"),
                    new ClassAndMethod("class3", "*")));
        Assertions.assertTrue(filter.matchClass("class1xx"));
        Assertions.assertTrue(filter.matchClass("class2xx"));
        Assertions.assertTrue(filter.matchClass("class3xx"));
    }

    @Test
    void matchMethod() {
        ClassAndMethodFilter filter = new ClassAndMethodFilter(null);
        Assertions.assertFalse(filter.matchMethod("class1", "method1"));

        filter = new ClassAndMethodFilter(Collections.singletonList(new ClassAndMethod("class1", "")));
        Assertions.assertFalse(filter.matchMethod("class1", "method1"));

        filter = new ClassAndMethodFilter(Collections.singletonList(new ClassAndMethod("", "method1")));
        Assertions.assertTrue(filter.matchMethod("class1", "method1"));

        filter = new ClassAndMethodFilter(Collections.singletonList(new ClassAndMethod("class2", "method1")));
        Assertions.assertFalse(filter.matchMethod("class1", "method1"));

        filter =
            new ClassAndMethodFilter(
                Arrays.asList(
                    new ClassAndMethod("", "method1"), new ClassAndMethod("class1", "method1")));
        Assertions.assertTrue(filter.matchMethod("class1", "method1"));
    }

    @Test
    void matchMethod_wildcard() {
        ClassAndMethodFilter filter =
            new ClassAndMethodFilter(Collections.singletonList(new ClassAndMethod("class1", "")));
        Assertions.assertFalse(filter.matchMethod("class1", "method1"));

        filter =
            new ClassAndMethodFilter(
                Arrays.asList(new ClassAndMethod("class1", ""), new ClassAndMethod("class1", "*")));
        Assertions.assertTrue(filter.matchMethod("class1", "method1"));
    }

    @Test
    void matchMethod_prefix() {
        ClassAndMethodFilter filter =
            new ClassAndMethodFilter(
                Arrays.asList(
                    new ClassAndMethod("package11.class1", "method1"),
                    new ClassAndMethod("package22", "method2")));
        Assertions.assertTrue(filter.matchMethod("package22.class2", "method2"));
        Assertions.assertFalse(filter.matchMethod("package2", "method2"));

        filter =
            new ClassAndMethodFilter(
                Arrays.asList(
                    new ClassAndMethod("package11.class1", "method1"),
                    new ClassAndMethod("package22", "method2"),
                    new ClassAndMethod("package33", "*")));
        Assertions.assertTrue(filter.matchMethod("package22.class2", "method2"));
        Assertions.assertFalse(filter.matchMethod("package2", "method2"));
        Assertions.assertTrue(filter.matchMethod("package33.xx.yy", "method3"));
    }
}
