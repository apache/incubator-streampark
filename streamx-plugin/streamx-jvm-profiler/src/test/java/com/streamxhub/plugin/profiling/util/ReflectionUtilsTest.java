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

import com.streamxhub.streamx.plugin.profiling.util.ReflectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

public class ReflectionUtilsTest {
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

  @Test
  public void executeStaticMethods()
      throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
          InvocationTargetException {
    Object result =
        ReflectionUtils.executeStaticMethods(
            "com.streamxhub.streamx.plugin.profiling.util.ReflectionUtilsTest$ClassB", "getClassA.method1");
    Assert.assertEquals("hello", result);
  }
}
