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

package org.apache.streampark.common.util

import java.util


object AssertUtils {

  /**
   * Assert a boolean expression, throwing <code>IllegalArgumentException</code> if the test result
   * is <code>false</code>.
   *
   * <pre class="code">AssertUtil.isTrue(i &gt; 0, "The value must be greater than zero");</pre>
   *
   * @param expression a boolean expression
   * @param message    the exception message to use if the assertion fails
   * @throws IllegalArgumentException if expression is <code>false</code>
   */
  def isTrue(expression: Boolean, message: String): Unit = {
    if (!expression) {
      throw new IllegalArgumentException(message)
    }
  }

  /**
   * Assert a boolean expression, throwing <code>IllegalArgumentException</code> if the test result
   * is <code>false</code>.
   *
   * <pre class="code">AssertUtil.isTrue(i &gt; 0);</pre>
   *
   * @param expression a boolean expression
   * @throws IllegalArgumentException if expression is <code>false</code>
   */
  def isTrue(expression: Boolean): Unit = {
    isTrue(expression, "[Assertion failed] - this expression must be true")
  }

  /**
   * Assert a boolean expression, throwing <code>IllegalArgumentException</code> if the test result
   * is <code>true</code>.
   *
   * <pre class="code">AssertUtil.trueException(i &gt; 0, "The value must be greater than zero");
   * </pre>
   *
   * @param expression a boolean expression
   * @throws IllegalArgumentException if expression is <code>false</code>
   */
  def trueException(expression: Boolean): Unit = {
    trueException(expression, "[Assertion failed] - when expression is true,throw exception")
  }

  def trueException(expression: Boolean, message: String): Unit = {
    if (expression) {
      throw new IllegalArgumentException(message)
    }
  }

  /**
   * Assert a boolean expression, throwing <code>IllegalArgumentException</code> if the test result
   * is <code>false</code>.
   *
   * <pre class="code">AssertUtil.falseException(i &gt; 0, "The value must be greater than zero");
   * </pre>
   *
   * @param expression a boolean expression
   * @throws IllegalArgumentException if expression is <code>false</code>
   */
  def falseException(expression: Boolean): Unit = {
    falseException(expression, "[Assertion failed] - when expression is false,throw exception")
  }

  /**
   * Assert a boolean expression, throwing <code>IllegalArgumentException</code> if the test result
   * is <code>false</code>.
   *
   * <pre class="code">AssertUtil.falseException(i &gt; 0, "The value must be greater than zero");
   * </pre>
   *
   * @param expression a boolean expression
   * @param message    the exception message to use if the assertion fails
   * @throws IllegalArgumentException if expression is <code>false</code>
   */
  def falseException(expression: Boolean, message: String): Unit = {
    if (!expression) {
      throw new IllegalArgumentException(message)
    }
  }

  /**
   * Assert that an object is <code>null</code> .
   *
   * <pre class="code">AssertUtil.isNull(value, "The value must be null");</pre>
   *
   * @param object  the object to check
   * @param message the exception message to use if the assertion fails
   * @throws IllegalArgumentException if the object is not <code>null</code>
   */
  def isNull(obj: Any, message: String): Unit = {
    if (obj != null) {
      throw new IllegalArgumentException(message)
    }
  }

  /**
   * Assert that an object is <code>null</code> .
   *
   * <pre class="code">AssertUtil.isNull(value);</pre>
   *
   * @param obj the object to check
   * @throws IllegalArgumentException if the object is not <code>null</code>
   */
  def isNull(obj: Any): Unit = {
    isNull(obj, "[Assertion failed] - the object argument must be null")
  }

  /**
   * Assert that an object is not <code>null</code> .
   *
   * <pre class="code">AssertUtil.notNull(clazz, "The class must not be null");</pre>
   *
   * @param object  the object to check
   * @param message the exception message to use if the assertion fails
   * @throws IllegalArgumentException if the object is <code>null</code>
   */
  def notNull(obj: Any, message: String): Unit = {
    if (obj == null) {
      throw new IllegalArgumentException(message)
    }
  }

