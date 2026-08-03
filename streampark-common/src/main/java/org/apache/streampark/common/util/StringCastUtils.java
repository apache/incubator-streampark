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

    public static <T> T cast(String v, Class<T> classType) {
        Object result;
        if (classType == String.class) {
            result = v;
        } else if (classType == Byte.class || classType == byte.class) {
            result = Byte.parseByte(v);
        } else if (classType == Integer.class || classType == int.class) {
            result = Integer.parseInt(v);
        } else if (classType == Long.class || classType == long.class) {
            result = Long.parseLong(v);
        } else if (classType == Float.class || classType == float.class) {
            result = Float.parseFloat(v);
        } else if (classType == Double.class || classType == double.class) {
            result = Double.parseDouble(v);
        } else if (classType == Short.class || classType == short.class) {
            result = Short.parseShort(v);
        } else if (classType == Boolean.class || classType == boolean.class) {
            result = Boolean.parseBoolean(v);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + classType);
        }
        return classType.cast(result);
    }
}
