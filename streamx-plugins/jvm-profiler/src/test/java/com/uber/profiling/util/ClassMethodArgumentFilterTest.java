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

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ClassMethodArgumentFilterTest {
    @Test
    public void matchClass() {
        ClassMethodArgumentFilter filter = new ClassMethodArgumentFilter(null);
        Assert.assertEquals(0, filter.matchMethod("class1", "method1").size());

        filter = new ClassMethodArgumentFilter(Arrays.asList(new ClassMethodArgument("class1", "", 1)));
        Assert.assertTrue(filter.matchClass("class1"));

        filter = new ClassMethodArgumentFilter(Arrays.asList(new ClassMethodArgument("", "method1", 1)));
        Assert.assertTrue(filter.matchClass("class1"));

        filter = new ClassMethodArgumentFilter(Arrays.asList(new ClassMethodArgument("class2", "method1", 1)));
        Assert.assertFalse(filter.matchClass("class1"));
        Assert.assertTrue(filter.matchClass("class2"));

        filter = new ClassMethodArgumentFilter(Arrays.asList(new ClassMethodArgument("class2", "method1", 1),
                new ClassMethodArgument("class1", "method1", 1)));
        Assert.assertTrue(filter.matchClass("class1"));
        Assert.assertTrue(filter.matchClass("class2"));

        filter = new ClassMethodArgumentFilter(Arrays.asList(new ClassMethodArgument("class2", "method1", 1),
                new ClassMethodArgument("class1", "method1", 1),
                new ClassMethodArgument("class3", "*", 1)));
        Assert.assertTrue(filter.matchClass("class1xx"));
        Assert.assertTrue(filter.matchClass("class2xx"));
        Assert.assertTrue(filter.matchClass("class3xx"));
    }

    @Test
    public void matchMethod() {
        ClassMethodArgumentFilter filter = new ClassMethodArgumentFilter(null);
        Assert.assertEquals(0, filter.matchMethod("class1", "method1").size());

        filter = new ClassMethodArgumentFilter(Arrays.asList(new ClassMethodArgument("class1", "", 1)));
        Assert.assertEquals(0, filter.matchMethod("class1", "method1").size());

        filter = new ClassMethodArgumentFilter(Arrays.asList(new ClassMethodArgument("", "method1", 10)));
        Assert.assertEquals(1, filter.matchMethod("class1", "method1").size());
        Assert.assertEquals(10, filter.matchMethod("class1", "method1").get(0).intValue());

        filter = new ClassMethodArgumentFilter(Arrays.asList(new ClassMethodArgument("class2", "method1", 1)));
        Assert.assertEquals(0, filter.matchMethod("class1", "method1").size());

        filter = new ClassMethodArgumentFilter(Arrays.asList(new ClassMethodArgument("", "method1", 10),
                new ClassMethodArgument("class1", "method1", 2)));
        Assert.assertEquals(2, filter.matchMethod("class1", "method1").size());
        Assert.assertEquals(10, filter.matchMethod("class1", "method1").get(0).intValue());
        Assert.assertEquals(2, filter.matchMethod("class1", "method1").get(1).intValue());
    }

    @Test
    public void matchMethod_wildcard() {
        ClassMethodArgumentFilter filter = new ClassMethodArgumentFilter(
                Arrays.asList(new ClassMethodArgument("class1", "", 10)));
        Assert.assertEquals(0, filter.matchMethod("class1", "method1").size());

        filter = new ClassMethodArgumentFilter(
                Arrays.asList(new ClassMethodArgument("class1", "", 10),
                        new ClassMethodArgument("class1", "*", 20)));
        Assert.assertEquals(1, filter.matchMethod("class1", "method1").size());
        Assert.assertEquals(20, filter.matchMethod("class1", "method1").get(0).intValue());
    }

    @Test
    public void matchMethod_prefix() {
        ClassMethodArgumentFilter filter = new ClassMethodArgumentFilter(
                Arrays.asList(new ClassMethodArgument("package11.class1", "method1", 10),
                        new ClassMethodArgument("package22", "method2", 20)));
        Assert.assertEquals(1, filter.matchMethod("package22.class2", "method2").size());
        Assert.assertEquals(0, filter.matchMethod("package2", "method2").size());

        filter = new ClassMethodArgumentFilter(
                Arrays.asList(new ClassMethodArgument("package11.class1", "method1", 10),
                        new ClassMethodArgument("package22", "method2", 20),
                        new ClassMethodArgument("package33", "*", 30)));
        Assert.assertEquals(1, filter.matchMethod("package22.class2", "method2").size());
        Assert.assertEquals(20, filter.matchMethod("package22.class2", "method2").get(0).intValue());
        Assert.assertEquals(0, filter.matchMethod("package2", "method2").size());
        Assert.assertEquals(1, filter.matchMethod("package33.xx.yy", "method3").size());
        Assert.assertEquals(30, filter.matchMethod("package33.xx.yy", "method3").get(0).intValue());
    }
}
