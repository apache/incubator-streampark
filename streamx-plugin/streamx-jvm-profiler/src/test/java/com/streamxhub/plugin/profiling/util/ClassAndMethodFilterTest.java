/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.streamxhub.plugin.profiling.util;

import com.streamxhub.streamx.plugin.profiling.util.ClassAndMethod;
import com.streamxhub.streamx.plugin.profiling.util.ClassAndMethodFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ClassAndMethodFilterTest {
  @Test
  public void matchClass() {
    ClassAndMethodFilter filter = new ClassAndMethodFilter(null);
    Assert.assertFalse(filter.matchMethod("class1", "method1"));

    filter = new ClassAndMethodFilter(Arrays.asList(new ClassAndMethod("class1", "")));
    Assert.assertTrue(filter.matchClass("class1"));

    filter = new ClassAndMethodFilter(Arrays.asList(new ClassAndMethod("", "method1")));
    Assert.assertTrue(filter.matchClass("class1"));

    filter = new ClassAndMethodFilter(Arrays.asList(new ClassAndMethod("class2", "method1")));
    Assert.assertFalse(filter.matchClass("class1"));
    Assert.assertTrue(filter.matchClass("class2"));

    filter =
        new ClassAndMethodFilter(
            Arrays.asList(
                new ClassAndMethod("class2", "method1"), new ClassAndMethod("class1", "method1")));
    Assert.assertTrue(filter.matchClass("class1"));
    Assert.assertTrue(filter.matchClass("class2"));

    filter =
        new ClassAndMethodFilter(
            Arrays.asList(
                new ClassAndMethod("class2", "method1"),
                new ClassAndMethod("class1", "method1"),
                new ClassAndMethod("class3", "*")));
    Assert.assertTrue(filter.matchClass("class1xx"));
    Assert.assertTrue(filter.matchClass("class2xx"));
    Assert.assertTrue(filter.matchClass("class3xx"));
  }

  @Test
  public void matchMethod() {
    ClassAndMethodFilter filter = new ClassAndMethodFilter(null);
    Assert.assertFalse(filter.matchMethod("class1", "method1"));

    filter = new ClassAndMethodFilter(Arrays.asList(new ClassAndMethod("class1", "")));
    Assert.assertFalse(filter.matchMethod("class1", "method1"));

    filter = new ClassAndMethodFilter(Arrays.asList(new ClassAndMethod("", "method1")));
    Assert.assertTrue(filter.matchMethod("class1", "method1"));

    filter = new ClassAndMethodFilter(Arrays.asList(new ClassAndMethod("class2", "method1")));
    Assert.assertFalse(filter.matchMethod("class1", "method1"));

    filter =
        new ClassAndMethodFilter(
            Arrays.asList(
                new ClassAndMethod("", "method1"), new ClassAndMethod("class1", "method1")));
    Assert.assertTrue(filter.matchMethod("class1", "method1"));
  }

  @Test
  public void matchMethod_wildcard() {
    ClassAndMethodFilter filter =
        new ClassAndMethodFilter(Arrays.asList(new ClassAndMethod("class1", "")));
    Assert.assertFalse(filter.matchMethod("class1", "method1"));

    filter =
        new ClassAndMethodFilter(
            Arrays.asList(new ClassAndMethod("class1", ""), new ClassAndMethod("class1", "*")));
    Assert.assertTrue(filter.matchMethod("class1", "method1"));
  }

  @Test
  public void matchMethod_prefix() {
    ClassAndMethodFilter filter =
        new ClassAndMethodFilter(
            Arrays.asList(
                new ClassAndMethod("package11.class1", "method1"),
                new ClassAndMethod("package22", "method2")));
    Assert.assertTrue(filter.matchMethod("package22.class2", "method2"));
    Assert.assertFalse(filter.matchMethod("package2", "method2"));

    filter =
        new ClassAndMethodFilter(
            Arrays.asList(
                new ClassAndMethod("package11.class1", "method1"),
                new ClassAndMethod("package22", "method2"),
                new ClassAndMethod("package33", "*")));
    Assert.assertTrue(filter.matchMethod("package22.class2", "method2"));
    Assert.assertFalse(filter.matchMethod("package2", "method2"));
    Assert.assertTrue(filter.matchMethod("package33.xx.yy", "method3"));
  }
}
