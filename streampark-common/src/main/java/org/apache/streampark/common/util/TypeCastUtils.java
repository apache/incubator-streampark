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

public final class TypeCastUtils {

    private TypeCastUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(String value, Class<?> classType) {
        if (classType == String.class)
            return (T) value;
        if (classType == Integer.class || classType == int.class)
            return (T) Integer.valueOf(value);
        if (classType == Long.class || classType == long.class)
            return (T) Long.valueOf(value);
        if (classType == Boolean.class || classType == boolean.class)
            return (T) Boolean.valueOf(value);
        if (classType == Double.class || classType == double.class)
            return (T) Double.valueOf(value);
        if (classType == Float.class || classType == float.class)
            return (T) Float.valueOf(value);
        if (classType == Short.class || classType == short.class)
            return (T) Short.valueOf(value);
        if (classType == Byte.class || classType == byte.class)
            return (T) Byte.valueOf(value);
        throw new IllegalArgumentException("Unsupported type: " + classType);
    }
}