  def notNull(obj: Any*): Unit = {
    notNull(obj, "[Assertion failed] - this argument is required; it must not be null")
    for (i <- 0 until obj.length) {
      notNull(obj(i), "[Assertion failed] - this argument index of " + i + " is required; it must not be null")
    }
  }

  /**
   * Assert that an object is not <code>null</code> .
   *
   * <pre class="code">AssertUtil.notNull(clazz);</pre>
   *
   * @param object the object to check
   * @throws IllegalArgumentException if the object is <code>null</code>
   */
  def notNull(obj: Any): Unit = {
    notNull(obj, "[Assertion failed] - this argument is required; it must not be null")
  }

  /**
   * Assert that the given String is not empty; that is, it must not be <code>null</code> and not
   * the empty String.
   *
   * <pre class="code">AssertUtil.hasLength(name, "Name must not be empty");</pre>
   *
   * @param text    the String to check
   * @param message the exception message to use if the assertion fails
   * @see org.springframework.util.StringUtils#hasLength
   */
  def hasLength(text: String, message: String): Unit = {
    if (Utils.isEmpty(text)) {
      throw new IllegalArgumentException(message)
    }
  }

  /**
   * Assert that the given String is not empty; that is, it must not be <code>null</code> and not
   * the empty String.
   *
   * <pre class="code">AssertUtil.hasLength(name);</pre>
   *
   * @param text the String to check
   */
  def hasLength(text: String): Unit = {
    hasLength(text, "[Assertion failed] - this String argument must have length; it must not be null or empty")
  }

  /**
   * Assert that the given String has valid text content; that is, it must not be <code>null</code>
   * and must contain at least one non-whitespace character.
   *
   * <pre class="code">AssertUtil.hasText(name, "'name' must not be empty");</pre>
   *
   * @param text    the String to check
   * @param message the exception message to use if the assertion fails
   */
  def hasText(text: String, message: String): Unit = {
    if (Utils.isEmpty(text)) {
      throw new IllegalArgumentException(message)
    }
  }

  /**
   * Assert that the given String has valid text content; that is, it must not be <code>null</code>
   * and must contain at least one non-whitespace character.
   *
   * <pre class="code">AssertUtil.hasText(name, "'name' must not be empty");</pre>
   *
   * @param text the String to check
   */
  def hasText(text: String): Unit = {
    hasText(text, "[Assertion failed] - this String argument must have text; it must not be null, empty, or blank")
  }

  /**
   * Assert that the given text does not contain the given substring.
   *
   * <pre class="code">AssertUtil.doesNotContain(name, "rod", "Name must not contain 'rod'");</pre>
   *
   * @param textToSearch the text to search
   * @param substring    the substring to find within the text
   * @param message      the exception message to use if the assertion fails
   */
  def doesNotContain(textToSearch: String, substring: String, message: String): Unit = {
    if (Utils.notEmpty(textToSearch) && Utils.notEmpty(substring) && textToSearch.indexOf(substring) != -1) {
      throw new IllegalArgumentException(message)
    }
  }

  /**
   * Assert that the given text does not contain the given substring.
   *
   * <pre class="code">AssertUtil.doesNotContain(name, "rod");</pre>
   *
   * @param textToSearch the text to search
   * @param substring    the substring to find within the text
   */
  def doesNotContain(textToSearch: String, substring: String): Unit = {
    doesNotContain(textToSearch, substring, "[Assertion failed] - this String argument must not contain the substring [" + substring + "]")
  }

  /**
   * Assert that an array has elements; that is, it must not be <code>null</code> and must have at
   * least one element.
   *
   * <pre class="code">AssertUtil.notEmpty(array, "The array must have elements");</pre>
   *
   * @param array   the array to check
   * @param message the exception message to use if the assertion fails
   * @throws IllegalArgumentException if the object array is <code>null</code> or has no elements
   */
  def notEmpty(array: Array[AnyRef], message: String): Unit = {
    if (Utils.isEmpty(array)) {
      throw new IllegalArgumentException(message)
    }
  }

