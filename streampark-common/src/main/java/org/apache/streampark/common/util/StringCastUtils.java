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

/** String to primitive/boxed type conversion utilities. */
public final class StringCastUtils {

    private StringCastUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(String v, Class<?> classType) {
        return (T) classType.cast(parseValue(v, classType));
    }

    private static Object parseValue(String v, Class<?> classType) {
        if (classType == String.class) {
            return v;
        }
        if (classType == Byte.class || classType == byte.class) {
            return Byte.parseByte(v);
        }
        if (classType == Integer.class || classType == int.class) {
            return Integer.parseInt(v);
        }
        if (classType == Long.class || classType == long.class) {
            return Long.parseLong(v);
        }
        if (classType == Float.class || classType == float.class) {
            return Float.parseFloat(v);
        }
        if (classType == Double.class || classType == double.class) {
            return Double.parseDouble(v);
        }
        if (classType == Short.class || classType == short.class) {
            return Short.parseShort(v);
        }
        if (classType == Boolean.class || classType == boolean.class) {
            return Boolean.parseBoolean(v);
        }
        throw new IllegalArgumentException("Unsupported type: " + classType);
    }
}
