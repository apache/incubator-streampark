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
        if (classType == String.class) {
            return (T) v;
        } else if (classType == byte.class || classType == Byte.class) {
            return (T) (classType == byte.class ? (Object) Byte.parseByte(v) : Byte.valueOf(v));
        } else if (classType == int.class || classType == Integer.class) {
            return (T) (classType == int.class ? (Object) Integer.parseInt(v) : Integer.valueOf(v));
        } else if (classType == long.class || classType == Long.class) {
            return (T) (classType == long.class ? (Object) Long.parseLong(v) : Long.valueOf(v));
        } else if (classType == float.class || classType == Float.class) {
            return (T) (classType == float.class ? (Object) Float.parseFloat(v) : Float.valueOf(v));
        } else if (classType == double.class || classType == Double.class) {
            return (T) (classType == double.class ? (Object) Double.parseDouble(v) : Double.valueOf(v));
        } else if (classType == short.class || classType == Short.class) {
            return (T) (classType == short.class ? (Object) Short.parseShort(v) : Short.valueOf(v));
        } else if (classType == boolean.class || classType == Boolean.class) {
            return (T) (classType == boolean.class ? (Object) Boolean.parseBoolean(v) : Boolean.valueOf(v));
        } else {
            throw new IllegalArgumentException("Unsupported type: " + classType);
        }
    }
}