  /**
   * Assert that an array has elements; that is, it must not be <code>null</code> and must have at
   * least one element.
   *
   * <pre class="code">AssertUtil.notEmpty(array);</pre>
   *
   * @param array the array to check
   * @throws IllegalArgumentException if the object array is <code>null</code> or has no elements
   */
  def notEmpty(array: Array[AnyRef]): Unit = {
    notEmpty(array, "[Assertion failed] - this array must not be empty: it must contain at least 1 element")
  }

  /**
   * Assert that an array has no null elements. Note: Does not complain if the array is empty!
   *
   * <pre class="code">AssertUtil.noNullElements(array, "The array must have non-null elements");
   * </pre>
   *
   * @param array   the array to check
   * @param message the exception message to use if the assertion fails
   * @throws IllegalArgumentException if the object array contains a <code>null</code> element
   */
  def noNullElements(array: Array[AnyRef], message: String): Unit = {
    if (array != null) for (i <- array.indices) {
      if (array(i) == null) {
        throw new IllegalArgumentException(message)
      }
    }
  }

  /**
   * Assert that an array has no null elements. Note: Does not complain if the array is empty!
   *
   * <pre class="code">AssertUtil.noNullElements(array);</pre>
   *
   * @param array the array to check
   * @throws IllegalArgumentException if the object array contains a <code>null</code> element
   */
  def noNullElements(array: Array[AnyRef]): Unit = {
    noNullElements(array, "[Assertion failed] - this array must not contain any null elements")
  }

  /**
   * Assert that a collection has elements; that is, it must not be <code>null</code> and must have
   * at least one element.
   *
   * <pre class="code">AssertUtil.notEmpty(collection, "Collection must have elements");</pre>
   *
   * @param collection the collection to check
   * @param message    the exception message to use if the assertion fails
   * @throws IllegalArgumentException if the collection is <code>null</code> or has no elements
   */
  def notEmpty(collection: util.Collection[_], message: String): Unit = {
    if (Utils.isEmpty(collection)) {
      throw new IllegalArgumentException(message)
    }
  }

  /**
   * Assert that a collection has elements; that is, it must not be <code>null</code> and must have
   * at least one element.
   *
   * <pre class="code">AssertUtil.notEmpty(collection, "Collection must have elements");</pre>
   *
   * @param collection the collection to check
   * @throws IllegalArgumentException if the collection is <code>null</code> or has no elements
   */
  def notEmpty(collection: util.Collection[_]): Unit = {
    notEmpty(collection, "[Assertion failed] - this collection must not be empty: it must contain at least 1 element")
  }

  /**
   * Assert that a Map has entries; that is, it must not be <code>null</code> and must have at least
   * one entry.
   *
   * <pre class="code">AssertUtil.notEmpty(map, "Map must have entries");</pre>
   *
   * @param map     the map to check
   * @param message the exception message to use if the assertion fails
   * @throws IllegalArgumentException if the map is <code>null</code> or has no entries
   */
  def notEmpty(map: util.Map[_, _], message: String): Unit = {
    if (Utils.isEmpty(map)) {
      throw new IllegalArgumentException(message)
    }
  }

  /**
   * Assert that a Map has entries; that is, it must not be <code>null</code> and must have at least
   * one entry.
   *
   * <pre class="code">AssertUtil.notEmpty(map);</pre>
   *
   * @param map the map to check
   * @throws IllegalArgumentException if the map is <code>null</code> or has no entries
   */
  def notEmpty(map: util.Map[_, _]): Unit = {
    notEmpty(map, "[Assertion failed] - this map must not be empty; it must contain at least one entry")
  }

