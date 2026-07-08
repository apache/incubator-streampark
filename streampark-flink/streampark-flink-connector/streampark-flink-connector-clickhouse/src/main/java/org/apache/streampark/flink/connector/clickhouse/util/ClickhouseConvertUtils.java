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

package org.apache.streampark.flink.connector.clickhouse.util;

import java.lang.reflect.Field;

/** ClickHouse value conversion utilities. */
public final class ClickhouseConvertUtils {

    private ClickhouseConvertUtils() {}

    public static <T> String convert(T value) {
        StringBuilder buffer = new StringBuilder("(");
        Field[] fields = value.getClass().getDeclaredFields();
        for (Field f : fields) {
            f.setAccessible(true);
            try {
                Object v = f.get(value);
                if (f.getType() == String.class) {
                    buffer.append("\"").append(v).append("\",");
                } else {
                    buffer.append(v).append(",");
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        String result = buffer.toString();
        return result.replaceFirst(",$", ")");
    }
}
