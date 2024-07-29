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

import java.util.Objects;

public final class ObjectUtils {

    private ObjectUtils() {
    }

    public static boolean trimEquals(Object o1, Object o2) {
        boolean equals = Objects.deepEquals(o1, o2);
        if (!equals && o1 instanceof String && o2 instanceof String) {
            return o1.toString().trim().equals(o2.toString().trim());
        }
        return equals;
    }

    public static boolean trimNoEquals(Object o1, Object o2) {
        return !trimEquals(o1, o2);
    }
}