  /**
   * Assert that <code>superType.isAssignableFrom(subType)</code> is <code>true</code>.
   *
   * <pre class="code">AssertUtil.isAssignable(Number.class, myClass);</pre>
   *
   * @param superType the super type to check
   * @param subType   the sub type to check
   * @throws IllegalArgumentException if the classes are not assignable
   */
  def isAssignable(superType: Class[_], subType: Class[_]): Unit = {
    isAssignable(superType, subType, "")
  }

  /**
   * Assert that <code>superType.isAssignableFrom(subType)</code> is <code>true</code>.
   *
   * <pre class="code">AssertUtil.isAssignable(Number.class, myClass);</pre>
   *
   * @param superType the super type to check against
   * @param subType   the sub type to check
   * @param message   a message which will be prepended to the message produced by the function
   *                  itself, and which may be used to provide context. It should normally end in a ": " or ". "
   *                  so that the function generate message looks ok when prepended to it.
   * @throws IllegalArgumentException if the classes are not assignable
   */
  def isAssignable(superType: Class[_], subType: Class[_], message: String): Unit = {
    notNull(superType, "Type to check against must not be null")
    if (subType == null || !superType.isAssignableFrom(subType)) {
      throw new IllegalArgumentException(message + subType + " is not assignable to " + superType)
    }
  }

  /**
   * Assert a boolean expression, throwing <code>IllegalStateException</code> if the test result is
   * <code>false</code>. Call isTrue if you wish to throw IllegalArgumentException on an assertion
   * failure.
   *
   * <pre class="code">
   * AssertUtil.state(id == null, "The id property must not already be initialized");</pre>
   *
   * @param expression a boolean expression
   * @param message    the exception message to use if the assertion fails
   * @throws IllegalStateException if expression is <code>false</code>
   */
  def state(expression: Boolean, message: String): Unit = {
    if (!expression) {
      throw new IllegalStateException(message)
    }
  }

  /**
   * Assert a boolean expression, throwing {@link IllegalStateException} if the test result is
   * <code>false</code>.
   *
   * <p>Call {@link #isTrue ( boolean )} if you wish to throw {@link IllegalArgumentException} on an
   * assertion failure.
   *
   * <pre class="code">AssertUtil.state(id == null);</pre>
   *
   * @param expression a boolean expression
   * @throws IllegalStateException if the supplied expression is <code>false</code>
   */
  def state(expression: Boolean): Unit = {
    state(expression, "[Assertion failed] - this state invariant must be true")
  }

  /**
   * Ensures that an object reference passed as a parameter to the calling method is not null.
   *
   * @param reference an object reference
   * @return the non-null reference that was validated
   * @throws NullPointerException if {@code reference} is null
   */
  def checkNotNull[T](reference: T): T = {
    if (reference == null) {
      throw new NullPointerException
    }
    reference
  }

  /**
   * Ensures that an object reference passed as a parameter to the calling method is not null.
   *
   * @param reference    an object reference
   * @param errorMessage the exception message to use if the check fails; will be converted to a
   *                     string using {@link String# valueOf ( Object )}
   * @return the non-null reference that was validated
   * @throws NullPointerException if {@code reference} is null
   */
  def checkNotNull[T](reference: T, errorMessage: Any): T = {
    if (reference == null) {
      throw new NullPointerException(String.valueOf(errorMessage))
    }
    reference
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   *
   * @param expression a boolean expression
   * @throws IllegalArgumentException if {@code expression} is false
   */
  def checkArgument(expression: Boolean): Unit = {
    if (!expression) {
      throw new IllegalArgumentException
    }
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   *
   * @param expression   a boolean expression
   * @param errorMessage the exception message to use if the check fails; will be converted to a
   *                     string using {@link String# valueOf ( Object )}
   * @throws IllegalArgumentException if {@code expression} is false
   */
  def checkArgument(expression: Boolean, errorMessage: Any): Unit = {
    if (!expression) {
      throw new IllegalArgumentException(String.valueOf(errorMessage))
    }
  }

}
