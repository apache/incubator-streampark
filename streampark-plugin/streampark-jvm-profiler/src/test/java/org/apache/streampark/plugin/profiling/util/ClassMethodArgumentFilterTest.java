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

class ClassMethodArgumentFilterTest {

    @Test
    void matchClass() {
        ClassMethodArgumentFilter filter = new ClassMethodArgumentFilter(null);
        Assertions.assertEquals(0, filter.matchMethod("class1", "method1").size());

        filter = new ClassMethodArgumentFilter(Collections.singletonList(new ClassMethodArgument("class1", "", 1)));
        Assertions.assertTrue(filter.matchClass("class1"));

        filter =
            new ClassMethodArgumentFilter(Collections.singletonList(new ClassMethodArgument("", "method1", 1)));
        Assertions.assertTrue(filter.matchClass("class1"));

        filter =
            new ClassMethodArgumentFilter(
                Collections.singletonList(new ClassMethodArgument("class2", "method1", 1)));
        Assertions.assertFalse(filter.matchClass("class1"));
        Assertions.assertTrue(filter.matchClass("class2"));

        filter =
            new ClassMethodArgumentFilter(
                Arrays.asList(
                    new ClassMethodArgument("class2", "method1", 1),
                    new ClassMethodArgument("class1", "method1", 1)));
        Assertions.assertTrue(filter.matchClass("class1"));
        Assertions.assertTrue(filter.matchClass("class2"));

        filter =
            new ClassMethodArgumentFilter(
                Arrays.asList(
                    new ClassMethodArgument("class2", "method1", 1),
                    new ClassMethodArgument("class1", "method1", 1),
                    new ClassMethodArgument("class3", "*", 1)));
        Assertions.assertTrue(filter.matchClass("class1xx"));
        Assertions.assertTrue(filter.matchClass("class2xx"));
        Assertions.assertTrue(filter.matchClass("class3xx"));
    }

    @Test
    void matchMethod() {
        ClassMethodArgumentFilter filter = new ClassMethodArgumentFilter(null);
        Assertions.assertEquals(0, filter.matchMethod("class1", "method1").size());

        filter = new ClassMethodArgumentFilter(Collections.singletonList(new ClassMethodArgument("class1", "", 1)));
        Assertions.assertEquals(0, filter.matchMethod("class1", "method1").size());

        filter =
            new ClassMethodArgumentFilter(Collections.singletonList(new ClassMethodArgument("", "method1", 10)));
        Assertions.assertEquals(1, filter.matchMethod("class1", "method1").size());
        Assertions.assertEquals(10, filter.matchMethod("class1", "method1").get(0).intValue());

        filter =
            new ClassMethodArgumentFilter(
                Collections.singletonList(new ClassMethodArgument("class2", "method1", 1)));
        Assertions.assertEquals(0, filter.matchMethod("class1", "method1").size());

        filter =
            new ClassMethodArgumentFilter(
                Arrays.asList(
                    new ClassMethodArgument("", "method1", 10),
                    new ClassMethodArgument("class1", "method1", 2)));
        Assertions.assertEquals(2, filter.matchMethod("class1", "method1").size());
        Assertions.assertEquals(10, filter.matchMethod("class1", "method1").get(0).intValue());
        Assertions.assertEquals(2, filter.matchMethod("class1", "method1").get(1).intValue());
    }

    @Test
    void matchMethod_wildcard() {
        ClassMethodArgumentFilter filter =
            new ClassMethodArgumentFilter(Collections.singletonList(new ClassMethodArgument("class1", "", 10)));
        Assertions.assertEquals(0, filter.matchMethod("class1", "method1").size());

        filter =
            new ClassMethodArgumentFilter(
                Arrays.asList(
                    new ClassMethodArgument("class1", "", 10),
                    new ClassMethodArgument("class1", "*", 20)));
        Assertions.assertEquals(1, filter.matchMethod("class1", "method1").size());
        Assertions.assertEquals(20, filter.matchMethod("class1", "method1").get(0).intValue());
    }

    @Test
    void matchMethod_prefix() {
        ClassMethodArgumentFilter filter =
            new ClassMethodArgumentFilter(
                Arrays.asList(
                    new ClassMethodArgument("package11.class1", "method1", 10),
                    new ClassMethodArgument("package22", "method2", 20)));
        Assertions.assertEquals(1, filter.matchMethod("package22.class2", "method2").size());
        Assertions.assertEquals(0, filter.matchMethod("package2", "method2").size());

        filter =
            new ClassMethodArgumentFilter(
                Arrays.asList(
                    new ClassMethodArgument("package11.class1", "method1", 10),
                    new ClassMethodArgument("package22", "method2", 20),
                    new ClassMethodArgument("package33", "*", 30)));
        Assertions.assertEquals(1, filter.matchMethod("package22.class2", "method2").size());
        Assertions.assertEquals(20, filter.matchMethod("package22.class2", "method2").get(0).intValue());
        Assertions.assertEquals(0, filter.matchMethod("package2", "method2").size());
        Assertions.assertEquals(1, filter.matchMethod("package33.xx.yy", "method3").size());
        Assertions.assertEquals(30, filter.matchMethod("package33.xx.yy", "method3").get(0).intValue());
    }
}
