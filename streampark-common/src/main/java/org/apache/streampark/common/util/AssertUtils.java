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

package org.apache.streampark.common.util;

import javax.annotation.Nullable;

import java.util.Collection;

/** Assertion utilities. @since 2.2.0 */
public final class AssertUtils {

    private AssertUtils() {
    }

    public static void required(boolean condition) {
        if (!condition)
            throw new IllegalArgumentException();
    }

    public static void required(boolean condition, @Nullable String message) {
        if (!condition)
            throw new IllegalArgumentException(message);
    }

    public static void state(boolean condition) {
        if (!condition)
            throw new IllegalStateException();
    }

    public static void state(boolean condition, @Nullable String message) {
        if (!condition)
            throw new IllegalStateException(message);
    }

    public static <T> T notNull(@Nullable T reference) {
        if (reference == null)
            throw new NullPointerException();
        return reference;
    }

    public static <T> T notNull(@Nullable T reference, @Nullable String message) {
        if (reference == null)
            throw new NullPointerException(message);
        return reference;
    }

    public static void notEmpty(@Nullable Object reference) {
        if (Utils.isEmpty(reference))
            throw new IllegalArgumentException();
    }

    public static void notEmpty(@Nullable Object reference, String message) {
        if (Utils.isEmpty(reference))
            throw new IllegalArgumentException(message);
    }

    public static void noNullElements(@Nullable Object[] array, String message) {
        if (array != null) {
            for (Object element : array) {
                if (element == null)
                    throw new IllegalArgumentException(message);
            }
        }
    }

    public static void noNullElements(@Nullable Collection<?> collection, String message) {
        if (collection != null) {
            for (Object element : collection) {
                if (element == null)
                    throw new IllegalArgumentException(message);
            }
        }
    }

    public static void hasLength(@Nullable String text) {
        if (!getHasLength(text))
            throw new IllegalArgumentException();
    }

    public static void hasLength(@Nullable String text, String message) {
        if (!getHasLength(text))
            throw new IllegalArgumentException(message);
    }

    public static void hasText(@Nullable String text) {
        if (!getHasText(text))
            throw new IllegalArgumentException();
    }

    public static void hasText(@Nullable String text, String message) {
        if (!getHasText(text))
            throw new IllegalArgumentException(message);
    }

    private static boolean getHasLength(@Nullable String str) {
        return str != null && !str.isEmpty();
    }

    private static boolean getHasText(@Nullable String str) {
        return str != null && !str.isEmpty() && containsText(str);
    }

    private static boolean containsText(CharSequence str) {
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i)))
                return true;
        }
        return false;
    }
}
