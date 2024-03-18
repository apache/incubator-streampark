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

package org.apache.streampark.console.base.util;

import org.apache.streampark.console.base.exception.AbstractApiException;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/** Determine whether to throw an exception based on the check condition */
public class PremisesUtils {

  /**
   * This exceptionClass is thrown when we want to check that an object is Null and call this method
   *
   * @param object Conditions that need to be checked
   * @param errorMessage Error messages can be customized and will be printed on the console or
   *     written to a log file
   * @param exceptionClass The actual exception class is currently unified as a custom exception
   *     class.
   * @param <T> Generic parameters
   */
  public static <T extends AbstractApiException> void throwIfNull(
      Object object, String errorMessage, Class<T> exceptionClass) {
    if (Objects.isNull(object)) {
      try {
        throw exceptionClass.getDeclaredConstructor(String.class).newInstance(errorMessage);
      } catch (InvocationTargetException
          | InstantiationException
          | IllegalAccessException
          | NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * This exceptionClass is thrown when we want to check that an expression is false and call this
   * method
   *
   * @param expression Checks whether the value of an expression is false
   * @param errorMessage Error messages can be customized and will be printed on the console or
   *     written to a log file
   * @param exceptionClass The actual exception class is currently unified as a custom exception
   *     class.
   * @param <T> Generic parameters
   */
  public static <T extends AbstractApiException> void throwIfFalse(
      boolean expression, String errorMessage, Class<T> exceptionClass) {
    if (!expression) {
      try {
        throw exceptionClass.getDeclaredConstructor(String.class).newInstance(errorMessage);
      } catch (InvocationTargetException
          | InstantiationException
          | IllegalAccessException
          | NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * This exceptionClass is thrown when we want to check that an expression is true and call this
   * method
   *
   * @param expression Checks whether the value of an expression is true
   * @param errorMessage Error messages can be customized and will be printed on the console or
   *     written to a log file
   * @param exceptionClass The actual exception class is currently unified as a custom exception
   *     class.
   * @param <T> Generic parameters
   */
  public static <T extends AbstractApiException> void throwIfTrue(
      boolean expression, String errorMessage, Class<T> exceptionClass) {
    if (expression) {
      try {
        throw exceptionClass.getDeclaredConstructor(String.class).newInstance(errorMessage);
      } catch (InvocationTargetException
          | InstantiationException
          | IllegalAccessException
          | NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
